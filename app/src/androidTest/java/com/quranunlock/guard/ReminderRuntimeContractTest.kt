package com.applicreation0.quransafeguard

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderRuntimeContractTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun resetPrefs() {
        context.getSharedPreferences("mindful_reminder_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun reminderChannelIsSilentWithOneGentleVibration() {
        ReminderNotifications.showAdhkar(context, AdhkarPeriod.MORNING)
        ReminderNotifications.showAdhkar(context, AdhkarPeriod.EVENING)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = manager.getNotificationChannel("mindful_reminders_banner_v2")
            assertTrue(channel != null)
            assertNull(channel.sound)
            assertTrue(channel.shouldVibrate())
            assertArrayEquals(longArrayOf(0L, 55L), channel.vibrationPattern)
        }
    }

    @Test
    fun adhkarTransliterationIsOffByDefaultAndExplicitlyOptional() {
        assertFalse(ReminderPrefs.adhkarTransliterationEnabled(context))
        ReminderPrefs.setAdhkarTransliterationEnabled(context, true)
        assertTrue(ReminderPrefs.adhkarTransliterationEnabled(context))
        ReminderPrefs.setAdhkarTransliterationEnabled(context, false)
        assertFalse(ReminderPrefs.adhkarTransliterationEnabled(context))
    }
}
