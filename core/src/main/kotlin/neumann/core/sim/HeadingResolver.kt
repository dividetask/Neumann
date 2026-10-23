package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.HeadingMode
import neumann.core.world.Vessel

/** The direction in space a vessel's heading points now, or null if it has none. */
class HeadingResolver {
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
        } ?: return null
        return if (dir.lengthSquared() == 0.0) null else dir.normalized()
    }
}
