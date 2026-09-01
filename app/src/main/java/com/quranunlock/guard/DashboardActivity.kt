package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class DashboardActivity : ComponentActivity() {
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
        val serviceEnabled = AccessibilityStatus.isEnabled(this@DashboardActivity)
        val protectedCount = GuardPrefs.protectedPackages(this@DashboardActivity).size
        val totalReadingMs = GuardPrefs.totalReadingMs(this@DashboardActivity)
        val today = GuardPrefs.dailyReadingSummary(this@DashboardActivity)
        val thought = DailyReminderManager.today(this@DashboardActivity)

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val navColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Text("⌂") },
                        label = { Text("Accueil") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    ReadingHistoryActivity::class.java
                                )
                            )
                        },
                        icon = { Text("▥") },
                        label = { Text("Statistiques") },
                        colors = navColors
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            startActivity(
                                Intent(
                                    this@DashboardActivity,
                                    ReadingSelectionActivity::class.java
                                )
                            )
                        },
                        icon = { Text("▤") },
                        label = { Text("Lecture") },
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
                        .clickable {
                            startActivity(
                                Intent(this@DashboardActivity, MainActivity::class.java)
                            )
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (serviceEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            if (serviceEnabled) "Protection active ✓"
                            else "Protection à activer",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (serviceEnabled) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (serviceEnabled) {
                                "Safeguard est actif. Touchez ici pour les réglages."
                            } else {
                                "Touchez ici pour activer la protection Android."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (serviceEnabled) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
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

                DashboardRow(
                    leftTitle = "Applications",
                    leftSubtitle = "Réseaux & navigateurs",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ApplicationsActivity::class.java)
                        )
                    },
                    rightTitle = "Juz / Hizb",
                    rightSubtitle = "Zones de lecture",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ReadingSelectionActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Rappel / Textes",
                    leftSubtitle = "Hadiths • Hikam • Ghazâlî",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, SpiritualLibraryActivity::class.java)
                        )
                    },
                    rightTitle = "Adhkâr",
                    rightSubtitle = "Matin • soir",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, AdhkarActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Historique",
                    leftSubtitle = "Lecture & progression",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ReadingHistoryActivity::class.java)
                        )
                    },
                    rightTitle = "Paramètres",
                    rightSubtitle = "Protection & rappels",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, MainActivity::class.java)
                        )
                    }
                )
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

@Composable
private fun DashboardRow(
    leftTitle: String,
    leftSubtitle: String,
    leftAction: () -> Unit,
    rightTitle: String,
    rightSubtitle: String,
    rightAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DashboardCard(
            modifier = Modifier.weight(1f),
            title = leftTitle,
            subtitle = leftSubtitle,
            onClick = leftAction
        )
        DashboardCard(
            modifier = Modifier.weight(1f),
            title = rightTitle,
            subtitle = rightSubtitle,
            onClick = rightAction
        )
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                subtitle,
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
