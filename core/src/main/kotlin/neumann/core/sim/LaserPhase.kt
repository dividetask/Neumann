package neumann.core.sim

import neumann.core.math.Units
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.Torpedo
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.max

/** Fires lasers: point defence against torpedoes first, then the target. */
class LaserPhase(private val damage: DamageResolver) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val laser = v.shipClass.laser ?: continue
            val bursting = v.laserBurstRemaining > 0.0
            v.laserBurstRemaining = max(0.0, v.laserBurstRemaining - dt)
            if (!v.working(PartKind.LASER)) continue
            val range = laser.rangeKm * Units.KM
            val torpedo = if (v.rules.laserPointDefence) nearestTorpedo(world, v, range) else null
            if (torpedo != null) {
                val d = (torpedo.position - v.position).length()
                torpedo.hitPoints -= laser.damagePerSecond * (1 - d / range) * dt
                if (torpedo.hitPoints <= 0.0) shotDown(world, v, torpedo)
                continue
            }
            if (!(v.rules.laserAutoFire || bursting)) continue
            val target = world.vessel(v.targetId) ?: continue
            if (target.faction == v.faction || v.contacts[target.id] == null) continue
            val d = (target.position - v.position).length()
            if (d >= range) continue
            damage.hit(world, target, laser.damagePerSecond * (1 - d / range) * dt, v.id, "laser")
        }
    }

    private fun nearestTorpedo(world: World, v: Vessel, range: Double): Torpedo? =
        world.torpedoes
            .filter { it.alive && it.faction != v.faction && v.contacts[it.id] != null }
            .map { it to (it.position - v.position).length() }
            .filter { it.second < range }
            .minByOrNull { it.second }?.first

    private fun shotDown(world: World, by: Vessel, t: Torpedo) {
        t.alive = false
        if (by === world.player || t.targetId == world.player.id || t.ownerId == world.player.id) {
            world.event(EventKind.INFO, by.id, "${t.name} shot down by ${by.name}")
        }
    }
}
