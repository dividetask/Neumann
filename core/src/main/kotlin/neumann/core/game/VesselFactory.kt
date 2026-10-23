package neumann.core.game

import neumann.core.data.GameData
import neumann.core.math.Frame
import neumann.core.math.Vec3
import neumann.core.world.Faction
import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.Vessel

/** Builds a vessel from its design: one module state per fitted module, fully loaded. */
class VesselFactory(private val data: GameData) {
    fun build(
        id: String,
        name: String,
        faction: Faction,
        designId: String,
        position: Vec3,
        velocity: Vec3,
        forward: Vec3,
    ): Vessel {
        val design = data.designs.getValue(designId)
        val modules = design.modules.flatMap { (typeId, count) ->
            List(count) { ModuleState(typeId, data.modules.getValue(typeId)) }
        }
        val f = forward.normalized()
        val v = Vessel(id, name, faction, design, modules, position, velocity, f, Frame.upFor(f))
        for (m in modules) if (m.kind == ModuleKind.MISSILE_BAY) m.torpedoes = m.type.missileBay!!.capacity
        v.fuel = modules.sumOf { it.type.fuelTank?.capacityT ?: 0.0 }
        v.slugs = modules.sumOf { it.type.ammoContainer?.capacity ?: 0 }
        v.energy = modules.sumOf { it.type.battery?.capacityMJ ?: 0.0 }
        return v
    }
}
