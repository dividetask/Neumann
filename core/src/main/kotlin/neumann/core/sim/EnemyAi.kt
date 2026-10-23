package neumann.core.sim

import neumann.core.data.AiConfig
import neumann.core.math.Units
import neumann.core.world.Burn
import neumann.core.world.Faction
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.Vessel
import neumann.core.world.VesselRole
import neumann.core.world.World
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Enemy behaviour: target the player once seen; ships close to the standoff
 * distance and match velocity. Inside weapon range they face the player and
 * burn only for large course errors. Ambushers lie still until the player is
 * near.
 */
class EnemyAi(private val config: AiConfig, private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || v.faction != Faction.ENEMY) continue
            val player = v.contacts[world.player.id] ?: continue
            if (v.ambush) {
                if (player.distance > v.ambushRange) continue
                v.ambush = false
            }
            v.targetId = player.id
            if (v.design.role == VesselRole.SHIP) steer(world, v, dt)
        }
    }

    private fun steer(world: World, v: Vessel, dt: Double) {
        val target = v.contacts[v.targetId] ?: return
        val accel = performance.accelerationMS2(v)
        if (!v.has(ModuleKind.PROPULSION) || v.fuel <= 0.0 || accel <= 0.0) {
            v.heading = Heading(HeadingMode.TOWARD_TARGET)
            v.burn = null
            return
        }
        val r = target.predictedPosition - v.position
        val gap = (r.length() - config.standoffKm * Units.KM).coerceAtLeast(0.0)
        val closing = min(sqrt(accel * gap), config.maxClosingSpeedKmS * Units.KM)
        val dv = r.normalized() * closing - (v.velocity - target.observedVelocity)
        val step = accel * dt
        val inRange = target.distance <= performance.threatRange(v).coerceAtMost(cannonRange(v))
        val ignore = if (inRange) config.combatDvKmS * Units.KM else step * 0.5
        if (dv.length() < ignore) {
            v.heading = Heading(HeadingMode.TOWARD_TARGET)
            v.burn = null
        } else {
            v.heading = Heading(HeadingMode.FIXED, dv)
            v.burn = Burn(1.0, dv.length(), null, world.time, v.position, v.velocity)
        }
    }

    /** Longest auto-fire range of its working cannons (or lasers, if it has no cannon), in metres. */
    private fun cannonRange(v: Vessel): Double {
        val cannons = v.working(ModuleKind.CANNON).map { it.type.cannon!!.autoRangeKm }
        val lasers = v.working(ModuleKind.LASER_TURRET).map { it.type.laser!!.rangeKm }
        return ((cannons.maxOrNull() ?: lasers.maxOrNull()) ?: 0.0) * Units.KM
    }
}
