package neumann.core.sim

import neumann.core.data.GameData
import neumann.core.data.ShipDesign
import neumann.core.math.Units
import neumann.core.world.Contact
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * How many torpedoes an automatic salvo should be: enough to get through the
 * target's point defence and destroy it, or all there are if that at least
 * gets one through, or none (see docs/weapons.md#torpedo-salvos).
 */
class SalvoPlanner(private val data: GameData) {
    private val torpedo = data.munitions.torpedo

    /** Salvo size from [available] torpedoes against [design], as seen in [contact]. */
    fun size(available: Int, design: ShipDesign, contact: Contact): Int {
        val k = shotDown(design, contact)
        val h = hitsToDestroy(design)
        return when {
            available >= k + h -> k + h
            available > k -> available
            else -> 0
        }
    }

    /** Torpedoes the target's point defence is expected to stop, judged on the least favourable closing speed. */
    fun shotDown(design: ShipDesign, contact: Contact): Int {
        val closing = max(0.0, contact.closingSpeed - 2 * contact.velocityUncertaintyMS)
        val burn = torpedo.deltaVKmS * Units.KM * (1 - torpedo.terminalReserveFraction)
        val v = closing + burn
        // Laser falloff integrated over the approach: full power inside focus range, 1/d² beyond.
        val damage = turrets(design).sumOf { (laser, count) ->
            val focus = laser.focusRangeKm * Units.KM
            count * laser.beamMW * focus * (2 - laser.focusRangeKm / laser.rangeKm) / v
        }
        return floor(damage / torpedo.hitPoints).toInt()
    }

    fun hitsToDestroy(design: ShipDesign): Int {
        val hp = design.modules.entries.sumOf { (id, n) -> data.modules.getValue(id).hitPoints * n }
        return ceil(hp / torpedo.damage).toInt()
    }

    private fun turrets(design: ShipDesign) =
        design.modules.mapNotNull { (id, n) -> data.modules.getValue(id).laser?.let { it to n } }
}
