package neumann.core.sim

import neumann.core.data.AutoPauseConfig
import neumann.core.world.EventKind
import neumann.core.world.World

/** Warns when the player's fuel or ammunition drops below the low mark of what its modules hold. */
class SupplyWatch(private val config: AutoPauseConfig, private val performance: ShipPerformance) {
    fun run(world: World) {
        val p = world.player
        if (!p.alive) return
        val w = world.playerWatch
        val f = config.lowSupplyFraction
        w.lowFuel = check(world, w.lowFuel, low(p.fuel, performance.fuelCapacity(p), f), EventKind.LOW_FUEL, "fuel", "Fuel low")
        w.lowSlugs = check(world, w.lowSlugs, low(p.slugs.toDouble(), performance.slugCapacity(p).toDouble(), f),
            EventKind.LOW_AMMO, "slugs", "Cannon slugs low")
        w.lowTorpedoes = check(world, w.lowTorpedoes, low(p.torpedoes.toDouble(), performance.torpedoCapacity(p).toDouble(), f),
            EventKind.LOW_AMMO, "torpedoes", "Torpedoes low")
    }

    private fun low(amount: Double, capacity: Double, fraction: Double) = capacity > 0.0 && amount < capacity * fraction

    /** Returns the new warned state; warns on the change from fine to low. */
    private fun check(world: World, warned: Boolean, low: Boolean, kind: EventKind, source: String, text: String): Boolean {
        if (low && !warned) world.event(kind, source, text)
        return low
    }
}
