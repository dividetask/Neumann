package neumann.core.sim

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.EventKind
import neumann.core.world.ModuleKind
import neumann.core.world.Torpedo
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.max

/**
 * Fires each laser turret continuously while it has something to fire at:
 * point defence against torpedoes (spreading turrets over different
 * torpedoes) and its target, in the order the point-defence-first switch
 * says. A turret fires only at things in its range
 * and arc, and only with enough stored power.
 */
class LaserPhase(private val damage: DamageResolver) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.active) continue
            val bursting = v.laserBurstRemaining > 0.0
            v.laserBurstRemaining = max(0.0, v.laserBurstRemaining - dt)
            val engaged = HashSet<String>()
            for (turret in v.working(ModuleKind.LASER_TURRET)) {
                val laser = turret.type.laser!!
                val power = laser.powerMW * dt
                val range = laser.rangeKm * Units.KM
                // Point defence first, or the target first and torpedoes only when the target is out of reach.
                val firing = v.rules.laserAutoFire || bursting
                var torpedo: Torpedo? = null
                var target: Vessel? = null
                if (v.rules.pointDefenceFirst) {
                    torpedo = torpedoFor(world, v, range, laser.arcDeg, engaged)
                    if (torpedo == null && firing) target = vesselTarget(world, v, range, laser.arcDeg)
                } else {
                    if (firing) target = vesselTarget(world, v, range, laser.arcDeg)
                    if (target == null) torpedo = torpedoFor(world, v, range, laser.arcDeg, engaged)
                }
                if ((torpedo == null && target == null) || v.energy < power) continue
                v.energy -= power
                if (torpedo != null) {
                    engaged.add(torpedo.id)
                    val d = (torpedo.position - v.position).length()
                    torpedo.hitPoints -= LaserFalloff.damagePerSecond(laser, d) * dt
                    if (torpedo.hitPoints <= 0.0 && torpedo.alive) shotDown(world, v, torpedo)
                } else if (target != null) {
                    val d = (target.position - v.position).length()
                    damage.hit(world, target, LaserFalloff.damagePerSecond(laser, d) * dt, v.id, "laser")
                }
            }
        }
    }

    private fun vesselTarget(world: World, v: Vessel, range: Double, arcDeg: Double): Vessel? {
        val target = world.vessel(v.laserTargetId ?: v.targetId) ?: return null
        if (target.faction == v.faction || v.contacts[target.id] == null) return null
        return target.takeIf { inReach(v, it.position, range, arcDeg) }
    }

    private fun inReach(v: Vessel, at: Vec3, range: Double, arcDeg: Double): Boolean {
        val to = at - v.position
        return to.length() < range && v.forward.angleTo(to) <= arcDeg * Units.DEGREE
    }

    /**
     * A hostile torpedo in reach: those aimed at this ship first, then those aimed at
     * allies; nearest first; preferring ones no other turret has engaged.
     */
    private fun torpedoFor(world: World, v: Vessel, range: Double, arcDeg: Double, engaged: Set<String>): Torpedo? {
        val inReach = world.torpedoes
            .filter { it.alive && it.faction != v.faction && v.contacts[it.id] != null && inReach(v, it.position, range, arcDeg) }
            .sortedWith(compareBy({ if (it.targetId == v.id) 0 else 1 }, { (it.position - v.position).length() }))
        return inReach.firstOrNull { it.id !in engaged } ?: inReach.firstOrNull()
    }

    private fun shotDown(world: World, by: Vessel, t: Torpedo) {
        t.alive = false
        if (by === world.player || t.targetId == world.player.id || t.ownerId == world.player.id) {
            world.event(EventKind.INFO, by.id, "${t.name} shot down by ${by.name}")
        }
    }
}
