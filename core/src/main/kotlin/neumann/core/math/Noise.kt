package neumann.core.math

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/** Random measurement errors. */
object Noise {
    /** A normally distributed number with mean 0 and standard deviation 1. */
    fun gaussian(random: Random): Double {
        val u = random.nextDouble().coerceAtLeast(1e-300)
        return sqrt(-2.0 * ln(u)) * cos(2 * Math.PI * random.nextDouble())
    }

    /** A random vector whose components each have standard deviation [sigma]. */
    fun vector(random: Random, sigma: Double) = Vec3(gaussian(random), gaussian(random), gaussian(random)) * sigma
}
