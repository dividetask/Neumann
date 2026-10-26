package neumann.core.sim

import neumann.core.data.ComputerConfig
import neumann.core.data.GameData
import neumann.core.math.Units
import neumann.core.world.Contact
import neumann.core.world.EventKind
import neumann.core.world.HistorySample
import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.Torpedo
import neumann.core.world.Vessel
import neumann.core.world.World

/**
 * Launches torpedoes. Each torpedo has its own launch cell, so a bay can
 * launch everything it holds at once. Automatic launches are planned
 * salvos: every ship of a side with auto-launch on and the same target in
 * range and arc pools its torpedoes, and they launch together only if
 * [SalvoPlanner] expects some to get through. A launch-now order launches
 * one torpedo, unplanned.
 */
class TorpedoLaunchPhase(data: GameData) {
    private val torpedo = data.munitions.torpedo
    private val planner = SalvoPlanner(data)

    fun run(world: World) {
        for (v in world.vessels) if (v.active && v.launchTorpedoNow) launchNow(world, v)
        val groups = world.vessels.filter { it.active && it.rules.torpedoAutoLaunch && it.targetId != null }
            .groupBy { Triple(it.faction, it.targetId!!, it.computer!!.type.computer!!) }
        for ((key, shooters) in groups) salvo(world, key.second, shooters, key.third)
    }

    private fun launchNow(world: World, v: Vessel) {
        v.launchTorpedoNow = false
        val target = v.contacts[v.targetId]?.takeIf { it.kind.isVessel }
        val bay = target?.let { t -> readyBays(v, t, inRange = false).firstOrNull() }
        if (target == null || bay == null) {
            if (v === world.player) world.event(EventKind.INFO, v.id, "Torpedo cannot launch: " + when {
                target == null -> "no target"
                v.torpedoes <= 0 -> "no torpedoes"
                else -> "target outside every missile bay's arc"
            })
            return
        }
        launch(world, v, bay, target)
    }

    /** Ships with the same computer plan alike, so they pool into one salvo. */
    private fun salvo(world: World, targetId: String, shooters: List<Vessel>, computer: ComputerConfig) {
        val design = world.vessel(targetId)?.design ?: return
        // Each shooter's bays that could launch at the target now.
        val ready = shooters.mapNotNull { v ->
            val c = v.contacts[targetId]?.takeIf { it.kind.isVessel } ?: return@mapNotNull null
            val bays = readyBays(v, c, inRange = true)
            if (bays.isEmpty()) null else Triple(v, c, bays)
        }
        if (ready.isEmpty()) return
        val available = ready.sumOf { (_, _, bays) -> bays.sumOf { it.torpedoes } }
        // Planned on the side's best track of the target.
        val best = ready.minBy { it.second.velocityUncertaintyMS }.second
        var left = planner.size(available, design, best, computer)
        for ((v, c, bays) in ready) for (b in bays) {
            while (left > 0 && b.torpedoes > 0) {
                launch(world, v, b, c)
                left--
            }
        }
    }

    /** Loaded bays with [target] in their arc (and, with [inRange], their launch range). */
    private fun readyBays(v: Vessel, target: Contact, inRange: Boolean): List<ModuleState> {
        val dir = (target.predictedPosition - v.position).normalized()
        return v.working(ModuleKind.MISSILE_BAY).filter { b ->
            val cfg = b.type.missileBay!!
            b.torpedoes > 0 && v.forward.angleTo(dir) <= cfg.arcDeg * Units.DEGREE &&
                (!inRange || target.distance <= cfg.launchRangeKm * Units.KM)
        }
    }

    private fun launch(world: World, v: Vessel, bay: ModuleState, target: Contact) {
        val dir = (target.predictedPosition - v.position).normalized()
        val id = world.newId("T")
        val t = Torpedo(id, "Torpedo $id", v.id, v.faction, target.id, torpedo, v.position,
            v.velocity + dir * (torpedo.launchSpeedKmS * Units.KM), world.time)
        t.history.record(HistorySample(world.time, t.position, t.velocity, false))
        world.torpedoes.add(t)
        bay.torpedoes--
        if (v === world.player) world.event(EventKind.INFO, v.id, "${t.name} launched at ${target.name}")
    }
}
