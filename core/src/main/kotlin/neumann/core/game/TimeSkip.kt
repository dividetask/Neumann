package neumann.core.game

import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.GameEvent
import neumann.core.world.World

/** How long a time skip ran, and the events that stopped it (empty if none). */
class TimeSkipResult(val elapsedSeconds: Double, val pausedBy: List<GameEvent>)

/** Runs simulation steps until the time is up, an auto-pause event, or the battle ends. */
class TimeSkip(
    private val step: SimulationStep,
    private val sizer: StepSizer,
    private val monitor: AutoPauseMonitor,
) {
    fun run(world: World, seconds: Double): TimeSkipResult {
        val start = world.time
        val end = start + seconds
        var seen = world.log.events.size
        while (!world.over && end - world.time > 1e-9) {
            step.run(world, sizer.next(world, end - world.time))
            val fresh = world.log.events.subList(seen, world.log.events.size).toList()
            seen = world.log.events.size
            val pausing = monitor.pausing(fresh)
            if (pausing.isNotEmpty()) return TimeSkipResult(world.time - start, pausing)
        }
        return TimeSkipResult(world.time - start, emptyList())
    }
}
