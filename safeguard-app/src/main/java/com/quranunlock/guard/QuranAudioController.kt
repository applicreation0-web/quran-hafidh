package com.applicreation0.quransafeguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * Al-Husary Muʿallim playback backed only by files downloaded by this app.
 * Nothing is bundled in the APK and playback never streams directly.
 */
class QuranAudioController(
    private val context: Context,
    private val event: (String) -> Unit
) {
    val available: Boolean = true

    private val manager = context.getSystemService(AudioManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()
    private var player: MediaPlayer? = null
    private var ready = false
    private var remainingRepeats = 1
    private var currentSurah = 0
    private var currentAyah = 0

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attrs)
        .setOnAudioFocusChangeListener { change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN) pause()
        }
        .build()

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            pause()
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            noisy,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun audioDir(): File =
        File(context.filesDir, "quran-audio/${QuranAudioSource.STORAGE_VERSION}")
            .apply { mkdirs() }

    private fun localFile(surah: Int, ayah: Int): File =
        File(audioDir(), QuranAudioSource.fileName(surah, ayah))

    private fun emit(
        state: String,
        surah: Int = currentSurah,
        ayah: Int = currentAyah,
        done: Int? = null,
        total: Int? = null,
        message: String? = null
    ) {
        val payload = JSONObject()
            .put("state", state)
            .put("surah", surah)
            .put("ayah", ayah)
        done?.let { payload.put("done", it) }
        total?.let { payload.put("total", it) }
        message?.let { payload.put("message", it) }
        event(payload.toString())
    }

    fun isDownloaded(surah: Int, ayah: Int): Boolean {
        if (!QuranAudioSource.isValidReference(surah, ayah)) return false
        val file = localFile(surah, ayah)
        return file.isFile && file.length() > MIN_AUDIO_BYTES && looksLikeMp3(file)
    }

    fun playVerse(surah: Int, ayah: Int, repeats: Int) {
        if (!QuranAudioSource.isValidReference(surah, ayah)) {
            emit("error", surah, ayah, message = "Référence audio invalide")
            return
        }
        val file = localFile(surah, ayah)
        if (!isDownloaded(surah, ayah)) {
            emit("download_required", surah, ayah)
            return
        }

        releasePlayer()
        currentSurah = surah
        currentAyah = ayah
        remainingRepeats = repeats.coerceIn(1, 100)
        ready = false

        player = MediaPlayer().apply {
            setAudioAttributes(attrs)
            setDataSource(file.absolutePath)
            setOnErrorListener { _, _, _ ->
                emit("error", message = "Lecture audio impossible")
                releasePlayer()
                true
            }
            setOnPreparedListener {
                ready = true
                resume()
            }
            setOnCompletionListener { completed ->
                emit("cycle_complete")
                remainingRepeats -= 1
                if (remainingRepeats > 0) {
                    runCatching {
                        completed.seekTo(0)
                        completed.start()
                        emit("playing")
                    }.onFailure {
                        emit("error", message = "Répétition audio impossible")
                        releasePlayer()
                    }
                } else {
                    manager.abandonAudioFocusRequest(focus)
                    emit("paused")
                }
            }
            prepareAsync()
        }
    }

    fun pause() {
        val active = player ?: return
        if (ready) runCatching { active.pause() }
        manager.abandonAudioFocusRequest(focus)
        emit("paused")
    }

    fun resume() {
        val active = player ?: return
        if (!ready) return
        if (manager.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            runCatching { active.start() }
                .onSuccess { emit("playing") }
                .onFailure { emit("error", message = "Reprise audio impossible") }
        }
    }

    fun downloadVerse(surah: Int, ayah: Int) {
        if (!QuranAudioSource.isValidReference(surah, ayah)) {
            emit("download_error", surah, ayah, message = "Référence audio invalide")
            return
        }
        executor.execute {
            emit("download_start", surah, ayah, done = 0, total = 1)
            val ok = downloadOne(surah, ayah)
            if (ok) {
                emit("download_progress", surah, ayah, done = 1, total = 1)
                emit("download_complete", surah, ayah, done = 1, total = 1)
            } else {
                emit("download_error", surah, ayah, message = "Téléchargement du verset impossible")
            }
        }
    }

    fun downloadSurah(surah: Int, ayahCount: Int) {
        val canonicalCount = runCatching { QuranAudioSource.ayahCount(surah) }.getOrNull()
        if (canonicalCount == null || ayahCount != canonicalCount) {
            emit("download_error", surah, 0, message = "Sourate ou nombre de versets invalide")
            return
        }
        executor.execute {
            emit("download_start", surah, 0, done = 0, total = canonicalCount)
            for (ayah in 1..canonicalCount) {
                if (!downloadOne(surah, ayah)) {
                    emit(
                        "download_error",
                        surah,
                        ayah,
                        done = ayah - 1,
                        total = canonicalCount,
                        message = "Téléchargement interrompu au verset $ayah"
                    )
                    return@execute
                }
                emit("download_progress", surah, ayah, done = ayah, total = canonicalCount)
            }
            emit("download_complete", surah, canonicalCount, done = canonicalCount, total = canonicalCount)
        }
    }

    fun deleteSurah(surah: Int, ayahCount: Int) {
        val canonicalCount = runCatching { QuranAudioSource.ayahCount(surah) }.getOrNull() ?: return
        if (ayahCount != canonicalCount) return
        if (currentSurah == surah) {
            pause()
            releasePlayer()
        }
        var deleted = 0
        for (ayah in 1..canonicalCount) {
            val target = localFile(surah, ayah)
            val part = File(target.parentFile, target.name + ".part")
            if (target.delete()) deleted += 1
            part.delete()
        }
        emit("delete_complete", surah, 0, done = deleted, total = canonicalCount)
    }

    private fun downloadOne(surah: Int, ayah: Int): Boolean {
        if (!QuranAudioSource.isValidReference(surah, ayah)) return false
        if (isDownloaded(surah, ayah)) return true

        val target = localFile(surah, ayah)
        val part = File(target.parentFile, target.name + ".part")
        val resumeFrom = part.length().coerceAtLeast(0L)
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(QuranAudioSource.url(surah, ayah))
            require(QuranAudioSource.isAllowedHttpsUrl(url.protocol, url.host))
            connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "Quran-Safeguard/0.10.10 private-local-audio")
                if (resumeFrom > 0L) {
                    setRequestProperty("Range", "bytes=$resumeFrom-")
                }
            }
            val code = connection.responseCode
            require(code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL)
            require(
                QuranAudioSource.isAllowedHttpsUrl(
                    connection.url.protocol,
                    connection.url.host
                )
            )

            val expectedCompleteLength = when (code) {
                HttpURLConnection.HTTP_OK -> {
                    if (part.exists()) part.delete()
                    connection.contentLengthLong.takeIf { it > 0L }
                }
                HttpURLConnection.HTTP_PARTIAL -> {
                    require(resumeFrom > 0L)
                    val contentRange = connection.getHeaderField("Content-Range")
                        ?: error("Content-Range manquant")
                    parseContentRange(contentRange, resumeFrom)
                }
                else -> null
            }

            val append = code == HttpURLConnection.HTTP_PARTIAL
            FileOutputStream(part, append).use { output ->
                connection.inputStream.use { input -> input.copyTo(output) }
            }

            if (expectedCompleteLength != null && part.length() != expectedCompleteLength) {
                // Keep the .part file so a later explicit retry can resume safely.
                return false
            }
            if (part.length() <= MIN_AUDIO_BYTES || !looksLikeMp3(part)) {
                part.delete()
                return false
            }
            if (target.exists() && !target.delete()) return false
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            isDownloaded(surah, ayah)
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Validates a resume response such as "bytes 124000-248999/249000" and
     * returns the authoritative complete object size. A server that resumes at
     * another offset is rejected rather than corrupting the local MP3.
     */
    private fun parseContentRange(value: String, expectedStart: Long): Long {
        val match = CONTENT_RANGE.matchEntire(value.trim())
            ?: error("Content-Range invalide")
        val start = match.groupValues[1].toLong()
        val end = match.groupValues[2].toLong()
        val total = match.groupValues[3].toLong()
        require(start == expectedStart)
        require(end >= start)
        require(total > end)
        return total
    }

    private fun looksLikeMp3(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val header = ByteArray(3)
            if (input.read(header) < 2) return@use false
            val id3 = header.size >= 3 &&
                header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte()
            val frame = (header[0].toInt() and 0xFF) == 0xFF &&
                (header[1].toInt() and 0xE0) == 0xE0
            id3 || frame
        }
    }.getOrDefault(false)

    private fun releasePlayer() {
        runCatching { player?.release() }
        player = null
        ready = false
    }

    fun release() {
        pause()
        releasePlayer()
        executor.shutdownNow()
        runCatching { context.unregisterReceiver(noisy) }
    }

    companion object {
        private const val MIN_AUDIO_BYTES = 1_024L
        private val CONTENT_RANGE = Regex("bytes\\s+(\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)
    }
}