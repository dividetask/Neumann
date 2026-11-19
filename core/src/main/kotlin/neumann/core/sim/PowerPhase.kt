package neumann.core.sim

import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.SensorType
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.max
import kotlin.math.min

/**
 * The computer and each sensor that is switched on draw their power from the
 * electrical engines' output first, then from the batteries; a sensor is off
 * this step if neither can cover it. What is left over charges the
 * batteries. A dead ship draws nothing.
 */
class PowerPhase(private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.active) {
                v.sensorsOn = emptyList()
                v.emitting = false
                continue
            }
            // Steady draws (computer, then sensors) come from the generator's output first, then the
            // batteries; what is left over charges the batteries. (A long step must not make a sensor
            // look too hungry for the batteries when the generator easily covers it.)
            var spareMW = performance.generatorMW(v) - v.computer!!.type.computer!!.powerMW
            if (spareMW < 0.0) {
                v.energy = max(0.0, v.energy + spareMW * dt)
                spareMW = 0.0
            }
            val on = ArrayList<ModuleState>()
            for (s in wanted(v)) {
                val mw = s.type.sensor!!.powerMW
                if (mw <= spareMW) {
                    spareMW -= mw
                } else {
                    val fromBattery = (mw - spareMW) * dt
                    if (v.energy < fromBattery) continue
                    v.energy -= fromBattery
                    spareMW = 0.0
                }
                on.add(s)
            }
            v.energy = min(performance.energyCapacity(v), v.energy + spareMW * dt)
            v.sensorsOn = on
            v.emitting = on.any { it.type.sensor!!.type.active }
        }
    }

    companion object {
        /** Working sensors the vessel's sensor rules have switched on. */
        fun wanted(v: Vessel): List<ModuleState> = v.working(ModuleKind.SENSOR).filter {
            when (it.type.sensor!!.type) {
                SensorType.RADAR -> v.sensorRules.radarActive
                SensorType.LIDAR -> v.sensorRules.lidarActive
                else -> true
            }
        }
    }
}
