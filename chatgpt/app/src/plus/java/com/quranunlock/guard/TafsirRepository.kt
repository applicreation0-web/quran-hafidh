package com.applicreation0.quransafeguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.zip.GZIPInputStream

internal object TafsirRepository {
    private val ASSET_PARTS = (0..3).map { index ->
        "tafsir/al_jalalayn_en.sqlite.gz.part%02d".format(index)
    }
    private const val DATABASE_NAME = "al_jalalayn_en.sqlite"
    private const val EXPECTED_ENTRIES = 6_236
    private const val EXPECTED_SHA256 =
        "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"

    suspend fun load(context: Context, verse: VerseRef): TafsirEntry? =
        withContext(Dispatchers.IO) {
            val databaseFile = materializeDatabase(context.applicationContext)
                ?: return@withContext null
            val database = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
            try {
                val count = database.rawQuery(
                    "SELECT value FROM source_metadata WHERE key = 'verse_count'",
                    emptyArray()
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).toIntOrNull() else null
                }
                if (count != EXPECTED_ENTRIES) return@withContext null

                val commentary = database.rawQuery(
                    "SELECT body_json FROM verse_commentary " +
                        "WHERE surah = ? AND ayah = ?",
                    arrayOf(verse.surah.toString(), verse.ayah.toString())
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                } ?: return@withContext null

                val commentaryRuns = parseRuns(commentary)
                if (commentaryRuns.isEmpty()) return@withContext null

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
                if (notes.any { it.runs.isEmpty() }) return@withContext null
                TafsirEntry(verse, commentaryRuns, notes)
            } finally {
                database.close()
            }
        }

    @Synchronized
    private fun materializeDatabase(context: Context): File? {
        val directory = File(context.noBackupFilesDir, "tafsir")
        if (!directory.exists() && !directory.mkdirs()) return null
        val destination = File(directory, DATABASE_NAME)
        if (destination.isFile && sha256(destination) == EXPECTED_SHA256) {
            return destination
        }

        val temporary = File(directory, "$DATABASE_NAME.tmp-${android.os.Process.myPid()}")
        return runCatching {
            openDatabaseArchive(context).use { input ->
                FileOutputStream(temporary).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(sha256(temporary) == EXPECTED_SHA256) {
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

    private fun openDatabaseArchive(context: Context): GZIPInputStream {
        val streams = ArrayList<InputStream>(ASSET_PARTS.size)
        return try {
            ASSET_PARTS.forEach { path -> streams += context.assets.open(path) }
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
        val array = JSONArray(json)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                val style = when (value.getString("style")) {
                    "regular" -> TafsirRunStyle.REGULAR
                    "italic" -> TafsirRunStyle.ITALIC
                    "bold" -> TafsirRunStyle.BOLD
                    "bold_italic" -> TafsirRunStyle.BOLD_ITALIC
                    "technical_term" -> TafsirRunStyle.TECHNICAL_TERM
                    "transliteration" -> TafsirRunStyle.TRANSLITERATION
                    "poetry" -> TafsirRunStyle.POETRY
                    "note_ref" -> TafsirRunStyle.NOTE_REF
                    else -> null
                } ?: return emptyList()
                val text = JalalaynHonorificPresentation.normalize(value.getString("text"))
                if (text.isNotEmpty()) add(TafsirRun(style, text))
            }
        }
    }
}
