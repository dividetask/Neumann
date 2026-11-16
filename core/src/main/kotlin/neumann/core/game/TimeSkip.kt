package neumann.core.game

import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.GameEvent
import neumann.core.world.World
import kotlin.math.min

/** How long a time skip ran, and the events that stopped it (empty if none). */
class TimeSkipResult(val elapsedSeconds: Double, val pausedBy: List<GameEvent>)

/**
 * Runs simulation steps until the time is up, a pause is due, or the battle
 * ends. Reasons to pause that arrive within [groupSeconds] of each other are
 * gathered into one pause (see [PauseCollector]).
 */
class TimeSkip(
    private val step: SimulationStep,
    private val sizer: StepSizer,
    private val monitor: AutoPauseMonitor,
    private val groupSeconds: Double,
    private val firstStepSeconds: Double,
) {
    /** Runs for [seconds], or until a pause, or until [until] says to stop. */
    fun run(world: World, seconds: Double, until: () -> Boolean = { false }): TimeSkipResult {
        val start = world.time
        val end = start + seconds
        val collector = PauseCollector(groupSeconds)
        var seen = world.log.events.size
        var first = true
        while (!world.over && end - world.time > 1e-9) {
            val stopAt = min(end, collector.deadline)
            // The first step is short, so whatever can be seen right away pauses at once.
            val dt = sizer.next(world, stopAt - world.time)
            step.run(world, if (first) min(dt, firstStepSeconds) else dt)
            first = false
            val fresh = world.log.events.subList(seen, world.log.events.size).toList()
            seen = world.log.events.size
            collector.offer(monitor.pausing(fresh), world.time)?.let { return TimeSkipResult(world.time - start, it) }
            if (until()) break
        }
        return TimeSkipResult(world.time - start, collector.remaining())
    }
}
