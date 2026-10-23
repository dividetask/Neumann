package neumann.core.game

import neumann.core.data.GameData
import neumann.core.math.Vec3
import neumann.core.sim.HeadingResolver
import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.ApproachVector
import neumann.core.world.World

/** What the UI talks to: enter the system, give orders, skip time. */
class GameSession(val data: GameData, approach: ApproachVector) {
    private val step = SimulationStep(data)
    private val timeSkip = TimeSkip(step, StepSizer(data.rules.sim), AutoPauseMonitor(data.rules.autoPause))

    val performance = step.performance
    val world: World = ScenarioBuilder(data).build(approach).also { step.sensors.run(it) }
    val orders = OrderDesk(world)

    private val headings = HeadingResolver()

    fun skip(seconds: Double): TimeSkipResult = timeSkip.run(world, seconds)

    /** Where the player's heading points now, in space; null if it has none. */
    fun headingDirection(): Vec3? = headings.resolve(world.player)

    /** The player's turn still to go, as dial angles; null with no heading. */
    fun turnToGo(): DirectionDials.Angles? {
        val p = world.player
        val dir = headingDirection() ?: return null
        return DirectionDials.remaining(p.forward, p.up, dir)
    }
}
