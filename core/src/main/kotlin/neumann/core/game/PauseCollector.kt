package neumann.core.game

import neumann.core.world.EventKind
import neumann.core.world.GameEvent

/**
 * Gathers reasons to pause that arrive at nearly the same time into one
 * pause. The first reason opens a window of [groupSeconds]; every reason
 * that arrives inside it joins the pause, which happens when the window
 * closes. Nothing is dropped. Claiming the system or losing the ship pauses
 * at once.
 */
class PauseCollector(private val groupSeconds: Double) {
    private val gathered = ArrayList<GameEvent>()
    private var closesAt = Double.POSITIVE_INFINITY

    /** Adds this step's reasons; returns the whole pause if it is due now, else null. */
    fun offer(reasons: List<GameEvent>, now: Double): List<GameEvent>? {
        if (reasons.isNotEmpty() && gathered.isEmpty()) closesAt = reasons.first().time + groupSeconds
        gathered.addAll(reasons)
        val ending = reasons.any { it.kind == EventKind.SYSTEM_CLAIMED || it.kind == EventKind.SHIP_LOST }
        return if (gathered.isNotEmpty() && (ending || now >= closesAt - 1e-9)) gathered.toList() else null
    }

    /** When the open window closes, so the skip can stop there; infinity if none is open. */
    val deadline: Double get() = if (gathered.isEmpty()) Double.POSITIVE_INFINITY else closesAt

    /** Whatever has been gathered (the skip ended before the window closed). */
    fun remaining(): List<GameEvent> = gathered.toList()
}
