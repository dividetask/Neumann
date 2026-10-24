package neumann.core.game

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.PlanetMover
import neumann.core.world.Planet
import neumann.core.world.Star
import kotlin.math.max

/**
 * Predicts a coasting path under the gravity of the star, planets, and moons
 * (which follow their orbits meanwhile), for the approach arrow.
 */
class TrajectoryPredictor(private val star: Star, private val bodies: List<Planet>) {
    /** The path, and the body it passes closest to (relative to that body's size). */
    class Prediction(val points: List<Vec3>, val closest: Planet?, val closestDistance: Double, val closestTime: Double, val hits: Boolean)

    private val mover = PlanetMover()

    fun predict(start: Vec3, velocity: Vec3, seconds: Double, step: Double, pointEvery: Int = 4): Prediction {
        var p = start
        var v = velocity
        val points = arrayListOf(start)
        var closest: Planet? = null
        var closestRatio = Double.MAX_VALUE
        var closestDistance = Double.MAX_VALUE
        var closestTime = 0.0
        var t = 0.0
        var i = 0
        while (t < seconds) {
            val at = bodies.associateWith { mover.positionAt(it, t) }
            var a = pull(-p, star.mass, star.radius)
            for ((b, pos) in at) a += pull(pos - p, b.mass, b.radius)
            v += a * step
            p += v * step
            t += step
            if (++i % pointEvery == 0) points.add(p)
            if (p.length() < star.radius) return Prediction(points + p, closest, closestDistance, closestTime, hits = true)
            for ((b, pos) in at) {
                val d = (p - pos).length()
                if (d < b.radius) return Prediction(points + p, b, d, t, hits = true)
                // Moons are small; judge closeness by distance over the body's own scale.
                val ratio = d / max(b.radius, 1.0)
                if (!b.isMoon && ratio < closestRatio) {
                    closestRatio = ratio
                    closest = b
                    closestDistance = d
                    closestTime = t
                }
            }
        }
        return Prediction(points, closest, closestDistance, closestTime, hits = false)
    }

    private fun pull(toBody: Vec3, mass: Double, radius: Double): Vec3 {
        val r = max(toBody.length(), radius)
        return toBody.normalized() * (Units.G * mass / (r * r))
    }
}
