package neumann.core.game

import neumann.core.data.GameData
import neumann.core.data.ScenarioConfig
import neumann.core.math.Vec3
import neumann.core.sim.HeadingResolver
import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.ApproachVector
import neumann.core.world.Vessel
import neumann.core.world.World

/** What the UI talks to: enter a system (fixed or generated), give orders, skip time. */
class GameSession(val data: GameData, val world: World) {
    /** After entry, already moving at the approach vector (the fixed test system). */
    constructor(data: GameData, scenario: ScenarioConfig, approach: ApproachVector) :
        this(data, ScenarioBuilder(data).build(scenario, approach))
    constructor(data: GameData, approach: ApproachVector) : this(data, data.scenario, approach)

    private val step = SimulationStep(data)
    private val timeSkip = TimeSkip(step, StepSizer(data.rules.sim, step.performance), AutoPauseMonitor(data.rules.autoPause),
        data.rules.autoPause.groupSeconds, data.rules.sim.baseStepSeconds)

    val performance = step.performance
    val orders = OrderDesk(world)

    init {
        step.sensors.run(world)
    }

    private val headings = HeadingResolver()
    private val estimator = BogeyEstimator(world)
    private val remaining = RemainingCount(world)
    private val fixtures = KnownFixtures(world)

    fun skip(seconds: Double): TimeSkipResult = timeSkip.run(world, seconds)

    /** Runs on, pausing as usual, until the player's burn ends (e.g. the entry burn). */
    fun skipUntilBurnEnds(): TimeSkipResult = timeSkip.run(world, BURN_SKIP_LIMIT_S) { world.player.burn == null }

    /** Every bogey the player has a position fix for: where it was seen, and where it probably is now. */
    fun bogeys(): List<BogeyEstimate> = estimator.estimate()

    /** What is left to destroy, as far as the player knows. */
    fun remaining(): Remaining = remaining.count()

    /** Fixed defences and bases found and not destroyed, where they are now. */
    fun knownFixtures(): List<Vessel> = fixtures.list()

    /** Where the player's heading points now, in space; null if it has none. */
    fun headingDirection(): Vec3? = headings.resolve(world.player)

    /** The player's turn still to go, as dial angles; null with no heading. */
    fun turnToGo(): DirectionDials.Angles? {
        val p = world.player
        val dir = headingDirection() ?: return null
        return DirectionDials.remaining(p.forward, p.up, dir)
    }

    companion object {
        /** No burn runs longer than this; a skip to a burn's end stops here regardless. */
        const val BURN_SKIP_LIMIT_S = 10 * 365.25 * 86400.0

        /** Arriving at a generated system, then entering it: a matching burn to [entryVelocity] at [drive]. */
        fun arriving(data: GameData, scenario: ScenarioConfig, entryVelocity: Vec3, drive: Double): GameSession =
            GameSession(data, ScenarioBuilder(data).arrive(scenario)).also { it.orders.enter(entryVelocity, drive) }
    }
}
