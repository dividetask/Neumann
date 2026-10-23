package neumann.core.game

import neumann.core.math.Frame
import neumann.core.math.Units
import neumann.core.math.Vec3
import kotlin.math.asin
import kotlin.math.atan2

/**
 * The YAW and PITCH dials. Angles are degrees from the top of each dial (the
 * ship's current front), clockwise positive: yaw right, pitch up. The turn is
 * yaw, then pitch.
 */
object DirectionDials {
    data class Angles(val yaw: Double, val pitch: Double)

    /** The direction reached by turning [a] from the facing ([forward], [up]). */
    fun direction(forward: Vec3, up: Vec3, a: Angles): Vec3 {
        // Yaw right turns about -up; pitch up turns about the new right.
        val f1 = Frame.rotate(forward, -up, a.yaw * Units.DEGREE)
        return Frame.rotate(f1, f1.cross(up).normalized(), a.pitch.coerceIn(-90.0, 90.0) * Units.DEGREE).normalized()
    }

    /** The turn still to go from the facing ([forward], [up]) to [target]. */
    fun remaining(forward: Vec3, up: Vec3, target: Vec3): Angles {
        val rel = Frame.toShip(forward, up, target)
        return Angles(Math.toDegrees(atan2(rel.y, rel.x)), Math.toDegrees(asin(rel.z.coerceIn(-1.0, 1.0))))
    }
}
