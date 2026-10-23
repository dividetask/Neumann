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
 * point defence against torpedoes first (spreading turrets over different
 * torpedoes), then the target. A turret fires only at things in its range
 * and arc, and only with enough stored power.
 */
class LaserPhase(private val damage: DamageResolver) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val bursting = v.laserBurstRemaining > 0.0
            v.laserBurstRemaining = max(0.0, v.laserBurstRemaining - dt)
            val engaged = HashSet<String>()
            for (turret in v.working(ModuleKind.LASER_TURRET)) {
                val laser = turret.type.laser!!
                val power = laser.powerMW * dt
                val range = laser.rangeKm * Units.KM
                val torpedo = if (v.rules.laserPointDefence) torpedoFor(world, v, range, laser.arcDeg, engaged) else null
                val target = if (torpedo == null && (v.rules.laserAutoFire || bursting)) vesselTarget(world, v, range, laser.arcDeg) else null
                if ((torpedo == null && target == null) || v.energy < power) continue
                v.energy -= power
                if (torpedo != null) {
                    engaged.add(torpedo.id)
                    val d = (torpedo.position - v.position).length()
                    torpedo.hitPoints -= laser.damagePerSecond * (1 - d / range) * dt
                    if (torpedo.hitPoints <= 0.0 && torpedo.alive) shotDown(world, v, torpedo)
                } else if (target != null) {
                    val d = (target.position - v.position).length()
                    damage.hit(world, target, laser.damagePerSecond * (1 - d / range) * dt, v.id, "laser")
                }
            }
        }
    }

    private fun vesselTarget(world: World, v: Vessel, range: Double, arcDeg: Double): Vessel? {
        val target = world.vessel(v.targetId) ?: return null
        if (target.faction == v.faction || v.contacts[target.id] == null) return null
        return target.takeIf { inReach(v, it.position, range, arcDeg) }
    }

    private fun inReach(v: Vessel, at: Vec3, range: Double, arcDeg: Double): Boolean {
        val to = at - v.position
        return to.length() < range && v.forward.angleTo(to) <= arcDeg * Units.DEGREE
    }

    /** Nearest hostile torpedo in reach, preferring ones no other turret has engaged. */
    private fun torpedoFor(world: World, v: Vessel, range: Double, arcDeg: Double, engaged: Set<String>): Torpedo? {
        val inReach = world.torpedoes
            .filter { it.alive && it.faction != v.faction && v.contacts[it.id] != null && inReach(v, it.position, range, arcDeg) }
            .sortedBy { (it.position - v.position).length() }
        return inReach.firstOrNull { it.id !in engaged } ?: inReach.firstOrNull()
    }

    private fun shotDown(world: World, by: Vessel, t: Torpedo) {
        t.alive = false
        if (by === world.player || t.targetId == world.player.id || t.ownerId == world.player.id) {
            world.event(EventKind.INFO, by.id, "${t.name} shot down by ${by.name}")
        }
    }
}
