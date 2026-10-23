package neumann.core.world

import neumann.core.data.TorpedoConfig
import neumann.core.math.Vec3

/** A cannon slug. Cold, so only radar can see it (signature 0). */
class Slug(
    override val id: String,
    val ownerId: String,
    override val faction: Faction,
    override var position: Vec3,
    override var velocity: Vec3,
    val massKg: Double,
    override val sizeM: Double,
    val launchTime: Double,
) : Tracked {
    override val name: String get() = "Slug $id"
    override val contactKind = ContactKind.SLUG
    override val history = PositionHistory(existedBefore = false)
    override val burning = false
    override val emitting = false
    override var alive = true
    var previousPosition: Vec3 = position

    override fun signature(burning: Boolean): Double = 0.0
}

class Torpedo(
    override val id: String,
    override val name: String,
    val ownerId: String,
    override val faction: Faction,
    val targetId: String,
    val config: TorpedoConfig,
    override var position: Vec3,
    override var velocity: Vec3,
    val launchTime: Double,
) : Tracked {
    override val history = PositionHistory(existedBefore = false)
    override val contactKind = ContactKind.TORPEDO
    override var alive = true
    override var burning = false
    override val sizeM: Double get() = config.sizeM
    override val emitting = false
    var previousPosition: Vec3 = position
    var deltaVLeft: Double = config.deltaVKmS * 1000.0
    var hitPoints: Double = config.hitPoints

    override fun signature(burning: Boolean): Double =
        if (burning) config.burningSignature else config.signature
}
