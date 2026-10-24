package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.Planet
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * Puts planets and moons where their fixed circular orbits say at the
 * current time. Moons orbit their planet, so planets come first.
 */
class PlanetMover {
    fun run(world: World) {
        for (p in world.planets) place(p, world.time)
    }

    fun place(p: Planet, time: Double) {
        p.position = positionAt(p, time)
        p.velocity = velocityAt(p, time)
    }

    /** Where [p] is at [time], without moving it. */
    fun positionAt(p: Planet, time: Double): Vec3 {
        val theta = p.phase + 2 * Math.PI / p.period * time
        val y = p.orbitRadius * sin(theta)
        val local = Vec3(p.orbitRadius * cos(theta), y * cos(p.inclination), y * sin(p.inclination))
        return (p.parent?.let { positionAt(it, time) } ?: Vec3.ZERO) + local
    }

    fun velocityAt(p: Planet, time: Double): Vec3 {
        val omega = 2 * Math.PI / p.period
        val theta = p.phase + omega * time
        val vy = p.orbitRadius * omega * cos(theta)
        val local = Vec3(-p.orbitRadius * omega * sin(theta), vy * cos(p.inclination), vy * sin(p.inclination))
        return (p.parent?.let { velocityAt(it, time) } ?: Vec3.ZERO) + local
    }
}
