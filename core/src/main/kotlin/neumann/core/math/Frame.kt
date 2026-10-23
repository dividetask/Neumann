package neumann.core.math

import kotlin.math.cos
import kotlin.math.sin

/** Facing helpers: rotation, choosing an "up", and ship-relative directions. */
object Frame {
    /** Rotates [v] about the unit [axis] by [angle] radians. */
    fun rotate(v: Vec3, axis: Vec3, angle: Double): Vec3 {
        val c = cos(angle)
        val s = sin(angle)
        return v * c + axis.cross(v) * s + axis * (axis.dot(v) * (1 - c))
    }

    /** An "up" at right angles to [forward], as close to the system's north (+z) as possible. */
    fun upFor(forward: Vec3): Vec3 {
        val f = forward.normalized()
        val north = if (kotlin.math.abs(f.z) > 0.999) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 0.0, 1.0)
        return (north - f * north.dot(f)).normalized()
    }

    /** A ship-relative direction (forward, right, up components) as a direction in space. */
    fun toWorld(forward: Vec3, up: Vec3, rel: Vec3): Vec3 =
        (forward * rel.x + forward.cross(up) * rel.y + up * rel.z).normalized()

    /** A direction in space as ship-relative (forward, right, up) components. */
    fun toShip(forward: Vec3, up: Vec3, dir: Vec3): Vec3 {
        val d = dir.normalized()
        return Vec3(d.dot(forward), d.dot(forward.cross(up)), d.dot(up))
    }
}
