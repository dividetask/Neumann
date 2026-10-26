package neumann.core.game

import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.Fix
import neumann.core.world.World
import kotlin.math.sqrt

/**
 * A ship or torpedo drawn twice: where it was [seen], and where it probably
 * is [now] within [radiusM]. A [ghost] has a fix too old to estimate from,
 * so [now] is null and only where it was seen is drawn. [tracked] is false
 * once the sensors have lost it.
 */
class ContactEstimate(val seen: Fix, val now: Vec3?, val radiusM: Double, val tracked: Boolean) {
    val contact: Contact get() = seen.contact
    val ghost: Boolean get() = now == null
}

/**
 * Dead reckoning for the player's map: every ship and torpedo with a
 * position fix, carried from that fix to the present under gravity,
 * assuming it coasts; ghosts once the fix is older than
 * `estimateMaxAgeSeconds` (see docs/sensors.md).
 */
class ContactEstimator(private val world: World) {
    private val coaster = Coaster(world.star, world.planets)
    private val maxAge = world.data.rules.map.estimateMaxAgeSeconds

    fun estimate(): List<ContactEstimate> {
        val book = world.player.contacts
        return book.lastFix.values.map { fix ->
            val since = world.time - fix.time
            val tracked = fix.contact.id in book.current
            if (since > maxAge) return@map ContactEstimate(fix, null, 0.0, tracked)
            val state = coaster.coast(fix.position, fix.velocity, fix.time, world.time)
            val drift = fix.contact.velocityUncertaintyMS * since
            val position = fix.contact.uncertaintyM
            ContactEstimate(fix, state.position, sqrt(position * position + drift * drift), tracked)
        }
    }
}
