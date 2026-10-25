package neumann.core.game

import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.Fix
import neumann.core.world.World
import kotlin.math.sqrt

/** A ship drawn twice: where it was [seen], and where it probably is [now], within [radiusM]. */
class ShipEstimate(val contact: Contact, val seen: Fix, val now: Vec3, val radiusM: Double)

/**
 * Dead reckoning for the player's map: every vessel contact with a position
 * fix, carried from that fix to the present under gravity, assuming it
 * coasts (see docs/sensors.md).
 */
class ContactEstimator(private val world: World) {
    private val coaster = Coaster(world.star, world.planets)

    fun estimate(): List<ShipEstimate> {
        val book = world.player.contacts
        return book.current.values.filter { it.kind.isVessel }.mapNotNull { c ->
            val fix = book.lastFix[c.id] ?: return@mapNotNull null
            val since = world.time - fix.time
            val state = coaster.coast(fix.position, fix.velocity, fix.time, world.time)
            val drift = fix.velocityUncertaintyMS * since
            ShipEstimate(c, fix, state.position, sqrt(fix.uncertaintyM * fix.uncertaintyM + drift * drift))
        }
    }
}
