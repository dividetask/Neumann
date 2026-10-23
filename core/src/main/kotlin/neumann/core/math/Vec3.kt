package neumann.core.math

import kotlin.math.sqrt

/** A 3D vector. */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Double) = Vec3(x / s, y / s, z / s)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun lengthSquared() = dot(this)
    fun length() = sqrt(lengthSquared())

    /** Unit vector in the same direction, or ZERO for a zero vector. */
    fun normalized(): Vec3 {
        val l = length()
        return if (l == 0.0) ZERO else this / l
    }

    companion object {
        val ZERO = Vec3(0.0, 0.0, 0.0)

        fun of(list: List<Double>): Vec3 {
            require(list.size == 3) { "a vector needs 3 numbers, got ${list.size}" }
            return Vec3(list[0], list[1], list[2])
        }
    }
}
