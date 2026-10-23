package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.World

/** Anything inside the star or a planet is destroyed. */
class BodyCollisionPhase(private val damage: DamageResolver) {
    fun run(world: World) {
        for (v in world.vessels) {
            if (!v.alive || v.station != null) continue
            bodyHit(world, v.position)?.let { damage.destroy(world, v, "crashing into $it") }
        }
        for (t in world.torpedoes) if (t.alive && bodyHit(world, t.position) != null) t.alive = false
        for (s in world.slugs) if (s.alive && bodyHit(world, s.position) != null) s.alive = false
    }

    private fun bodyHit(world: World, p: Vec3): String? {
        if (p.length() < world.star.radius) return world.star.name
        return world.planets.firstOrNull { (p - it.position).length() < it.radius }?.name
    }
}
