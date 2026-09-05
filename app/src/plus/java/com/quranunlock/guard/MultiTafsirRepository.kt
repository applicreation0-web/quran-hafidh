package com.applicreation0.quransafeguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

enum class PrivateTafsirEdition(
    val storageValue: String,
    val displayName: String
) {
    JALALAYN("jalalayn", "Jalalayn"),
    QURTUBI("qurtubi", "Qurtubi"),
    QUSHAYRI("qushayri", "Qushayri");

    companion object {
        fun fromStorage(value: String?): PrivateTafsirEdition =
            entries.firstOrNull { it.storageValue == value } ?: JALALAYN
    }
}

data class MultiTafsirRequestKey(
    val verse: VerseRef,
    val edition: PrivateTafsirEdition
)

internal data class MultiTafsirAvailability(
    val entries: Map<PrivateTafsirEdition, TafsirEntry>
) {
    val editions: List<PrivateTafsirEdition>
        get() = PrivateTafsirEdition.entries.filter(entries::containsKey)

    fun resolveEdition(preferred: PrivateTafsirEdition): PrivateTafsirEdition? =
        when {
            entries.containsKey(preferred) -> preferred
            entries.containsKey(PrivateTafsirEdition.JALALAYN) ->
                PrivateTafsirEdition.JALALAYN
            else -> editions.firstOrNull()
        }
}

internal object MultiTafsirRepository {
    private data class CorpusSpec(
        val edition: PrivateTafsirEdition,
        val databaseName: String,
        val partCount: Int,
        val expectedEntries: Int
    ) {
        val assetParts: List<String>
            get() = (0 until partCount).map { index ->
                "tafsir/$databaseName.gz.b64.part%02d".format(index)
            }
    }

    private val qushayri = CorpusSpec(
        edition = PrivateTafsirEdition.QUSHAYRI,
        databaseName = "qushayri_en.sqlite",
        partCount = 1,
        expectedEntries = 720
    )

    private val qurtubi = CorpusSpec(
        edition = PrivateTafsirEdition.QURTUBI,
        databaseName = "qurtubi_en.sqlite",
        partCount = 4,
        expectedEntries = 432
    )

    /**
     * Loads only real source-backed entries for the tapped verse. The returned
     * edition list is therefore the authority for the selector: an edition with
     * no matching row is not advertised to the reader.
     */
    suspend fun loadAvailable(
        context: Context,
        verse: VerseRef
    ): MultiTafsirAvailability {
        val available = linkedMapOf<PrivateTafsirEdition, TafsirEntry>()
        PrivateTafsirEdition.entries.forEach { edition ->
            load(context, verse, edition)?.let { entry ->
                available[edition] = entry
            }
        }
        return MultiTafsirAvailability(available)
    }

    suspend fun load(
        context: Context,
        verse: VerseRef,
        edition: PrivateTafsirEdition
    ): TafsirEntry? = withContext(Dispatchers.IO) {
        when (edition) {
            PrivateTafsirEdition.JALALAYN ->
                TafsirRepository.load(context.applicationContext, verse)
            PrivateTafsirEdition.QURTUBI ->
                loadV2(context.applicationContext, verse, qurtubi)
            PrivateTafsirEdition.QUSHAYRI ->
                loadV2(context.applicationContext, verse, qushayri)
        }
    }

    private fun loadV2(
        context: Context,
        verse: VerseRef,
        spec: CorpusSpec
    ): TafsirEntry? {
        val file = materialize(context, spec) ?: return null
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        try {
            if (!metadataMatches(database, spec)) return null
            val rows = database.rawQuery(
                "SELECT verse_start, verse_end, segment_no, verse_translation, commentary " +
                    "FROM tafsir_entry WHERE surah = ? AND verse_start <= ? AND verse_end >= ? " +
                    "ORDER BY verse_start, verse_end, segment_no, id",
                arrayOf(verse.surah.toString(), verse.ayah.toString(), verse.ayah.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            SourceBackedTafsirSegment(
                                verseStart = cursor.getInt(0),
                                verseEnd = cursor.getInt(1),
                                segment = cursor.getInt(2),
                                translation = cursor.getString(3).trim(),
                                commentary = cursor.getString(4).trim()
                            )
                        )
                    }
                }
            }
            if (rows.isEmpty()) return null

            val runs = renderSourceBackedTafsirSegments(rows)
            if (runs.isEmpty()) return null
            return TafsirEntry(verse = verse, commentaryRuns = runs, notes = emptyList())
        } finally {
            database.close()
        }
    }

    private fun metadataMatches(database: SQLiteDatabase, spec: CorpusSpec): Boolean {
        if (database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == "ok"
            }.not()
        ) return false

        fun value(key: String): String? = database.rawQuery(
            "SELECT value FROM source_metadata WHERE key = ?",
            arrayOf(key)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        val rowCount = database.rawQuery(
            "SELECT COUNT(*) FROM tafsir_entry",
            null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else -1 }

        return value("schema_version") == "2" &&
            value("edition_id") == spec.edition.storageValue &&
            value("entry_count")?.toIntOrNull() == spec.expectedEntries &&
            value("arabic_included") == "false" &&
            rowCount == spec.expectedEntries
    }

    private fun databaseFileMatches(file: File, spec: CorpusSpec): Boolean = runCatching {
        val database = SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )
        try {
            metadataMatches(database, spec)
        } finally {
            database.close()
        }
    }.getOrDefault(false)

    @Synchronized
    private fun materialize(context: Context, spec: CorpusSpec): File? {
        val directory = File(context.noBackupFilesDir, "tafsir")
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, spec.databaseName)
        if (destination.isFile && databaseFileMatches(destination, spec)) {
            return destination
        }

        val temporary = File(directory, "${spec.databaseName}.tmp-${android.os.Process.myPid()}")
        return runCatching {
            val encoded = buildString {
                spec.assetParts.forEach { path ->
                    context.assets.open(path).bufferedReader(Charsets.US_ASCII).use {
                        append(it.readText())
                    }
                }
            }
            val compressed = Base64.decode(encoded, Base64.DEFAULT)
            GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(databaseFileMatches(temporary, spec)) {
                "Tafsir database structural integrity check failed"
            }
            if (destination.exists() && !destination.delete()) {
                error("Cannot replace previous tafsir database")
            }
            check(temporary.renameTo(destination)) {
                "Cannot install tafsir database atomically"
            }
            destination
        }.getOrNull().also { if (it == null) temporary.delete() }
    }
}
