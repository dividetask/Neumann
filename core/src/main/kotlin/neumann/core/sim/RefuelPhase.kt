package neumann.core.sim

import neumann.core.data.AiConfig
import neumann.core.math.Units
import neumann.core.world.VesselRole
import neumann.core.world.World

/**
 * Ships take on fuel from one of their bases: within `refuelRangeKm` of it and
 * moving within `refuelSpeedMS` of its velocity, at `refuelTPerHour`.
 */
class RefuelPhase(private val config: AiConfig, private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        val bases = world.vessels.filter { it.active && it.design.role == VesselRole.BASE }
        for (v in world.vessels) {
            if (!v.active || v.design.role != VesselRole.SHIP) continue
            val capacity = performance.fuelCapacity(v)
            if (v.fuel >= capacity) continue
            val near = bases.any {
                it.faction == v.faction && (it.position - v.position).length() <= config.refuelRangeKm * Units.KM &&
                    (it.velocity - v.velocity).length() <= config.refuelSpeedMS
            }
            if (near) v.fuel = minOf(capacity, v.fuel + config.refuelTPerHour * dt / 3600.0)
        }
    }
}
