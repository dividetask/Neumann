package neumann.core.sim

import neumann.core.data.SlugConfig
import neumann.core.math.Units
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.ModuleKind
import neumann.core.world.Slug
import neumann.core.world.World
import kotlin.math.max

/**
 * Fires cannons at where the target will be if it keeps its velocity. Each
 * cannon fires on its own when ready and the aim point is in its arc; slugs
 * come from the shared ammo containers. A fire-now order waits until a
 * cannon can fire.
 */
class CannonPhase(private val solver: InterceptSolver, private val slug: SlugConfig) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val cannons = v.working(ModuleKind.CANNON)
            for (c in cannons) c.cooldown = max(0.0, c.cooldown - dt)
            val target = v.contacts[v.targetId]?.takeIf { it.kind != ContactKind.TORPEDO }
            if (v.fireCannonNow && (target == null || v.slugs <= 0 || cannons.isEmpty())) {
                v.fireCannonNow = false
                if (v === world.player) world.event(EventKind.INFO, v.id, "Cannon cannot fire: " + when {
                    target == null -> "no target"
                    v.slugs <= 0 -> "no slugs"
                    else -> "no working cannon"
                })
                continue
            }
            if (target == null) continue
            val r = target.predictedPosition - v.position
            for (c in cannons) {
                if (v.slugs <= 0 || c.cooldown > 0.0) continue
                val cfg = c.type.cannon!!
                val auto = v.rules.cannonAutoFire && target.distance <= cfg.autoRangeKm * Units.KM
                if (!(v.fireCannonNow || auto)) continue
                val speed = cfg.muzzleSpeedKmS * Units.KM
                val aim = solver.aim(r, target.observedVelocity - v.velocity, speed) ?: r.normalized()
                if (v.forward.angleTo(aim) > cfg.arcDeg * Units.DEGREE) continue
                world.slugs.add(Slug(v.id, v.faction, v.position, v.velocity + aim * speed, slug.damage, world.time))
                v.slugs--
                c.cooldown = cfg.cooldownSeconds
                v.fireCannonNow = false
            }
        }
    }
}
