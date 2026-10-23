package neumann.core.sim

import neumann.core.data.MiningConfig
import neumann.core.math.Units
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.Planet
import neumann.core.world.Vessel
import neumann.core.world.World

/**
 * Mines when the area is cleared: no known enemy within the clear radius,
 * close to a planet, and moving slowly relative to it.
 */
class MiningPhase(private val config: MiningConfig) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || v.shipClass.factory == null) continue
            val planet = if (v.working(PartKind.FACTORY) && cleared(v)) minable(world, v) else null
            val mining = planet != null
            if (mining) v.minerals += config.mineralsPerHour * dt / Units.HOUR
            if (mining != v.mining && v === world.player) {
                world.event(EventKind.INFO, v.id, if (mining) "Mining started at ${planet!!.name}" else "Mining stopped")
            }
            v.mining = mining
        }
    }

    private fun cleared(v: Vessel): Boolean =
        v.contacts.current.values.none { it.distance <= config.clearRadiusKm * Units.KM }

    private fun minable(world: World, v: Vessel): Planet? = world.planets.firstOrNull {
        (v.position - it.position).length() <= config.miningDistanceKm * Units.KM &&
            (v.velocity - it.velocity).length() <= config.maxRelativeSpeedKmS * Units.KM
    }
}
