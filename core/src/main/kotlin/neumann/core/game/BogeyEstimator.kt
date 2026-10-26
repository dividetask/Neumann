package neumann.core.game

import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.Fix
import neumann.core.world.World
import kotlin.math.sqrt

/**
 * A bogey drawn twice: where it was [seen], and where it probably is [now]
 * within [radiusM]. A [ghost] has a fix too old to estimate from, so [now]
 * is null and only where it was seen is drawn. [tracked] is false once the
 * sensors have lost it.
 */
class BogeyEstimate(val seen: Fix, val now: Vec3?, val radiusM: Double, val tracked: Boolean) {
    val contact: Contact get() = seen.contact
    val ghost: Boolean get() = now == null
}

/**
 * Dead reckoning for the player's map: every bogey with a position fix,
 * carried from that fix to the present under gravity, assuming it coasts;
 * a ghost once the fix is older than `estimateMaxAgeSeconds`, and gone
 * after `ghostMaxAgeSeconds` (see docs/sensors.md).
 */
class BogeyEstimator(private val world: World) {
    private val coaster = Coaster(world.star, world.planets)
    private val config = world.data.rules.map

    fun estimate(): List<BogeyEstimate> {
        val book = world.player.contacts
        return book.lastFix.values.mapNotNull { fix ->
            val since = world.time - fix.time
            val tracked = fix.contact.id in book.current
            when {
                since > config.ghostMaxAgeSeconds -> null
                since > config.estimateMaxAgeSeconds -> BogeyEstimate(fix, null, 0.0, tracked)
                else -> {
                    val state = coaster.coast(fix.position, fix.velocity, fix.time, world.time)
                    val drift = fix.contact.velocityUncertaintyMS * since
                    val position = fix.contact.uncertaintyM
                    BogeyEstimate(fix, state.position, sqrt(position * position + drift * drift), tracked)
                }
            }
        }
    }
}
