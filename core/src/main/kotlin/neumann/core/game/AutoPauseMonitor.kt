package neumann.core.game

import neumann.core.data.AutoPauseConfig
import neumann.core.world.EventKind
import neumann.core.world.GameEvent

/**
 * Decides which new events stop a time skip. After an event pauses, the same
 * kind from the same source does not pause again for the suppression time.
 */
class AutoPauseMonitor(private val config: AutoPauseConfig) {
    private val lastPause = HashMap<String, Double>()

    fun pausing(events: List<GameEvent>): List<GameEvent> = events.filter { e ->
        if (e.kind !in config.pauseOn) return@filter false
        if (e.kind == EventKind.SYSTEM_CLAIMED || e.kind == EventKind.SHIP_LOST) return@filter true
        val key = "${e.kind}:${e.sourceId}"
        val last = lastPause[key]
        if (last != null && e.time - last < config.repeatSuppressionSeconds) return@filter false
        lastPause[key] = e.time
        true
    }
}
