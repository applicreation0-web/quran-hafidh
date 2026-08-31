package com.applicreation0.quransafeguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val serviceEnabledState = mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        serviceEnabledState.value = AccessibilityStatus.isEnabled(this)
    }

    private fun openAccessibilitySettings() {
        runCatching {
            openAccessibilitySettings()
        }
    }

    private fun openAppSystemSettings() {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            QuranSafeguardTheme {
                var showDisclosure by remember {
                    mutableStateOf(!GuardPrefs.hasAccessibilityConsent(this@MainActivity))
                }

                if (showDisclosure) {
                    AccessibilityDisclosureScreen(
                        onAccept = {
                            GuardPrefs.saveAccessibilityConsent(this@MainActivity)
                            showDisclosure = false
                            openAccessibilitySettings()
                        },
                        onLater = { showDisclosure = false }
                    )
                } else {
                    Dashboard(
                        serviceEnabled = serviceEnabledState.value,
                        onActivateProtection = {
                            if (GuardPrefs.hasAccessibilityConsent(this@MainActivity)) {
                                openAccessibilitySettings()
                            } else {
                                showDisclosure = true
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun Dashboard(
        serviceEnabled: Boolean,
        onActivateProtection: () -> Unit
    ) {
        var mode by remember {
            mutableStateOf(GuardPrefs.selectionMode(this@MainActivity))
        }
        val selectedJuz = remember {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedJuz(this@MainActivity).sorted())
            }
        }
        val selectedHizb = remember {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedHizb(this@MainActivity).sorted())
            }
        }
        val installedApps = remember {
            AppCatalog.launchableApps(this@MainActivity)
                .filterNot { it.packageName in BrowserDetector.supportedPackages }
        }
        val protectedPackages = remember {
            mutableStateListOf<String>().apply {
                addAll(
                    GuardPrefs.protectedPackages(this@MainActivity)
                        .filterNot { it in BrowserDetector.supportedPackages }
                        .sorted()
                )
            }
        }
        var unlockMinutes by remember {
            mutableStateOf(GuardPrefs.unlockMinutes(this@MainActivity))
        }
        var saved by remember { mutableStateOf(false) }
        val jokers = GuardPrefs.remainingJokers(this@MainActivity)
        val readingsCompleted = GuardPrefs.readingsCompleted(this@MainActivity)
        val totalReadingMs = GuardPrefs.totalReadingMs(this@MainActivity)
        val averageReadingMs = GuardPrefs.averageReadingMs(this@MainActivity)
        val lastReadingMs = GuardPrefs.lastReadingMs(this@MainActivity)

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "QURAN SAFEGUARD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Une pause consciente avant l’impulsion.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Simple, volontaire et privé. Tu gardes toujours le contrôle de ton téléphone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Réglages",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Protection, applications, lecture et récurrence sont configurables ici.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (serviceEnabled) "Protection active ✓" else "Protection en pause",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (serviceEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            "8 navigateurs couverts • ${protectedPackages.size} autres applications choisies"
                        )
                        Text(
                            "$jokers/${GuardPrefs.DAILY_JOKERS} jokers disponibles aujourd’hui"
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onActivateProtection
                        ) {
                            Text(
                                if (serviceEnabled) {
                                    "Ouvrir les réglages de protection"
                                } else {
                                    "Activer la protection"
                                }
                            )
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openAppSystemSettings() }
                        ) {
                            Text("Infos et autorisations Android")
                        }
                    }
                }

                SectionTitle("Suivi de lecture")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "$readingsCompleted page(s) validée(s)",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Temps total : ${formatDashboardDuration(totalReadingMs)}")
                        Text("Moyenne : ${formatDashboardDuration(averageReadingMs)} par page")
                        Text(
                            if (lastReadingMs > 0L) {
                                "Dernière lecture : ${formatDashboardDuration(lastReadingMs)}"
                            } else {
                                "Aucune lecture terminée enregistrée pour le moment."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Suivi enregistré uniquement sur ce téléphone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionTitle("Règles du jeu")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("• 1 page complète avant de continuer.")
                        Text("• 60 secondes réelles de lecture au premier plan.")
                        Text("• 3 jokers maximum par jour.")
                        Text("• Un joker ouvre au maximum ${GuardPrefs.JOKER_MAX_UNLOCK_MINUTES} minutes.")
                        Text("• Les changements simples de date ne rechargent pas immédiatement les jokers.")
                        Text(
                            "• Paramètres Android et désinstallation restent accessibles : la règle est stricte tant que tu choisis de jouer.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SectionTitle("Navigateurs")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Couverture volontaire fixe", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Chrome • Firefox • Edge • Brave • Opera • Samsung Internet • DuckDuckGo • Vivaldi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Aucun navigateur inconnu ni WebView d’une autre application n’est bloqué automatiquement.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                SectionTitle("Applications")
                Text(
                    "Choisis les autres applications auxquelles appliquer la pause Quran.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            protectedPackages.clear()
                            protectedPackages.addAll(installedApps.map { it.packageName })
                            saved = false
                        }
                    ) { Text("Tout choisir") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            protectedPackages.clear()
                            saved = false
                        }
                    ) { Text("Effacer") }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        installedApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = app.packageName in protectedPackages,
                                    onCheckedChange = { checked ->
                                        saved = false
                                        if (checked) {
                                            if (app.packageName !in protectedPackages) {
                                                protectedPackages.add(app.packageName)
                                            }
                                        } else {
                                            protectedPackages.remove(app.packageName)
                                        }
                                    }
                                )
                                Column {
                                    Text(app.label)
                                    Text(
                                        app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                SectionTitle("Lecture Quran")
                Text(
                    "Le Mushaf de Médine (604 pages, Hafs ‘an ‘Asim) est intégré et fonctionne hors ligne.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == QuranSelectionMode.JUZ,
                        onClick = {
                            mode = QuranSelectionMode.JUZ
                            saved = false
                        }
                    )
                    Text("Choisir par Juz")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == QuranSelectionMode.HIZB,
                        onClick = {
                            mode = QuranSelectionMode.HIZB
                            saved = false
                        }
                    )
                    Text("Choisir par Hizb")
                }

                val currentSelection =
                    if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        (1..maxUnit).chunked(3).forEach { units ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                units.forEach { unit ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = unit in currentSelection,
                                            onCheckedChange = { checked ->
                                                saved = false
                                                if (checked) {
                                                    if (unit !in currentSelection) currentSelection.add(unit)
                                                } else if (currentSelection.size > 1) {
                                                    currentSelection.remove(unit)
                                                }
                                            }
                                        )
                                        Text("$unitLabel $unit")
                                    }
                                }
                            }
                        }
                    }
                }

                SectionTitle("Récurrence")
                Text(
                    "Après une lecture, l’application reste accessible pendant la durée choisie. Un joker reste plafonné à ${GuardPrefs.JOKER_MAX_UNLOCK_MINUTES} minutes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val durationChoices = listOf(1, 5, 10, 15, 30, 60, 120)
                durationChoices.chunked(2).forEach { rowChoices ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowChoices.forEach { minutes ->
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = unlockMinutes == minutes,
                                    onClick = {
                                        unlockMinutes = minutes
                                        saved = false
                                    }
                                )
                                Text(
                                    when (minutes) {
                                        60 -> "1 heure"
                                        120 -> "2 heures"
                                        else -> "$minutes min"
                                    }
                                )
                            }
                        }
                        if (rowChoices.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                HorizontalDivider()

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        GuardPrefs.saveSelectionMode(this@MainActivity, mode)
                        when (mode) {
                            QuranSelectionMode.JUZ ->
                                GuardPrefs.saveSelectedJuz(this@MainActivity, selectedJuz.toSet())
                            QuranSelectionMode.HIZB ->
                                GuardPrefs.saveSelectedHizb(this@MainActivity, selectedHizb.toSet())
                        }
                        GuardPrefs.saveProtectedPackages(
                            this@MainActivity,
                            protectedPackages.toSet()
                        )
                        GuardPrefs.saveUnlockMinutes(this@MainActivity, unlockMinutes)
                        saved = true
                    }
                ) {
                    Text(if (saved) "Réglages enregistrés ✓" else "Enregistrer mes choix")
                }

                Text(
                    "Version privée • installation officielle via le canal de distribution autorisé",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AccessibilityDisclosureScreen(
    onAccept: () -> Unit,
    onLater: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "QURAN SAFEGUARD",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Avant d’activer la protection",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Quran Safeguard utilise le service d’accessibilité uniquement pour détecter le changement de fenêtre et le nom de l’application au premier plan. Lorsqu’une application choisie est détectée, Quran Safeguard quitte son écran, revient à l’accueil Android et affiche la pause Quran."
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Le contenu affiché à l’écran n’est pas lu, tes saisies ne sont pas enregistrées et ces informations ne sont pas envoyées ni partagées. Le service peut être désactivé à tout moment dans les Paramètres Android.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAccept
            ) { Text("J’accepte et je continue") }

            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLater
            ) { Text("Plus tard") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun QuranSafeguardTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Color(0xFF0B6B4F),
        onPrimary = Color.White,
        secondary = Color(0xFF8B6B2B),
        background = Color(0xFFF8F6EF),
        surface = Color(0xFFFFFCF5),
        surfaceVariant = Color(0xFFEDE9DD),
        onSurface = Color(0xFF1D2622),
        onSurfaceVariant = Color(0xFF56615B)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

private fun formatDashboardDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return when {
        hours > 0L -> "${hours}h ${minutes}min"
        minutes > 0L -> "${minutes}min ${seconds}s"
        else -> "${seconds}s"
    }
}
