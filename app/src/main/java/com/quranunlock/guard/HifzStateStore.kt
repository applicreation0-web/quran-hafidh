package com.applicreation0.quransafeguard

import android.content.Context
import java.time.LocalDate
import java.util.Base64

data class HifzTaskProgress(
    val taskId: String,
    val stepIndex: Int = 0,
    val stepProgress: HifzStepProgress? = null,
    val completed: Boolean = false
) {
    init {
        require(taskId.isNotBlank())
        require(stepIndex >= 0)
        require(stepProgress == null || stepProgress.stepId.isNotBlank())
    }
}

data class HifzState(
    val schema: Int = HifzStateCodec.SCHEMA,
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
 * T records contain a typed Quran cursor; P records contain training progress.
 */
object HifzStateCodec {
    const val SCHEMA = 2
    private const val SEP = "|"
    private const val TASK = "T"
    private const val PROGRESS = "P"
    private const val NONE = "-"

    fun encode(state: HifzState): String = buildString {
        append(SCHEMA).append('\n')
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
            append(progress.stepIndex).append(SEP)
            append(step?.stepId?.let(::encodeText) ?: NONE).append(SEP)
            append(step?.repetitions ?: 0).append(SEP)
            append(step?.consecutiveSuccesses ?: 0).append(SEP)
            append(step?.revealCount ?: 0).append(SEP)
            append(step?.assistedSinceLastAttempt ?: false).append(SEP)
            append(progress.completed).append('\n')
        }
    }

    fun decode(raw: String): HifzState {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing Hifz state." }
        require(lines.first().toIntOrNull() == SCHEMA) { "Unsupported Hifz state schema." }

        val tasks = mutableListOf<HifzTask>()
        val progress = linkedMapOf<String, HifzTaskProgress>()

        lines.drop(1).forEach { line ->
            val fields = line.split(SEP)
            when (fields.firstOrNull()) {
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
                    require(fields.size == 9) { "Malformed Hifz progress." }
                    val taskId = decodeText(fields[1])
                    val step = if (fields[3] == NONE) null else HifzStepProgress(
                        stepId = decodeText(fields[3]),
                        repetitions = fields[4].toInt(),
                        consecutiveSuccesses = fields[5].toInt(),
                        revealCount = fields[6].toInt(),
                        assistedSinceLastAttempt = fields[7].toBooleanStrict()
                    )
                    require(taskId !in progress) { "Duplicate Hifz progress record." }
                    progress[taskId] = HifzTaskProgress(
                        taskId = taskId,
                        stepIndex = fields[2].toInt(),
                        stepProgress = step,
                        completed = fields[8].toBooleanStrict()
                    )
                }

                else -> error("Unknown Hifz state record.")
            }
        }

        return HifzState(tasks = tasks, progressByTask = progress)
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}

/**
 * Hifz uses its own SharedPreferences file. Free reader memorisation state is never read
 * or written here, which prevents either feature from silently changing the other.
 */
object HifzStateStore {
    internal const val FILE = QuranPersistenceNamespaces.HIFZ
    private const val KEY_STATE = "state_v2"
    private const val LEGACY_KEY_STATE = "state_v1"

    fun load(context: Context): HifzLoadResult {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STATE, null)
        if (raw == null) {
            // An unreleased schema-1 state is ambiguous because its cursor was free text.
            // Never silently replace it with an empty schema-2 state.
            if (prefs.contains(LEGACY_KEY_STATE)) {
                return HifzLoadResult(HifzState(), corrupted = true)
            }
            return HifzLoadResult(HifzState(), corrupted = false)
        }

        return runCatching { HifzStateCodec.decode(raw) }
            .fold(
                onSuccess = { state ->
                    if (HifzMushafCursorVerifier.allCoherent(context, state.tasks.map(HifzTask::cursor))) {
                        HifzLoadResult(state, corrupted = false)
                    } else {
                        HifzLoadResult(HifzState(), corrupted = true)
                    }
                },
                onFailure = { HifzLoadResult(HifzState(), corrupted = true) }
            )
    }

    private fun save(context: Context, state: HifzState): Boolean {
        if (!HifzMushafCursorVerifier.allCoherent(context, state.tasks.map(HifzTask::cursor))) {
            return false
        }
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATE, HifzStateCodec.encode(state))
            .commit()
    }

    @Synchronized
    fun upsertTask(context: Context, task: HifzTask): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val existing = loaded.state.tasks.associateBy { it.id }.toMutableMap()
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
        if (loaded.state.tasks.none { it.id == taskId }) return false
        val before = loaded.state.progressByTask[taskId] ?: HifzTaskProgress(taskId = taskId)
        val after = transform(before)
        require(after.taskId == taskId) { "Hifz progress cannot change task identity." }
        val updated = loaded.state.progressByTask.toMutableMap()
        updated[taskId] = after
        return save(context, loaded.state.copy(progressByTask = updated))
    }

    /**
     * Persists the explicit training-complete -> schedule-complete transition as one
     * complete state replacement. Corrupted or incomplete state is never overwritten.
     */
    @Synchronized
    fun completeTask(context: Context, taskId: String): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val completed = runCatching {
            HifzJourneyCoordinator.completeTask(loaded.state, taskId)
        }.getOrNull() ?: return false
        return save(context, completed)
    }
}
