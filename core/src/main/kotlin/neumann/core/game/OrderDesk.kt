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

    /** Turn to a direction and hold it. Cancels any burn. */
    fun face(mode: HeadingMode, vector: Vec3 = Vec3.ZERO): String? {
        headingProblem(mode, vector)?.let { return it }
        player.heading = Heading(mode, vector)
        player.burn = null
        return null
    }

    /**
     * Turn to a direction, then burn toward a desired speed (km/s), a desired
     * distance (km), or both, at drive setting [drive] (1 = full thrust, 0 =
     * most economical). Match target velocity needs neither.
     */
    fun burn(mode: HeadingMode, vector: Vec3, drive: Double, speedKmS: Double?, distanceKm: Double?): String? {
        headingProblem(mode, vector)?.let { return it }
        if (player.fuel <= 0.0) return "No fuel"
        if (mode != HeadingMode.MATCH_TARGET && speedKmS == null && distanceKm == null) return "Set a speed, a distance, or both"
        player.heading = Heading(mode, vector)
        player.burn = Burn(drive.coerceIn(0.0, 1.0), speedKmS?.times(Units.KM), distanceKm?.times(Units.KM),
            world.time, player.position, player.velocity)
        return null
    }

    fun cancelBurn() {
        player.burn = null
    }

    /** Evasive manoeuvres: jink sideways with the translating thrusters. */
    fun setEvasive(on: Boolean): String? {
        if (on && player.working(ModuleKind.PROPULSION).none { it.type.propulsion!!.translates }) return "No working thrusters to jink with"
        player.evasive = on
        return null
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
        if (!c.kind.isVessel) return "Only ships, defences, and bases can be targeted"
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
