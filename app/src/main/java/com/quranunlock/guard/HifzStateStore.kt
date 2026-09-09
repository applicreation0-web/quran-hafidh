package com.applicreation0.quransafeguard

import android.content.Context
import java.time.LocalDate
import java.util.Base64

data class HifzState(
    val schema: Int = HifzStateCodec.SCHEMA,
    val tasks: List<HifzTask> = emptyList()
) {
    init {
        require(schema == HifzStateCodec.SCHEMA) { "Unsupported Hifz state schema: $schema" }
        require(tasks.map { it.id }.distinct().size == tasks.size) {
            "Hifz task ids must be unique."
        }
    }
}

data class HifzLoadResult(
    val state: HifzState,
    val corrupted: Boolean
)

/**
 * Small deterministic codec kept independent from reader109 and GuardPrefs.
 * The first line is the schema; each following line is one stable Hifz task.
 */
object HifzStateCodec {
    const val SCHEMA = 1
    private const val SEP = "|"

    fun encode(state: HifzState): String = buildString {
        append(SCHEMA).append('\n')
        state.tasks.sortedBy { it.id }.forEach { task ->
            append(encodeText(task.id)).append(SEP)
            append(task.track.name).append(SEP)
            append(task.originalScheduledDate.toEpochDay()).append(SEP)
            append(task.scheduledDate.toEpochDay()).append(SEP)
            append(encodeText(task.cursor)).append(SEP)
            append(task.quota).append(SEP)
            append(task.status.name).append('\n')
        }
    }

    fun decode(raw: String): HifzState {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.isNotEmpty()) { "Missing Hifz state." }
        require(lines.first().toIntOrNull() == SCHEMA) { "Unsupported Hifz state schema." }

        val tasks = lines.drop(1).map { line ->
            val fields = line.split(SEP)
            require(fields.size == 7) { "Malformed Hifz task." }
            HifzTask(
                id = decodeText(fields[0]),
                track = HifzTrack.valueOf(fields[1]),
                originalScheduledDate = LocalDate.ofEpochDay(fields[2].toLong()),
                scheduledDate = LocalDate.ofEpochDay(fields[3].toLong()),
                cursor = decodeText(fields[4]),
                quota = fields[5].toInt(),
                status = HifzTaskStatus.valueOf(fields[6])
            )
        }
        return HifzState(tasks = tasks)
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
    internal const val FILE = "hifz_01010"
    private const val KEY_STATE = "state_v1"

    fun load(context: Context): HifzLoadResult {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_STATE, null)
            ?: return HifzLoadResult(HifzState(), corrupted = false)

        return runCatching { HifzStateCodec.decode(raw) }
            .fold(
                onSuccess = { HifzLoadResult(it, corrupted = false) },
                onFailure = { HifzLoadResult(HifzState(), corrupted = true) }
            )
    }

    fun save(context: Context, state: HifzState): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATE, HifzStateCodec.encode(state))
            .commit()

    @Synchronized
    fun upsertTask(context: Context, task: HifzTask): Boolean {
        val loaded = load(context)
        if (loaded.corrupted) return false
        val existing = loaded.state.tasks.associateBy { it.id }.toMutableMap()
        existing[task.id] = task
        return save(context, HifzState(tasks = existing.values.sortedBy { it.id }))
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
        return save(context, HifzState(tasks = updated))
    }
}
