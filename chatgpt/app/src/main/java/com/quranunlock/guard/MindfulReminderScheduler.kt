package com.applicreation0.quransafeguard

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

object ReminderPrefs {
    private const val FILE = "mindful_reminder_prefs"
    private const val DAILY_ENABLED = "daily_enabled"
    private const val ADHKAR_ENABLED = "adhkar_enabled"
    private const val LAT = "latitude"
    private const val LON = "longitude"
    private const val HANAFI_ASR = "hanafi_asr"
    private const val ADHKAR_TRANSLITERATION = "adhkar_transliteration"
    private const val LAST_THOUGHT_NOTIFICATION_DAY = "last_thought_notification_day"

    fun dailyEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(DAILY_ENABLED, true)

    fun setDailyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(DAILY_ENABLED, enabled).apply()
    }

    fun adhkarEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(ADHKAR_ENABLED, true)

    fun setAdhkarEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(ADHKAR_ENABLED, enabled).apply()
    }

    fun saveLocation(context: Context, latitude: Double, longitude: Double) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(LAT, latitude.toString())
            .putString(LON, longitude.toString())
            .apply()
    }

    fun location(context: Context): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val lat = prefs.getString(LAT, null)?.toDoubleOrNull() ?: return null
        val lon = prefs.getString(LON, null)?.toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    fun hanafiAsr(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(HANAFI_ASR, false)

    fun setHanafiAsr(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(HANAFI_ASR, enabled).apply()
    }

    fun adhkarTransliterationEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(ADHKAR_TRANSLITERATION, false)

    fun setAdhkarTransliterationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(ADHKAR_TRANSLITERATION, enabled).apply()
    }

    @Synchronized
    fun markThoughtNotificationIfNeeded(
        context: Context,
        epochDay: Long = LocalDate.now().toEpochDay()
    ): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val lastDay = prefs.getLong(LAST_THOUGHT_NOTIFICATION_DAY, Long.MIN_VALUE)
        if (!ThoughtOfDayPolicy.shouldNotify(lastDay, epochDay)) {
            return false
        }
        return prefs.edit()
            .putLong(LAST_THOUGHT_NOTIFICATION_DAY, epochDay)
            .commit()
    }

}

data class AdhkarWindow(
    val startEpochMs: Long,
    val endEpochMs: Long,
    val reminderEpochMs: Long
)

object LocalPrayerWindows {
    fun morning(context: Context, date: LocalDate): AdhkarWindow? =
        calculate(context, date)?.let { times ->
            val start = times.fajr.toEpochMilliseconds()
            val end = times.sunrise.toEpochMilliseconds()
            AdhkarWindow(start, end, midpoint(start, end))
        }

    fun evening(context: Context, date: LocalDate): AdhkarWindow? =
        calculate(context, date)?.let { times ->
            val start = times.asr.toEpochMilliseconds()
            val end = times.maghrib.toEpochMilliseconds()
            AdhkarWindow(start, end, midpoint(start, end))
        }

    private fun calculate(context: Context, date: LocalDate): PrayerTimes? {
        val (lat, lon) = ReminderPrefs.location(context) ?: return null
        val coordinates = Coordinates(lat, lon)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(
            madhab = if (ReminderPrefs.hanafiAsr(context)) Madhab.HANAFI else Madhab.SHAFI
        )
        return runCatching {
            PrayerTimes(
                coordinates,
                DateComponents(date.year, date.monthValue, date.dayOfMonth),
                params
            )
        }.getOrNull()
    }

    private fun midpoint(start: Long, end: Long): Long =
        if (end > start) start + (end - start) / 2L else start
}

object MindfulReminderScheduler {
    const val ACTION_DAILY = "com.applicreation0.quransafeguard.DAILY_REMINDER"
    const val ACTION_MORNING = "com.applicreation0.quransafeguard.MORNING_ADHKAR"
    const val ACTION_EVENING = "com.applicreation0.quransafeguard.EVENING_ADHKAR"

    private const val REQUEST_DAILY = 8100
    private const val REQUEST_MORNING = 8101
    private const val REQUEST_EVENING = 8102

    fun scheduleAll(context: Context) {
        if (ReminderPrefs.dailyEnabled(context)) scheduleDaily(context) else cancel(context, ACTION_DAILY, REQUEST_DAILY)
        if (ReminderPrefs.adhkarEnabled(context) && ReminderPrefs.location(context) != null) {
            scheduleAdhkar(context, AdhkarPeriod.MORNING)
            scheduleAdhkar(context, AdhkarPeriod.EVENING)
        } else {
            cancel(context, ACTION_MORNING, REQUEST_MORNING)
            cancel(context, ACTION_EVENING, REQUEST_EVENING)
        }
    }

    fun scheduleDaily(context: Context) {
        val now = ZonedDateTime.now()
        var next = now.toLocalDate()
            .atTime(ThoughtOfDayPolicy.REMINDER_HOUR, 0)
            .atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        schedule(context, ACTION_DAILY, REQUEST_DAILY, next.toInstant().toEpochMilli())
    }

    private fun scheduleAdhkar(context: Context, period: AdhkarPeriod) {
        val now = System.currentTimeMillis()
        var date = LocalDate.now()
        var window = when (period) {
            AdhkarPeriod.MORNING -> LocalPrayerWindows.morning(context, date)
            AdhkarPeriod.EVENING -> LocalPrayerWindows.evening(context, date)
        }

        if (window == null) return
        if (window.reminderEpochMs <= now) {
            date = date.plusDays(1)
            window = when (period) {
                AdhkarPeriod.MORNING -> LocalPrayerWindows.morning(context, date)
                AdhkarPeriod.EVENING -> LocalPrayerWindows.evening(context, date)
            }
        }
        if (window == null) return

        val action = if (period == AdhkarPeriod.MORNING) ACTION_MORNING else ACTION_EVENING
        val request = if (period == AdhkarPeriod.MORNING) REQUEST_MORNING else REQUEST_EVENING
        schedule(context, action, request, window.reminderEpochMs)
    }

    private fun schedule(context: Context, action: String, requestCode: Int, triggerAt: Long) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pending(context, action, requestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pending(context, action, requestCode))
    }

    private fun pending(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, MindfulReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class MindfulReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            MindfulReminderScheduler.ACTION_DAILY -> ReminderNotifications.showDaily(context)
            MindfulReminderScheduler.ACTION_MORNING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.MORNING)
            MindfulReminderScheduler.ACTION_EVENING -> ReminderNotifications.showAdhkar(context, AdhkarPeriod.EVENING)
        }
        MindfulReminderScheduler.scheduleAll(context)
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MindfulReminderScheduler.scheduleAll(context)
    }
}

object ReminderNotifications {
    // Separate channel so Android does not keep the old low-importance/silent
    // channel configuration after an app update.
    private const val CHANNEL = "mindful_reminders_banner_v2"
    private val SINGLE_GENTLE_VIBRATION = longArrayOf(0L, 55L)

    fun showDaily(context: Context) {
        ensureChannel(context)
        if (!ReminderPrefs.markThoughtNotificationIfNeeded(context)) return

        val thought = DailyReminderManager.today(context)
        val intent = Intent(context, ThoughtOfDayActivity::class.java)
            .putExtra(ThoughtOfDayActivity.EXTRA_REMINDER_ID, thought.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        show(
            context = context,
            id = ThoughtOfDayPolicy.NOTIFICATION_ID,
            title = "Pensée du jour 🌿",
            text = thought.frenchText,
            contentIntent = intent
        )
    }

    fun showAdhkar(context: Context, period: AdhkarPeriod) {
        ensureChannel(context)
        val title = if (period == AdhkarPeriod.MORNING) "Adhkâr du matin 🌿" else "Adhkâr du soir 🌿"
        val text = if (period == AdhkarPeriod.MORNING) {
            "Un moment calme entre Fajr et le lever du soleil."
        } else {
            "Un moment calme entre ‘Asr et Maghrib."
        }
        val intent = Intent(context, AdhkarActivity::class.java)
            .putExtra(AdhkarActivity.EXTRA_PERIOD, period.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        show(
            context = context,
            id = if (period == AdhkarPeriod.MORNING) 8201 else 8202,
            title = title,
            text = text,
            contentIntent = intent
        )
    }

    private fun show(
        context: Context,
        id: Int,
        title: String,
        text: String,
        contentIntent: Intent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val pending = PendingIntent.getActivity(
            context,
            id,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .setVibrate(SINGLE_GENTLE_VIBRATION)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { manager.notify(id, notification) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL,
            "Rappels bienveillants",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pensée du jour à 20:00 et adhkâr matin/soir"
            enableVibration(true)
            vibrationPattern = SINGLE_GENTLE_VIBRATION
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun formatShort(milliseconds: Long): String {
        val minutes = (milliseconds / 60_000L).coerceAtLeast(0L)
        return if (minutes > 0) minutes.toString() + " min de lecture" else "moins d’une minute de lecture"
    }
}
