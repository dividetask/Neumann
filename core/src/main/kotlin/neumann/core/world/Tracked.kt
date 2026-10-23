package neumann.core.world

import neumann.core.math.Vec3

/** Anything sensors can see. */
interface Tracked {
    val id: String
    val name: String
    val faction: Faction
    val contactKind: ContactKind
    val position: Vec3
    val velocity: Vec3
    val history: PositionHistory
    val alive: Boolean
    val burning: Boolean
    fun signature(burning: Boolean): Double
}
