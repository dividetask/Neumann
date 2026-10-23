package neumann.core.game

import neumann.core.data.GameData
import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.ApproachVector
import neumann.core.world.World

/** What the UI talks to: enter the system, give orders, skip time. */
class GameSession(val data: GameData, approach: ApproachVector) {
    private val step = SimulationStep(data)
    private val timeSkip = TimeSkip(step, StepSizer(data.rules.sim), AutoPauseMonitor(data.rules.autoPause))

    val world: World = ScenarioBuilder(data).build(approach).also { step.sensors.run(it) }
    val orders = OrderDesk(world)

    fun skip(seconds: Double): TimeSkipResult = timeSkip.run(world, seconds)
}
