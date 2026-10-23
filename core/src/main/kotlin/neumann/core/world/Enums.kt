package neumann.core.world

enum class Faction { PLAYER, ENEMY }

enum class VesselRole { SHIP, DEFENCE, BASE }

enum class PartKind { HULL, ENGINE, SENSORS, LASER, CANNON, TORPEDO_LAUNCHER, FUEL_TANK }

/** Burn direction presets. VECTOR is used only by the AI. */
enum class BurnDirection { PROGRADE, RETROGRADE, TOWARD_TARGET, AWAY_FROM_TARGET, MATCH_TARGET, VECTOR }

enum class WeaponKind { LASER, CANNON, TORPEDO }

enum class ContactKind { SHIP, DEFENCE, BASE, TORPEDO }
