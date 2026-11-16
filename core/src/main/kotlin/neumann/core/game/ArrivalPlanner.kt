package neumann.core.game

import neumann.core.data.GameData
import neumann.core.sim.ShipPerformance
import neumann.core.world.Arrival
import neumann.core.world.Vessel
import kotlin.math.exp
import kotlin.math.ln

/**
 * Works out the probe's arrival (see docs/game-design.md#arrival): the cruise
 * speed its full tanks allow while keeping a reserve after braking to a stop
 * at the most economical drive setting, and the speed and fuel it has at the
 * start point, slower if it had to start braking before.
 */
class ArrivalPlanner(private val data: GameData) {
    private val performance = ShipPerformance(data.munitions)

    /** [probe] fully fuelled; [distanceM] from the start point to the star. */
    fun plan(probe: Vessel, originLy: Double, distanceM: Double): Arrival {
        val full = probe.fuel
        val exhaust = performance.exhaustMS(probe, 0.0)
        val thrust = performance.thrustN(probe, 0.0)
        val dry = performance.massKg(probe) - full * 1000.0
        val reserved = dry + full * 1000.0 * data.generation.entry.arrivalFuelReserveFraction
        // Speed up to cruise, then brake from it to a stop: half the delta-v each way.
        val cruise = exhaust * ln((dry + full * 1000.0) / reserved) / 2
        val speed = if (stoppingDistance(cruise, reserved, exhaust, thrust) <= distanceM) cruise
            else speedToStopWithin(distanceM, cruise, reserved, exhaust, thrust)
        val fuel = (reserved * exp(speed / exhaust) - dry) / 1000.0
        return Arrival(originLy, cruise, originLy * LIGHT_YEAR_M / cruise / YEAR_S, speed, fuel)
    }

    /**
     * Braking to a stop from [speed] at constant [thrust], ending at mass [endMass]:
     * the mass at speed u is endMass·e^(u/exhaust), so the distance is
     * endMass/thrust · exhaust² · ((x − 1)eˣ + 1), x = speed/exhaust.
     */
    private fun stoppingDistance(speed: Double, endMass: Double, exhaust: Double, thrust: Double): Double {
        val x = speed / exhaust
        return endMass / thrust * exhaust * exhaust * ((x - 1) * exp(x) + 1)
    }

    private fun speedToStopWithin(distance: Double, cruise: Double, endMass: Double, exhaust: Double, thrust: Double): Double {
        var lo = 0.0
        var hi = cruise
        repeat(60) {
            val mid = (lo + hi) / 2
            if (stoppingDistance(mid, endMass, exhaust, thrust) <= distance) lo = mid else hi = mid
        }
        return lo
    }

    companion object {
        const val LIGHT_YEAR_M = 9.4607e15
        const val YEAR_S = 3.15576e7
    }
}
