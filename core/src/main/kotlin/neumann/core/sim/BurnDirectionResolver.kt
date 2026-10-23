package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.Burn
import neumann.core.world.BurnDirection
import neumann.core.world.Vessel

/** Turns a burn's direction preset into a unit vector, or null if it has none. */
class BurnDirectionResolver {
    fun resolve(vessel: Vessel, burn: Burn): Vec3? {
        val target = vessel.contacts[vessel.targetId]
        val dir = when (burn.direction) {
            BurnDirection.PROGRADE -> vessel.velocity
            BurnDirection.RETROGRADE -> -vessel.velocity
            BurnDirection.TOWARD_TARGET -> target?.let { it.predictedPosition - vessel.position }
            BurnDirection.AWAY_FROM_TARGET -> target?.let { vessel.position - it.predictedPosition }
            BurnDirection.MATCH_TARGET -> target?.let { it.observedVelocity - vessel.velocity }
            BurnDirection.VECTOR -> burn.vector
        } ?: return null
        return if (dir.lengthSquared() == 0.0) null else dir.normalized()
    }
}
