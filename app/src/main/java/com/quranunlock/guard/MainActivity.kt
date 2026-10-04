package com.applicreation0.quransafeguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val serviceEnabledState = mutableStateOf(false)
    private val otherEditionEnabledState = mutableStateOf(false)
    private val refreshState = mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        serviceEnabledState.value = AccessibilityStatus.isEnabled(this)
        otherEditionEnabledState.value = AccessibilityStatus.isOtherEditionEnabled(this)
        refreshState.value += 1
    }

    private fun openProtectionSetup() {
        startActivity(Intent(this, ProtectionSetupActivity::class.java))
    }

    private fun shareInstallationGuide() {
        val guide = """
            Quran Safeguard — version à partager

            Installation simple :
            1. Utilise uniquement l’APK signé « Quran Safeguard » qui t’a été fourni avec son contrôle SHA-256. N’installe pas une APK présentée comme une édition privée : elle n’est pas destinée au partage.
            2. Installe l’APK puis ouvre Quran Safeguard.
            3. Suis l’étape “Activer la protection” pour autoriser le service d’accessibilité.
            4. Autorise les rappels et, si tu le souhaites, la localisation approximative utilisée uniquement sur le téléphone pour calculer Fajr, le lever du soleil, ‘Asr et Maghrib.
            5. Choisis tranquillement les applications à protéger et les Juz/Hizb souhaités.
            6. Appuie sur “Tester la protection” pour vérifier que tout est prêt.

            L’identifiant attendu est com.applicreation0.quransafeguard. N’installe pas une autre APK portant le même nom depuis une source différente. En cas de doute, vérifie le SHA-256 ou le certificat de signature de la version fournie.
            Bonne installation.
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

    private fun captureApproximateLocation(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val manager = getSystemService(LocationManager::class.java) ?: return false
        val location = manager.getProviders(true)
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }
            ?: return false

        ReminderPrefs.saveLocation(this, location.latitude, location.longitude)
        MindfulReminderScheduler.scheduleAll(this)
        return true
    }

    private fun testProtection() {
        val candidates = GuardPrefs.protectedPackages(this@MainActivity).toList()

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
                            openProtectionSetup()
                        },
                        onLater = { showDisclosure = false }
                    )
                } else {
                    Dashboard(
                        serviceEnabled = serviceEnabledState.value,
                        otherEditionEnabled = otherEditionEnabledState.value,
                        refreshToken = refreshState.value,
                        onActivateProtection = {
                            if (GuardPrefs.hasAccessibilityConsent(this@MainActivity)) {
                                openProtectionSetup()
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
        otherEditionEnabled: Boolean,
        refreshToken: Int,
        onActivateProtection: () -> Unit
    ) {
        @Suppress("UNUSED_VARIABLE")
        val refresh = refreshToken
        var mode by remember(refreshToken) {
            mutableStateOf(GuardPrefs.selectionMode(this@MainActivity))
        }
        val selectedJuz = remember(refreshToken) {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedJuz(this@MainActivity).sorted())
            }
        }
        val selectedHizb = remember(refreshToken) {
            mutableStateListOf<Int>().apply {
                addAll(GuardPrefs.selectedHizb(this@MainActivity).sorted())
            }
        }
        val protectedPackages = remember(refreshToken) {
            mutableStateListOf<String>().apply {
                addAll(GuardPrefs.protectedPackages(this@MainActivity).sorted())
            }
        }
        val usageProgress = SafeguardCyclePrefs.progress(this@MainActivity)
        val targetUsageMs = GuardPrefs.completedTargetUsageMs(this@MainActivity)
        val intervalPresenceMs = GuardPrefs.currentIntervalTargetPresenceMs(this@MainActivity)
        val cyclePresenceMs = GuardPrefs.currentCycleTargetPresenceMs(this@MainActivity)
        val remainingIntervalMs = GuardPrefs.globalRemainingUnlockMs(this@MainActivity)
        val jokers = GuardPrefs.remainingJokers(this@MainActivity)
        val readingsCompleted = GuardPrefs.readingsCompleted(this@MainActivity)
        val totalReadingMs = GuardPrefs.totalReadingMs(this@MainActivity)
        val averageReadingMs = GuardPrefs.averageReadingMs(this@MainActivity)
        val lastReadingMs = GuardPrefs.lastReadingMs(this@MainActivity)
        val todaySummary = GuardPrefs.dailyReadingSummary(this@MainActivity)
        val sevenDayAverage = GuardPrefs.averageReadingMsForWindow(this@MainActivity, 7)
        val previousSevenDayAverage = GuardPrefs.averageReadingMsForWindow(this@MainActivity, 7, offsetDays = 7)
        val thirtyDayAverage = GuardPrefs.averageReadingMsForWindow(this@MainActivity, 30)
        val atypicalReadingCount = GuardPrefs.atypicalReadingCount(this@MainActivity, 30)
        val serviceAlive = GuardHealth.serviceLooksAlive(this@MainActivity)
        val lastEventAge = GuardHealth.lastProtectedEventAgeMs(this@MainActivity)
        val recentDiagnostics = GuardDiagnostics.recent(this@MainActivity, 5)
        val recentHistory = GuardPrefs.readingHistory(this@MainActivity, 5)
        val todayReminder = remember(refreshToken) { DailyReminderManager.today(this@MainActivity) }
        val notificationGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        val locationSaved = ReminderPrefs.location(this@MainActivity) != null

        val reminderPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val locationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

            if (locationGranted) {
                if (!captureApproximateLocation()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Localisation autorisée. Ouvrez l’application un peu plus tard si Android n’a pas encore de position disponible.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            MindfulReminderScheduler.scheduleAll(this@MainActivity)
            refreshState.value += 1
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.app_name).uppercase(),
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
                if (otherEditionEnabled) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Deux éditions sont actives",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Les deux éditions ne doivent pas utiliser simultanément le service d’accessibilité. La protection est suspendue pour éviter une double interception. Désactivez le service de l’autre édition dans les réglages Android."
                            )
                            SafeguardButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onActivateProtection
                            ) { Text("Ouvrir les réglages d’accessibilité") }
                        }
                    }
                }
                Text("Réglages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Protection, applications, lecture et récurrence sont configurables ici.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            if (serviceEnabled && serviceAlive) "Protection active ✓" else if (serviceEnabled) "Protection activée • service à confirmer" else "Protection en pause",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text("${protectedPackages.size} cible(s) installée(s) active(s) • réseaux sociaux et navigateurs uniquement")
                        Text("$jokers/${GuardPrefs.DAILY_JOKERS} jokers disponibles aujourd’hui")
                        Text(
                            if (lastEventAge != null) "Dernière détection : ${formatAge(lastEventAge)}" else "Aucune application protégée détectée depuis l’installation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!serviceEnabled) {
                            SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = onActivateProtection) {
                                Text("Activer la protection via Android")
                            }
                        } else {
                            Text(
                                "Tous les réglages Safeguard se modifient directement dans cette application. Accessibilité ne sert qu’à activer ou désactiver le service.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { testProtection() },
                            enabled = serviceEnabled && !otherEditionEnabled
                        ) { Text("Tester la protection") }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SafeguardOutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { startActivity(Intent(this@MainActivity, SpiritualLibraryActivity::class.java)) }
                            ) { Text("Bibliothèque") }
                            SafeguardOutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = { startActivity(Intent(this@MainActivity, AdhkarActivity::class.java)) }
                            ) { Text("Adhkâr") }
                        }
                    }
                }

                SectionTitle("Aujourd’hui")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(dailyReadingMessage(todaySummary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (usageProgress.morningCompleted) "Filtre matinal terminé ✓" else "Filtre matinal à accomplir • 20 pages",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Usage des applications cibles : ${formatDashboardDuration(targetUsageMs)} aujourd’hui")
                        Text(
                            "Cycle courant : ${formatDashboardDuration(cyclePresenceMs)} de présence cible / 90 min • ${usageProgress.completedNinetyMinuteCycles} cycle(s) de 90 min terminé(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (todaySummary.pages > 0) {
                            Text("${todaySummary.pages} page(s) • ${formatDashboardDuration(todaySummary.totalMs)}")
                            Text("Temps moyen : ${formatDashboardDuration(todaySummary.averageMs)} par page")
                            if (sevenDayAverage > 0L) {
                                Text(
                                    "Moyenne 7 jours : ${formatDashboardDuration(sevenDayAverage)} • 30 jours : ${formatDashboardDuration(thirtyDayAverage)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val trend = readingTrendLabel(sevenDayAverage, previousSevenDayAverage)
                            if (trend.isNotBlank()) {
                                Text(trend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                SectionTitle("Pensée du jour")
                DailyReminderCard(reminder = todayReminder)

                SectionTitle("Rappels bienveillants")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("20:00 • Pensée du jour", fontWeight = FontWeight.SemiBold)
                        Text("Adhkâr : matin entre Fajr et le lever du soleil ; soir entre ‘Asr et Maghrib.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Alerte : petite bannière + une vibration courte, sans son et sans ouverture forcée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Les horaires sont calculés sur ce téléphone. La localisation approximative n’est ni envoyée ni partagée.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Notifications : " + (if (notificationGranted) "autorisées ✓" else "à autoriser") + " • horaires locaux : " + (if (locationSaved) "configurés ✓" else "à configurer"),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ReminderPrefs.dailyEnabled(this@MainActivity),
                                onCheckedChange = { enabled ->
                                    ReminderPrefs.setDailyEnabled(this@MainActivity, enabled)
                                    MindfulReminderScheduler.scheduleAll(this@MainActivity)
                                    refreshState.value += 1
                                }
                            )
                            Column {
                                Text("Pensée du jour à 20:00")
                                Text(if (ReminderPrefs.dailyEnabled(this@MainActivity)) "Activé" else "Désactivé", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ReminderPrefs.adhkarEnabled(this@MainActivity),
                                onCheckedChange = { enabled ->
                                    ReminderPrefs.setAdhkarEnabled(this@MainActivity, enabled)
                                    MindfulReminderScheduler.scheduleAll(this@MainActivity)
                                    refreshState.value += 1
                                }
                            )
                            Column {
                                Text("Rappels adhkâr matin / soir")
                                Text(if (ReminderPrefs.adhkarEnabled(this@MainActivity)) "Activés" else "Désactivés", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ReminderPrefs.hanafiAsr(this@MainActivity),
                                onCheckedChange = { enabled ->
                                    ReminderPrefs.setHanafiAsr(this@MainActivity, enabled)
                                    MindfulReminderScheduler.scheduleAll(this@MainActivity)
                                    refreshState.value += 1
                                }
                            )
                            Column {
                                Text("Calcul de ‘Asr hanafi")
                                Text(
                                    if (ReminderPrefs.hanafiAsr(this@MainActivity)) "Activé • méthode MWL avec ‘Asr hanafi" else "Désactivé • méthode MWL avec ‘Asr standard",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SafeguardButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val requested = buildList {
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                                }.toTypedArray()
                                reminderPermissionLauncher.launch(requested)
                            }
                        ) {
                            Text(if (notificationGranted && locationSaved) "Actualiser les horaires des rappels" else "Activer les rappels matin / soir")
                        }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                startActivity(
                                    Intent(this@MainActivity, AdhkarActivity::class.java)
                                        .putExtra(AdhkarActivity.EXTRA_PERIOD, AdhkarPeriod.MORNING.name)
                                )
                            }
                        ) { Text("Voir les adhkâr authentifiés") }
                    }
                }

                SectionTitle("Suivi de lecture")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$readingsCompleted page(s) validée(s)", fontWeight = FontWeight.SemiBold)
                        Text("Temps total : ${formatDashboardDuration(totalReadingMs)}")
                        Text("Moyenne : ${formatDashboardDuration(averageReadingMs)} par page")
                        Text(
                            if (lastReadingMs > 0L) "Dernière lecture : ${formatDashboardDuration(lastReadingMs)}" else "Aucune lecture terminée enregistrée pour le moment.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Suivi enregistré uniquement sur ce téléphone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (atypicalReadingCount > 0) {
                            Text("$atypicalReadingCount lecture(s) au rythme inhabituel repérée(s) sur 30 jours — aucune sanction automatique.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (recentHistory.isNotEmpty()) {
                            HorizontalDivider()
                            Text("Historique récent", fontWeight = FontWeight.SemiBold)
                            recentHistory.forEach { entry ->
                                Text(
                                    if (entry.method.startsWith("joker")) {
                                        "Joker • page ${entry.page} • ${entry.packageName}"
                                    } else {
                                        "Page ${entry.page} • ${formatDashboardDuration(entry.elapsedMs)} • ${entry.packageName}" + if (entry.atypicalFast) " • rythme inhabituel" else ""
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if ((applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    SectionTitle("Diagnostic")
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (serviceAlive) "Service connecté ✓" else "Service non confirmé", fontWeight = FontWeight.SemiBold)
                            recentDiagnostics.forEach { entry ->
                                Text("${GuardDiagnostics.formatTime(entry)} • ${entry.code}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (recentDiagnostics.isEmpty()) Text("Le journal local est vide.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                SectionTitle("Règles du jeu")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Premier accès quotidien : 20 pages, au moins 60 secondes actives par page.")
                        Text("• Ensuite : une page après chaque tranche de 15 minutes d’usage cible effectif.")
                        Text("• À 90 minutes, un Hizb de 10 pages remplace la pause simple et relance le cycle.")
                        Text("• Le compteur est commun aux applications cibles et s’arrête dès qu’on les quitte.")
                        Text("• Les appels classiques et WhatsApp audio/vidéo ne sont jamais décomptés.")
                        Text("• 3 jokers maximum par jour, utilisables à chacun des trois niveaux.")
                        Text("• Les changements simples de date ne rechargent pas immédiatement les jokers.")
                        Text("• Toutes les applications non ciblées restent hors de Safeguard ; aucune liste d’exclusion n’est nécessaire.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                SectionTitle("Accès rapides")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startActivity(Intent(this@MainActivity, ApplicationsActivity::class.java)) }
                        ) { Text("Applications protégées") }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startActivity(Intent(this@MainActivity, ReadingSelectionActivity::class.java)) }
                        ) { Text("Choix Juz / Hizb") }
                        SafeguardOutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startActivity(Intent(this@MainActivity, SpiritualLibraryActivity::class.java)) }
                        ) { Text("Bibliothèque spirituelle") }
                    }
                }

                SectionTitle("Récurrence")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Intervalle fixe : 15 minutes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Un seul compteur additionne en dur le temps de présence au premier plan dans toutes les applications cibles. Changer de cible ne remet jamais les 15 minutes ni les 90 minutes à zéro.")
                        Text("Toutes les 15 minutes : 1 page • Toutes les 90 minutes : 10 pages à la place de la sixième pause.")
                        Text(
                            if (remainingIntervalMs > 0L) {
                                "Cumul actuel : ${formatDashboardDuration(intervalPresenceMs)} / 15 min • prochaine pause dans ${formatDashboardDuration(remainingIntervalMs)}"
                            } else {
                                when (usageProgress.pendingLevel) {
                                    ChallengeLevel.MORNING -> "Filtre matinal en attente"
                                    ChallengeLevel.MICRO -> "Lecture d’une page en attente"
                                    ChallengeLevel.HIZB -> "Lecture du Hizb en attente"
                                    null -> "Cycle prêt"
                                }
                            },
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SectionTitle("Installation & mise à jour")
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("APK privée signée", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("Une mise à jour signée avec le même certificat s’installe directement par-dessus la version actuelle : inutile de désinstaller l’application.")
                        Text(
                            "Quran Safeguard reste volontairement hors ligne et ne vérifie pas les nouvelles versions sur Internet. La disponibilité d’une nouvelle APK doit donc être communiquée séparément.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = { shareInstallationGuide() }) {
                            Text("Partager le guide d’installation")
                        }
                    }
                }

                HorizontalDivider()
                Text("Réglages enregistrés automatiquement ✓", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(
                    "Version " + (runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "—") + " • installation privée",
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
private fun AccessibilityDisclosureScreen(onAccept: () -> Unit, onLater: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
            Text("QURAN SAFEGUARD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("Avant d’activer la protection", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            Text("Quran Safeguard utilise le service d’accessibilité uniquement pour détecter les applications cibles au premier plan et arrêter leur compteur dès que vous les quittez. Lorsqu’une application choisie est détectée, Quran Safeguard affiche immédiatement la pause Quran au-dessus de cette application.")
            Spacer(Modifier.height(12.dp))
            Text("Le contenu affiché à l’écran n’est pas lu, tes saisies ne sont pas enregistrées et les applications hors cible ne sont ni classées ni conservées. Le service peut être désactivé à tout moment dans les Paramètres Android.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = onAccept) { Text("J’accepte et je continue") }
            Spacer(Modifier.height(10.dp))
            SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onLater) { Text("Plus tard") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
fun QuranSafeguardTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = SafeguardInk,
        onPrimary = SafeguardSurface,
        primaryContainer = SafeguardNeutralContainer,
        onPrimaryContainer = SafeguardInk,
        secondary = SafeguardInk,
        onSecondary = SafeguardSurface,
        secondaryContainer = Color(0xFFF2F0EA),
        onSecondaryContainer = SafeguardInk,
        tertiary = SafeguardInk,
        onTertiary = SafeguardSurface,
        tertiaryContainer = Color(0xFFE8E6E0),
        onTertiaryContainer = SafeguardInk,
        background = SafeguardAppBackground,
        onBackground = SafeguardInk,
        surface = SafeguardSurface,
        surfaceVariant = Color(0xFFF2F0EA),
        onSurface = SafeguardInk,
        onSurfaceVariant = SafeguardSecondaryText,
        outline = SafeguardOutline,
        outlineVariant = Color(0xFFD6D5D0),
        error = Color(0xFF30302D),
        onError = SafeguardSurface,
        errorContainer = Color(0xFFE8E6E0),
        onErrorContainer = SafeguardInk
    )
    MaterialTheme(colorScheme = colors, shapes = SafeguardShapes, typography = SafeguardTypography, content = content)
}

private fun dailyReadingMessage(summary: DailyReadingSummary): String =
    if (summary.pages > 0) "Belle progression aujourd’hui." else "Chaque page compte. Votre prochaine lecture vous attend."

private fun readingTrendLabel(current: Long, previous: Long): String {
    if (current <= 0L || previous <= 0L) return ""
    val percent = ((previous - current) * 100L / previous).toInt()
    return when {
        percent >= 5 -> "Votre temps moyen est plus court de $percent% cette semaine — sans objectif de vitesse."
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
