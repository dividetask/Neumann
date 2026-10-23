package neumann.core.sim

import neumann.core.world.EventKind
import neumann.core.world.GameEvent
import neumann.core.world.PartKind
import neumann.core.world.Vessel
import neumann.core.world.World

/**
 * Applies a hit to a vessel. The hit lands on one part chosen by hit weight;
 * damage beyond what that part has left goes to the hull.
 */
class DamageResolver {
    fun hit(world: World, target: Vessel, amount: Double, attackerId: String, weapon: String) {
        if (!target.alive || amount <= 0.0) return
        val part = pickPart(world, target)
        val overflow = applyTo(world, target, part, amount)
        if (overflow > 0.0 && part != PartKind.HULL) applyTo(world, target, PartKind.HULL, overflow)
        report(world, target, amount, attackerId, weapon)
        if (target.parts[PartKind.HULL]?.destroyed != false) destroy(world, target, "by ${nameOf(world, attackerId)}'s $weapon")
    }

    fun destroy(world: World, target: Vessel, how: String) {
        if (!target.alive) return
        target.alive = false
        target.burn = null
        world.event(EventKind.DESTROYED, target.id, "${target.name} destroyed $how")
    }

    private fun pickPart(world: World, target: Vessel): PartKind {
        val parts = target.parts.values
        var roll = world.random.nextDouble() * parts.sumOf { it.hitWeight }
        for (p in parts) {
            roll -= p.hitWeight
            if (roll < 0) return p.kind
        }
        return PartKind.HULL
    }

    /** Damages one part; returns the damage it could not absorb. */
    private fun applyTo(world: World, target: Vessel, kind: PartKind, amount: Double): Double {
        val part = target.parts[kind] ?: return amount
        if (part.destroyed) return amount
        part.hitPoints -= amount
        if (!part.destroyed) return 0.0
        val overflow = -part.hitPoints
        part.hitPoints = 0.0
        when (kind) {
            PartKind.FUEL_TANK -> target.fuel = 0.0
            PartKind.ENGINE -> target.burn = null
            else -> {}
        }
        if (kind != PartKind.HULL) {
            world.event(EventKind.INFO, target.id, "${target.name}: ${kind.name.lowercase().replace('_', ' ')} destroyed")
        }
        return overflow
    }

    private fun report(world: World, target: Vessel, amount: Double, attackerId: String, weapon: String) {
        val kind = when {
            target === world.player -> EventKind.DAMAGE_TAKEN
            attackerId == world.player.id -> EventKind.DAMAGE_DEALT
            else -> return
        }
        val text = if (kind == EventKind.DAMAGE_TAKEN) "Hit by ${nameOf(world, attackerId)}'s $weapon"
        else "${target.name} hit by $weapon"
        world.log.addMerging(GameEvent(world.time, kind, attackerId, text, target.id, amount))
    }

    private fun nameOf(world: World, id: String) = world.vessels.firstOrNull { it.id == id }?.name ?: id
}
