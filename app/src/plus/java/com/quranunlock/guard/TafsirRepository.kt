package com.applicreation0.quransafeguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.zip.GZIPInputStream

internal object TafsirRepository {
    private val JALALAYN_ASSET_PARTS = (0..3).map { index ->
        "tafsir/al_jalalayn_en.sqlite.gz.part%02d".format(index)
    }
    private const val JALALAYN_DATABASE_NAME = "al_jalalayn_en.sqlite"
    private const val JALALAYN_EXPECTED_ENTRIES = 6_236
    private const val JALALAYN_EXPECTED_SHA256 =
        "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
    private const val V2_MANIFEST_ASSET = "tafsir/tafsir_v2_manifest.json"

    suspend fun load(
        context: Context,
        verse: VerseRef,
        editionId: TafsirEditionId
    ): TafsirEntry? = withContext(Dispatchers.IO) {
        if (!editionId.covers(verse)) return@withContext null
        when (editionId) {
            TafsirEditionId.JALALAYN -> loadJalalayn(context.applicationContext, verse)
            TafsirEditionId.QURTUBI,
            TafsirEditionId.QUSHAYRI -> loadV2(
                context.applicationContext,
                verse,
                editionId
            )
        }
    }

    private fun loadJalalayn(context: Context, verse: VerseRef): TafsirEntry? {
        val databaseFile = materializeDatabase(
            context = context,
            databaseName = JALALAYN_DATABASE_NAME,
            expectedSha256 = JALALAYN_EXPECTED_SHA256,
            assetParts = JALALAYN_ASSET_PARTS
        ) ?: return null
        val database = openReadOnly(databaseFile)
        try {
            val count = database.rawQuery(
                "SELECT value FROM source_metadata WHERE key = 'verse_count'",
                emptyArray()
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() else null
            }
            if (count != JALALAYN_EXPECTED_ENTRIES) return null

            val commentary = database.rawQuery(
                "SELECT body_json FROM verse_commentary " +
                    "WHERE surah = ? AND ayah = ?",
                arrayOf(verse.surah.toString(), verse.ayah.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: return null

            val commentaryRuns = parseRuns(commentary)
            if (commentaryRuns.isEmpty()) return null

            val notes = database.rawQuery(
                "SELECT label, body_json FROM verse_note " +
                    "WHERE surah = ? AND ayah = ? ORDER BY ordinal",
                arrayOf(verse.surah.toString(), verse.ayah.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            TafsirNote(
                                number = cursor.getInt(0),
                                runs = parseRuns(cursor.getString(1))
                            )
                        )
                    }
                }
            }
            if (notes.any { it.runs.isEmpty() }) return null
            return TafsirEntry(
                verse = verse,
                commentaryRuns = commentaryRuns,
                notes = notes,
                editionId = TafsirEditionId.JALALAYN
            )
        } finally {
            database.close()
        }
    }

    private fun loadV2(
        context: Context,
        verse: VerseRef,
        editionId: TafsirEditionId
    ): TafsirEntry? {
        val spec = loadV2Spec(context, editionId) ?: return null
        if (!spec.readyForDistribution ||
            spec.databaseSha256.isBlank() ||
            spec.assetParts.isEmpty()
        ) {
            return null
        }
        val databaseFile = materializeDatabase(
            context = context,
            databaseName = spec.databaseName,
            expectedSha256 = spec.databaseSha256,
            assetParts = spec.assetParts
        ) ?: return null
        val database = openReadOnly(databaseFile)
        try {
            val blocks = database.rawQuery(
                "SELECT e.entry_id, e.verse_start, e.verse_end, " +
                    "e.segment_no, e.body_json " +
                    "FROM entry_verse_map m " +
                    "JOIN tafsir_entry e ON e.entry_id = m.entry_id " +
                    "WHERE m.surah = ? AND m.ayah = ? " +
                    "ORDER BY m.ordinal, e.segment_no, e.entry_id",
                arrayOf(verse.surah.toString(), verse.ayah.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val runs = parseRuns(cursor.getString(4))
                        if (runs.isEmpty()) return null
                        add(
                            V2Block(
                                entryId = cursor.getLong(0),
                                verseStart = cursor.getInt(1),
                                verseEnd = cursor.getInt(2),
                                segmentNo = cursor.getInt(3),
                                runs = runs
                            )
                        )
                    }
                }
            }
            if (blocks.isEmpty()) return null

            val commentaryRuns = buildList {
                blocks.forEachIndexed { index, block ->
                    if (index > 0) add(TafsirRun(TafsirRunStyle.REGULAR, "\n\n"))
                    addAll(block.runs)
                }
            }
            val notes = buildList {
                blocks.forEach { block ->
                    addAll(loadV2Notes(database, block.entryId))
                }
            }
            if (notes.any { it.runs.isEmpty() }) return null

            return TafsirEntry(
                verse = verse,
                commentaryRuns = commentaryRuns,
                notes = notes,
                editionId = editionId,
                verseStart = blocks.minOf { it.verseStart },
                verseEnd = blocks.maxOf { it.verseEnd },
                segmentCount = blocks.size
            )
        } finally {
            database.close()
        }
    }

    private fun loadV2Notes(database: SQLiteDatabase, entryId: Long): List<TafsirNote> =
        database.rawQuery(
            "SELECT ordinal, label, body_json FROM entry_note " +
                "WHERE entry_id = ? ORDER BY ordinal",
            arrayOf(entryId.toString())
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val ordinal = cursor.getInt(0)
                    val label = cursor.getString(1)?.trim().orEmpty()
                    add(
                        TafsirNote(
                            number = label.toIntOrNull() ?: ordinal,
                            runs = parseRuns(cursor.getString(2))
                        )
                    )
                }
            }
        }

    private fun loadV2Spec(context: Context, editionId: TafsirEditionId): V2Spec? {
        val raw = runCatching {
            context.assets.open(V2_MANIFEST_ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }.getOrNull() ?: return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val editions = root.optJSONArray("editions") ?: return null
        for (index in 0 until editions.length()) {
            val obj = editions.optJSONObject(index) ?: continue
            if (obj.optString("edition_id") != editionId.stableId) continue
            val parts = obj.optJSONArray("asset_parts").toStringList()
            return V2Spec(
                databaseName = obj.optString("database_name").trim(),
                databaseSha256 = obj.optString("database_sha256").trim(),
                assetParts = parts,
                readyForDistribution = obj.optBoolean("ready_for_distribution", false)
            ).takeIf { it.databaseName.isNotBlank() }
        }
        return null
    }

    private fun openReadOnly(file: File): SQLiteDatabase =
        SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
        )

    @Synchronized
    private fun materializeDatabase(
        context: Context,
        databaseName: String,
        expectedSha256: String,
        assetParts: List<String>
    ): File? {
        if (databaseName.isBlank() || expectedSha256.isBlank() || assetParts.isEmpty()) {
            return null
        }
        val directory = File(context.noBackupFilesDir, "tafsir")
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, databaseName)
        if (destination.isFile && sha256(destination) == expectedSha256) {
            return destination
        }

        val temporary = File(directory, "$databaseName.tmp-${android.os.Process.myPid()}")
        return runCatching {
            openDatabaseArchive(context, assetParts).use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(sha256(temporary) == expectedSha256) {
                "Tafsir database integrity check failed"
            }
            if (destination.exists() && !destination.delete()) {
                error("Cannot replace the previous tafsir database")
            }
            check(temporary.renameTo(destination)) {
                "Cannot install the tafsir database atomically"
            }
            destination
        }.getOrNull().also {
            if (it == null) temporary.delete()
        }
    }

    private fun openDatabaseArchive(
        context: Context,
        assetParts: List<String>
    ): GZIPInputStream {
        val streams = ArrayList<InputStream>(assetParts.size)
        return try {
            assetParts.forEach { path -> streams += context.assets.open(path) }
            GZIPInputStream(SequenceInputStream(Collections.enumeration(streams)))
        } catch (error: Throwable) {
            streams.forEach { stream -> runCatching { stream.close() } }
            throw error
        }
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

    private fun parseRuns(json: String): List<TafsirRun> {
        val array = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                val style = when (value.getString("style")) {
                    "regular" -> TafsirRunStyle.REGULAR
                    "italic" -> TafsirRunStyle.ITALIC
                    "bold" -> TafsirRunStyle.BOLD
                    "bold_italic" -> TafsirRunStyle.BOLD_ITALIC
                    "note_ref" -> TafsirRunStyle.NOTE_REF
                    else -> null
                } ?: return emptyList()
                val text = value.getString("text")
                if (text.isNotEmpty()) add(TafsirRun(style, text))
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    private data class V2Spec(
        val databaseName: String,
        val databaseSha256: String,
        val assetParts: List<String>,
        val readyForDistribution: Boolean
    )

    private data class V2Block(
        val entryId: Long,
        val verseStart: Int,
        val verseEnd: Int,
        val segmentNo: Int,
        val runs: List<TafsirRun>
    )
}
