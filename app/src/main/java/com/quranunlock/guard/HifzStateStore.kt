package com.applicreation0.quransafeguard

import android.content.Context
import java.time.LocalDate
import java.util.Base64

data class HifzTaskProgress(
    val taskId: String,
    val segmentIndex: Int = 0,
    val stepIndex: Int = 0,
    val stepProgress: HifzStepProgress? = null,
    val totalRevealCount: Int = 0,
    val totalIncorrectAttempts: Int = 0,
    val activeSeconds: Long = 0L,
    val completed: Boolean = false
) {
    init {
        require(taskId.isNotBlank())
        require(segmentIndex >= 0)
        require(stepIndex >= 0)
        require(stepProgress == null || stepProgress.stepId.isNotBlank())
        require(totalRevealCount >= 0)
        require(totalIncorrectAttempts >= 0)
        require(activeSeconds >= 0L)
    }
}

data class HifzState(
    val schema: Int = HifzStateCodec.SCHEMA,
    val journeyConfig: HifzJourneyConfig = HifzJourneyConfig(),
    val planningDates: Set<LocalDate> = emptySet(),
    val tasks: List<HifzTask> = emptyList(),
    val progressByTask: Map<String, HifzTaskProgress> = emptyMap()
) {
    init {
        require(schema == HifzStateCodec.SCHEMA) { "Unsupported Hifz state schema: $schema" }
        require(tasks.map { it.id }.distinct().size == tasks.size) {
            "Hifz task ids must be unique."
        }
        val taskIds = tasks.map { it.id }.toSet()
        require(progressByTask.keys.all { it in taskIds }) {
            "Hifz progress cannot reference an unknown task."
        }
        require(progressByTask.all { (id, progress) -> id == progress.taskId }) {
            "Hifz progress map key must match its task identity."
        }
    }
}

data class HifzLoadResult(
    val state: HifzState,
    val corrupted: Boolean
)

/**
 * Deterministic, versioned codec kept independent from reader109 and GuardPrefs.
 * C stores Sabqi bounds, measured paces and available time. I records ordered Itqan
 * intervals. D records dates already considered by the daily planner. T records schedule
 * tasks and P records segment-aware training progress plus local training metrics.
 */
object HifzStateCodec {
    const val SCHEMA = 6
    private const val SEP = "|"
    private const val CONFIG = "C"
    private const val ITQAN_INTERVAL = "I"
    private const val PLANNING_DATE = "D"
    private const val TASK = "T"
    private const val PROGRESS = "P"
    private const val NONE = "-"

    fun encode(state: HifzState): String = buildString {
        append(SCHEMA).append('\n')
        appendConfig(state.journeyConfig)
        state.planningDates.sorted().forEach { date ->
            append(PLANNING_DATE).append(SEP).append(date.toEpochDay()).append('\n')
        }
        state.tasks.sortedBy { it.id }.forEach { task ->
            val cursor = task.cursor
            append(TASK).append(SEP)
            append(encodeText(task.id)).append(SEP)
            append(task.track.name).append(SEP)
            append(task.originalScheduledDate.toEpochDay()).append(SEP)
            append(task.scheduledDate.toEpochDay()).append(SEP)
            append(cursor.start.surah).append(SEP)
            append(cursor.start.ayah).append(SEP)
            append(cursor.end.surah).append(SEP)
            append(cursor.end.ayah).append(SEP)
            append(cursor.startPage).append(SEP)
            append(cursor.endPage).append(SEP)
            append(task.quota).append(SEP)
            append(task.status.name).append('\n')
        }
        state.progressByTask.toSortedMap().forEach { (_, progress) ->
            val step = progress.stepProgress
            append(PROGRESS).append(SEP)
            append(encodeText(progress.taskId)).append(SEP)
            append(progress.segmentIndex).append(SEP)
            append(progress.stepIndex).append(SEP)
            append(step?.stepId?.let(::encodeText) ?: NONE).append(SEP)
            append(step?.repetitions ?: 0).append(SEP)
            append(step?.consecutiveSuccesses ?: 0).append(SEP)
            append(step?.revealCount ?: 0).append(SEP)
            append(step?.assistedSinceLastAttempt ?: false).append(SEP)
            append(progress.totalRevealCount).append(SEP)
            append(progress.totalIncorrectAttempts).append(SEP)
            append(progress.activeSeconds).append(SEP)
            append(progress.completed).append('\n')
        }
    }

    private fun StringBuilder.appendConfig(config: HifzJourneyConfig) {
        append(CONFIG).append(SEP)
        val sabqiFields = config.bounds?.sabqi?.let { range ->
            listOf(
                range.start.surah.toString(),
                range.start.ayah.toString(),
                range.end.surah.toString(),
                range.end.ayah.toString()
            )
        } ?: List(4) { NONE }
        sabqiFields.forEach { append(it).append(SEP) }
        append(encodeOptionalDouble(config.pace.sabqiMinutesPerPage)).append(SEP)
        append(encodeOptionalDouble(config.pace.itqanMinutesPerPage)).append(SEP)
        append(encodeOptionalDouble(config.pace.murajaahMinutesPerPage)).append(SEP)
        append(encodeOptionalInt(config.availableMinutes.sabqi)).append(SEP)
        append(encodeOptionalInt(config.availableMinutes.itqan)).append(SEP)
        append(encodeOptionalInt(config.availableMinutes.murajaah)).append('\n')

        config.bounds?.itqan.orEmpty().forEachIndexed { index, range ->
            append(ITQAN_INTERVAL).append(SEP)
            append(index).append(SEP)
            append(range.start.surah).append(SEP)
            append(range.start.ayah).append(SEP)
            append(range.end.surah).append(SEP)
            append(range.end.ayah).append('\n')
        }
    }

    fun decode(raw: String): HifzState {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing Hifz state." }
        require(lines.first().toIntOrNull() == SCHEMA) { "Unsupported Hifz state schema." }

        var configSeen = false
        var declaredSabqi: HifzVerseRange? = null
        var boundsDeclared: Boolean? = null
        var pace: HifzPaceProfile? = null
        var availableMinutes: HifzAvailableMinutes? = null
        val itqanIntervals = sortedMapOf<Int, HifzVerseRange>()
        val planningDates = linkedSetOf<LocalDate>()
        val tasks = mutableListOf<HifzTask>()
        val progress = linkedMapOf<String, HifzTaskProgress>()

        lines.drop(1).forEach { line ->
            val fields = line.split(SEP)
            when (fields.firstOrNull()) {
                CONFIG -> {
                    require(!configSeen) { "Duplicate Hifz config record." }
                    require(fields.size == 11) { "Malformed Hifz config." }
                    configSeen = true
                    val rawSabqi = fields.subList(1, 5)
                    boundsDeclared = when {
                        rawSabqi.all { it == NONE } -> false
                        rawSabqi.any { it == NONE } -> error("Incomplete Sabqi journey bounds.")
                        else -> {
                            declaredSabqi = HifzVerseRange(
                                QuranVerseRef(rawSabqi[0].toInt(), rawSabqi[1].toInt()),
                                QuranVerseRef(rawSabqi[2].toInt(), rawSabqi[3].toInt())
                            )
                            true
                        }
                    }
                    pace = HifzPaceProfile(
                        sabqiMinutesPerPage = decodeOptionalDouble(fields[5]),
                        itqanMinutesPerPage = decodeOptionalDouble(fields[6]),
                        murajaahMinutesPerPage = decodeOptionalDouble(fields[7])
                    )
                    availableMinutes = HifzAvailableMinutes(
                        sabqi = decodeOptionalInt(fields[8]),
                        itqan = decodeOptionalInt(fields[9]),
                        murajaah = decodeOptionalInt(fields[10])
                    )
                }

                ITQAN_INTERVAL -> {
                    require(fields.size == 6) { "Malformed Itqan interval." }
                    val index = fields[1].toInt()
                    require(index >= 0) { "Invalid Itqan interval index." }
                    require(index !in itqanIntervals) { "Duplicate Itqan interval index." }
                    itqanIntervals[index] = HifzVerseRange(
                        QuranVerseRef(fields[2].toInt(), fields[3].toInt()),
                        QuranVerseRef(fields[4].toInt(), fields[5].toInt())
                    )
                }

                PLANNING_DATE -> {
                    require(fields.size == 2) { "Malformed Hifz planning-date record." }
                    val date = LocalDate.ofEpochDay(fields[1].toLong())
                    require(planningDates.add(date)) { "Duplicate Hifz planning date." }
                }

                TASK -> {
                    require(fields.size == 13) { "Malformed Hifz task." }
                    tasks += HifzTask(
                        id = decodeText(fields[1]),
                        track = HifzTrack.valueOf(fields[2]),
                        originalScheduledDate = LocalDate.ofEpochDay(fields[3].toLong()),
                        scheduledDate = LocalDate.ofEpochDay(fields[4].toLong()),
                        cursor = HifzCursor(
                            start = QuranVerseRef(fields[5].toInt(), fields[6].toInt()),
                            end = QuranVerseRef(fields[7].toInt(), fields[8].toInt()),
                            startPage = fields[9].toInt(),
                            endPage = fields[10].toInt()
                        ),
                        quota = fields[11].toInt(),
                        status = HifzTaskStatus.valueOf(fields[12])
                    )
                }

                PROGRESS -> {
                    require(fields.size == 13) { "Malformed Hifz progress." }
                    val taskId = decodeText(fields[1])
                    val step = if (fields[4] == NONE) null else HifzStepProgress(
                        stepId = decodeText(fields[4]),
                        repetitions = fields[5].toInt(),
                        consecutiveSuccesses = fields[6].toInt(),
                        revealCount = fields[7].toInt(),
                        assistedSinceLastAttempt = fields[8].toBooleanStrict()
                    )
                    require(taskId !in progress) { "Duplicate Hifz progress record." }
                    progress[taskId] = HifzTaskProgress(
                        taskId = taskId,
                        segmentIndex = fields[2].toInt(),
                        stepIndex = fields[3].toInt(),
                        stepProgress = step,
                        totalRevealCount = fields[9].toInt(),
                        totalIncorrectAttempts = fields[10].toInt(),
                        activeSeconds = fields[11].toLong(),
                        completed = fields[12].toBooleanStrict()
                    )
                }

                else -> error("Unknown Hifz state record.")
            }
        }

        require(configSeen) { "Missing Hifz config record." }
        val expectedIndexes = (0 until itqanIntervals.size).toList()
        require(itqanIntervals.keys.toList() == expectedIndexes) {
            "Itqan interval indexes must be contiguous and ordered."
        }
        val bounds = when (requireNotNull(boundsDeclared)) {
            false -> {
                require(itqanIntervals.isEmpty()) {
                    "Itqan intervals cannot exist without declared journey bounds."
                }
                null
            }
            true -> HifzJourneyBounds(
                sabqi = requireNotNull(declaredSabqi),
                itqan = itqanIntervals.values.toList()
            )
        }

        return HifzState(
            journeyConfig = HifzJourneyConfig(
                bounds = bounds,
                pace = requireNotNull(pace),
                availableMinutes = requireNotNull(availableMinutes)
            ),
            planningDates = planningDates,
            tasks = tasks,
            progressByTask = progress
        )
    }

    private fun encodeOptionalDouble(value: Double?): String = value?.toString() ?: NONE
    private fun encodeOptionalInt(value: Int?): String = value?.toString() ?: NONE

    private fun decodeOptionalDouble(value: String): Double? =
        if (value == NONE) null else value.toDouble()

    private fun decodeOptionalInt(value: String): Int? =
        if (value == NONE) null else value.toInt()

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}

object HifzStateStore {
    internal const val FILE = QuranPersistenceNamespaces.HIFZ
    private const val KEY_STATE = "state_v6"
    private const val LEGACY_KEY_STATE_V5 = "state_v5"
    private const val LEGACY_KEY_STATE_V4 = "state_v4"
    private const val LEGACY_KEY_STATE_V3 = "state_v3"
    private const val LEGACY_KEY_STATE_V2 = "state_v2"
    private const val LEGACY_KEY_STATE_V1 = "state_v1"

    fun load(context: Context): HifzLoadResult {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STATE, null)
        if (raw == null) {
            if (
                prefs.contains(LEGACY_KEY_STATE_V5) ||
                prefs.contains(LEGACY_KEY_STATE_V4) ||
                prefs.contains(LEGACY_KEY_STATE_V3) ||
                prefs.contains(LEGACY_KEY_STATE_V2) ||
                prefs.contains(LEGACY_KEY_STATE_V1)
            ) {
                return HifzLoadResult(HifzState(), corrupted = true)
            }
            return HifzLoadResult(HifzState(), corrupted = false)
        }

        return runCatching { HifzStateCodec.decode(raw) }
            .fold(
                onSuccess = { state ->
                    if (
                        HifzMushafCursorVerifier.allCoherent(context, state.tasks.map { it.cursor }) &&
                        isSemanticallyCoherent(context, state)
                    ) {
                        HifzLoadResult(state, corrupted = false)
                    } else {
                        HifzLoadResult(HifzState(), corrupted = true)
                    }
                },
                onFailure = { HifzLoadResult(HifzState(), corrupted = true) }
            )
    }

    internal fun save(context: Context, state: HifzState): Boolean {
        if (!HifzMushafCursorVerifier.allCoherent(context, state.tasks.map { it.cursor })) {
            return false
        }
        if (!isSemanticallyCoherent(context, state)) return false
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATE, HifzStateCodec.encode(state))
            .commit()
    }

    @Synchronized
    fun updateJourneyConfig(
        context: Context,
        transform: (HifzJourneyConfig) -> HifzJourneyConfig
    ): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        return save(
            context,
            loaded.state.copy(journeyConfig = transform(loaded.state.journeyConfig))
        )
    }

    @Synchronized
    fun upsertTask(context: Context, task: HifzTask): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val existing = loaded.state.tasks.associateBy { it.id }.toMutableMap()
        val previous = existing[task.id]
        if (previous?.status == HifzTaskStatus.COMPLETED && task != previous) return false
        existing[task.id] = task
        return save(
            context,
            loaded.state.copy(tasks = existing.values.sortedBy { it.id })
        )
    }

    @Synchronized
    fun updateTask(
        context: Context,
        taskId: String,
        transform: (HifzTask) -> HifzTask
    ): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val index = loaded.state.tasks.indexOfFirst { it.id == taskId }
        if (index < 0) return false
        val updated = loaded.state.tasks.toMutableList()
        val before = updated[index]
        val after = transform(before)
        require(after.id == before.id) { "A Hifz task update cannot change its identity." }
        if (before.status == HifzTaskStatus.COMPLETED && after != before) return false
        updated[index] = after
        return save(context, loaded.state.copy(tasks = updated))
    }

    @Synchronized
    fun updateProgress(
        context: Context,
        taskId: String,
        transform: (HifzTaskProgress) -> HifzTaskProgress
    ): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val task = loaded.state.tasks.firstOrNull { it.id == taskId } ?: return false
        if (task.status == HifzTaskStatus.COMPLETED) return false
        val segmentCount = segmentCount(context, task) ?: return false
        val before = loaded.state.progressByTask[taskId]
            ?: HifzTrainingEngine.initial(task, segmentCount)
        val after = transform(before)
        require(after.taskId == taskId) { "Hifz progress cannot change task identity." }
        if (!HifzTrainingEngine.isSemanticallyCoherent(task, after, segmentCount)) return false
        val updated = loaded.state.progressByTask.toMutableMap()
        updated[taskId] = after
        return save(context, loaded.state.copy(progressByTask = updated))
    }

    @Synchronized
    fun completeTask(context: Context, taskId: String): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val task = loaded.state.tasks.firstOrNull { it.id == taskId } ?: return false
        val segmentCount = segmentCount(context, task) ?: return false
        val completed = runCatching {
            HifzJourneyCoordinator.completeTask(loaded.state, taskId, segmentCount)
        }.getOrNull() ?: return false
        return save(context, completed)
    }

    @Synchronized
    fun replaceState(context: Context, transform: (HifzState) -> HifzState): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        return save(context, transform(loaded.state))
    }

    internal fun segmentCount(context: Context, task: HifzTask): Int? = runCatching {
        val geometry = HifzGeometryAssetLoader.load(context)
        HifzGeometryPolicy.segment(
            geometry,
            HifzVerseRange(task.cursor.start, task.cursor.end)
        ).size.also { require(it > 0) }
    }.getOrNull()

    private fun isSemanticallyCoherent(context: Context, state: HifzState): Boolean = runCatching {
        val bounds = state.journeyConfig.bounds
        require(state.tasks.isEmpty() || bounds != null) {
            "Hifz tasks cannot exist before journey bounds are configured."
        }
        val geometry = HifzGeometryAssetLoader.load(context)

        state.tasks.forEach { task ->
            require(HifzSchedulePolicy.defaultTrackFor(task.originalScheduledDate.dayOfWeek) == task.track) {
                "Hifz task original date does not match its track."
            }
            require(HifzSchedulePolicy.defaultTrackFor(task.scheduledDate.dayOfWeek) == task.track) {
                "Hifz task scheduled date does not match its track."
            }
            require(taskRangeIsInsideConfiguredCorpus(task, requireNotNull(bounds))) {
                "Hifz task lies outside configured journey bounds."
            }

            val target = HifzVerseRange(task.cursor.start, task.cursor.end)
            val segmentCount = HifzGeometryPolicy.segment(geometry, target).size
            require(segmentCount > 0) { "Hifz task has no real Mushaf geometry segment." }

            val progress = state.progressByTask[task.id]
            if (progress != null) {
                require(HifzTrainingEngine.isSemanticallyCoherent(task, progress, segmentCount)) {
                    "Hifz training progress is inconsistent with its task protocol."
                }
            }
            if (task.status == HifzTaskStatus.COMPLETED) {
                require(progress?.completed == true) {
                    "A completed Hifz task requires completed coherent training progress."
                }
            }
        }
        true
    }.getOrDefault(false)

    private fun taskRangeIsInsideConfiguredCorpus(
        task: HifzTask,
        bounds: HifzJourneyBounds
    ): Boolean {
        val target = HifzVerseRange(task.cursor.start, task.cursor.end)
        fun inside(container: HifzVerseRange): Boolean =
            container.contains(target.start) && container.contains(target.end)

        return when (task.track) {
            HifzTrack.SABQI -> inside(bounds.sabqi)
            HifzTrack.ITQAN -> bounds.itqan.any(::inside)
            HifzTrack.MURAJAAH -> inside(bounds.sabqi) || bounds.itqan.any(::inside)
        }
    }
}
