package neumann.core.data

import kotlinx.serialization.Serializable
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.VesselRole

// Config holders loaded from data/. Numbers and enums only; no logic.

@Serializable
data class RulesFile(
    val sim: SimConfig,
    val autoPause: AutoPauseConfig,
    val mining: MiningConfig,
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
data class MiningConfig(
    val clearRadiusKm: Double,
    val miningDistanceKm: Double,
    val maxRelativeSpeedKmS: Double,
    val mineralsPerHour: Double,
)

@Serializable
data class AiConfig(
    val standoffKm: Double,
    val maxClosingSpeedKmS: Double,
)

@Serializable
data class ShipsFile(val ships: Map<String, ShipClass>)

@Serializable
data class ShipClass(
    val name: String,
    val role: VesselRole,
    val sizeM: Double,
    val parts: List<PartConfig>,
    val sensor: SensorConfig,
    val drive: DriveConfig? = null,
    val laser: LaserConfig? = null,
    val cannon: CannonConfig? = null,
    val torpedo: TorpedoConfig? = null,
    val factory: FactoryConfig? = null,
)

@Serializable
data class PartConfig(val kind: PartKind, val hitPoints: Double, val hitWeight: Double)

@Serializable
data class DriveConfig(val accelerationMS2: Double, val fuelCapacity: Double, val fuelPerKmS: Double)

@Serializable
data class SensorConfig(
    val rangeAu: Double,
    val signature: Double,
    val burningSignature: Double,
    val damagedFraction: Double,
)

@Serializable
data class LaserConfig(val rangeKm: Double, val damagePerSecond: Double, val burstSeconds: Double)

@Serializable
data class CannonConfig(
    val muzzleSpeedKmS: Double,
    val cooldownSeconds: Double,
    val ammo: Int,
    val damage: Double,
    val autoRangeKm: Double,
)

@Serializable
data class TorpedoConfig(
    val launchSpeedKmS: Double,
    val accelerationMS2: Double,
    val deltaVKmS: Double,
    val hitPoints: Double,
    val damage: Double,
    val blastRadiusKm: Double,
    val ammo: Int,
    val cooldownSeconds: Double,
    val launchRangeKm: Double,
    val signature: Double,
    val burningSignature: Double,
)

@Serializable
data class FactoryConfig(
    val mineralsPerHour: Double,
    val repairPointsPerMineral: Double,
    val fuelPerMineral: Double,
    val mineralsPerSlug: Double,
    val mineralsPerTorpedo: Double,
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
    val minSpeedKmS: Double,
    val maxSpeedKmS: Double,
    val defaultSpeedKmS: Double,
)

@Serializable
data class PlayerPlacement(val name: String, val shipClass: String)

@Serializable
data class EnemyPlacement(
    val name: String,
    val shipClass: String,
    val planet: String,
    val offsetKm: List<Double>,
    val ambush: Boolean = false,
    val ambushRangeKm: Double = 0.0,
)

/** Everything loaded from data/. */
data class GameData(
    val rules: RulesFile,
    val ships: Map<String, ShipClass>,
    val scenario: ScenarioConfig,
)
