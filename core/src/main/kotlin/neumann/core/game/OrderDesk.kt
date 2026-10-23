package neumann.core.game

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.Burn
import neumann.core.world.ContactKind
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.WeaponKind
import neumann.core.world.World

/**
 * Applies the player's orders. Each returns a problem to show, or null if
 * accepted. [vector] is a direction in space, used by HeadingMode.FIXED.
 */
class OrderDesk(private val world: World) {
    private val player get() = world.player

    /** Turn to a direction (and, for FIXED, the roll [up]) and hold it. Cancels any burn. */
    fun face(mode: HeadingMode, vector: Vec3 = Vec3.ZERO, up: Vec3? = null): String? {
        headingProblem(mode, vector)?.let { return it }
        player.heading = Heading(mode, vector, up.takeIf { mode == HeadingMode.FIXED })
        player.burn = null
        return null
    }

    /** Turn to a direction, then thrust along it for [seconds] of thrusting. */
    fun burn(mode: HeadingMode, vector: Vec3, throttle: Double, seconds: Double, up: Vec3? = null): String? {
        headingProblem(mode, vector)?.let { return it }
        if (player.fuel <= 0.0) return "No fuel"
        if (throttle <= 0.0) return "Throttle is zero"
        player.heading = Heading(mode, vector, up.takeIf { mode == HeadingMode.FIXED })
        player.burn = Burn(throttle.coerceAtMost(1.0), seconds)
        return null
    }

    fun cancelBurn() {
        player.burn = null
    }

    private fun headingProblem(mode: HeadingMode, vector: Vec3): String? {
        if (!player.has(ModuleKind.PROPULSION)) return "No working propulsion"
        if (mode == HeadingMode.FIXED && vector.lengthSquared() == 0.0) return "No direction chosen"
        val needsTarget = mode == HeadingMode.TOWARD_TARGET || mode == HeadingMode.AWAY_FROM_TARGET ||
            mode == HeadingMode.MATCH_TARGET
        if (needsTarget && player.contacts[player.targetId] == null) return "No target"
        return null
    }

    fun setTarget(id: String): String? {
        val c = player.contacts[id] ?: return "Not a current contact"
        if (c.kind == ContactKind.TORPEDO) return "Torpedoes can't be targeted"
        player.targetId = id
        return null
    }

    fun fireNow(weapon: WeaponKind): String? {
        val target = player.contacts[player.targetId] ?: return "No target"
        return when (weapon) {
            WeaponKind.LASER -> {
                val lasers = player.working(ModuleKind.LASER_TURRET).map { it.type.laser!! }
                if (lasers.isEmpty()) return "No working laser turret"
                if (target.distance > lasers.maxOf { it.rangeKm } * Units.KM) return "Target out of laser range"
                player.laserBurstRemaining = lasers.maxOf { it.burstSeconds }
                null
            }
            WeaponKind.CANNON -> {
                if (!player.has(ModuleKind.CANNON)) return "No working cannon"
                if (player.slugs <= 0) return "No slugs"
                player.fireCannonNow = true
                null
            }
            WeaponKind.TORPEDO -> {
                if (player.torpedoes <= 0) return "No torpedoes"
                player.launchTorpedoNow = true
                null
            }
        }
    }
}
