package neumann.core.sim

import neumann.core.data.ShipClass
import neumann.core.math.Units

/** The longest range at which a ship class's weapons fire on their own. */
object ThreatRange {
    fun of(c: ShipClass): Double = Units.KM * maxOf(
        c.laser?.rangeKm ?: 0.0,
        c.cannon?.autoRangeKm ?: 0.0,
        c.torpedo?.launchRangeKm ?: 0.0,
    )
}
