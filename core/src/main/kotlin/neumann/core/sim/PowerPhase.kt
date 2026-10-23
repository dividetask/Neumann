package neumann.core.sim

import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.SensorType
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.min

/**
 * Electrical engines charge the batteries; each sensor that is switched on
 * draws its power, and is off this step if the batteries cannot cover it.
 */
class PowerPhase(private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            v.energy = min(performance.energyCapacity(v), v.energy + performance.generatorMW(v) * dt)
            val on = ArrayList<ModuleState>()
            for (s in wanted(v)) {
                val draw = s.type.sensor!!.powerMW * dt
                if (v.energy < draw) continue
                v.energy -= draw
                on.add(s)
            }
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
