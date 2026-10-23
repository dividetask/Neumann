package neumann.core.sim

import neumann.core.data.SimConfig
import neumann.core.math.Vec3
import neumann.core.world.Torpedo
import neumann.core.world.World
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Steers and thrusts torpedoes (zero-effort-miss guidance). A torpedo uses
 * its own seeker, so it sees its target's true position. It speeds up until
 * only its terminal reserve of delta-v is left, then uses that only to steer.
 */
class TorpedoGuidance(private val config: SimConfig) {
    fun run(world: World, dt: Double) {
        for (t in world.torpedoes) {
            if (!t.alive) continue
            t.burning = false
            val target = world.vessel(t.targetId)
            if (target == null || world.time - t.launchTime > config.torpedoLifetimeSeconds) {
                t.alive = false
                continue
            }
            if (t.deltaVLeft <= 0.0) continue
            val reserve = t.config.deltaVKmS * 1000.0 * t.config.terminalReserveFraction
            val a = command(t, target.position - t.position, target.velocity - t.velocity, steerOnly = t.deltaVLeft <= reserve)
            if (a.lengthSquared() < 1e-12) continue
            val dv = min(min(a.length(), t.config.accelerationMS2) * dt, t.deltaVLeft)
            t.velocity += a.normalized() * dv
            t.deltaVLeft -= dv
            t.burning = true
        }
    }

    /** Acceleration wanted; with [steerOnly], just the sideways correction. */
    private fun command(t: Torpedo, r: Vec3, v: Vec3, steerOnly: Boolean): Vec3 {
        val aMax = t.config.accelerationMS2
        val dist = r.length()
        val los = r.normalized()
        val closing = -r.dot(v) / dist
        if (closing <= 0.0) return los * aMax
        val tGo = dist / closing
        val zem = r + v * tGo
        val cmd = zem * (NAVIGATION_GAIN / (tGo * tGo))
        val lateral = cmd - los * cmd.dot(los)
        val lat = lateral.length()
        if (steerOnly) return if (lat >= aMax) lateral.normalized() * aMax else lateral
        return if (lat >= aMax) lateral.normalized() * aMax else lateral + los * sqrt(aMax * aMax - lat * lat)
    }

    companion object {
        const val NAVIGATION_GAIN = 3.0
    }
}
