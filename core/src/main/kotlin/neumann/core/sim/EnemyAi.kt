package neumann.core.sim

import neumann.core.data.AiConfig
import neumann.core.math.Units
import neumann.core.world.Burn
import neumann.core.world.BurnDirection
import neumann.core.world.Faction
import neumann.core.world.PartKind
import neumann.core.world.Vessel
import neumann.core.world.VesselRole
import neumann.core.world.World
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Enemy behaviour: target the player once seen; ships close to the standoff
 * distance and match velocity; ambushers lie still until the player is near.
 */
class EnemyAi(private val config: AiConfig) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || v.faction != Faction.ENEMY) continue
            val player = v.contacts[world.player.id] ?: continue
            if (v.ambush) {
                if (player.distance > v.ambushRange) continue
                v.ambush = false
            }
            v.targetId = player.id
            if (v.shipClass.role == VesselRole.SHIP) steer(v, dt)
        }
    }

    private fun steer(v: Vessel, dt: Double) {
        val drive = v.shipClass.drive ?: return
        if (!v.working(PartKind.ENGINE) || v.fuel <= 0.0) return
        val target = v.contacts[v.targetId] ?: return
        val r = target.predictedPosition - v.position
        val gap = (r.length() - config.standoffKm * Units.KM).coerceAtLeast(0.0)
        val closing = min(sqrt(drive.accelerationMS2 * gap), config.maxClosingSpeedKmS * Units.KM)
        val wanted = r.normalized() * closing
        val dv = wanted - (v.velocity - target.observedVelocity)
        val step = drive.accelerationMS2 * dt
        v.burn = if (dv.length() < step * 0.5) null
        else Burn(BurnDirection.VECTOR, min(1.0, dv.length() / step), dt, dv)
    }
}
