package neumann.core.game

import neumann.core.math.Vec3
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The FRONT and REAR direction circles. A dot at (x, y) in the unit circle
 * (x right, y up) is a ship-relative direction (forward, right, up):
 * the FRONT circle's centre is straight ahead and its edge 90° off; the REAR
 * circle's centre is straight back and its edge 90° off, seen from the front.
 */
object DirectionCircles {
    data class Dot(val rear: Boolean, val x: Double, val y: Double)

    fun toShip(dot: Dot): Vec3 {
        val r = min(1.0, sqrt(dot.x * dot.x + dot.y * dot.y))
        val off = r * Math.PI / 2
        val around = atan2(dot.y, dot.x)
        val ahead = if (dot.rear) -cos(off) else cos(off)
        return Vec3(ahead, sin(off) * cos(around), sin(off) * sin(around))
    }

    fun toDot(ship: Vec3): Dot {
        val d = ship.normalized()
        val off = acos(kotlin.math.abs(d.x).coerceAtMost(1.0))
        val r = off / (Math.PI / 2)
        val around = atan2(d.z, d.y)
        return Dot(d.x < 0, r * cos(around), r * sin(around))
    }
}
