package neumann.core.sim

import neumann.core.data.FactoryConfig
import neumann.core.math.Units
import neumann.core.world.PartKind
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.min

/**
 * Turns minerals into repairs, then fuel, then slugs, then torpedoes, at the
 * factory's working rate.
 */
class FactoryPhase {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || !v.working(PartKind.FACTORY)) continue
            val f = v.shipClass.factory ?: continue
            var budget = min(v.minerals, f.mineralsPerHour * dt / Units.HOUR)
            val start = budget
            budget = repair(v, f, budget)
            budget = refuel(v, f, budget)
            budget = makeSlugs(v, f, budget)
            budget = makeTorpedoes(v, f, budget)
            v.minerals -= start - budget
        }
    }

    private fun repair(v: Vessel, f: FactoryConfig, budget: Double): Double {
        var left = budget
        val order = listOfNotNull(v.parts[PartKind.HULL]) + v.parts.values.filter { it.kind != PartKind.HULL }
        for (p in order) {
            if (left <= 0.0) break
            val use = min(left, (p.maxHitPoints - p.hitPoints) / f.repairPointsPerMineral)
            p.hitPoints += use * f.repairPointsPerMineral
            left -= use
        }
        return left
    }

    private fun refuel(v: Vessel, f: FactoryConfig, budget: Double): Double {
        val drive = v.shipClass.drive ?: return budget
        if (!v.working(PartKind.FUEL_TANK) || budget <= 0.0) return budget
        val use = min(budget, (drive.fuelCapacity - v.fuel) / f.fuelPerMineral)
        v.fuel += use * f.fuelPerMineral
        return budget - use
    }

    private fun makeSlugs(v: Vessel, f: FactoryConfig, budget: Double): Double {
        val cap = v.shipClass.cannon?.ammo ?: return budget
        if (v.slugs >= cap || budget <= 0.0) return budget
        val use = min(budget, (cap - v.slugs) * f.mineralsPerSlug - v.slugProgress)
        v.slugProgress += use
        val made = (v.slugProgress / f.mineralsPerSlug).toInt()
        v.slugs += made
        v.slugProgress -= made * f.mineralsPerSlug
        return budget - use
    }

    private fun makeTorpedoes(v: Vessel, f: FactoryConfig, budget: Double): Double {
        val cap = v.shipClass.torpedo?.ammo ?: return budget
        if (v.torpedoes >= cap || budget <= 0.0) return budget
        val use = min(budget, (cap - v.torpedoes) * f.mineralsPerTorpedo - v.torpedoProgress)
        v.torpedoProgress += use
        val made = (v.torpedoProgress / f.mineralsPerTorpedo).toInt()
        v.torpedoes += made
        v.torpedoProgress -= made * f.mineralsPerTorpedo
        return budget - use
    }
}
