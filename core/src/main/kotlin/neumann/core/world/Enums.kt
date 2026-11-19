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
    SENSOR,
    LASER_TURRET,
    CANNON,
    MISSILE_BAY,
    COMPUTER,
}

/**
 * How a heading picks its direction. FIXED is a direction fixed in space
 * (chosen with the direction circles, or by the AI); the rest track as
 * things move.
 */
/** MATCH_VELOCITY: burn until the velocity is the heading's vector (m/s, relative to the star). */
enum class HeadingMode { FIXED, PROGRADE, RETROGRADE, TOWARD_TARGET, AWAY_FROM_TARGET, MATCH_TARGET, MATCH_VELOCITY }

/** Telescope and emission receiver are passive; radar and lidar are active. */
enum class SensorType(val active: Boolean) { TELESCOPE(false), RADAR(true), LIDAR(true), EMISSION_RECEIVER(false) }

enum class WeaponKind { LASER, CANNON, TORPEDO }

/** [isBogey]: moves on its own, so once seen its position can only be estimated (see docs/sensors.md). */
enum class ContactKind(val isVessel: Boolean, val isBogey: Boolean) {
    SHIP(true, true), DEFENCE(true, false), BASE(true, false), TORPEDO(false, true), SLUG(false, false)
}

/** What modules are made of (see docs/ships.md). */
enum class Material { IRON, ALUMINIUM, TITANIUM, CARBON, SILICON, COPPER, RARE_EARTHS, TUNGSTEN, LITHIUM }

/** How a computer judges a target's point defence: its real design, or an assumption. */
enum class PointDefenceModel { DESIGN, ASSUMED }

/** Whose torpedoes a salvo counts: allies with the target in range, or every ally with the same target. */
enum class Coordination { IN_RANGE, ALL_ALLIES }

/** When an automatic torpedo salvo is fired: only to destroy, or also when at least one should hit. */
enum class SalvoGoal { DESTROY, DESTROY_OR_HIT }

/** Whether a ship keeps its radar on while patrolling, or stays dark until it has seen the player. */
enum class SensorPosture { RADAR, DARK }

/** How the system generator picks a raider's patrol: the base's planet; the outer planets; between (the last two Pending). */
enum class RaiderPlacement { GUARD, PICKET, SCREEN }
