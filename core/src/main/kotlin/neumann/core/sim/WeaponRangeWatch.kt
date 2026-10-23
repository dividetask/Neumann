package neumann.core.sim

import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.World

/** Warns when the player enters a known enemy's weapon range. */
class WeaponRangeWatch {
    fun run(world: World) {
        val player = world.player
        if (!player.alive) return
        val watch = world.playerWatch
        val inside = HashSet<String>()
        for (c in player.contacts.current.values) {
            if (c.kind == ContactKind.TORPEDO) continue
            val enemy = world.vessel(c.id) ?: continue
            if (c.distance > ThreatRange.of(enemy.shipClass)) continue
            inside.add(c.id)
            if (c.id !in watch.inRangeOf) {
                world.event(EventKind.ENTERED_WEAPON_RANGE, c.id, "Entered weapon range of ${c.name}")
            }
        }
        watch.inRangeOf.clear()
        watch.inRangeOf.addAll(inside)
    }
}
