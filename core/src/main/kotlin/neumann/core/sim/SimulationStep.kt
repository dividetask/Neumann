package neumann.core.sim

import neumann.core.data.GameData
import neumann.core.world.World

/** Runs one simulation step: every phase, in order. */
class SimulationStep(data: GameData) {
    private val rules = data.rules
    val performance = ShipPerformance(data.munitions)
    private val damage = DamageResolver()
    private val headings = HeadingResolver()
    private val planetMover = PlanetMover()
    private val enemyAi = EnemyAi(rules.ai, performance)
    private val power = PowerPhase(performance)
    private val turns = TurnPhase(headings, performance, rules.ships)
    private val burns = BurnPhase(headings, performance, rules.ships)
    private val guidance = TorpedoGuidance(rules.sim)
    private val lasers = LaserPhase(damage)
    private val cannons = CannonPhase(InterceptSolver(), data.munitions.slug)
    private val launches = TorpedoLaunchPhase(data.munitions.torpedo)
    private val motion = MotionPhase(GravityField(), rules.sim)
    private val impacts = ImpactPhase(damage)
    private val collisions = BodyCollisionPhase(damage)
    val sensors = SensorPhase(LightDelay())
    private val rangeWatch = WeaponRangeWatch(performance)
    private val supplyWatch = SupplyWatch(rules.autoPause, performance)
    private val claimCheck = ClaimCheck()
    private val history = HistoryPhase(rules.sim)

    fun run(world: World, dt: Double) {
        enemyAi.run(world, dt)
        power.run(world, dt)
        turns.run(world, dt)
        burns.run(world, dt)
        guidance.run(world, dt)
        lasers.run(world, dt)
        cannons.run(world, dt)
        launches.run(world, dt)
        motion.run(world, dt)
        world.time += dt
        planetMover.run(world)
        impacts.run(world)
        collisions.run(world)
        sensors.run(world)
        rangeWatch.run(world)
        supplyWatch.run(world)
        claimCheck.run(world)
        history.run(world)
        world.torpedoes.removeAll { !it.alive }
        world.slugs.removeAll { !it.alive }
    }
}
