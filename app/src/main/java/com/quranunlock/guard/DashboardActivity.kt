package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "QURAN SAFEGUARD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Un espace simple pour protéger votre attention.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            startActivity(
                                Intent(this@DashboardActivity, MainActivity::class.java)
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            if (AccessibilityStatus.isEnabled(this@DashboardActivity)) {
                                "Protection active ✓"
                            } else {
                                "Protection à activer"
                            },
                            color = if (AccessibilityStatus.isEnabled(this@DashboardActivity)) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (AccessibilityStatus.isEnabled(this@DashboardActivity)) {
                                "Les réglages se font dans Safeguard. Android Accessibility reste simplement activé en arrière-plan."
                            } else {
                                "Ouvrir les paramètres Safeguard pour effectuer l’activation Android une seule fois."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DashboardRow(
                    leftTitle = "Applications",
                    leftSubtitle = "Réseaux sociaux & navigateurs",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ApplicationsActivity::class.java)
                        )
                    },
                    rightTitle = "Juz / Hizb",
                    rightSubtitle = "Choix de lecture",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, ReadingSelectionActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Bibliothèque",
                    leftSubtitle = "Hadiths • Al-Hikam • Al-Ghazâlî",
                    leftAction = {
                        startActivity(
                            Intent(this@DashboardActivity, SpiritualLibraryActivity::class.java)
                        )
                    },
                    rightTitle = "Adhkâr",
                    rightSubtitle = "Matin • soir • favoris",
                    rightAction = {
                        startActivity(
                            Intent(this@DashboardActivity, AdhkarActivity::class.java)
                        )
                    }
                )
                DashboardRow(
                    leftTitle = "Historique",
                    leftSubtitle = "Pages, temps, tendances",
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

                Text(
                    "Rappel du jour",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                DailyReminderCard(
                    DailyReminderManager.today(this@DashboardActivity)
                )
            }
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
