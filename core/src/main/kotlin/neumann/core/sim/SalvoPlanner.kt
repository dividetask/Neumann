package neumann.core.sim

import neumann.core.data.ComputerConfig
import neumann.core.data.GameData
import neumann.core.data.LaserConfig
import neumann.core.data.ShipDesign
import neumann.core.math.Units
import neumann.core.world.Contact
import neumann.core.world.PointDefenceModel
import neumann.core.world.SalvoGoal
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * How many torpedoes an automatic salvo should be: enough to get through the
 * target's point defence and destroy it, or (if the computer's salvo goal
 * allows) all there are if that at least gets one through, or none. The
 * computer also decides whose point defence is counted: the target's real
 * design, or an assumed one (see docs/weapons.md#torpedo-salvos).
 */
class SalvoPlanner(private val data: GameData) {
    private val torpedo = data.munitions.torpedo

    /** Salvo size from [available] torpedoes against [design], as seen in [contact], planned by [computer]. */
    fun size(available: Int, design: ShipDesign, contact: Contact, computer: ComputerConfig): Int {
        val k = shotDown(design, contact, computer)
        val h = hitsToDestroy(design)
        return when {
            available >= k + h -> k + h
            available > k && computer.salvoGoal == SalvoGoal.DESTROY_OR_HIT -> available
            else -> 0
        }
    }

    /** Torpedoes the target's point defence is expected to stop, judged on the least favourable closing speed. */
    fun shotDown(design: ShipDesign, contact: Contact, computer: ComputerConfig): Int {
        val closing = max(0.0, contact.closingSpeed - 2 * contact.velocityUncertaintyMS)
        val burn = torpedo.deltaVKmS * Units.KM * (1 - torpedo.terminalReserveFraction)
        val v = closing + burn
        // Laser falloff integrated over the approach: full power inside focus range, 1/d² beyond.
        val damage = turrets(design, computer).sumOf { (laser, count) ->
            val focus = laser.focusRangeKm * Units.KM
            count * laser.beamMW * focus * (2 - laser.focusRangeKm / laser.rangeKm) / v
        }
        return floor(damage / torpedo.hitPoints).toInt()
    }

    fun hitsToDestroy(design: ShipDesign): Int {
        val hp = design.modules.entries.sumOf { (id, n) -> data.modules.getValue(id).hitPoints * n }
        return ceil(hp / torpedo.damage).toInt()
    }

    /** The laser turrets counted: the design's, or the computer's assumption. */
    private fun turrets(design: ShipDesign, computer: ComputerConfig): List<Pair<LaserConfig, Int>> = when (computer.pointDefence) {
        PointDefenceModel.DESIGN -> design.modules.mapNotNull { (id, n) -> data.modules.getValue(id).laser?.let { it to n } }
        PointDefenceModel.ASSUMED -> listOf(data.modules.getValue(computer.assumedLaser!!).laser!! to computer.assumedLaserTurrets)
    }
}
