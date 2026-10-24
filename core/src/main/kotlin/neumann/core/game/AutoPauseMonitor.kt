package neumann.core.game

import neumann.core.data.AutoPauseConfig
import neumann.core.world.EventKind
import neumann.core.world.GameEvent

/**
 * Decides which new events stop a time skip. Pausing requires new
 * information: an event does not pause if the same kind from the same source
 * happened within the suppression time before it, or if the same kind (from
 * any source) already paused within the group time — events arriving at
 * nearly the same time are one pause.
 */
class AutoPauseMonitor(private val config: AutoPauseConfig) {
    private val lastSeen = HashMap<String, Double>()
    private val lastPause = HashMap<EventKind, Double>()

    fun pausing(events: List<GameEvent>): List<GameEvent> {
        val batchStart = events.minOfOrNull { it.time } ?: return emptyList()
        return events.filter { e ->
            val key = "${e.kind}:${e.sourceId}"
            val last = lastSeen.put(key, e.time)
            val paused = lastPause[e.kind]
            val pauses = when {
                e.kind !in config.pauseOn -> false
                e.kind == EventKind.SYSTEM_CLAIMED || e.kind == EventKind.SHIP_LOST -> true
                last != null && e.time - last < config.repeatSuppressionSeconds -> false
                // Same kind already paused a moment ago (in an earlier batch): part of that news.
                paused != null && paused < batchStart && e.time - paused < config.groupSeconds -> false
                else -> true
            }
            if (pauses) lastPause[e.kind] = e.time
            pauses
        }
    }
}
