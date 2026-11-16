package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.BurnStage
import neumann.core.world.HeadingMode
import neumann.core.world.Vessel

/** The direction in space a vessel's heading points now, or null if it has none. */
class HeadingResolver {
    /** Which way the vessel should face: the heading, or its reverse while braking. */
    fun facing(v: Vessel): Vec3? {
        val dir = resolve(v) ?: return null
        return if (v.burn?.stage == BurnStage.BRAKE) -dir else dir
    }

    fun resolve(v: Vessel): Vec3? {
        val heading = v.heading ?: return null
        val target = v.contacts[v.targetId]
        val dir = when (heading.mode) {
            HeadingMode.FIXED -> heading.vector
            HeadingMode.PROGRADE -> v.velocity
            HeadingMode.RETROGRADE -> -v.velocity
            HeadingMode.TOWARD_TARGET -> target?.let { it.predictedPosition - v.position }
            HeadingMode.AWAY_FROM_TARGET -> target?.let { v.position - it.predictedPosition }
            HeadingMode.MATCH_TARGET -> target?.let { it.observedVelocity - v.velocity }
            HeadingMode.MATCH_VELOCITY -> heading.vector - v.velocity
        } ?: return null
        return if (dir.lengthSquared() == 0.0) null else dir.normalized()
    }

    /** For a matching heading, how far the velocity still is from the one to match (m/s); null otherwise. */
    fun matchGap(v: Vessel): Double? {
        val heading = v.heading ?: return null
        return when (heading.mode) {
            HeadingMode.MATCH_TARGET -> v.contacts[v.targetId]?.let { (it.observedVelocity - v.velocity).length() } ?: 0.0
            HeadingMode.MATCH_VELOCITY -> (heading.vector - v.velocity).length()
            else -> null
        }
    }
}
