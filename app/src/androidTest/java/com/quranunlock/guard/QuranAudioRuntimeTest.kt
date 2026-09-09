package com.applicreation0.quransafeguard

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-source smoke proof: fetch one Al-Husary Muʿallim ayah through the same
 * controller used by the reader, keep it in private local storage, then prove
 * Android MediaPlayer can prepare/start that local MP3. No audio bytes are
 * packaged with the test APK.
 */
@RunWith(AndroidJUnit4::class)
class QuranAudioRuntimeTest {
    @Test
    fun downloadsAndStartsRealLocalHusaryAyah() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val downloadFinished = CountDownLatch(1)
        val playbackStarted = CountDownLatch(1)
        val failures = mutableListOf<String>()

        val controller = QuranAudioController(context) { raw ->
            val event = JSONObject(raw)
            when (event.optString("state")) {
                "download_complete" -> downloadFinished.countDown()
                "playing" -> playbackStarted.countDown()
                "download_error", "error" -> synchronized(failures) {
                    failures += event.optString("message", event.optString("state"))
                }
            }
        }

        try {
            controller.deleteSurah(1, 7)
            controller.downloadVerse(1, 1)
            assertTrue(
                "Real Al-Husary ayah download did not complete: $failures",
                downloadFinished.await(60, TimeUnit.SECONDS)
            )
            assertTrue("Downloaded ayah is not valid local audio", controller.isDownloaded(1, 1))

            controller.playVerse(1, 1, 1)
            assertTrue(
                "MediaPlayer did not start the downloaded Al-Husary ayah: $failures",
                playbackStarted.await(30, TimeUnit.SECONDS)
            )
        } finally {
            controller.pause()
            controller.deleteSurah(1, 7)
            controller.release()
        }
    }
}
