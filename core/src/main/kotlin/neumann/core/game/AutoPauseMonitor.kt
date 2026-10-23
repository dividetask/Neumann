package neumann.core.game

import neumann.core.data.AutoPauseConfig
import neumann.core.world.EventKind
import neumann.core.world.GameEvent

/**
 * Decides which new events stop a time skip. Pausing requires new
 * information: an event does not pause if the same kind from the same source
 * happened within the suppression time before it.
 */
class AutoPauseMonitor(private val config: AutoPauseConfig) {
    private val lastSeen = HashMap<String, Double>()

    fun pausing(events: List<GameEvent>): List<GameEvent> = events.filter { e ->
        val key = "${e.kind}:${e.sourceId}"
        val last = lastSeen.put(key, e.time)
        when {
            e.kind !in config.pauseOn -> false
            e.kind == EventKind.SYSTEM_CLAIMED || e.kind == EventKind.SHIP_LOST -> true
            else -> last == null || e.time - last >= config.repeatSuppressionSeconds
        }
    }
}
