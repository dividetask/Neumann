package neumann.core.sim

import neumann.core.math.ClosestApproach
import neumann.core.math.Units
import neumann.core.world.EventKind
import neumann.core.world.Slug
import neumann.core.world.World

/** Slug hits (on vessels and on torpedoes) and torpedo detonations during the last step. */
class ImpactPhase(private val damage: DamageResolver) {
    fun run(world: World) {
        for (s in world.slugs) {
            if (!s.alive) continue
            if (hitTorpedo(world, s)) continue
            for (v in world.vessels) {
                if (!v.alive || v.faction == s.faction) continue
                val ca = ClosestApproach.between(s.previousPosition, s.position, v.previousPosition, v.position)
                if (ca.distance <= v.design.sizeM) {
                    s.alive = false
                    // Kinetic energy relative to the target, in MJ.
                    val rel = (s.velocity - v.velocity).length()
                    damage.hit(world, v, 0.5 * s.massKg * rel * rel / 1e6, s.ownerId, "cannon")
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

    /** A slug passing within a hostile torpedo's size hits it for its kinetic energy (cannon point defence). */
    private fun hitTorpedo(world: World, s: Slug): Boolean {
        for (t in world.torpedoes) {
            if (!t.alive || t.faction == s.faction) continue
            if (ClosestApproach.between(s.previousPosition, s.position, t.previousPosition, t.position).distance > t.sizeM) continue
            s.alive = false
            val rel = (s.velocity - t.velocity).length()
            t.hitPoints -= 0.5 * s.massKg * rel * rel / 1e6
            if (t.hitPoints <= 0.0) {
                t.alive = false
                if (s.ownerId == world.player.id || t.targetId == world.player.id) {
                    world.event(EventKind.INFO, s.ownerId, "${t.name} shot down by ${world.vessel(s.ownerId)?.name ?: "?"}'s cannon")
                }
            }
            return true
        }
        return false
    }
}
