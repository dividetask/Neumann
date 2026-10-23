package neumann.core.world

import neumann.core.data.ModuleType
import neumann.core.data.ShipDesign
import neumann.core.math.Vec3

/** One module fitted to a vessel. */
class ModuleState(val typeId: String, val type: ModuleType) {
    val kind: ModuleKind get() = type.kind
    var hitPoints: Double = type.hitPoints
    val destroyed: Boolean get() = hitPoints <= 0.0

    /** Cannons and missile bays: seconds until it can fire again. */
    var cooldown = 0.0

    /** Missile bays: torpedoes held. */
    var torpedoes = 0
}

/** The direction a vessel turns to and holds. [vector] is used by FIXED. */
class Heading(val mode: HeadingMode, val vector: Vec3 = Vec3.ZERO)

enum class BurnStage { ACCELERATE, COAST, BRAKE }

/**
 * An active burn along the heading, toward a desired speed (m/s), a desired
 * distance (m), or both, measured along the burn direction from where the
 * ship was ([startPosition], [startVelocity] at [startTime]).
 */
class Burn(
    val throttle: Double,
    val targetSpeed: Double?,
    val targetDistance: Double?,
    val startTime: Double,
    val startPosition: Vec3,
    val startVelocity: Vec3,
) {
    var stage = BurnStage.ACCELERATE
}

/** Switches for the active sensors. Passive sensors are always on. */
class SensorRules(var radarActive: Boolean = true, var lidarActive: Boolean = true)

class WeaponRules(
    var laserPointDefence: Boolean = true,
    var laserAutoFire: Boolean = false,
    var cannonAutoFire: Boolean = false,
    var torpedoAutoLaunch: Boolean = false,
)

/** A ship, fixed defence, or base, built from a design's modules. */
class Vessel(
    override val id: String,
    override val name: String,
    override val faction: Faction,
    val design: ShipDesign,
    val modules: List<ModuleState>,
    override var position: Vec3,
    override var velocity: Vec3,
    var forward: Vec3,
    var up: Vec3,
) : Tracked {
    override val history = PositionHistory(existedBefore = true)
    override var alive = true
    override var burning = false
    override val contactKind: ContactKind = when (design.role) {
        VesselRole.SHIP -> ContactKind.SHIP
        VesselRole.DEFENCE -> ContactKind.DEFENCE
        VesselRole.BASE -> ContactKind.BASE
    }
    var previousPosition: Vec3 = position

    /** Fuel, in tonnes. */
    var fuel = 0.0
    var slugs = 0

    /** Stored power, in MJ. */
    var energy = 0.0

    /** Sensors working and powered this step. */
    var sensorsOn: List<ModuleState> = emptyList()
    override var emitting = false
    override val sizeM: Double get() = design.sizeM
    val sensorRules = SensorRules()

    var heading: Heading? = null
    var burn: Burn? = null
    var targetId: String? = null
    val rules = WeaponRules()
    var laserBurstRemaining = 0.0
    var fireCannonNow = false
    var launchTorpedoNow = false

    var ambush = false
    var ambushRange = 0.0

    val contacts = ContactBook()

    val right: Vec3 get() = forward.cross(up)

    /** Torpedoes held in working missile bays. */
    val torpedoes: Int get() = working(ModuleKind.MISSILE_BAY).sumOf { it.torpedoes }

    fun working(kind: ModuleKind): List<ModuleState> = modules.filter { it.kind == kind && !it.destroyed }

    fun has(kind: ModuleKind): Boolean = modules.any { it.kind == kind && !it.destroyed }

    override fun signature(burning: Boolean): Double =
        if (burning) design.burningSignature else design.signature
}
