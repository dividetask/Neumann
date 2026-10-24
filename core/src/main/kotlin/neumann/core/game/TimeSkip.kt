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
) {
    fun run(world: World, seconds: Double): TimeSkipResult {
        val start = world.time
        val end = start + seconds
        val collector = PauseCollector(groupSeconds)
        var seen = world.log.events.size
        while (!world.over && end - world.time > 1e-9) {
            val stopAt = min(end, collector.deadline)
            step.run(world, sizer.next(world, stopAt - world.time))
            val fresh = world.log.events.subList(seen, world.log.events.size).toList()
            seen = world.log.events.size
            collector.offer(monitor.pausing(fresh), world.time)?.let { return TimeSkipResult(world.time - start, it) }
        }
        return TimeSkipResult(world.time - start, collector.remaining())
    }
}
