package neumann.core.game

import neumann.core.data.GameData
import neumann.core.sim.SensorPhase
import neumann.core.world.SensorType
import kotlin.math.sqrt

/**
 * The furthest any enemy design could detect the player's design, in AU: the
 * longest reach of any enemy sensor, against the probe at its most visible
 * (burning, and emitting). Same rules as [SensorPhase].
 */
class DetectionReach(private val data: GameData) {
    fun au(): Double {
        val player = data.designs.getValue(data.scenario.player.design)
        val enemies = data.designs.filterKeys { it != data.scenario.player.design }.values
        return enemies.flatMap { it.modules.keys }.mapNotNull { data.modules.getValue(it).sensor }.maxOf { s ->
            when (s.type) {
                SensorType.TELESCOPE -> s.rangeAu * player.burningSignature
                SensorType.RADAR -> s.rangeAu * sqrt(player.sizeM / SensorPhase.RADAR_REFERENCE_SIZE_M)
                SensorType.LIDAR, SensorType.EMISSION_RECEIVER -> s.rangeAu
            }
        }
    }
}
