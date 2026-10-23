package neumann.core.world

import neumann.core.data.ShipClass
import neumann.core.math.Vec3

class PartState(val kind: PartKind, val maxHitPoints: Double, var hitPoints: Double, val hitWeight: Double) {
    val destroyed: Boolean get() = hitPoints <= 0.0
}

/** The planet and offset a fixed vessel holds station at. */
class Station(val planet: Planet, val offset: Vec3)

/** An active burn. [vector] is set only for BurnDirection.VECTOR. */
class Burn(
    val direction: BurnDirection,
    val throttle: Double,
    var remainingSeconds: Double,
    val vector: Vec3 = Vec3.ZERO,
)

class WeaponRules(
    var laserPointDefence: Boolean = true,
    var laserAutoFire: Boolean = false,
    var cannonAutoFire: Boolean = false,
    var torpedoAutoLaunch: Boolean = false,
)

/** A ship, fixed defence, or base. */
class Vessel(
    override val id: String,
    override val name: String,
    override val faction: Faction,
    val shipClass: ShipClass,
    override var position: Vec3,
    override var velocity: Vec3,
    val station: Station?,
) : Tracked {
    override val history = PositionHistory(existedBefore = true)
    override var alive = true
    override var burning = false
    override val contactKind: ContactKind = when (shipClass.role) {
        VesselRole.SHIP -> ContactKind.SHIP
        VesselRole.DEFENCE -> ContactKind.DEFENCE
        VesselRole.BASE -> ContactKind.BASE
    }

    val parts: Map<PartKind, PartState> = shipClass.parts.associate {
        it.kind to PartState(it.kind, it.hitPoints, it.hitPoints, it.hitWeight)
    }
    var previousPosition: Vec3 = position

    var fuel: Double = shipClass.drive?.fuelCapacity ?: 0.0
    var slugs: Int = shipClass.cannon?.ammo ?: 0
    var torpedoes: Int = shipClass.torpedo?.ammo ?: 0

    var burn: Burn? = null
    var targetId: String? = null
    val rules = WeaponRules()
    var laserBurstRemaining = 0.0
    var cannonCooldown = 0.0
    var torpedoCooldown = 0.0
    var fireCannonNow = false
    var launchTorpedoNow = false

    var ambush = false
    var ambushRange = 0.0

    val contacts = ContactBook()

    /** True if the vessel has this part and it is not destroyed. */
    fun working(kind: PartKind): Boolean = parts[kind]?.destroyed == false

    override fun signature(burning: Boolean): Double =
        if (burning) shipClass.sensor.burningSignature else shipClass.sensor.signature
}
