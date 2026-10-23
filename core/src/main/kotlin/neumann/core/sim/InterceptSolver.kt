package neumann.core.sim

import neumann.core.math.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Aim direction for a projectile fired at [speed] relative to the shooter, to
 * meet a target at relative position [r] moving at relative velocity [v].
 * Null if it can never catch the target.
 */
class InterceptSolver {
    fun aim(r: Vec3, v: Vec3, speed: Double): Vec3? {
        val a = v.dot(v) - speed * speed
        val b = 2 * r.dot(v)
        val c = r.dot(r)
        val t = if (abs(a) < 1e-9) {
            if (b >= 0) return null
            -c / b
        } else {
            val disc = b * b - 4 * a * c
            if (disc < 0) return null
            val sq = sqrt(disc)
            listOf((-b - sq) / (2 * a), (-b + sq) / (2 * a)).filter { it > 0 }.minOrNull() ?: return null
        }
        return (r + v * t).normalized()
    }
}
