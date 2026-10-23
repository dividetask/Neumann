package neumann.core.sim

import neumann.core.math.Units
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.HistorySample
import neumann.core.world.PartKind
import neumann.core.world.Torpedo
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.max

/** Launches torpedoes at the target. */
class TorpedoLaunchPhase {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            val config = v.shipClass.torpedo ?: continue
            v.torpedoCooldown = max(0.0, v.torpedoCooldown - dt)
            val target = v.contacts[v.targetId]?.takeIf { it.kind != ContactKind.TORPEDO }
            if (v.launchTorpedoNow && (target == null || v.torpedoes <= 0 || !v.working(PartKind.TORPEDO_LAUNCHER))) {
                v.launchTorpedoNow = false
                tellPlayer(world, v, "Torpedo cannot launch: " + when {
                    target == null -> "no target"
                    v.torpedoes <= 0 -> "no torpedoes"
                    else -> "launcher destroyed"
                })
                continue
            }
            if (target == null || v.torpedoes <= 0 || !v.working(PartKind.TORPEDO_LAUNCHER) || v.torpedoCooldown > 0.0) continue
            val auto = v.rules.torpedoAutoLaunch && target.distance <= config.launchRangeKm * Units.KM
            if (!(v.launchTorpedoNow || auto)) continue

            val dir = (target.predictedPosition - v.position).normalized()
            val id = world.newId("T")
            val t = Torpedo(id, "Torpedo $id", v.id, v.faction, target.id, config, v.position,
                v.velocity + dir * (config.launchSpeedKmS * Units.KM), world.time)
            t.history.record(HistorySample(world.time, t.position, t.velocity, false))
            world.torpedoes.add(t)
            v.torpedoes--
            v.torpedoCooldown = config.cooldownSeconds
            v.launchTorpedoNow = false
            tellPlayer(world, v, "${t.name} launched at ${target.name}")
        }
    }

    private fun tellPlayer(world: World, v: Vessel, text: String) {
        if (v === world.player) world.event(EventKind.INFO, v.id, text)
    }
}
