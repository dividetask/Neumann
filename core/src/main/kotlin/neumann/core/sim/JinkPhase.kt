package neumann.core.sim

import neumann.core.data.ShipRules
import neumann.core.math.Noise
import neumann.core.world.World

/**
 * Evasive manoeuvres: a vessel with evasion on pushes sideways with its
 * translating thrusters, in a new random direction (at right angles to its
 * front) every jink interval. Uses fuel; counts as burning.
 */
class JinkPhase(private val performance: ShipPerformance, private val rules: ShipRules) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || !v.evasive || v.fuel <= 0.0) continue
            val thrust = performance.translationThrustN(v)
            if (thrust <= 0.0) continue
            v.jinkTimeLeft -= dt
            if (v.jinkTimeLeft <= 0.0 || v.jinkDirection.lengthSquared() == 0.0) {
                val r = Noise.vector(world.random, 1.0)
                v.jinkDirection = (r - v.forward * r.dot(v.forward)).normalized()
                v.jinkTimeLeft = rules.jinkIntervalSeconds
            }
            v.velocity += v.jinkDirection * (thrust / performance.massKg(v) * dt)
            v.fuel = (v.fuel - performance.translationFuelKgS(v) * dt / 1000.0).coerceAtLeast(0.0)
            v.burning = true
        }
    }
}
