package neumann.core.sim

import neumann.core.data.MunitionsConfig
import neumann.core.data.PropulsionConfig
import neumann.core.math.Units
import neumann.core.world.ModuleKind
import neumann.core.world.Vessel
import kotlin.math.max

/** What a vessel can do, worked out from its working modules and what it carries. */
class ShipPerformance(private val munitions: MunitionsConfig) {
    /** Modules (working or not) plus fuel, slugs, and torpedoes, in kg. */
    fun massKg(v: Vessel): Double =
        v.modules.sumOf { it.type.massT } * 1000.0 + v.fuel * 1000.0 +
            v.slugs * munitions.slug.massKg + v.torpedoes * munitions.torpedo.massKg

    /** Main engines' thrust at drive setting [drive] (1 = full thrust, 0 = most economical), N. Thrusters only jink. */
    fun thrustN(v: Vessel, drive: Double = 1.0): Double =
        mainEngines(v).sumOf { val p = it.type.propulsion!!; p.thrustKN * 1000.0 * slowdown(p, drive) }

    /** Main engines' fuel use at drive setting [drive], kg/s. */
    fun fuelKgS(v: Vessel, drive: Double = 1.0): Double =
        mainEngines(v).sumOf { val p = it.type.propulsion!!; val s = slowdown(p, drive); p.fuelKgS * s * s }

    private fun mainEngines(v: Vessel) = v.working(ModuleKind.PROPULSION).filter { !it.type.propulsion!!.translates }

    fun accelerationMS2(v: Vessel, drive: Double = 1.0): Double = thrustN(v, drive) / massKg(v)

    /** Effective exhaust speed at [drive], m/s: speed change per kg of fuel × mass. */
    fun exhaustMS(v: Vessel, drive: Double = 1.0): Double = fuelKgS(v, drive).let { if (it > 0.0) thrustN(v, drive) / it else 0.0 }

    /**
     * At the same jet power, thrust goes as 1 / exhaust speed and fuel use as
     * 1 / exhaust speed²: the factor, full-thrust exhaust ÷ exhaust at [drive].
     * The exhaust moves on a log scale from full thrust (drive 1) to the
     * fastest (drive 0).
     */
    private fun slowdown(p: PropulsionConfig, drive: Double): Double {
        val slowest = p.thrustKN * 1000.0 / p.fuelKgS
        val fastest = maxOf(slowest, p.maxExhaustKmS * Units.KM)
        val exhaust = slowest * Math.pow(fastest / slowest, 1.0 - drive.coerceIn(0.0, 1.0))
        return slowest / exhaust
    }

    /** Sideways thrust from modules that can translate (for jinking), N. */
    fun translationThrustN(v: Vessel): Double =
        v.working(ModuleKind.PROPULSION).filter { it.type.propulsion!!.translates }.sumOf { it.type.propulsion!!.thrustKN } * 1000.0

    fun translationFuelKgS(v: Vessel): Double =
        v.working(ModuleKind.PROPULSION).filter { it.type.propulsion!!.translates }.sumOf { it.type.propulsion!!.fuelKgS }

    /** Turn rate in radians per second: total turn authority / mass in tonnes, in degrees. */
    fun turnRate(v: Vessel): Double =
        v.working(ModuleKind.PROPULSION).sumOf { it.type.propulsion!!.turnAuthority } / (massKg(v) / 1000.0) * Units.DEGREE

    fun fuelCapacity(v: Vessel): Double = v.working(ModuleKind.FUEL_TANK).sumOf { it.type.fuelTank!!.capacityT }

    fun slugCapacity(v: Vessel): Int = v.working(ModuleKind.AMMO_CONTAINER).sumOf { it.type.ammoContainer!!.capacity }

    fun torpedoCapacity(v: Vessel): Int = v.working(ModuleKind.MISSILE_BAY).sumOf { it.type.missileBay!!.capacity }

    fun energyCapacity(v: Vessel): Double = v.working(ModuleKind.BATTERY).sumOf { it.type.battery!!.capacityMJ }

    fun generatorMW(v: Vessel): Double = v.working(ModuleKind.GENERATOR).sumOf { it.type.generator!!.powerMW }

    /** Longest range at which its working weapons fire on their own, in metres. */
    fun threatRange(v: Vessel): Double {
        var r = 0.0
        for (m in v.modules) {
            if (m.destroyed) continue
            r = max(r, m.type.laser?.rangeKm ?: 0.0)
            r = max(r, m.type.cannon?.autoRangeKm ?: 0.0)
            r = max(r, m.type.missileBay?.launchRangeKm ?: 0.0)
        }
        return r * Units.KM
    }
}
