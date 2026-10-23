package neumann.core.sim

import neumann.core.data.SimConfig
import neumann.core.world.World

/** Moves vessels, torpedoes, and slugs under gravity. */
class MotionPhase(private val gravity: GravityField, private val config: SimConfig) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            v.previousPosition = v.position
            v.velocity += gravity.acceleration(world, v.position) * dt
            v.position += v.velocity * dt
        }
        for (t in world.torpedoes) {
            if (!t.alive) continue
            t.previousPosition = t.position
            t.velocity += gravity.acceleration(world, t.position) * dt
            t.position += t.velocity * dt
        }
        for (s in world.slugs) {
            if (!s.alive) continue
            if (world.time - s.launchTime > config.slugLifetimeSeconds) {
                s.alive = false
                continue
            }
            s.previousPosition = s.position
            s.velocity += gravity.acceleration(world, s.position) * dt
            s.position += s.velocity * dt
        }
    }
}
