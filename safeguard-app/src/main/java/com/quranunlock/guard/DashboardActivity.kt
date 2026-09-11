package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DashboardActivity : ComponentActivity() {
    private val serviceEnabledState = mutableStateOf(false)
    private val refreshState = mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        serviceEnabledState.value = AccessibilityStatus.isEnabled(this)
        refreshState.intValue += 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuranSafeguardTheme {
                DashboardScreen()
            }
        }
    }

    @Composable
    private fun DashboardScreen() {
        val serviceEnabled = serviceEnabledState.value
        @Suppress("UNUSED_VARIABLE")
        val refresh = refreshState.intValue
        val protectedCount = GuardPrefs.protectedPackages(this@DashboardActivity).size
        val totalReadingMs = GuardPrefs.totalReadingMs(this@DashboardActivity)
        val today = GuardPrefs.dailyReadingSummary(this@DashboardActivity)
        val thought = DailyReminderManager.today(this@DashboardActivity)
        val usageProgress = SafeguardCyclePrefs.progress(this@DashboardActivity)
        val targetUsageMs = GuardPrefs.completedTargetUsageMs(this@DashboardActivity)
        val intervalPresenceMs = GuardPrefs.currentIntervalTargetPresenceMs(this@DashboardActivity)
        val cyclePresenceMs = GuardPrefs.currentCycleTargetPresenceMs(this@DashboardActivity)
        val jokers = GuardPrefs.remainingJokers(this@DashboardActivity)
        val usedJokers = GuardPrefs.DAILY_JOKERS - jokers

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_home),
                                contentDescription = null
                            )
                        },
                        label = { Text("Accueil") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            startActivity(Intent(this@DashboardActivity, QuranHubActivity::class.java))
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_quran),
                                contentDescription = null
                            )
                        },
                        label = { Text("Qur’an") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    SpiritualLibraryActivity::class.java
                                )
                            )
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_library),
                                contentDescription = null
                            )
                        },
                        label = { Text("Textes") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            startActivity(Intent(this@DashboardActivity, SettingsHubActivity::class.java))
                        },
                        icon = {
                            Icon(
                                painterResource(R.drawable.ic_nav_settings),
                                contentDescription = null
                            )
                        },
                        label = { Text("Réglages") },
                        colors = navColors
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            "QURAN SAFEGUARD",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Accueil",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .clickable {
                            val destination = when {
                                serviceEnabled -> SettingsHubActivity::class.java
                                !GuardPrefs.hasAccessibilityConsent(this@DashboardActivity) ->
                                    MainActivity::class.java
                                else -> ProtectionSetupActivity::class.java
                            }
                            startActivity(Intent(this@DashboardActivity, destination))
                        },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            if (serviceEnabled) "Protection active ✓" else "Protection à activer",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (serviceEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (serviceEnabled) {
                                "Touchez pour ouvrir les réglages de protection."
                            } else {
                                "Touchez pour terminer l’activation guidée."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = protectedCount.toString(),
                        label = "Applications"
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = compactDuration(totalReadingMs),
                        label = "Lecture"
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = today.pages.toString(),
                        label = "Pages"
                    )
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "AUJOURD’HUI",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (usageProgress.morningCompleted) {
                                "Filtre matinal terminé ✓"
                            } else {
                                "Filtre matinal : 20 pages à lire"
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Prochaine pause • ${preciseDuration(intervalPresenceMs)} / 15:00",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        SafeguardProgressBar(
                            progress = intervalPresenceMs.toFloat() /
                                UsageCyclePolicy.INTERVAL_MS.toFloat()
                        )
                        Text(
                            "Palier Hizb • ${preciseDuration(cyclePresenceMs)} / 90:00",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        SafeguardProgressBar(
                            progress = cyclePresenceMs.toFloat() /
                                UsageCyclePolicy.CUMULATIVE_MS.toFloat()
                        )
                        Text(
                            "Total cible : ${compactDuration(targetUsageMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$usedJokers joker(s) utilisé(s) aujourd’hui • $jokers disponible(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "ⓘ Le cumul est partagé entre les applications cibles. Le temps hors cible et les appels ne comptent pas. Détails : Réglages › Protection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    ThoughtOfDayActivity::class.java
                                )
                            )
                        },
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
                            "PENSÉE DU JOUR",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            thought.arabicText,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = TextDirection.Rtl,
                                lineHeight = 28.sp
                            ),
                            textAlign = TextAlign.Right,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            thought.frenchText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            thought.author + " • " + thought.book,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    value: String,
    label: String
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun compactDuration(milliseconds: Long): String {
    val minutes = (milliseconds / 60_000L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val rest = minutes % 60L
    return when {
        hours > 0L -> hours.toString() + "h" + rest.toString().padStart(2, '0')
        else -> minutes.toString() + "m"
    }
}

private fun preciseDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
