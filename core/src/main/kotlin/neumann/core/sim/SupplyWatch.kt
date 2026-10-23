package neumann.core.sim

import neumann.core.data.AutoPauseConfig
import neumann.core.world.EventKind
import neumann.core.world.World

/** Warns when the player's fuel or ammunition drops below the low mark. */
class SupplyWatch(private val config: AutoPauseConfig) {
    fun run(world: World) {
        val p = world.player
        if (!p.alive) return
        val w = world.playerWatch
        val c = p.shipClass
        val f = config.lowSupplyFraction
        c.drive?.let {
            w.lowFuel = check(world, w.lowFuel, p.fuel < it.fuelCapacity * f, EventKind.LOW_FUEL, "fuel", "Fuel low")
        }
        c.cannon?.let {
            w.lowSlugs = check(world, w.lowSlugs, p.slugs < it.ammo * f, EventKind.LOW_AMMO, "slugs", "Cannon slugs low")
        }
        c.torpedo?.let {
            w.lowTorpedoes = check(world, w.lowTorpedoes, p.torpedoes < it.ammo * f, EventKind.LOW_AMMO, "torpedoes", "Torpedoes low")
        }
    }

    /** Returns the new warned state; warns on the change from fine to low. */
    private fun check(world: World, warned: Boolean, low: Boolean, kind: EventKind, source: String, text: String): Boolean {
        if (low && !warned) world.event(kind, source, text)
        return low
    }
}
