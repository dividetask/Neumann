# Architecture

Two Gradle modules:

- `core/` — plain Kotlin (JVM), no Android. All game rules. Unit-tested with
  `./gradlew :core:test`.
- `app/` — the Android app. Screens only; it drives `core` through
  `GameSession`.

`data/` (YAML: `rules.yaml`, `modules.yaml`, `ships.yaml`, `scenario.yaml`,
`system-generation.yaml`) is loaded by `GameDataLoader`. `scenario.yaml` is
the fixed system the tests use; games use random systems from
`SystemGenerator`. The app packages `data/` as its assets.

`settings.gradle.kts` includes `app/` only when an Android SDK is found
(`local.properties`, `ANDROID_HOME`, or `ANDROID_SDK_ROOT`), and the root
build script only then adds the Android Gradle plugin and Google's Maven
repository. The agent's environment has neither, so it builds and tests
`core/` alone.

Units inside `core` are SI (metres, seconds, kilograms). YAML field names
carry their unit (`rangeKm`, `orbitAu`, `accelerationMS2`) and are converted
on load into the world.

## Classes

### `neumann.core.math`

- `Vec3` — 3D vector (data).
- `Frame` — rotation, choosing an "up", ship-relative ↔ space directions.
- `Units` — unit constants (AU, km, G, c, solar and Earth mass).
- `Noise` — random measurement errors (normal distribution).
- `ClosestApproach` — nearest distance between two objects moving in straight
  lines over one step.

### `neumann.core.data` — config holders (no logic)

- `GameData` — everything loaded from `data/`.
- `RulesFile` → `SimConfig`, `AutoPauseConfig`, `AiConfig`.
- `RulesFile` also holds `ShipRules` (align tolerance, turning fuel).
- `ModulesFile` → `ModuleType` (one per module type) → `PropulsionConfig`,
  `GeneratorConfig`, `BatteryConfig`, `FuelTankConfig`, `AmmoContainerConfig`,
  `SensorConfig`, `LaserConfig`, `CannonConfig`, `MissileBayConfig`; and
  `MunitionsConfig` → `SlugConfig`, `TorpedoConfig`.
- `ShipsFile` → `ShipDesign` (modules and counts).
- `ScenarioConfig` → `StarConfig`, `PlanetConfig`, `EntryConfig`,
  `PlayerPlacement`, `EnemyPlacement`.
- `DataSource` — interface: read a data file's text by name (platform
  supplies it).
- `GameDataLoader` — parses the YAML files into `GameData`.

### `neumann.core.world` — state holders (no flow logic)

- `World` — the whole state of one battle.
- `Star`, `Planet` — bodies on fixed circular orbits. A `Planet` with a
  `parent` is a moon of that planet.
- `Vessel` — a ship, fixed defence, or base. `VesselRole`, `Faction`.
- `ModuleKind`, `ModuleState` — one fitted module: hit points, and for
  cannons and missile bays their cooldown (and torpedoes held).
- `Heading`, `HeadingMode` — the direction a vessel turns to and holds.
- `Burn`, `BurnStage` — an active burn along the heading toward a desired
  speed and/or distance: accelerate, coast, brake.
- `SensorType` — telescope, radar, lidar, emission receiver.
  `SensorRules` — the radar and lidar switches.
- `WeaponRules` — the weapon-rule toggles. `WeaponKind` — laser, cannon, torpedo.
- `Torpedo`, `Slug` — projectiles. Both are `Tracked`: torpedoes are hot,
  slugs cold and tiny (radar only).
- `Tracked` — interface for anything sensors can see (vessels, torpedoes).
- `PositionHistory`, `HistorySample` — past positions, for light delay.
- `Contact`, `ContactKind`, `ContactBook` — what one vessel's sensors see:
  estimates with an uncertainty, possibly bearing only.
- `DamageTally` — damage dealt so far, by attacker and weapon.
- `PlayerWatch` — what the player has already been warned about (weapon
  ranges entered, low supplies).
- `GameEvent`, `EventKind`, `EventLog` — things that happened. Repeated
  damage from one source is merged into one entry a minute.
- `ApproachVector` — the player's entry choice.

### `neumann.core.sim` — flow (one job each)

Run in this order by `SimulationStep`:

| Class | Job |
|---|---|
| `EnemyAi` | Sets enemy targets, headings, and burns. |
| `PowerPhase` | Generators charge batteries; decides which sensors are on (powered) and whether the vessel is emitting. |
| `TurnPhase` | Turns vessels toward their heading at their turn rate; uses turning fuel. |
| `BurnPhase` | Runs burn stages (accelerate, coast, brake); thrusts once facing the right way; fuel use; burn end. |
| `JinkPhase` | Evasive manoeuvres: sideways pushes from translating thrusters. |
| `TorpedoGuidance` | Steers and thrusts torpedoes. |
| `LaserPhase` | Fires lasers (point defence first). |
| `CannonPhase` | Fires cannon slugs. Uses `InterceptSolver`. |
| `TorpedoLaunchPhase` | Launches torpedoes. |
| `MotionPhase` | Gravity and movement of vessels, torpedoes, and slugs. Uses `GravityField`. |
| *(time advances)* | |
| `PlanetMover` | Moves planets to the new time. |
| `ImpactPhase` | Slug hits and torpedo detonations. Uses `ClosestApproach`. |
| `BodyCollisionPhase` | Anything inside a star or planet is destroyed. |
| `SensorPhase` | Updates every vessel's contacts from its sensors that are on, with errors. Uses `LightDelay`. |
| `WeaponRangeWatch` | Warns when the player enters an enemy's weapon range. |
| `SupplyWatch` | Warns when the player's fuel or ammunition runs low. |
| `ClaimCheck` | Detects a claimed system or a lost ship. |
| `HistoryPhase` | Records positions for light delay. |

Helpers: `LaserFalloff` (laser damage by distance: full to the focus range, then 1/distance²), `StepSizer` (step length; short steps when projectiles are near
targets), `DamageResolver` (applies a hit to a module), `HeadingResolver`
(where a heading points now), `ShipPerformance` (mass, thrust, turn rate,
capacities, sensor and threat range from the working modules).

### `neumann.core.game` — orchestration

- `SystemGenerator` — a random `ScenarioConfig` from a seed (same seed, same
  system).
- `ScenarioBuilder` — builds a `World` from a scenario and the approach
  vector.
- `TrajectoryPredictor` — the coasting path from a position and velocity
  under gravity, and the body it passes closest to.
- `VesselFactory` — builds a vessel from its design, fully loaded.
- `DirectionDials` — turns YAW and PITCH dial angles into a direction in
  space, and a direction back into the turn still to go.
- `AutoPauseMonitor` — decides which new events stop a time skip (with repeat
  suppression).
- `PauseCollector` — gathers reasons to pause arriving within `groupSeconds`
  into one pause; drops nothing.
- `TimeSkip` — runs steps until the time is up or a pause is due.
- `TimeSkipResult` — how long it ran and why it stopped.
- `OrderDesk` — applies the player's orders.
- `GameSession` — what the UI talks to.

### `neumann.app` (Android)

- `MainActivity` — hosts the screens.
- `AssetDataSource` — `DataSource` over the APK's assets.
- `ApproachScreen` — choose the approach vector by dragging an arrow from
  the ship on the map.
- `BridgeScreen` — the map, top bar, and bottom tabs (SHIP, CONTACTS,
  MANOEUVRE, WEAPONS, LOG); no scrolling.
- `DirectionPad` — the two direction dials.
- `MapView` — the full-screen 3D map and its gestures. `ApproachArrow` —
  the arrow it draws on the approach screen.
- `MapCamera` — the map's centre, scale, rotation, and tilt; projects space
  to the screen.
- `Controls` — `ConsoleButton`, `ChoiceRow`, `Section`, `Line`.
- `DisplayFormat` — distances, speeds, times as text.
- `MessagePanel` — the most recent messages, and how the last time skip
  ended.
- `Theme` — ship-display colours.
