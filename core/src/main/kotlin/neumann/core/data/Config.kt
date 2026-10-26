package neumann.core.data

import kotlinx.serialization.Serializable
import neumann.core.world.EventKind
import neumann.core.world.ModuleKind
import neumann.core.world.SensorType
import neumann.core.world.VesselRole

// Config holders loaded from data/. Numbers and enums only; no logic.

@Serializable
data class RulesFile(
    val sim: SimConfig,
    val ships: ShipRules,
    val autoPause: AutoPauseConfig,
    val ai: AiConfig,
    val map: MapConfig,
)

@Serializable
data class SimConfig(
    val baseStepSeconds: Double,
    val fineStepSeconds: Double,
    val fineStepDivisions: Double,
    val historySampleSeconds: Double,
    val historyKeepSeconds: Double,
    val slugLifetimeSeconds: Double,
    val torpedoLifetimeSeconds: Double,
    val seed: Long,
)

@Serializable
data class AutoPauseConfig(
    val lowSupplyFraction: Double,
    val repeatSuppressionSeconds: Double,
    val groupSeconds: Double,
    val pauseOn: List<EventKind>,
)

@Serializable
data class MapConfig(
    val estimateMaxAgeSeconds: Double,
    val ghostMaxAgeSeconds: Double,
)

@Serializable
data class AiConfig(
    val jinkRangeKm: Double,
    val standoffKm: Double,
    val maxClosingSpeedKmS: Double,
    val combatDvKmS: Double,
)

@Serializable
data class ShipRules(
    val alignToleranceDeg: Double,
    val jinkIntervalSeconds: Double,
    /** A slug whose straight-line path passes this close raises slug inbound. */
    val slugAlertKm: Double,
    val turnFuelKgPerTonneDeg: Double,
)

@Serializable
data class ModulesFile(val modules: Map<String, ModuleType>, val munitions: MunitionsConfig)

/** One kind of module. Exactly the sub-config matching [kind] is set. */
@Serializable
data class ModuleType(
    val name: String,
    val kind: ModuleKind,
    val cost: Double,
    val massT: Double,
    val buildHours: Double,
    val hitPoints: Double,
    val propulsion: PropulsionConfig? = null,
    val generator: GeneratorConfig? = null,
    val battery: BatteryConfig? = null,
    val fuelTank: FuelTankConfig? = null,
    val ammoContainer: AmmoContainerConfig? = null,
    val sensor: SensorConfig? = null,
    val laser: LaserConfig? = null,
    val cannon: CannonConfig? = null,
    val missileBay: MissileBayConfig? = null,
)

@Serializable
data class PropulsionConfig(
    val thrustKN: Double,
    val fuelKgS: Double,
    val turnAuthority: Double,
    /** Can push sideways without turning (for jinking). */
    val translates: Boolean = false,
)

@Serializable
data class GeneratorConfig(val powerMW: Double)

@Serializable
data class BatteryConfig(val capacityMJ: Double)

@Serializable
data class FuelTankConfig(val capacityT: Double)

@Serializable
data class AmmoContainerConfig(val capacity: Int)

@Serializable
data class SensorConfig(
    val type: SensorType,
    val rangeAu: Double,
    val bearingErrorDeg: Double,
    val rangeErrorFraction: Double,
    val velocityErrorKmS: Double,
    val powerMW: Double,
    /** Passive tracking: errors shrink with tracking time on this scale (0 = no improvement). */
    val trackTimeConstantSeconds: Double = 0.0,
    val minRangeErrorFraction: Double = 0.0,
    val minVelocityErrorKmS: Double = 0.0,
)

@Serializable
data class LaserConfig(
    /** No damage beyond this. */
    val rangeKm: Double,
    /** Full damage out to here; beyond, it falls with the square of distance. */
    val focusRangeKm: Double,
    /** Beam power: damage per second (MJ/s) at full focus. */
    val beamMW: Double,
    /** Electrical draw while firing. */
    val powerMW: Double,
    val burstSeconds: Double,
    val arcDeg: Double,
)

@Serializable
data class CannonConfig(
    val muzzleSpeedKmS: Double,
    /** Drawn from the batteries per shot. */
    val shotEnergyMJ: Double,
    val cooldownSeconds: Double,
    val autoRangeKm: Double,
    val arcDeg: Double,
)

@Serializable
data class MissileBayConfig(
    val capacity: Int,
    val cooldownSeconds: Double,
    val launchRangeKm: Double,
    val arcDeg: Double,
)

@Serializable
data class MunitionsConfig(val slug: SlugConfig, val torpedo: TorpedoConfig)

/** Slug damage is its kinetic energy at impact, so it has no damage field. */
@Serializable
data class SlugConfig(val massKg: Double, val sizeM: Double, val cost: Double, val buildHours: Double)

@Serializable
data class TorpedoConfig(
    val launchSpeedKmS: Double,
    val accelerationMS2: Double,
    val deltaVKmS: Double,
    val terminalReserveFraction: Double,
    val sizeM: Double,
    val hitPoints: Double,
    val damage: Double,
    val blastRadiusKm: Double,
    val signature: Double,
    val burningSignature: Double,
    val massKg: Double,
    val cost: Double,
    val buildHours: Double,
)

@Serializable
data class ShipsFile(val designs: Map<String, ShipDesign>)

/** A ship design: its modules (type id to count) and design-level values. */
@Serializable
data class ShipDesign(
    val name: String,
    val role: VesselRole,
    val sizeM: Double,
    val signature: Double,
    val burningSignature: Double,
    val modules: Map<String, Int>,
)

@Serializable
data class ScenarioConfig(
    val name: String,
    val star: StarConfig,
    /** Planets and moons; a moon names its planet as parent and comes after it. */
    val planets: List<PlanetConfig>,
    val entry: EntryConfig,
    val player: PlayerPlacement,
    val enemies: List<EnemyPlacement>,
    val belt: BeltConfig? = null,
    /** The seed a random system was generated from (0 for a fixed system). */
    val seed: Long = 0,
)

/** An asteroid belt: drawn only, no gravity. */
@Serializable
data class BeltConfig(val innerAu: Double, val outerAu: Double)

@Serializable
data class StarConfig(val name: String, val massSolar: Double, val radiusKm: Double)

@Serializable
data class PlanetConfig(
    val name: String,
    /** Orbit around the star (planets). */
    val orbitAu: Double = 0.0,
    val inclinationDeg: Double,
    val phaseDeg: Double,
    val massEarth: Double,
    val radiusKm: Double,
    /** For a moon: the planet it orbits, and its distance from that planet. */
    val parent: String? = null,
    val orbitKm: Double = 0.0,
)

@Serializable
data class EntryConfig(
    val radiusAu: Double,
    /** Bearing of the arrival point from the star: azimuth, elevation. */
    val bearingDeg: List<Double>,
    val minSpeedKmS: Double,
    val maxSpeedKmS: Double,
    val defaultSpeedKmS: Double,
)

@Serializable
data class PlayerPlacement(val name: String, val design: String)

@Serializable
data class EnemyPlacement(
    val name: String,
    val design: String,
    /** The planet or star it starts in a circular orbit of. */
    val orbits: String,
    val offsetKm: List<Double>,
    val ambush: Boolean = false,
    val ambushRangeKm: Double = 0.0,
)

/** Ranges for generating a random star system (data/system-generation.yaml). */
@Serializable
data class GenerationConfig(
    val starNames: List<String>,
    val star: StarRange,
    val planets: PlanetRanges,
    val moons: MoonRanges,
    val belt: BeltRanges,
    val entry: EntryRanges,
    val enemies: EnemyPlacementRanges,
)

@Serializable
data class StarRange(val massSolarMin: Double, val massSolarMax: Double, val radiusKmPerSolarMass: Double)

@Serializable
data class PlanetRanges(
    val countMin: Int,
    val countMax: Int,
    val firstOrbitAuMin: Double,
    val firstOrbitAuMax: Double,
    /** Each orbit is this many times the one inside it. */
    val spacingMin: Double,
    val spacingMax: Double,
    /** Rocky planets inside this; gas and ice giants beyond. */
    val rockyLimitAu: Double,
    val inclinationDegMax: Double,
    val rocky: PlanetKindRange,
    val giant: PlanetKindRange,
)

@Serializable
data class PlanetKindRange(val massEarthMin: Double, val massEarthMax: Double, val moonsMin: Int, val moonsMax: Int)

@Serializable
data class MoonRanges(
    /** First moon's orbit, in planet radii. */
    val firstOrbitRadiiMin: Double,
    val firstOrbitRadiiMax: Double,
    val spacingMin: Double,
    val spacingMax: Double,
    val massEarthMin: Double,
    val massEarthMax: Double,
    val maxMassFraction: Double,
    val minOrbitKm: Double,
)

@Serializable
data class BeltRanges(val chance: Double, val minGapAu: Double)

@Serializable
data class EntryRanges(
    /** Entry sphere radius, as a multiple of the outermost rocky planet's orbit. */
    val radiusOverInnerSystem: Double,
    val radiusAuMin: Double,
    val elevationDegMax: Double,
)

@Serializable
data class EnemyPlacementRanges(
    val stationOffsetKm: Double,
    val raiderCount: Int,
    val patrolKmMin: Double,
    val patrolKmMax: Double,
    /** Orbits are kept inside this fraction of the body's Hill sphere, so they stay bound. */
    val hillFraction: Double,
    val nearBaseChance: Double,
    val ambushChance: Double,
    val ambushRangeKm: Double,
)

/** Everything loaded from data/. [scenario] is the fixed test system; games use generated ones. */
data class GameData(
    val rules: RulesFile,
    val modules: Map<String, ModuleType>,
    val munitions: MunitionsConfig,
    val designs: Map<String, ShipDesign>,
    val scenario: ScenarioConfig,
    val generation: GenerationConfig,
)
