package com.applicreation0.quransafeguard

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

internal object TaddaburPolicy {
    const val FIRST_HIZB = 1
    const val LAST_HIZB = 60
    const val START_HOUR = 7
    const val DEADLINE_HOUR = 20
    const val MIN_PAGE_MS = 90_000L

    fun nextHizb(current: Int): Int =
        if (current >= LAST_HIZB) FIRST_HIZB else current + 1

    fun mayAccumulate(time: LocalTime): Boolean = time.hour >= START_HOUR

    fun deadlinePassed(time: LocalTime): Boolean = time.hour >= DEADLINE_HOUR
}

internal data class TaddaburProgress(
    val epochDay: Long,
    val hizb: Int,
    val startPage: Int,
    val endPage: Int,
    val completedPages: Set<Int>,
    val bookmarkPage: Int,
    val completedAtEpochMs: Long?,
    val penaltyActive: Boolean
) {
    val totalPages: Int get() = endPage - startPage + 1
    val completedCount: Int get() = completedPages.count { it in startPage..endPage }
    val complete: Boolean get() = completedCount >= totalPages
    val fraction: Float get() =
        if (totalPages <= 0) 0f else completedCount.toFloat() / totalPages.toFloat()
}

internal data class TaddaburDailyHistory(
    val epochDay: Long,
    val hizb: Int,
    val completedPages: Int,
    val totalPages: Int,
    val complete: Boolean,
    val completedAtEpochMs: Long?,
    val totalReadingMs: Long
)

internal object TaddaburPrefs {
    private const val FILE = "taddabur_prefs"
    private const val ACTIVE_DAY = "active_day"
    private const val CURRENT_HIZB = "current_hizb"
    private const val COMPLETED_PAGES = "completed_pages"
    private const val BOOKMARK_PAGE = "bookmark_page"
    private const val COMPLETED_DAY = "completed_day"
    private const val COMPLETED_AT = "completed_at"
    private const val PENALTY_DAY = "penalty_day"
    private const val HISTORY_PREFIX = "history_"
    private const val ELAPSED_PREFIX = "elapsed_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun division(hizb: Int): QuranDivision =
        QuranStructureMetadata.division(QuranSelectionMode.HIZB, hizb)

    @Synchronized
    fun progress(context: Context): TaddaburProgress {
        syncDay(context)
        val prefs = prefs(context)
        val day = LocalDate.now().toEpochDay()
        val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val division = division(hizb)
        val completed = prefs.getStringSet(COMPLETED_PAGES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in division.pageRange }
            .toSet()
        val bookmark = prefs.getInt(BOOKMARK_PAGE, division.startPage)
            .coerceIn(division.startPage, division.endPage)
        val completedDay = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE)
        val completedAt = if (completedDay == day) {
            prefs.getLong(COMPLETED_AT, 0L).takeIf { it > 0L }
        } else {
            null
        }
        val penalty = prefs.getLong(PENALTY_DAY, Long.MIN_VALUE) == day
        return TaddaburProgress(
            epochDay = day,
            hizb = hizb,
            startPage = division.startPage,
            endPage = division.endPage,
            completedPages = completed,
            bookmarkPage = bookmark,
            completedAtEpochMs = completedAt,
            penaltyActive = penalty
        )
    }

    @Synchronized
    fun setBookmark(context: Context, page: Int) {
        val state = progress(context)
        if (page !in state.startPage..state.endPage) return
        prefs(context).edit().putInt(BOOKMARK_PAGE, page).commit()
        writeSnapshot(context)
    }

    @Synchronized
    fun elapsedMs(context: Context, page: Int): Long {
        val state = progress(context)
        if (page !in state.startPage..state.endPage) return 0L
        return prefs(context).getLong(elapsedKey(state.hizb, page), 0L).coerceAtLeast(0L)
    }

    @Synchronized
    fun recordActiveMs(context: Context, page: Int, deltaMs: Long): TaddaburProgress {
        var state = progress(context)
        if (page !in state.startPage..state.endPage || deltaMs <= 0L) return state
        val prefs = prefs(context)
        val key = elapsedKey(state.hizb, page)
        val next = (prefs.getLong(key, 0L).coerceAtLeast(0L) + deltaMs)
            .coerceAtMost(24L * 60L * 60L * 1000L)
        val editor = prefs.edit().putLong(key, next).putInt(BOOKMARK_PAGE, page)

        if (next >= TaddaburPolicy.MIN_PAGE_MS && page !in state.completedPages) {
            val pages = state.completedPages.map(Int::toString).toMutableSet()
            pages += page.toString()
            editor.putStringSet(COMPLETED_PAGES, pages)
            val total = state.totalPages
            if (pages.mapNotNull { it.toIntOrNull() }
                    .count { it in state.startPage..state.endPage } >= total
            ) {
                val today = LocalDate.now().toEpochDay()
                if (TaddaburPolicy.deadlinePassed(LocalTime.now())) {
                    editor.putLong(PENALTY_DAY, today)
                }
                editor.putLong(COMPLETED_DAY, today)
                    .putLong(COMPLETED_AT, System.currentTimeMillis())
            }
        }
        check(editor.commit()) { "Unable to persist Taddabur reading progress" }
        state = progress(context)
        writeSnapshot(context)
        return state
    }

    @Synchronized
    fun shouldBlockNow(context: Context): Boolean {
        val state = progress(context)
        val now = LocalTime.now()
        if (!TaddaburPolicy.deadlinePassed(now)) return false
        if (state.penaltyActive) return true
        if (state.complete && state.completedAtEpochMs != null) return false

        val today = LocalDate.now().toEpochDay()
        check(prefs(context).edit().putLong(PENALTY_DAY, today).commit()) {
            "Unable to arm Taddabur deadline"
        }
        writeSnapshot(context)
        return true
    }

    @Synchronized
    fun history(context: Context, limit: Int = 7): List<TaddaburDailyHistory> {
        syncDay(context)
        writeSnapshot(context)
        return prefs(context).all.entries
            .asSequence()
            .filter { it.key.startsWith(HISTORY_PREFIX) && it.value is String }
            .mapNotNull { entry ->
                val day = entry.key.removePrefix(HISTORY_PREFIX).toLongOrNull()
                    ?: return@mapNotNull null
                parseHistory(day, entry.value as String)
            }
            .sortedByDescending { it.epochDay }
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    @Synchronized
    private fun syncDay(context: Context) {
        val prefs = prefs(context)
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(ACTIVE_DAY, Long.MIN_VALUE)
        if (storedDay == Long.MIN_VALUE) {
            val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
                .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
            val division = division(hizb)
            prefs.edit()
                .putLong(ACTIVE_DAY, today)
                .putInt(CURRENT_HIZB, hizb)
                .putInt(BOOKMARK_PAGE, division.startPage)
                .commit()
            return
        }
        if (storedDay == today) return

        writeSnapshotForDay(context, storedDay)
        val oldHizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val completedYesterday = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE) == storedDay
        val nextHizb = if (completedYesterday) TaddaburPolicy.nextHizb(oldHizb) else oldHizb
        val nextDivision = division(nextHizb)
        val editor = prefs.edit()
            .putLong(ACTIVE_DAY, today)
            .putInt(CURRENT_HIZB, nextHizb)
            .putStringSet(COMPLETED_PAGES, emptySet())
            .putInt(BOOKMARK_PAGE, nextDivision.startPage)
            .remove(COMPLETED_DAY)
            .remove(COMPLETED_AT)
            .remove(PENALTY_DAY)
        prefs.all.keys.filter { it.startsWith(ELAPSED_PREFIX) }.forEach(editor::remove)
        check(editor.commit()) { "Unable to roll Taddabur to the next day" }
    }

    private fun writeSnapshot(context: Context) {
        val day = prefs(context).getLong(ACTIVE_DAY, LocalDate.now().toEpochDay())
        writeSnapshotForDay(context, day)
    }

    private fun writeSnapshotForDay(context: Context, day: Long) {
        val prefs = prefs(context)
        val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val division = division(hizb)
        val completed = prefs.getStringSet(COMPLETED_PAGES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .count { it in division.pageRange }
        val total = division.pageRange.count()
        val complete = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE) == day && completed >= total
        val completedAt = if (complete) prefs.getLong(COMPLETED_AT, 0L) else 0L
        val totalMs = division.pageRange.sumOf { page ->
            prefs.getLong(elapsedKey(hizb, page), 0L).coerceAtLeast(0L)
        }
        val encoded = listOf(
            hizb.toString(),
            completed.toString(),
            total.toString(),
            if (complete) "1" else "0",
            completedAt.toString(),
            totalMs.toString()
        ).joinToString("|")
        prefs.edit().putString(HISTORY_PREFIX + day, encoded).commit()
    }

    private fun parseHistory(day: Long, raw: String): TaddaburDailyHistory? {
        val p = raw.split('|')
        if (p.size != 6) return null
        val hizb = p[0].toIntOrNull() ?: return null
        val completed = p[1].toIntOrNull() ?: return null
        val total = p[2].toIntOrNull() ?: return null
        val complete = p[3] == "1"
        val completedAt = p[4].toLongOrNull()?.takeIf { it > 0L }
        val totalMs = p[5].toLongOrNull() ?: return null
        return TaddaburDailyHistory(day, hizb, completed, total, complete, completedAt, totalMs)
    }

    private fun elapsedKey(hizb: Int, page: Int): String =
        "${ELAPSED_PREFIX}h${hizb}_p$page"
}

object TaddaburEdition {
    const val isEnabled: Boolean = true
    private const val ACTION_REMINDER =
        "com.applicreation0.quransafeguard.TADDABUR_DEADLINE"
    private const val REQUEST_REMINDER = 8300
    private const val NOTIFICATION_ID = 8301
    private const val CHANNEL = "taddabur_deadline_v1"

    @Composable
    fun DashboardCard() {
        val context = LocalContext.current
        var refresh by remember { mutableIntStateOf(0) }
        val state = remember(refresh) { TaddaburPrefs.progress(context) }
        val sevenDay = remember(refresh) { TaddaburPrefs.history(context, 7) }
        val completedDays = sevenDay.count { it.complete }

        LaunchedEffect(Unit) {
            scheduleReminder(context)
            while (true) {
                delay(30_000L)
                refresh += 1
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open(context) },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "TADDABUR • PLUS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Hizb ${state.hizb} • pool fixe 1–60",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${state.completedCount}/${state.totalPages} pages • minimum 90 s par page",
                    style = MaterialTheme.typography.bodyMedium
                )
                SafeguardProgressBar(progress = state.fraction)
                Text(
                    "🔖 Marque-page : page ${state.bookmarkPage} • reprendre ici",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    when {
                        state.penaltyActive -> "Blocage Taddabur actif jusqu’à minuit."
                        state.complete -> "Hizb du jour terminé ✓"
                        LocalTime.now().hour < TaddaburPolicy.START_HOUR -> "Le suivi actif commence à 07:00."
                        else -> "À terminer avant 20:00. Lecture possible au fil de la journée."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Suivi 7 jours : $completedDays/${sevenDay.size.coerceAtLeast(1)} jour(s) terminé(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    fun shouldBlockNow(context: Context): Boolean = TaddaburPrefs.shouldBlockNow(context)

    fun scheduleReminder(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()
        var next = now.toLocalDate()
            .atTime(TaddaburPolicy.DEADLINE_HOUR, 0)
            .atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_REMINDER,
            Intent(context, MindfulReminderReceiver::class.java).setAction(ACTION_REMINDER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            alarm.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                pending
            )
        } else {
            alarm.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                next.toInstant().toEpochMilli(),
                pending
            )
        }
    }

    fun handlesReminder(action: String?): Boolean = action == ACTION_REMINDER

    fun handleReminder(context: Context, action: String?) {
        if (!handlesReminder(action)) return
        val blocked = TaddaburPrefs.shouldBlockNow(context)
        if (blocked) showDeadlineNotification(context)
        scheduleReminder(context)
    }

    fun renderBlockingGate(
        activity: ComponentActivity,
        targetPackage: String
    ): Boolean {
        if (!shouldBlockNow(activity)) return false
        activity.setContent {
            QuranSafeguardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var refresh by remember { mutableIntStateOf(0) }
                    val state = remember(refresh) { TaddaburPrefs.progress(activity) }
                    LaunchedEffect(Unit) {
                        while (shouldBlockNow(activity)) {
                            delay(1_000L)
                            refresh += 1
                        }
                        if (GuardPrefs.isUnlocked(activity, targetPackage)) {
                            GuardRuntime.interception.markUnlocked(targetPackage)
                            TargetReturnCoordinator.returnImmediately(
                                activity,
                                targetPackage,
                                "taddabur_midnight_release"
                            )
                        } else {
                            activity.recreate()
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Taddabur • jusqu’à minuit",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            if (state.complete) {
                                "Le Hizb ${state.hizb} a été terminé après 20:00. Le blocage prévu reste actif jusqu’à minuit."
                            } else {
                                "Le Hizb ${state.hizb} n’était pas terminé à 20:00. Les applications protégées restent bloquées jusqu’à minuit."
                            },
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Aucun joker et aucune lecture de déblocage ne contournent cette règle. Les applications hors scope restent hors scope.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(22.dp))
                        SafeguardButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { open(activity) }
                        ) {
                            Text("🔖 Reprendre Taddabur — page ${state.bookmarkPage}")
                        }
                    }
                }
            }
        }
        return true
    }

    fun open(context: Context) {
        context.startActivity(
            Intent(context, TaddaburActivity::class.java).apply {
                if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun showDeadlineNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    "Taddabur à 20:00",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Rappel Taddabur si le hizb du jour n’est pas terminé"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0L, 55L)
                    setSound(null, null)
                }
            )
        }
        val state = TaddaburPrefs.progress(context)
        val intent = Intent(context, TaddaburActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Taddabur • Hizb ${state.hizb}")
            .setContentText("Hizb non terminé à 20:00 • applications protégées bloquées jusqu’à minuit.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Le hizb du jour n’est pas terminé. Vous pouvez poursuivre la lecture et consulter le Tafsīr ; le blocage des applications protégées reste actif jusqu’à minuit."
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(null)
            .setVibrate(longArrayOf(0L, 55L))
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
