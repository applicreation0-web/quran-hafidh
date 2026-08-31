package com.applicreation0.quransafeguard

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZonedDateTime

object DailyReminderScheduler {
    private const val REQUEST_CODE = 2080
    private const val CHANNEL_ID = "daily_spiritual_reminder"
    private const val NOTIFICATION_ID = 2080

    fun scheduleNext(context: Context) {
        if (!DailyReminderManager.notificationsEnabled(context)) {
            cancel(context)
            return
        }

        val now = ZonedDateTime.now()
        var next = now
            .withHour(DailyReminderManager.NOTIFICATION_HOUR)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            reminderPendingIntent(context)
        )

        GuardDiagnostics.log(
            context,
            "DAILY_REMINDER_SCHEDULED",
            detail = "next=" + next.toString()
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(reminderPendingIntent(context))
    }

    fun showToday(context: Context) {
        if (!DailyReminderManager.notificationsEnabled(context)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            GuardDiagnostics.log(
                context,
                "DAILY_REMINDER_SKIPPED",
                detail = "notification_permission_missing"
            )
            return
        }

        val reminder = runCatching { DailyReminderManager.today(context) }
            .getOrElse {
                GuardDiagnostics.log(
                    context,
                    "DAILY_REMINDER_SKIPPED",
                    detail = "library_error=" + it.javaClass.simpleName
                )
                return
            }

        val manager = context.getSystemService(NotificationManager::class.java)
        ensureChannel(manager)

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val today = GuardPrefs.dailyReadingSummary(context)
        val daySummary = if (today.pages > 0) {
            "Aujourd’hui : " + today.pages + " page(s) • " +
                formatReminderDuration(today.totalMs) +
                " • moyenne " + formatReminderDuration(today.averageMs) + " / page"
        } else {
            "Aujourd’hui, chaque occasion de lecture reste une nouvelle possibilité 🌿"
        }

        val sourceLine = buildString {
            append(reminder.book)
            if (reminder.reference.isNotBlank()) {
                append(" • ")
                append(reminder.reference)
            }
            if (!reminder.authenticity.isNullOrBlank()) {
                append(" • ")
                append(reminder.authenticity)
            }
        }

        val bigText = buildString {
            append(daySummary)
            append("\n\n")
            append(reminder.arabicText)
            append("\n\n")
            append(reminder.frenchText)
            append("\n\n")
            append(sourceLine)
            if (reminder.type == ReminderType.HADITH) {
                append("\n")
                append("Source/Traduction : ")
                append(reminder.sourceProvider)
                append(" • ")
                append(reminder.sourceVersion)
            }
        }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Rappel du jour 🌿")
            .setContentText(reminder.frenchText.take(90))
            .setStyle(Notification.BigTextStyle().bigText(bigText))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        GuardDiagnostics.log(context, "DAILY_REMINDER_SHOWN", detail = reminder.id)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_SHOW_DAILY_REMINDER
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun ensureChannel(manager: NotificationManager) {
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Rappel spirituel quotidien",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Rappel Quran Safeguard, discret et bienveillant, à 20h."
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        )
    }
}

private fun formatReminderDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> hours.toString() + "h " + minutes + "min"
        minutes > 0L -> minutes.toString() + "min " + seconds + "s"
        else -> seconds.toString() + "s"
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_SHOW_DAILY_REMINDER =
            "com.applicreation0.quransafeguard.SHOW_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_SHOW_DAILY_REMINDER) return
        DailyReminderScheduler.showToday(context)
        DailyReminderScheduler.scheduleNext(context)
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> DailyReminderScheduler.scheduleNext(context)
        }
    }
}
