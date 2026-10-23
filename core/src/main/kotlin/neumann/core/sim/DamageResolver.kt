package neumann.core.sim

import neumann.core.world.EventKind
import neumann.core.world.GameEvent
import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.Vessel
import neumann.core.world.World

/**
 * Applies a hit to a vessel. The hit lands on one module chosen at random by
 * weight; damage it cannot absorb goes to a working hull module. The vessel
 * is destroyed when no hull module is left.
 */
class DamageResolver {
    fun hit(world: World, target: Vessel, amount: Double, attackerId: String, weapon: String) {
        if (!target.alive || amount <= 0.0) return
        var left = applyTo(world, target, pickModule(world, target), amount)
        while (left > 0.0) {
            val hull = target.working(ModuleKind.HULL).firstOrNull() ?: break
            left = applyTo(world, target, hull, left)
        }
        report(world, target, amount, attackerId, weapon)
        if (!target.has(ModuleKind.HULL)) destroy(world, target, "by ${nameOf(world, attackerId)}'s $weapon")
    }

    fun destroy(world: World, target: Vessel, how: String) {
        if (!target.alive) return
        target.alive = false
        target.burn = null
        world.event(EventKind.DESTROYED, target.id, "${target.name} destroyed $how")
    }

    private fun pickModule(world: World, target: Vessel): ModuleState {
        var roll = world.random.nextDouble() * target.modules.sumOf { it.type.massT }
        for (m in target.modules) {
            roll -= m.type.massT
            if (roll < 0) return m
        }
        return target.modules.last()
    }

    /** Damages one module; returns the damage it could not absorb. */
    private fun applyTo(world: World, target: Vessel, m: ModuleState, amount: Double): Double {
        if (m.destroyed) return amount
        val share = shareOfContents(target, m)
        m.hitPoints -= amount
        if (!m.destroyed) return 0.0
        val overflow = -m.hitPoints
        m.hitPoints = 0.0
        loseContents(target, m, share)
        world.event(EventKind.INFO, target.id, "${target.name}: ${m.type.name} destroyed")
        return overflow
    }

    /** This module's share of what all working modules of its kind hold. */
    private fun shareOfContents(v: Vessel, m: ModuleState): Double {
        fun share(cap: (ModuleState) -> Double): Double {
            val total = v.working(m.kind).sumOf(cap)
            return if (total == 0.0) 0.0 else cap(m) / total
        }
        return when (m.kind) {
            ModuleKind.FUEL_TANK -> share { it.type.fuelTank!!.capacityT }
            ModuleKind.AMMO_CONTAINER -> share { it.type.ammoContainer!!.capacity.toDouble() }
            ModuleKind.BATTERY -> share { it.type.battery!!.capacityMJ }
            else -> 0.0
        }
    }

    private fun loseContents(v: Vessel, m: ModuleState, share: Double) {
        when (m.kind) {
            ModuleKind.FUEL_TANK -> v.fuel -= v.fuel * share
            ModuleKind.AMMO_CONTAINER -> v.slugs -= Math.round(v.slugs * share).toInt()
            ModuleKind.BATTERY -> v.energy -= v.energy * share
            ModuleKind.MISSILE_BAY -> m.torpedoes = 0
            ModuleKind.PROPULSION -> if (!v.has(ModuleKind.PROPULSION)) v.burn = null
            else -> {}
        }
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
