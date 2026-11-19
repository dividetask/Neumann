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
 * ship was ([startPosition], [startVelocity] at [startTime]). [drive] is the
 * drive dial: 1 = full thrust, 0 = most economical.
 */
class Burn(
    val drive: Double,
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

/**
 * Weapon switches. [pointDefenceFirst]: lasers shoot torpedoes aimed at the
 * ship before their target (off: target first, torpedoes only when the
 * target is out of reach). The auto-fire switches fire each weapon at its
 * target whenever it can; the player sets them per enemy (see
 * docs/game-design.md#orders), the AI ship-wide.
 */
class WeaponRules(
    var pointDefenceFirst: Boolean = true,
    /** Cannons shoot the nearest torpedo coming at the ship before their target. */
    var cannonPointDefenceFirst: Boolean = false,
    var laserAutoFire: Boolean = false,
    var cannonAutoFire: Boolean = false,
    var torpedoAutoLaunch: Boolean = false,
)

/** A ship's part of an allied salvo: [torpedoes] still to launch at [targetId]. */
class SalvoShare(val targetId: String, var torpedoes: Int)

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
    /** The manoeuvre target (toward / away / match), and the target of any weapon without its own. */
    var targetId: String? = null
    /** Each weapon's own target, if it has one; otherwise it uses [targetId]. */
    var laserTargetId: String? = null
    var cannonTargetId: String? = null
    var torpedoTargetId: String? = null
    /** What lidar tracks (one contact); otherwise [targetId]. */
    var lidarTargetId: String? = null
    val rules = WeaponRules()
    var laserBurstRemaining = 0.0
    var fireCannonNow = false
    /** Torpedoes to launch now at the torpedo target, as one salvo (a launch-now order). */
    var torpedoesToLaunch = 0
    /** Torpedoes this ship has promised to an allied salvo, to launch once the target is in range. */
    var salvoShare: SalvoShare? = null

    /** Evasive manoeuvres: jink sideways with translating thrusters. */
    var evasive = false
    var jinkDirection: Vec3 = Vec3.ZERO
    var jinkTimeLeft = 0.0

    var ambush = false
    var ambushRange = 0.0
    /** An enemy ship's patrol, if it has one, and whether it keeps radar on while patrolling. */
    var patrol: Patrol? = null
    var sensorPosture = SensorPosture.DARK

    val contacts = ContactBook()

    val right: Vec3 get() = forward.cross(up)

    /** Torpedoes held in working missile bays. */
    val torpedoes: Int get() = working(ModuleKind.MISSILE_BAY).sumOf { it.torpedoes }

    /** The working computer; without one the ship is dead (see docs/ships.md#computers). */
    val computer: ModuleState? get() = working(ModuleKind.COMPUTER).firstOrNull()

    /** Alive and with a working computer: able to sense, steer, and fire. */
    val active: Boolean get() = alive && computer != null

    fun working(kind: ModuleKind): List<ModuleState> = modules.filter { it.kind == kind && !it.destroyed }

    fun has(kind: ModuleKind): Boolean = modules.any { it.kind == kind && !it.destroyed }

    override fun signature(burning: Boolean): Double =
        if (burning) design.burningSignature else design.signature
}
