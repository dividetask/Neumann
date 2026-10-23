package neumann.core.sim

import neumann.core.math.ClosestApproach
import neumann.core.math.Units
import neumann.core.world.World

/** Slug hits and torpedo detonations during the last step. */
class ImpactPhase(private val damage: DamageResolver) {
    fun run(world: World) {
        for (s in world.slugs) {
            if (!s.alive) continue
            for (v in world.vessels) {
                if (!v.alive || v.faction == s.faction) continue
                val ca = ClosestApproach.between(s.previousPosition, s.position, v.previousPosition, v.position)
                if (ca.distance <= v.design.sizeM) {
                    s.alive = false
                    damage.hit(world, v, s.damage, s.ownerId, "cannon")
                    break
                }
            }
        }
        for (t in world.torpedoes) {
            if (!t.alive) continue
            val blast = t.config.blastRadiusKm * Units.KM
            val caught = world.vessels.filter {
                it.alive && it.faction != t.faction &&
                    ClosestApproach.between(t.previousPosition, t.position, it.previousPosition, it.position).distance <= blast
            }
            if (caught.isEmpty()) continue
            t.alive = false
            for (v in caught) damage.hit(world, v, t.config.damage, t.ownerId, "torpedo")
        }
    }
}
