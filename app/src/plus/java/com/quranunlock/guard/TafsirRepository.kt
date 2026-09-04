package com.applicreation0.quransafeguard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
    private const val V2_SCHEMA_VERSION = "tafsir-v2"
    private val APPROVED_RIGHTS_STATUSES = setOf(
        "licensed",
        "public_domain",
        "permission_documented"
    )
    private val SAFE_DATABASE_NAME = Regex("[A-Za-z0-9._-]+\\.sqlite")
    private val SAFE_SHA256 = Regex("[0-9a-f]{64}")

    suspend fun load(
        context: Context,
        verse: VerseRef,
        editionId: TafsirEditionId
    ): TafsirEntry? = withContext(Dispatchers.IO) {
        if (!editionId.covers(verse)) return@withContext null
        try {
            when (editionId) {
                TafsirEditionId.JALALAYN ->
                    loadJalalayn(context.applicationContext, verse)
                TafsirEditionId.QURTUBI,
                TafsirEditionId.QUSHAYRI -> loadV2(
                    context.applicationContext,
                    verse,
                    editionId
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A malformed/corrupt corpus must never crash the reader. Fail closed.
            null
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
        if (!spec.distributionReady) return null

        val databaseFile = materializeDatabase(
            context = context,
            databaseName = spec.databaseName,
            expectedSha256 = spec.databaseSha256,
            assetParts = spec.assetParts
        ) ?: return null
        val database = openReadOnly(databaseFile)
        try {
            if (!validateV2Metadata(database, editionId, spec)) return null

            val blocks = database.rawQuery(
                "SELECT e.entry_id, e.verse_start, e.verse_end, " +
                    "e.segment_no, e.body_json " +
                    "FROM entry_verse_map m " +
                    "JOIN tafsir_entry e ON e.entry_id = m.entry_id " +
                    "WHERE m.surah = ? AND m.ayah = ? " +
                    "ORDER BY e.segment_no, e.entry_id",
                arrayOf(verse.surah.toString(), verse.ayah.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val runs = parseRuns(cursor.getString(4))
                        if (runs.isEmpty()) return null
                        val start = cursor.getInt(1)
                        val end = cursor.getInt(2)
                        if (start < 1 || end < start || verse.ayah !in start..end) return null
                        add(
                            V2Block(
                                entryId = cursor.getLong(0),
                                verseStart = start,
                                verseEnd = end,
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
                segmentCount = blocks.size,
                sourceRanges = blocks.map { it.verseStart..it.verseEnd }.distinct()
            )
        } finally {
            database.close()
        }
    }

    private fun validateV2Metadata(
        database: SQLiteDatabase,
        editionId: TafsirEditionId,
        spec: V2Spec
    ): Boolean =
        metadataValue(database, "schema_version") == V2_SCHEMA_VERSION &&
            metadataValue(database, "edition_id") == editionId.stableId &&
            metadataValue(database, "rights_status") == spec.rightsStatus &&
            metadataValue(database, "source_audit_status") == "verified" &&
            metadataValue(database, "content_audit_status") == "verified"

    private fun metadataValue(database: SQLiteDatabase, key: String): String? =
        database.rawQuery(
            "SELECT value FROM source_metadata WHERE key = ?",
            arrayOf(key)
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
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
                            runs = parseRuns(cursor.getString(2)),
                            sourceLabel = label.takeIf(String::isNotBlank)
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
                readyForDistribution = obj.optBoolean("ready_for_distribution", false),
                schemaVersion = obj.optString("schema_version").trim(),
                rightsStatus = obj.optString("rights_status").trim(),
                sourceAuditStatus = obj.optString("source_audit_status").trim(),
                contentAuditStatus = obj.optString("content_audit_status").trim()
            ).takeIf { it.safe }
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
        if (!SAFE_DATABASE_NAME.matches(databaseName) ||
            !SAFE_SHA256.matches(expectedSha256) ||
            assetParts.isEmpty() ||
            assetParts.any { !safeAssetPath(it) }
        ) {
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
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
            destination
        }.getOrNull().also {
            if (it == null) temporary.delete()
        }
    }

    private fun safeAssetPath(path: String): Boolean =
        path.startsWith("tafsir/") &&
            !path.contains("..") &&
            path.endsWith(".gz.part00") ||
            (path.startsWith("tafsir/") &&
                !path.contains("..") &&
                Regex(".*\\.gz\\.part\\d{2}").matches(path))

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
        val readyForDistribution: Boolean,
        val schemaVersion: String,
        val rightsStatus: String,
        val sourceAuditStatus: String,
        val contentAuditStatus: String
    ) {
        val safe: Boolean
            get() =
                SAFE_DATABASE_NAME.matches(databaseName) &&
                    (databaseSha256.isBlank() || SAFE_SHA256.matches(databaseSha256)) &&
                    assetParts.distinct().size == assetParts.size &&
                    assetParts.all(::safeAssetPath)

        val distributionReady: Boolean
            get() =
                readyForDistribution &&
                    schemaVersion == V2_SCHEMA_VERSION &&
                    rightsStatus in APPROVED_RIGHTS_STATUSES &&
                    sourceAuditStatus == "verified" &&
                    contentAuditStatus == "verified" &&
                    SAFE_SHA256.matches(databaseSha256) &&
                    assetParts.isNotEmpty()
    }

    private data class V2Block(
        val entryId: Long,
        val verseStart: Int,
        val verseEnd: Int,
        val segmentNo: Int,
        val runs: List<TafsirRun>
    )
}
