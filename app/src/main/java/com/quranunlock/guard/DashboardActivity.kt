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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Text("⌂") },
                        label = { Text("Accueil") }
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
                        label = { Text("Statistiques") }
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
                        label = { Text("Lecture") }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 20.dp),
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
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        "Quran Safeguard",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startActivity(
                                Intent(this@DashboardActivity, MainActivity::class.java)
                            )
                        },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (serviceEnabled) "Protection active ✓"
                            else "Protection à activer",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (serviceEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (serviceEnabled) {
                                "Le service fonctionne. Tous les réglages se font ensuite directement dans Quran Safeguard."
                            } else {
                                "Une activation Android est nécessaire une seule fois pour démarrer la protection."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        label = "Pages aujourd’hui"
                    )
                }

                DashboardRow(
                    leftTitle = "Applications",
                    leftSubtitle = "Gérer les cibles et la protection",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ApplicationsActivity::class.java)
                        )
                    },
                    rightTitle = "Juz / Hizb",
                    rightSubtitle = "Choisir les zones de lecture",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ReadingSelectionActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Rappel / Textes",
                    leftSubtitle = "Hadiths • Al-Hikam • Al-Ghazâlî",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, SpiritualLibraryActivity::class.java)
                        )
                    },
                    rightTitle = "Adhkâr",
                    rightSubtitle = "Matin, soir et favoris",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, AdhkarActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Historique",
                    leftSubtitle = "Activité et statistiques",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ReadingHistoryActivity::class.java)
                        )
                    },
                    rightTitle = "Paramètres",
                    rightSubtitle = "Protection et rappels",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, MainActivity::class.java)
                        )
                    }
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "Applications sensibles toujours exclues",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Banque, paiement, identité, authentification, mots de passe, sécurité, appels et alarmes ne sont jamais des cibles Safeguard.",
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
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
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
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
        hours > 0L -> "${hours}h${rest.toString().padStart(2, '0')}"
        else -> "${minutes}m"
    }
}
