package com.applicreation0.quransafeguard

import android.content.Context

/**
 * Commits only foreground time observed by the structured Hifz reader. A failed or
 * corrupted state never receives invented time and completed tasks cannot accrue time.
 */
object HifzActiveTimeRecorder {
    fun record(context: Context, taskId: String, seconds: Long): Boolean {
        if (taskId.isBlank() || seconds <= 0L) return seconds == 0L
        return synchronized(HifzStateStore) {
            val loaded = HifzStateStore.load(context)
            if (loaded.corrupted) return@synchronized false
            val task = loaded.state.tasks.firstOrNull { it.id == taskId }
                ?: return@synchronized false
            if (task.status == HifzTaskStatus.COMPLETED) return@synchronized false
            val segmentCount = HifzStateStore.segmentCount(context, task)
                ?: return@synchronized false
            HifzStateStore.updateProgress(context, taskId) { progress ->
                HifzTrainingEngine.recordActiveSeconds(
                    task = task,
                    progress = progress,
                    seconds = seconds,
                    segmentCount = segmentCount
                )
            }
        }
    }
}
