package neumann.core.sim

import neumann.core.world.EventKind
import neumann.core.world.Faction
import neumann.core.world.World

/** Detects a claimed system (every enemy destroyed or dead) or a lost ship (destroyed, or its sentient core lost). */
class ClaimCheck {
    fun run(world: World) {
        if (world.over) return
        if (!world.player.active) {
            world.lost = true
            world.event(EventKind.SHIP_LOST, world.player.id, "${world.player.name} lost")
        } else if (world.vessels.none { it.active && it.faction == Faction.ENEMY }) {
            world.claimed = true
            world.event(EventKind.SYSTEM_CLAIMED, world.player.id, "System claimed")
        }
    }
}
