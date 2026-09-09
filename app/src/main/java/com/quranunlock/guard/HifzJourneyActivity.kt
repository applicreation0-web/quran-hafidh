package com.applicreation0.quransafeguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Structured Hifz surface, independent from free Memorisation. */
class HifzJourneyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { QuranSafeguardTheme { HifzJourneyScreen() } }
    }

    @Composable
    private fun HifzJourneyScreen() {
        var refresh by remember { mutableIntStateOf(0) }
        val load = remember(refresh) { HifzStateStore.load(this) }
        val today = LocalDate.now()
        var message by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Parcours Hifz",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Sabqi lundi/mercredi/vendredi • Itqān mardi/jeudi • Murājaʿah samedi/dimanche",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Mémorisation libre reste indépendante : travailler librement un passage ne déplace jamais ce parcours.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (load.corrupted) {
                Card(modifier = Modifier.fillMaxWidth(), shape = SafeguardShapes.large) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("État Hifz non exploitable", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Text("Safeguard n’a ni validé ni déplacé de séance. Reconfigurez le parcours avant de continuer.")
                    }
                }
            }

            val config = load.state.journeyConfig
            if (config.bounds == null || config.pace.sabqiMinutesPerPage == null ||
                config.pace.itqanMinutesPerPage == null || config.pace.murajaahMinutesPerPage == null ||
                config.availableMinutes.sabqi == null || config.availableMinutes.itqan == null ||
                config.availableMinutes.murajaah == null
            ) {
                SetupCard(
                    existing = config,
                    onSaved = { newConfig ->
                        val ok = if (load.corrupted) false else HifzStateStore.updateJourneyConfig(this) { newConfig }
                        message = if (ok) "Configuration Hifz enregistrée." else "Configuration refusée : état ou bornes invalides."
                        if (ok) refresh++
                    }
                )
            } else {
                val plannedState = remember(refresh, today) {
                    runCatching {
                        val geometry = HifzGeometryAssetLoader.load(this)
                        HifzDailyPlanner.planDate(load.state, today, geometry)
                    }.getOrNull()
                }
                if (plannedState != null && plannedState != load.state) {
                    if (HifzStateStore.replaceState(this) { plannedState }) {
                        refresh++
                        return@Column
                    }
                }

                val normalizedTasks = load.state.tasks.map { HifzSchedulePolicy.markOverdue(it, today) }
                if (normalizedTasks != load.state.tasks) {
                    if (HifzStateStore.replaceState(this) { it.copy(tasks = normalizedTasks) }) {
                        refresh++
                        return@Column
                    }
                }

                val task = HifzSchedulePolicy.nextTask(today, normalizedTasks)
                if (task == null) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = SafeguardShapes.large) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Aucune séance à effectuer maintenant", fontWeight = FontWeight.SemiBold)
                            Text("Aucune charge supplémentaire n’est créée automatiquement.")
                        }
                    }
                } else {
                    TaskCard(
                        task = task,
                        state = load.state.copy(tasks = normalizedTasks),
                        onRefresh = { refresh++ },
                        onMessage = { message = it }
                    )
                }
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { finish() }) {
                Text("Retour")
            }
        }
    }

    @Composable
    private fun SetupCard(existing: HifzJourneyConfig, onSaved: (HifzJourneyConfig) -> Unit) {
        val bounds = existing.bounds
        var sabqiStartS by remember { mutableStateOf(bounds?.sabqi?.start?.surah?.toString() ?: "") }
        var sabqiStartA by remember { mutableStateOf(bounds?.sabqi?.start?.ayah?.toString() ?: "") }
        var sabqiEndS by remember { mutableStateOf(bounds?.sabqi?.end?.surah?.toString() ?: "") }
        var sabqiEndA by remember { mutableStateOf(bounds?.sabqi?.end?.ayah?.toString() ?: "") }
        var itqanStartS by remember { mutableStateOf(bounds?.itqan?.firstOrNull()?.start?.surah?.toString() ?: "") }
        var itqanStartA by remember { mutableStateOf(bounds?.itqan?.firstOrNull()?.start?.ayah?.toString() ?: "") }
        var itqanEndS by remember { mutableStateOf(bounds?.itqan?.firstOrNull()?.end?.surah?.toString() ?: "") }
        var itqanEndA by remember { mutableStateOf(bounds?.itqan?.firstOrNull()?.end?.ayah?.toString() ?: "") }
        var sabqiPace by remember { mutableStateOf(existing.pace.sabqiMinutesPerPage?.toString() ?: "") }
        var itqanPace by remember { mutableStateOf(existing.pace.itqanMinutesPerPage?.toString() ?: "") }
        var murajaahPace by remember { mutableStateOf(existing.pace.murajaahMinutesPerPage?.toString() ?: "") }
        var sabqiMinutes by remember { mutableStateOf(existing.availableMinutes.sabqi?.toString() ?: "") }
        var itqanMinutes by remember { mutableStateOf(existing.availableMinutes.itqan?.toString() ?: "") }
        var murajaahMinutes by remember { mutableStateOf(existing.availableMinutes.murajaah?.toString() ?: "") }
        var localError by remember { mutableStateOf<String?>(null) }

        Card(modifier = Modifier.fillMaxWidth(), shape = SafeguardShapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Configuration requise", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Aucune vitesse ni durée n’est inventée. Indiquez vos bornes, votre rythme observé et votre temps disponible.",
                    style = MaterialTheme.typography.bodySmall
                )
                VersePair("Début Sabqi", sabqiStartS, sabqiStartA, { sabqiStartS = it }, { sabqiStartA = it })
                VersePair("Fin Sabqi", sabqiEndS, sabqiEndA, { sabqiEndS = it }, { sabqiEndA = it })
                VersePair("Début Itqān", itqanStartS, itqanStartA, { itqanStartS = it }, { itqanStartA = it })
                VersePair("Fin Itqān", itqanEndS, itqanEndA, { itqanEndS = it }, { itqanEndA = it })
                NumberField("Sabqi min/page observées", sabqiPace) { sabqiPace = it }
                NumberField("Itqān min/page observées", itqanPace) { itqanPace = it }
                NumberField("Murājaʿah min/page observées", murajaahPace) { murajaahPace = it }
                NumberField("Minutes disponibles Sabqi", sabqiMinutes) { sabqiMinutes = it }
                NumberField("Minutes disponibles Itqān", itqanMinutes) { itqanMinutes = it }
                NumberField("Minutes disponibles Murājaʿah", murajaahMinutes) { murajaahMinutes = it }
                localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    val candidate = runCatching {
                        HifzJourneyConfig(
                            bounds = HifzJourneyBounds(
                                sabqi = HifzVerseRange(
                                    QuranVerseRef(sabqiStartS.toInt(), sabqiStartA.toInt()),
                                    QuranVerseRef(sabqiEndS.toInt(), sabqiEndA.toInt())
                                ),
                                itqan = listOf(
                                    HifzVerseRange(
                                        QuranVerseRef(itqanStartS.toInt(), itqanStartA.toInt()),
                                        QuranVerseRef(itqanEndS.toInt(), itqanEndA.toInt())
                                    )
                                )
                            ),
                            pace = HifzPaceProfile(
                                sabqiMinutesPerPage = sabqiPace.toDouble(),
                                itqanMinutesPerPage = itqanPace.toDouble(),
                                murajaahMinutesPerPage = murajaahPace.toDouble()
                            ),
                            availableMinutes = HifzAvailableMinutes(
                                sabqi = sabqiMinutes.toInt(),
                                itqan = itqanMinutes.toInt(),
                                murajaah = murajaahMinutes.toInt()
                            )
                        )
                    }.getOrElse {
                        localError = "Valeurs invalides. Vérifiez les sourates, versets, durées et l’ordre des bornes."
                        return@SafeguardButton
                    }
                    localError = null
                    onSaved(candidate)
                }) { Text("Enregistrer le parcours") }
            }
        }
    }

    @Composable
    private fun TaskCard(
        task: HifzTask,
        state: HifzState,
        onRefresh: () -> Unit,
        onMessage: (String) -> Unit
    ) {
        val segmentCount = remember(task.id) { HifzStateStore.segmentCount(this, task) ?: 1 }
        val progress = state.progressByTask[task.id] ?: HifzTrainingEngine.initial(task, segmentCount)
        val step = HifzTrainingEngine.currentStep(task, progress, segmentCount)
        val suggested = if (task.status == HifzTaskStatus.OVERDUE) {
            HifzSchedulePolicy.suggestReplanDate(task, LocalDate.now(), state.tasks)
        } else null

        Card(modifier = Modifier.fillMaxWidth(), shape = SafeguardShapes.large) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(trackLabel(task.track), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${task.cursor.start.surah}:${task.cursor.start.ayah} → ${task.cursor.end.surah}:${task.cursor.end.ayah}")
                Text("Pages ${task.cursor.startPage}–${task.cursor.endPage} • quota ${task.quota} min")
                Text(if (task.status == HifzTaskStatus.OVERDUE) "En retard — aucune charge doublée" else "Séance du ${task.scheduledDate}")
                step?.let { Text("Étape : ${it.label}", fontWeight = FontWeight.SemiBold) }
                Text(
                    "Segment ${progress.segmentIndex + 1} • temps actif ${progress.activeSeconds}s • aides ${progress.totalRevealCount} • erreurs ${progress.totalIncorrectAttempts}",
                    style = MaterialTheme.typography.bodySmall
                )

                SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = {
                    startActivity(
                        Intent(this@HifzJourneyActivity, FreeQuranReaderActivity::class.java)
                            .putExtra(FreeQuranReaderActivity.EXTRA_PAGE, task.cursor.startPage)
                            .putExtra(FreeQuranReaderActivity.EXTRA_MEMORIZATION, true)
                    )
                }) { Text("Ouvrir le Muṣḥaf de séance") }

                if (!progress.completed) {
                    SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.updateProgress(this@HifzJourneyActivity, task.id) {
                            HifzTrainingEngine.attempt(task, it, false, segmentCount)
                        }
                        onMessage(if (ok) "Essai incorrect enregistré localement." else "Progression refusée.")
                        if (ok) onRefresh()
                    }) { Text("Incorrect") }
                    SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.updateProgress(this@HifzJourneyActivity, task.id) {
                            HifzTrainingEngine.attempt(task, it, true, segmentCount)
                        }
                        onMessage(if (ok) "Essai correct enregistré localement." else "Progression refusée.")
                        if (ok) onRefresh()
                    }) { Text("Correct") }
                    SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.updateProgress(this@HifzJourneyActivity, task.id) {
                            HifzTrainingEngine.reveal(task, it, segmentCount)
                        }
                        onMessage(if (ok) "Aide/reveal comptabilisé." else "Aide non enregistrée.")
                        if (ok) onRefresh()
                    }) { Text("J’ai utilisé une aide / reveal") }
                    SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.updateProgress(this@HifzJourneyActivity, task.id) {
                            HifzTrainingEngine.advanceIfValid(task, it, segmentCount)
                        }
                        onMessage(if (ok) "Validation évaluée selon le protocole." else "Étape non validable actuellement.")
                        if (ok) onRefresh()
                    }) { Text("Valider l’étape") }
                } else {
                    SafeguardButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.completeTask(this@HifzJourneyActivity, task.id)
                        onMessage(if (ok) "Séance Hifz terminée." else "La séance ne peut pas être clôturée.")
                        if (ok) onRefresh()
                    }) { Text("Clôturer la séance") }
                }

                if (suggested != null) {
                    SafeguardOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val ok = HifzStateStore.updateTask(this@HifzJourneyActivity, task.id) {
                            HifzSchedulePolicy.replan(it, suggested)
                        }
                        onMessage(if (ok) "Séance reportée explicitement au $suggested, quota inchangé." else "Report refusé.")
                        if (ok) onRefresh()
                    }) { Text("Reporter au prochain créneau : $suggested") }
                }
            }
        }
    }

    @Composable
    private fun VersePair(
        label: String,
        surah: String,
        ayah: String,
        onSurah: (String) -> Unit,
        onAyah: (String) -> Unit
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = surah,
                onValueChange = onSurah,
                label = { Text("Sourate") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = ayah,
                onValueChange = onAyah,
                label = { Text("Verset") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }

    @Composable
    private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValue,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }

    private fun trackLabel(track: HifzTrack): String = when (track) {
        HifzTrack.SABQI -> "Sabqi"
        HifzTrack.ITQAN -> "Itqān"
        HifzTrack.MURAJAAH -> "Murājaʿah"
    }
}
