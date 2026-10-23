package neumann.core.game

import neumann.core.world.Burn
import neumann.core.world.BurnDirection
import neumann.core.world.ContactKind
import neumann.core.world.PartKind
import neumann.core.world.WeaponKind
import neumann.core.world.World

/** Applies the player's orders. Each returns a problem to show, or null if accepted. */
class OrderDesk(private val world: World) {
    private val player get() = world.player

    fun setBurn(direction: BurnDirection, throttle: Double, seconds: Double): String? {
        if (direction == BurnDirection.VECTOR) return "Not a player burn"
        if (!player.working(PartKind.ENGINE)) return "Engine destroyed"
        if (player.fuel <= 0.0) return "No fuel"
        val needsTarget = direction == BurnDirection.TOWARD_TARGET || direction == BurnDirection.AWAY_FROM_TARGET ||
            direction == BurnDirection.MATCH_TARGET
        if (needsTarget && player.contacts[player.targetId] == null) return "No target"
        player.burn = Burn(direction, throttle.coerceIn(0.0, 1.0), seconds)
        return null
    }

    fun cancelBurn() {
        player.burn = null
    }

    fun setTarget(id: String): String? {
        val c = player.contacts[id] ?: return "Not a current contact"
        if (c.kind == ContactKind.TORPEDO) return "Torpedoes can't be targeted"
        player.targetId = id
        return null
    }

    fun fireNow(weapon: WeaponKind): String? {
        val target = player.contacts[player.targetId] ?: return "No target"
        val c = player.shipClass
        return when (weapon) {
            WeaponKind.LASER -> {
                val laser = c.laser ?: return "No laser"
                if (!player.working(PartKind.LASER)) return "Laser destroyed"
                if (target.distance > laser.rangeKm * 1000.0) return "Target out of laser range"
                player.laserBurstRemaining = laser.burstSeconds
                null
            }
            WeaponKind.CANNON -> {
                if (c.cannon == null) return "No cannon"
                if (!player.working(PartKind.CANNON)) return "Cannon destroyed"
                if (player.slugs <= 0) return "No slugs"
                player.fireCannonNow = true
                null
            }
            WeaponKind.TORPEDO -> {
                if (c.torpedo == null) return "No torpedoes"
                if (!player.working(PartKind.TORPEDO_LAUNCHER)) return "Launcher destroyed"
                if (player.torpedoes <= 0) return "No torpedoes"
                player.launchTorpedoNow = true
                null
            }
        }
    }
}
