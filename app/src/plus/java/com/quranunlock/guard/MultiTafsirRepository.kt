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
        val expectedEntries: Int,
        val presentationRevision: String
    ) {
        val assetParts: List<String>
            get() = (0 until partCount).map { index ->
                "tafsir/$databaseName.gz.b64.part%02d".format(index)
            }
    }

    private data class LoadedV2Row(
        val id: Long,
        val segment: SourceBackedTafsirSegment
    )

    private val qushayri = CorpusSpec(
        edition = PrivateTafsirEdition.QUSHAYRI,
        databaseName = "qushayri_en.sqlite",
        partCount = 2,
        expectedEntries = 720,
        presentationRevision = "0106-qushayri-source-semantics-v1"
    )

    private val qurtubi = CorpusSpec(
        edition = PrivateTafsirEdition.QURTUBI,
        databaseName = "qurtubi_en.sqlite",
        partCount = 4,
        expectedEntries = 432,
        presentationRevision = "0106-qurtubi-hide-verse-labels-v1"
    )

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
                "SELECT id, verse_start, verse_end, segment_no, verse_translation, commentary " +
                    "FROM tafsir_entry WHERE surah = ? AND verse_start <= ? AND verse_end >= ? " +
                    "ORDER BY verse_start, verse_end, segment_no, id",
                arrayOf(verse.surah.toString(), verse.ayah.toString(), verse.ayah.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            LoadedV2Row(
                                id = cursor.getLong(0),
                                segment = SourceBackedTafsirSegment(
                                    verseStart = cursor.getInt(1),
                                    verseEnd = cursor.getInt(2),
                                    segment = cursor.getInt(3),
                                    translation = cursor.getString(4).trim(),
                                    commentary = cursor.getString(5).trim()
                                )
                            )
                        )
                    }
                }
            }
            if (rows.isEmpty()) return null

            val runs = if (spec.edition == PrivateTafsirEdition.QUSHAYRI) {
                renderQushayriRows(database, rows)
            } else {
                renderSourceBackedTafsirSegments(rows.map { it.segment })
            }
            if (runs.isEmpty()) return null
            return TafsirEntry(verse = verse, commentaryRuns = runs, notes = emptyList())
        } finally {
            database.close()
        }
    }

    private fun renderQushayriRows(
        database: SQLiteDatabase,
        rows: List<LoadedV2Row>
    ): List<TafsirRun> = buildList {
        rows.forEachIndexed { index, row ->
            if (index > 0) add(TafsirRun(TafsirRunStyle.REGULAR, "\n"))
            val segment = row.segment
            if (segment.translation.isNotBlank()) {
                add(TafsirRun(TafsirRunStyle.BOLD_ITALIC, segment.translation))
                if (segment.commentary.isNotBlank()) {
                    add(TafsirRun(TafsirRunStyle.REGULAR, "\n"))
                }
            }

            val semanticRuns = database.rawQuery(
                "SELECT style, text FROM tafsir_run WHERE entry_id = ? ORDER BY run_no",
                arrayOf(row.id.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val style = when (cursor.getString(0)) {
                            "REGULAR" -> TafsirRunStyle.REGULAR
                            "POETRY" -> TafsirRunStyle.POETRY
                            else -> error("Unsupported Qushayri semantic Tafsir run style")
                        }
                        add(TafsirRun(style, cursor.getString(1)))
                    }
                }
            }
            check(semanticRuns.isNotEmpty()) {
                "Qushayri semantic run table is empty for entry ${row.id}"
            }
            check(semanticRuns.joinToString(separator = "") { it.text } == segment.commentary) {
                "Qushayri semantic run text does not match verified commentary"
            }
            addAll(semanticRuns)
        }
    }

    private fun metadataValue(database: SQLiteDatabase, key: String): String? =
        database.rawQuery(
            "SELECT value FROM source_metadata WHERE key = ?",
            arrayOf(key)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun metadataMatches(database: SQLiteDatabase, spec: CorpusSpec): Boolean {
        if (database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                cursor.moveToFirst() && cursor.getString(0) == "ok"
            }.not()
        ) return false

        val rowCount = database.rawQuery(
            "SELECT COUNT(*) FROM tafsir_entry",
            null
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else -1 }

        val baseMatches = metadataValue(database, "schema_version") == "2" &&
            metadataValue(database, "edition_id") == spec.edition.storageValue &&
            metadataValue(database, "entry_count")?.toIntOrNull() == spec.expectedEntries &&
            metadataValue(database, "arabic_included") == "false" &&
            metadataValue(database, "presentation_revision") == spec.presentationRevision &&
            rowCount == spec.expectedEntries
        if (!baseMatches) return false

        if (spec.edition == PrivateTafsirEdition.QUSHAYRI) {
            if (metadataValue(database, "semantic_run_table") != "tafsir_run") return false
            if (metadataValue(database, "verified_note_call_count") != "928") return false
            if (metadataValue(database, "poetry_index_entry_count") != "121") return false
            if (metadataValue(database, "poetry_index_occurrence_count") != "126") return false
            if (metadataValue(database, "poetry_unique_line_count") != "542") return false
            val runTableExists = database.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='tafsir_run'",
                null
            ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }
            if (!runTableExists) return false
        }
        return true
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
