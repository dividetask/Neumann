package neumann.core.sim

import neumann.core.world.Vessel
import kotlin.math.abs
import kotlin.math.max

/** Estimates how long a vessel's burn has left, in seconds, so steps can shorten near its end. */
class BurnClock(private val performance: ShipPerformance, private val resolver: HeadingResolver) {
    fun secondsLeft(v: Vessel, now: Double): Double? {
        val burn = v.burn ?: return null
        val a = performance.accelerationMS2(v, burn.drive)
        if (a <= 0.0) return null
        resolver.matchGap(v)?.let { return it / a }
        val dir = resolver.resolve(v) ?: return null
        val speed = (v.velocity - burn.startVelocity).dot(dir)
        val bySpeed = burn.targetSpeed?.let { abs(it - speed) / a }
        val byDistance = burn.targetDistance?.let {
            val travelled = (v.position - burn.startPosition - burn.startVelocity * (now - burn.startTime)).dot(dir)
            (it - travelled) / max(abs(speed), 1.0)
        }
        return listOfNotNull(bySpeed, byDistance).minOrNull()
    }
}
