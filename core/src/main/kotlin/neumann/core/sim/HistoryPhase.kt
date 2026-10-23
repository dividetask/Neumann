package neumann.core.sim

import neumann.core.data.SimConfig
import neumann.core.world.HistorySample
import neumann.core.world.World

/** Records positions for light delay, and forgets very old ones. */
class HistoryPhase(private val config: SimConfig) {
    fun run(world: World) {
        if (world.time - world.lastHistorySample < config.historySampleSeconds) return
        world.lastHistorySample = world.time
        for (t in world.tracked()) {
            t.history.record(HistorySample(world.time, t.position, t.velocity, t.burning))
            t.history.prune(world.time - config.historyKeepSeconds)
        }
    }
}
