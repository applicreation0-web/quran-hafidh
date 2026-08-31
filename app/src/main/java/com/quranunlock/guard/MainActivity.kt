package com.applicreation0.quransafeguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
    private val refreshState = mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        serviceEnabledState.value = AccessibilityStatus.isEnabled(this)
        refreshState.value += 1
    }

    private fun openAccessibilitySettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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

    private fun shareInstallationGuide() {
        val guide = """
            Quran Safeguard 🌿

            Installation simple :
            1. Installe l’application qui t’a été envoyée.
            2. Ouvre Quran Safeguard.
            3. Suis l’étape “Activer la protection” pour autoriser le service d’accessibilité.
            4. Autorise l’application à fonctionner normalement en arrière-plan si Android te le demande.
            5. Choisis tranquillement les applications à protéger et les Juz/Hizb souhaités.
            6. Appuie sur “Tester la protection” pour vérifier que tout est prêt.

            Si une étape te semble inhabituelle, reviens simplement dans Quran Safeguard : le guide reste disponible.
            Bonne installation 🌿
        """.trimIndent()

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, guide)
                },
                "Partager le guide d’installation"
            )
        )
    }

    private fun testProtection() {
        val candidates = buildList {
            add("com.android.chrome")
            addAll(BrowserDetector.supportedPackages.filterNot { it == "com.android.chrome" })
            addAll(GuardPrefs.protectedPackages(this@MainActivity))
        }.distinct()

        val target = candidates.firstOrNull {
            packageManager.getLaunchIntentForPackage(it) != null
        }

        if (target == null) {
            Toast.makeText(this, "Aucune application protégée testable trouvée.", Toast.LENGTH_SHORT).show()
            return
        }

        GuardDiagnostics.log(this, "MANUAL_TEST_STARTED", target)
        startActivity(packageManager.getLaunchIntentForPackage(target))
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
                        refreshToken = refreshState.value,
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
        refreshToken: Int,
        onActivateProtection: () -> Unit
    ) {
        @Suppress("UNUSED_VARIABLE")
        val refresh = refreshToken
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
        val jokers = GuardPrefs.remainingJokers(this@MainActivity)
        val readingsCompleted = GuardPrefs.readingsCompleted(this@MainActivity)
        val totalReadingMs = GuardPrefs.totalReadingMs(this@MainActivity)
        val averageReadingMs = GuardPrefs.averageReadingMs(this@MainActivity)
        val lastReadingMs = GuardPrefs.lastReadingMs(this@MainActivity)
        val todaySummary = GuardPrefs.dailyReadingSummary(this@MainActivity)
        val sevenDayAverage = GuardPrefs.averageReadingMsForWindow(this@MainActivity, 7)
        val previousSevenDayAverage =
            GuardPrefs.averageReadingMsForWindow(this@MainActivity, 7, offsetDays = 7)
        val thirtyDayAverage = GuardPrefs.averageReadingMsForWindow(this@MainActivity, 30)
        val atypicalReadingCount = GuardPrefs.atypicalReadingCount(this@MainActivity, 30)
        val serviceAlive = GuardHealth.serviceLooksAlive(this@MainActivity)
        val lastEventAge = GuardHealth.lastProtectedEventAgeMs(this@MainActivity)
        val recentDiagnostics = GuardDiagnostics.recent(this@MainActivity, 5)
        val recentHistory = GuardPrefs.readingHistory(this@MainActivity, 5)

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
                            if (serviceEnabled && serviceAlive) "Protection active ✓" else if (serviceEnabled) "Protection activée • service à confirmer" else "Protection en pause",
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
                        Text(
                            if (lastEventAge != null) {
                                "Dernière détection : ${formatAge(lastEventAge)}"
                            } else {
                                "Aucune application protégée détectée depuis l’installation."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            onClick = { testProtection() },
                            enabled = serviceEnabled
                        ) {
                            Text("Tester la protection")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openAppSystemSettings() }
                        ) {
                            Text("Infos et autorisations Android")
                        }
                    }
                }

                SectionTitle("Aujourd’hui")
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            dailyReadingMessage(todaySummary),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (todaySummary.pages > 0) {
                            Text(
                                todaySummary.pages.toString() + " page(s) • " +
                                    formatDashboardDuration(todaySummary.totalMs)
                            )
                            Text(
                                "Temps moyen : " +
                                    formatDashboardDuration(todaySummary.averageMs) +
                                    " par page"
                            )
                            if (sevenDayAverage > 0L) {
                                Text(
                                    "Moyenne 7 jours : " +
                                        formatDashboardDuration(sevenDayAverage) +
                                        " • 30 jours : " +
                                        formatDashboardDuration(thirtyDayAverage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val trend = readingTrendLabel(
                                sevenDayAverage,
                                previousSevenDayAverage
                            )
                            if (trend.isNotBlank()) {
                                Text(
                                    trend,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                        if (atypicalReadingCount > 0) {
                            Text(
                                atypicalReadingCount.toString() +
                                    " lecture(s) au rythme inhabituel repérée(s) sur 30 jours — aucune sanction automatique.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (recentHistory.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Historique récent", fontWeight = FontWeight.SemiBold)
                            recentHistory.forEach { entry ->
                                Text(
                                    if (entry.method == "joker") {
                                        "Joker • page ${entry.page} • ${entry.packageName}"
                                    } else {
                                        "Page " + entry.page + " • " +
                                            formatDashboardDuration(entry.elapsedMs) + " • " +
                                            entry.packageName +
                                            if (entry.atypicalFast) " • rythme inhabituel" else ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                SectionTitle("Diagnostic")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            if (serviceAlive) "Service connecté ✓" else "Service non confirmé",
                            fontWeight = FontWeight.SemiBold
                        )
                        recentDiagnostics.forEach { entry ->
                            Text(
                                "${GuardDiagnostics.formatTime(entry)} • ${entry.code}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (recentDiagnostics.isEmpty()) {
                            Text(
                                "Le journal local est vide.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                SectionTitle("Règles du jeu")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("• 1 page complète avant de continuer.")
                        Text("• La page conserve son format 15 lignes et se parcourt naturellement jusqu’en bas.")
                        Text("• Le chrono mesure votre rythme réel sans imposer une vitesse de lecture.")
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
                            GuardPrefs.saveProtectedPackages(this@MainActivity, protectedPackages.toSet())
                        }
                    ) { Text("Tout sélectionner") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            protectedPackages.clear()
                            GuardPrefs.saveProtectedPackages(this@MainActivity, emptySet())
                        }
                    ) { Text("Tout désélectionner") }
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
                                        if (checked) {
                                            if (app.packageName !in protectedPackages) {
                                                protectedPackages.add(app.packageName)
                                            }
                                        } else {
                                            protectedPackages.remove(app.packageName)
                                        }
                                        GuardPrefs.saveProtectedPackages(
                                            this@MainActivity,
                                            protectedPackages.toSet()
                                        )
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
                            GuardPrefs.saveSelectionMode(this@MainActivity, mode)
                        }
                    )
                    Text("Choisir par Juz")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = mode == QuranSelectionMode.HIZB,
                        onClick = {
                            mode = QuranSelectionMode.HIZB
                            GuardPrefs.saveSelectionMode(this@MainActivity, mode)
                        }
                    )
                    Text("Choisir par Hizb")
                }

                val currentSelection =
                    if (mode == QuranSelectionMode.JUZ) selectedJuz else selectedHizb
                val maxUnit = if (mode == QuranSelectionMode.JUZ) 30 else 60
                val unitLabel = if (mode == QuranSelectionMode.JUZ) "Juz" else "Hizb"

                Text(
                    currentSelection.size.toString() + " " + unitLabel +
                        if (currentSelection.size > 1) " sélectionnés" else " sélectionné",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentSelection.clear()
                            currentSelection.addAll(1..maxUnit)
                            when (mode) {
                                QuranSelectionMode.JUZ -> GuardPrefs.saveSelectedJuz(
                                    this@MainActivity,
                                    selectedJuz.toSet()
                                )
                                QuranSelectionMode.HIZB -> GuardPrefs.saveSelectedHizb(
                                    this@MainActivity,
                                    selectedHizb.toSet()
                                )
                            }
                        }
                    ) { Text("Tout sélectionner") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            currentSelection.clear()
                            when (mode) {
                                QuranSelectionMode.JUZ -> GuardPrefs.saveSelectedJuz(
                                    this@MainActivity,
                                    emptySet()
                                )
                                QuranSelectionMode.HIZB -> GuardPrefs.saveSelectedHizb(
                                    this@MainActivity,
                                    emptySet()
                                )
                            }
                        }
                    ) { Text("Tout désélectionner") }
                }
                if (currentSelection.isEmpty()) {
                    Text(
                        "Aucun filtre actif : l’ensemble du Quran reste éligible jusqu’à votre prochaine sélection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                                                if (checked) {
                                                    if (unit !in currentSelection) currentSelection.add(unit)
                                                } else if (currentSelection.size > 1) {
                                                    currentSelection.remove(unit)
                                                }
                                                when (mode) {
                                                    QuranSelectionMode.JUZ -> GuardPrefs.saveSelectedJuz(
                                                        this@MainActivity,
                                                        selectedJuz.toSet()
                                                    )
                                                    QuranSelectionMode.HIZB -> GuardPrefs.saveSelectedHizb(
                                                        this@MainActivity,
                                                        selectedHizb.toSet()
                                                    )
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
                    "Après une lecture, chaque application dispose de son propre budget. Le compteur avance uniquement lorsqu’elle est réellement au premier plan, avec un maximum de 20 minutes. Un joker reste plafonné à " +
                        GuardPrefs.JOKER_MAX_UNLOCK_MINUTES + " minutes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val durationChoices = listOf(1, 5, 10, 15, 20)
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
                                        GuardPrefs.saveUnlockMinutes(this@MainActivity, minutes)
                                    }
                                )
                                Text(minutes.toString() + " min")
                            }
                        }
                        if (rowChoices.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                SectionTitle("Installation & invitation")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Une installation simple, étape par étape 🌿",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("1. Installer puis ouvrir Quran Safeguard.")
                        Text("2. Activer calmement la protection dans les réglages d’accessibilité.")
                        Text("3. Autoriser le fonctionnement en arrière-plan si Android le propose.")
                        Text("4. Choisir les applications et les Juz/Hizb souhaités.")
                        Text("5. Tester la protection pour confirmer que tout fonctionne.")
                        Text(
                            "Le guide peut être partagé avec une invitation afin que la personne sache exactement quoi faire.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { shareInstallationGuide() }
                        ) {
                            Text("Partager le guide d’installation")
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "Réglages enregistrés automatiquement ✓",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

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
                "Quran Safeguard utilise le service d’accessibilité uniquement pour détecter le changement de fenêtre et le nom de l’application au premier plan. Lorsqu’une application choisie est détectée, Quran Safeguard affiche immédiatement la pause Quran au-dessus de cette application."
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
        secondary = Color(0xFF2F7D63),
        background = Color(0xFFF7FBF8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE8F3ED),
        onSurface = Color(0xFF17231E),
        onSurfaceVariant = Color(0xFF52635B)
    )
    MaterialTheme(colorScheme = colors, content = content)
}

private fun dailyReadingMessage(summary: DailyReadingSummary): String =
    if (summary.pages > 0) {
        "Belle progression aujourd’hui 🌿"
    } else {
        "Chaque page compte. Votre prochaine lecture vous attend 🌿"
    }

private fun readingTrendLabel(current: Long, previous: Long): String {
    if (current <= 0L || previous <= 0L) return ""
    val percent = ((previous - current) * 100L / previous).toInt()
    return when {
        percent >= 5 -> "Votre temps moyen s’améliore progressivement : " + percent + "% sur les 7 derniers jours."
        percent <= -5 -> "Votre rythme est plus posé cette semaine — prenez le temps qui vous convient."
        else -> "Votre rythme reste régulier cette semaine."
    }
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

private fun formatAge(ageMs: Long): String {
    val seconds = ageMs.coerceAtLeast(0L) / 1000L
    return when {
        seconds < 5L -> "à l’instant"
        seconds < 60L -> "il y a ${seconds}s"
        seconds < 3600L -> "il y a ${seconds / 60L} min"
        else -> "il y a ${seconds / 3600L} h"
    }
}
