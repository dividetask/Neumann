package neumann.core.game

import neumann.core.data.GameData
import neumann.core.data.ScenarioConfig
import neumann.core.math.Vec3
import neumann.core.sim.HeadingResolver
import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.ApproachVector
import neumann.core.world.World

/** What the UI talks to: enter a system (fixed or generated), give orders, skip time. */
class GameSession(val data: GameData, scenario: ScenarioConfig, approach: ApproachVector) {
    constructor(data: GameData, approach: ApproachVector) : this(data, data.scenario, approach)

    private val step = SimulationStep(data)
    private val timeSkip = TimeSkip(step, StepSizer(data.rules.sim), AutoPauseMonitor(data.rules.autoPause),
        data.rules.autoPause.groupSeconds)

    val performance = step.performance
    val world: World = ScenarioBuilder(data).build(scenario, approach).also { step.sensors.run(it) }
    val orders = OrderDesk(world)

    private val headings = HeadingResolver()
    private val estimator = ContactEstimator(world)

    fun skip(seconds: Double): TimeSkipResult = timeSkip.run(world, seconds)

    /** Every ship and torpedo the player has a position fix for: where it was seen, and where it probably is now. */
    fun estimates(): List<ContactEstimate> = estimator.estimate()

    /** Where the player's heading points now, in space; null if it has none. */
    fun headingDirection(): Vec3? = headings.resolve(world.player)

    /** The player's turn still to go, as dial angles; null with no heading. */
    fun turnToGo(): DirectionDials.Angles? {
        val p = world.player
        val dir = headingDirection() ?: return null
        return DirectionDials.remaining(p.forward, p.up, dir)
    }
}
