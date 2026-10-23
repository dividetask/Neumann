package neumann.core.game

import neumann.core.math.Frame
import neumann.core.math.Units
import neumann.core.math.Vec3
import kotlin.math.asin
import kotlin.math.atan2

/**
 * The YAW, PITCH, and ROLL dials. Angles are degrees from the top of each
 * dial (the ship's current facing), clockwise positive: yaw right, pitch up,
 * roll right. The turn is yaw, then pitch, then roll.
 */
object DirectionDials {
    data class Angles(val yaw: Double, val pitch: Double, val roll: Double)

    /** An orientation in space. */
    data class Orientation(val forward: Vec3, val up: Vec3)

    /** The orientation reached by turning [a] from the facing ([forward], [up]). */
    fun orientation(forward: Vec3, up: Vec3, a: Angles): Orientation {
        val yaw = a.yaw * Units.DEGREE
        val pitch = a.pitch.coerceIn(-90.0, 90.0) * Units.DEGREE
        // Yaw right turns about -up; pitch up turns about +right; roll right turns about +forward.
        val f1 = Frame.rotate(forward, -up, yaw)
        val right1 = f1.cross(up)
        val f2 = Frame.rotate(f1, right1, pitch)
        val u2 = Frame.rotate(up, right1, pitch)
        val u3 = Frame.rotate(u2, f2, a.roll * Units.DEGREE)
        return Orientation(f2.normalized(), u3.normalized())
    }

    /** The turn still to go from the facing ([forward], [up]) to [target]; roll is 0 if [target] has no up. */
    fun remaining(forward: Vec3, up: Vec3, targetForward: Vec3, targetUp: Vec3?): Angles {
        val rel = Frame.toShip(forward, up, targetForward)
        val yaw = Math.toDegrees(atan2(rel.y, rel.x))
        val pitch = Math.toDegrees(asin(rel.z.coerceIn(-1.0, 1.0)))
        if (targetUp == null) return Angles(yaw, pitch, 0.0)
        val unrolled = orientation(forward, up, Angles(yaw, pitch, 0.0))
        val tu = targetUp - unrolled.forward * targetUp.dot(unrolled.forward)
        val sign = if (unrolled.up.cross(tu).dot(unrolled.forward) >= 0) 1.0 else -1.0
        return Angles(yaw, pitch, sign * Math.toDegrees(unrolled.up.angleTo(tu)))
    }
}
