package neumann.core.sim

import neumann.core.data.LaserConfig
import neumann.core.math.Units

/** Laser damage per second at a distance: full to the focus range, then falling with the square of distance. */
object LaserFalloff {
    fun damagePerSecond(laser: LaserConfig, distance: Double): Double {
        val focus = laser.focusRangeKm * Units.KM
        return when {
            distance >= laser.rangeKm * Units.KM -> 0.0
            distance <= focus -> laser.beamMW
            else -> laser.beamMW * (focus / distance) * (focus / distance)
        }
    }
}
