package neumann.core.sim

import neumann.core.math.Vec3
import neumann.core.world.Planet
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin

/** Puts planets where their fixed circular orbits say at the current time. */
class PlanetMover {
    fun run(world: World) {
        for (p in world.planets) place(p, world.time)
    }

    fun place(p: Planet, time: Double) {
        val omega = 2 * Math.PI / p.period
        val theta = p.phase + omega * time
        val ci = cos(p.inclination)
        val si = sin(p.inclination)
        val x = p.orbitRadius * cos(theta)
        val y = p.orbitRadius * sin(theta)
        p.position = Vec3(x, y * ci, y * si)
        val vx = -p.orbitRadius * omega * sin(theta)
        val vy = p.orbitRadius * omega * cos(theta)
        p.velocity = Vec3(vx, vy * ci, vy * si)
    }
}
