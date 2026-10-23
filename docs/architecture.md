# Architecture

Two Gradle modules:

- `core/` — plain Kotlin (JVM), no Android. All game rules. Unit-tested with
  `./gradlew :core:test`.
- `app/` — the Android app. Screens only; it drives `core` through
  `GameSession`.

`data/` (YAML: `rules.yaml`, `modules.yaml`, `ships.yaml`, `scenario.yaml`) is loaded by
`GameDataLoader`. The app packages `data/` as its assets.

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
- `ClosestApproach` — nearest distance between two objects moving in straight
  lines over one step.

### `neumann.core.data` — config holders (no logic)

- `GameData` — everything loaded from `data/`.
- `RulesFile` → `SimConfig`, `AutoPauseConfig`, `AiConfig`.
- `RulesFile` also holds `ShipRules` (align tolerance, unpowered sensor
  fraction).
- `ModulesFile` → `ModuleType` (one per module type) → `PropulsionConfig`,
  `GeneratorConfig`, `BatteryConfig`, `FuelTankConfig`, `AmmoContainerConfig`,
  `ScannerConfig`, `LaserConfig`, `CannonConfig`, `MissileBayConfig`; and
  `MunitionsConfig` → `SlugConfig`, `TorpedoConfig`.
- `ShipsFile` → `ShipDesign` (modules and counts).
- `ScenarioConfig` → `StarConfig`, `PlanetConfig`, `EntryConfig`,
  `PlayerPlacement`, `EnemyPlacement`.
- `DataSource` — interface: read a data file's text by name (platform
  supplies it).
- `GameDataLoader` — parses the YAML files into `GameData`.

### `neumann.core.world` — state holders (no flow logic)

- `World` — the whole state of one battle.
- `Star`, `Planet` — bodies on fixed orbits.
- `Vessel` — a ship, fixed defence, or base. `VesselRole`, `Faction`.
- `ModuleKind`, `ModuleState` — one fitted module: hit points, and for
  cannons and missile bays their cooldown (and torpedoes held).
- `Station` — the planet and offset a fixed vessel holds station at.
- `Heading`, `HeadingMode` — the direction a vessel turns to and holds, and
  for FIXED headings an optional up (roll).
- `Burn` — an active burn along the heading.
- `WeaponRules` — the weapon-rule toggles. `WeaponKind` — laser, cannon, torpedo.
- `Torpedo`, `Slug` — projectiles.
- `Tracked` — interface for anything sensors can see (vessels, torpedoes).
- `PositionHistory`, `HistorySample` — past positions, for light delay.
- `Contact`, `ContactKind`, `ContactBook` — what one vessel's sensors see.
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
| `PowerPhase` | Generators charge batteries; scanners draw power. |
| `TurnPhase` | Turns vessels toward their heading (then roll) at their turn rate; uses turning fuel. |
| `BurnPhase` | Thrusts once facing the heading (fuel use, burn end). |
| `TorpedoGuidance` | Steers and thrusts torpedoes. |
| `LaserPhase` | Fires lasers (point defence first). |
| `CannonPhase` | Fires cannon slugs. Uses `InterceptSolver`. |
| `TorpedoLaunchPhase` | Launches torpedoes. |
| `MotionPhase` | Gravity and movement of free ships, torpedoes, and slugs. Uses `GravityField`. |
| *(time advances)* | |
| `PlanetMover` | Moves planets to the new time. |
| `StationKeeping` | Keeps fixed defences and bases at their station. |
| `ImpactPhase` | Slug hits and torpedo detonations. Uses `ClosestApproach`. |
| `BodyCollisionPhase` | Anything inside a star or planet is destroyed. |
| `SensorPhase` | Updates every vessel's contacts. Uses `LightDelay`. |
| `WeaponRangeWatch` | Warns when the player enters an enemy's weapon range. |
| `SupplyWatch` | Warns when the player's fuel or ammunition runs low. |
| `ClaimCheck` | Detects a claimed system or a lost ship. |
| `HistoryPhase` | Records positions for light delay. |

Helpers: `StepSizer` (step length; short steps when projectiles are near
targets), `DamageResolver` (applies a hit to a module), `HeadingResolver`
(where a heading points now), `ShipPerformance` (mass, thrust, turn rate,
capacities, sensor and threat range from the working modules).

### `neumann.core.game` — orchestration

- `ScenarioBuilder` — builds a `World` from the scenario and the approach
  vector.
- `VesselFactory` — builds a vessel from its design, fully loaded.
- `DirectionDials` — turns YAW, PITCH, and ROLL dial angles into an
  orientation in space, and an orientation back into the turn still to go.
- `AutoPauseMonitor` — decides which new events stop a time skip (with repeat
  suppression).
- `TimeSkip` — runs steps until the time is up or a pause.
- `TimeSkipResult` — how long it ran and why it stopped.
- `OrderDesk` — applies the player's orders.
- `GameSession` — what the UI talks to.

### `neumann.app` (Android)

- `MainActivity` — hosts the screens.
- `AssetDataSource` — `DataSource` over the APK's assets.
- `ApproachScreen` — choose the approach vector.
- `BridgeScreen` — status, plot, contacts, orders, time skips, event log.
- `DirectionPad` — the three direction dials.
- `SystemPlot` — top and side views of the system. `PlotZoom` — its zoom
  levels.
- `Controls` — `ConsoleButton`, `ChoiceRow`, `Section`, `Line`.
- `DisplayFormat` — distances, speeds, times as text.
- `Theme` — ship-display colours.
