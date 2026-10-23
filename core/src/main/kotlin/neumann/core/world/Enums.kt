package neumann.core.world

enum class Faction { PLAYER, ENEMY }

enum class VesselRole { SHIP, DEFENCE, BASE }

enum class ModuleKind {
    HULL,
    PROPULSION,
    GENERATOR,
    BATTERY,
    FUEL_TANK,
    AMMO_CONTAINER,
    SCANNER,
    LASER_TURRET,
    CANNON,
    MISSILE_BAY,
}

/**
 * How a heading picks its direction. FIXED is a direction fixed in space
 * (chosen with the direction circles, or by the AI); the rest track as
 * things move.
 */
enum class HeadingMode { FIXED, PROGRADE, RETROGRADE, TOWARD_TARGET, AWAY_FROM_TARGET, MATCH_TARGET }

enum class WeaponKind { LASER, CANNON, TORPEDO }

enum class ContactKind { SHIP, DEFENCE, BASE, TORPEDO }
