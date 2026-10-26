package neumann.core.game

import neumann.core.world.Vessel
import neumann.core.world.World

/**
 * Fixed defences and bases the player has detected and not destroyed. They
 * cannot move on their own, so once found their position is known: the map
 * shows them where they are now (see docs/sensors.md).
 */
class KnownFixtures(private val world: World) {
    fun list(): List<Vessel> {
        val book = world.player.contacts
        return world.vessels.filter { v ->
            val kind = book.everSeen[v.id]
            kind != null && !kind.isBogey && v.alive && v.id !in book.destroyed
        }
    }
}
