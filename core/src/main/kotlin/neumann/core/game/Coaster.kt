package neumann.core.game

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.PlanetMover
import neumann.core.world.Planet
import neumann.core.world.Star
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Carries a coasting object forward in time under the gravity of the star,
 * planets, and moons, which move on their orbits meanwhile.
 */
class Coaster(private val star: Star, private val bodies: List<Planet>) {
    class State(val position: Vec3, val velocity: Vec3)

    private val mover = PlanetMover()

    /** From [position] and [velocity] at time [from] to time [to]. */
    fun coast(position: Vec3, velocity: Vec3, from: Double, to: Double): State {
        val span = to - from
        if (span <= 0.0) return State(position, velocity)
        val steps = min(MAX_STEPS, max(1, ceil(span / STEP_SECONDS).toInt()))
        val dt = span / steps
        var p = position
        var v = velocity
        for (i in 0 until steps) {
            val t = from + i * dt
            var a = pull(-p, star.mass, star.radius)
            for (b in bodies) a += pull(mover.positionAt(b, t) - p, b.mass, b.radius)
            v += a * dt
            p += v * dt
        }
        return State(p, v)
    }

    private fun pull(toBody: Vec3, mass: Double, radius: Double): Vec3 {
        val r = max(toBody.length(), radius)
        return toBody.normalized() * (Units.G * mass / (r * r))
    }

    companion object {
        const val STEP_SECONDS = 60.0
        /** Caps the work for a fix days old; the steps then grow longer. */
        const val MAX_STEPS = 3000
    }
}
