package neumann.core.game

import neumann.core.world.ContactKind
import neumann.core.world.World

/** How many enemy ships, fixed defences, and bases the player has seen and not destroyed. */
class Remaining(val ships: Int, val defences: Int, val bases: Int) {
    val total: Int get() = ships + defences + bases
}

/**
 * Counts what is left to destroy to claim the system, as far as the
 * player's sensors know: not what is really there. Torpedoes do not count.
 */
class RemainingCount(private val world: World) {
    fun count(): Remaining {
        val book = world.player.contacts
        val left = book.everSeen.filter { (id, kind) -> kind.isVessel && id !in book.destroyed }.values
        return Remaining(left.count { it == ContactKind.SHIP }, left.count { it == ContactKind.DEFENCE }, left.count { it == ContactKind.BASE })
    }
}
