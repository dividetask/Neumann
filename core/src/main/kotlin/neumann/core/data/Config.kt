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
    val pauseOn: List<EventKind>,
)

@Serializable
data class AiConfig(
    val standoffKm: Double,
    val maxClosingSpeedKmS: Double,
    val combatDvKmS: Double,
)

@Serializable
data class ShipRules(
    val alignToleranceDeg: Double,
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
data class PropulsionConfig(val thrustKN: Double, val fuelKgS: Double, val turnAuthority: Double)

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
)

@Serializable
data class LaserConfig(
    val rangeKm: Double,
    val damagePerSecond: Double,
    val powerMW: Double,
    val shotSeconds: Double,
    val cooldownSeconds: Double,
    val burstSeconds: Double,
    val arcDeg: Double,
)

@Serializable
data class CannonConfig(
    val muzzleSpeedKmS: Double,
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

@Serializable
data class SlugConfig(val damage: Double, val massKg: Double, val cost: Double, val buildHours: Double)

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
    val planets: List<PlanetConfig>,
    val entry: EntryConfig,
    val player: PlayerPlacement,
    val enemies: List<EnemyPlacement>,
)

@Serializable
data class StarConfig(val name: String, val massSolar: Double, val radiusKm: Double)

@Serializable
data class PlanetConfig(
    val name: String,
    val orbitAu: Double,
    val inclinationDeg: Double,
    val phaseDeg: Double,
    val massEarth: Double,
    val radiusKm: Double,
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

/** Everything loaded from data/. */
data class GameData(
    val rules: RulesFile,
    val modules: Map<String, ModuleType>,
    val munitions: MunitionsConfig,
    val designs: Map<String, ShipDesign>,
    val scenario: ScenarioConfig,
)
