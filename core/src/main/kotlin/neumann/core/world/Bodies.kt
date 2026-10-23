package neumann.core.world

import neumann.core.math.Vec3

/** The system's star. Always at the origin. */
class Star(val name: String, val mass: Double, val radius: Double)

/** A planet on a fixed circular orbit around the star. */
class Planet(
    val name: String,
    val mass: Double,
    val radius: Double,
    val orbitRadius: Double,
    val inclination: Double,
    val phase: Double,
    val period: Double,
) {
    var position: Vec3 = Vec3.ZERO
    var velocity: Vec3 = Vec3.ZERO
}
