package neumann.core.world

import neumann.core.math.Vec3

/**
 * A raider's patrol: [targets] in turn (planets, moons, the star, or bases by
 * name), with the one it is on, how far round its orbit it has come, and
 * whether it has broken off to refuel.
 */
class Patrol(val targets: List<String>) {
    var index = 0
    /** Angle swept round the current target so far, radians; and where it was last step (relative to the target). */
    var swept = 0.0
    var lastOffset: Vec3? = null
    var refuelling = false

    val current: String? get() = targets.getOrNull(index)

    /** On to the next target (round again after the last). */
    fun advance() {
        index = (index + 1) % maxOf(1, targets.size)
        swept = 0.0
        lastOffset = null
    }
}
