package neumann.core.game

import neumann.core.math.Units
import neumann.core.sim.LaserFalloff
import neumann.core.sim.SalvoPlanner
import neumann.core.world.Contact
import neumann.core.world.ModuleKind
import neumann.core.world.World
import kotlin.math.max

/**
 * How well each of the player's weapons would do against one enemy now:
 * laser damage per second at its range, how long a slug would take to get
 * there and how far off its aim may be against the enemy's size, and the
 * torpedoes a salvo needs.
 */
class Engagement(
    val distanceM: Double,
    val laserMJS: Double,
    val laserRangeM: Double,
    val slugSeconds: Double?,
    val slugAimErrorM: Double?,
    val cannonRangeM: Double,
    val targetSizeM: Double,
    val salvo: SalvoEstimate?,
)

class EngagementEstimator(private val world: World, private val salvos: SalvoPlanner) {
    fun estimate(contact: Contact): Engagement {
        val p = world.player
        val lasers = p.working(ModuleKind.LASER_TURRET).map { it.type.laser!! }
        val cannon = p.working(ModuleKind.CANNON).map { it.type.cannon!! }.maxByOrNull { it.muzzleSpeedKmS }
        val design = world.vessel(contact.id)?.design
        // A slug's flight: its muzzle speed plus how fast the two close; its aim error grows with the track's velocity error.
        val slugSeconds = if (cannon == null || contact.bearingOnly) null
            else contact.distance / max(cannon.muzzleSpeedKmS * Units.KM + contact.closingSpeed, 1.0)
        val computer = p.computer?.type?.computer
        val salvo = if (design == null || computer == null) null else {
            val k = salvos.shotDown(design, contact, computer)
            SalvoEstimate(k + 1, k + salvos.hitsToDestroy(design, computer))
        }
        return Engagement(
            distanceM = contact.distance,
            laserMJS = if (contact.bearingOnly) 0.0 else lasers.sumOf { LaserFalloff.damagePerSecond(it, contact.distance) },
            laserRangeM = (lasers.maxOfOrNull { it.rangeKm } ?: 0.0) * Units.KM,
            slugSeconds = slugSeconds,
            slugAimErrorM = slugSeconds?.let { contact.uncertaintyM + contact.velocityUncertaintyMS * it },
            cannonRangeM = (cannon?.autoRangeKm ?: 0.0) * Units.KM,
            targetSizeM = design?.sizeM ?: 0.0,
            salvo = salvo,
        )
    }
}
