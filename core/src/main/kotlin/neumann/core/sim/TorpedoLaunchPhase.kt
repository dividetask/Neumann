package neumann.core.sim

import neumann.core.data.TorpedoConfig
import neumann.core.math.Units
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.HistorySample
import neumann.core.world.ModuleKind
import neumann.core.world.Torpedo
import neumann.core.world.World
import kotlin.math.max

/**
 * Launches torpedoes from missile bays. Each bay launches its own torpedoes
 * when ready and the target is in its arc. A launch-now order waits until a
 * bay can launch.
 */
class TorpedoLaunchPhase(private val torpedo: TorpedoConfig) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val bays = v.working(ModuleKind.MISSILE_BAY)
            for (b in bays) b.cooldown = max(0.0, b.cooldown - dt)
            val target = v.contacts[v.targetId]?.takeIf { it.kind != ContactKind.TORPEDO }
            if (v.launchTorpedoNow && (target == null || v.torpedoes <= 0)) {
                v.launchTorpedoNow = false
                if (v === world.player) world.event(EventKind.INFO, v.id, "Torpedo cannot launch: " +
                    if (target == null) "no target" else "no torpedoes")
                continue
            }
            if (target == null) continue
            val dir = (target.predictedPosition - v.position).normalized()
            for (b in bays) {
                if (b.torpedoes <= 0 || b.cooldown > 0.0) continue
                val cfg = b.type.missileBay!!
                val auto = v.rules.torpedoAutoLaunch && target.distance <= cfg.launchRangeKm * Units.KM
                if (!(v.launchTorpedoNow || auto)) continue
                if (v.forward.angleTo(dir) > cfg.arcDeg * Units.DEGREE) continue
                val id = world.newId("T")
                val t = Torpedo(id, "Torpedo $id", v.id, v.faction, target.id, torpedo, v.position,
                    v.velocity + dir * (torpedo.launchSpeedKmS * Units.KM), world.time)
                t.history.record(HistorySample(world.time, t.position, t.velocity, false))
                world.torpedoes.add(t)
                b.torpedoes--
                b.cooldown = cfg.cooldownSeconds
                v.launchTorpedoNow = false
                if (v === world.player) world.event(EventKind.INFO, v.id, "${t.name} launched at ${target.name}")
            }
        }
    }
}
