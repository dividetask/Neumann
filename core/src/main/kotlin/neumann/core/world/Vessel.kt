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

/** The planet and offset a fixed vessel holds station at. */
class Station(val planet: Planet, val offset: Vec3)

/** The direction a vessel turns to and holds. [vector] is used by FIXED. */
class Heading(val mode: HeadingMode, val vector: Vec3 = Vec3.ZERO)

/** An active burn along the heading. [remainingSeconds] counts thrusting time. */
class Burn(val throttle: Double, var remainingSeconds: Double)

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
    val station: Station?,
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
    var scannersPowered = true

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
