package neumann.core.sim

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.World
import kotlin.math.max

/** Gravitational acceleration from the star and planets at a point. */
class GravityField {
    fun acceleration(world: World, at: Vec3): Vec3 {
        var a = pull(Vec3.ZERO - at, world.star.mass, world.star.radius)
        for (p in world.planets) a += pull(p.position - at, p.mass, p.radius)
        return a
    }

    private fun pull(toBody: Vec3, mass: Double, radius: Double): Vec3 {
        val r = max(toBody.length(), radius)
        return toBody.normalized() * (Units.G * mass / (r * r))
    }
}
