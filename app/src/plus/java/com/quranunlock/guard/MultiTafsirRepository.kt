package com.applicreation0.quransafeguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
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

internal object MultiTafsirRepository {
    private data class CorpusSpec(
        val edition: PrivateTafsirEdition,
        val databaseName: String,
        val partCount: Int,
        val expectedEntries: Int,
        val expectedSha256: String
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
        expectedEntries = 806,
        expectedSha256 = "4356e836e8e14818f6b4f5007eeac12454cb388e6aa59759a926bdc560569c5b"
    )

    private val qurtubi = CorpusSpec(
        edition = PrivateTafsirEdition.QURTUBI,
        databaseName = "qurtubi_en.sqlite",
        partCount = 4,
        expectedEntries = 432,
        expectedSha256 = "4f3e890085f8d991818d7b3fe9280d534adcf2242ac1694c9cc985aa842ea87e"
    )

    suspend fun load(
        context: Context,
        verse: VerseRef,
        edition: PrivateTafsirEdition
    ): TafsirEntry? = withContext(Dispatchers.IO) {
        when (edition) {
            PrivateTafsirEdition.JALALAYN -> TafsirRepository.load(context.applicationContext, verse)
            PrivateTafsirEdition.QURTUBI -> loadV2(context.applicationContext, verse, qurtubi)
            PrivateTafsirEdition.QUSHAYRI -> loadV2(context.applicationContext, verse, qushayri)
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
                            SourceRow(
                                start = cursor.getInt(0),
                                end = cursor.getInt(1),
                                segment = cursor.getInt(2),
                                translation = cursor.getString(3).trim(),
                                commentary = cursor.getString(4).trim()
                            )
                        )
                    }
                }
            }
            if (rows.isEmpty()) return null

            val runs = buildList {
                rows.forEachIndexed { index, row ->
                    if (index > 0) add(TafsirRun(TafsirRunStyle.REGULAR, "\n\n"))
                    if (row.start != row.end) {
                        add(
                            TafsirRun(
                                TafsirRunStyle.BOLD_ITALIC,
                                "Commentary on ${verse.surah}:${row.start}–${row.end}\n\n"
                            )
                        )
                    }
                    if (row.translation.isNotBlank()) {
                        add(TafsirRun(TafsirRunStyle.ITALIC, row.translation))
                        if (row.commentary.isNotBlank()) {
                            add(TafsirRun(TafsirRunStyle.REGULAR, "\n\n"))
                        }
                    }
                    if (row.commentary.isNotBlank()) {
                        add(TafsirRun(TafsirRunStyle.REGULAR, row.commentary))
                    }
                }
            }
            if (runs.isEmpty()) return null
            return TafsirEntry(verse = verse, commentaryRuns = runs, notes = emptyList())
        } finally {
            database.close()
        }
    }

    private data class SourceRow(
        val start: Int,
        val end: Int,
        val segment: Int,
        val translation: String,
        val commentary: String
    )

    private fun metadataMatches(database: SQLiteDatabase, spec: CorpusSpec): Boolean {
        fun value(key: String): String? = database.rawQuery(
            "SELECT value FROM source_metadata WHERE key = ?",
            arrayOf(key)
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        return value("schema_version") == "2" &&
            value("edition_id") == spec.edition.storageValue &&
            value("entry_count")?.toIntOrNull() == spec.expectedEntries &&
            value("arabic_included") == "false"
    }

    @Synchronized
    private fun materialize(context: Context, spec: CorpusSpec): File? {
        val directory = File(context.noBackupFilesDir, "tafsir")
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, spec.databaseName)
        if (destination.isFile && sha256(destination) == spec.expectedSha256) {
            return destination
        }
        val temporary = File(directory, "${spec.databaseName}.tmp-${android.os.Process.myPid()}")
        return runCatching {
            val encoded = buildString {
                spec.assetParts.forEach { path ->
                    context.assets.open(path).bufferedReader(Charsets.US_ASCII).use { append(it.readText()) }
                }
            }
            val compressed = Base64.decode(encoded, Base64.DEFAULT)
            GZIPInputStream(ByteArrayInputStream(compressed)).use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(sha256(temporary) == spec.expectedSha256) {
                "Tafsir database integrity check failed"
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

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
