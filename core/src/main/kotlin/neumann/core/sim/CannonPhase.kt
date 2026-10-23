package neumann.core.sim

import neumann.core.math.Units
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.Slug
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.max

/** Fires cannon slugs at where the target will be if it keeps its velocity. */
class CannonPhase(private val solver: InterceptSolver) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val cannon = v.shipClass.cannon ?: continue
            v.cannonCooldown = max(0.0, v.cannonCooldown - dt)
            val target = v.contacts[v.targetId]?.takeIf { it.kind != ContactKind.TORPEDO }
            if (v.fireCannonNow && (target == null || v.slugs <= 0 || !v.working(PartKind.CANNON))) {
                v.fireCannonNow = false
                tellPlayer(world, v, "Cannon cannot fire: " + when {
                    target == null -> "no target"
                    v.slugs <= 0 -> "no slugs"
                    else -> "cannon destroyed"
                })
                continue
            }
            if (target == null || v.slugs <= 0 || !v.working(PartKind.CANNON) || v.cannonCooldown > 0.0) continue
            val auto = v.rules.cannonAutoFire && target.distance <= cannon.autoRangeKm * Units.KM
            if (!(v.fireCannonNow || auto)) continue

            val speed = cannon.muzzleSpeedKmS * Units.KM
            val r = target.predictedPosition - v.position
            val aim = solver.aim(r, target.observedVelocity - v.velocity, speed) ?: r.normalized()
            world.slugs.add(Slug(v.id, v.faction, v.position, v.velocity + aim * speed, cannon.damage, world.time))
            v.slugs--
            v.cannonCooldown = cannon.cooldownSeconds
            v.fireCannonNow = false
        }
    }

    private fun tellPlayer(world: World, v: Vessel, text: String) {
        if (v === world.player) world.event(EventKind.INFO, v.id, text)
    }
}
