package neumann.core.math

/**
 * Nearest distance between two objects that each moved in a straight line
 * during one step. Takes their positions at the start and end of the step.
 */
object ClosestApproach {
    data class Result(val distance: Double, val fraction: Double)

    fun between(aStart: Vec3, aEnd: Vec3, bStart: Vec3, bEnd: Vec3): Result {
        val r0 = aStart - bStart
        val dr = (aEnd - bEnd) - r0
        val dd = dr.lengthSquared()
        val f = if (dd == 0.0) 0.0 else (-r0.dot(dr) / dd).coerceIn(0.0, 1.0)
        return Result((r0 + dr * f).length(), f)
    }
}
