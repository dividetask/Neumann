package neumann.core.data

import com.charleskorn.kaml.Yaml
import neumann.core.math.Vec3
import neumann.core.world.ModuleKind

/** Parses the YAML data files into GameData. */
class GameDataLoader(private val source: DataSource) {
    fun load(): GameData {
        val rules = Yaml.default.decodeFromString(RulesFile.serializer(), source.read("rules.yaml"))
        val modules = Yaml.default.decodeFromString(ModulesFile.serializer(), source.read("modules.yaml"))
        val ships = Yaml.default.decodeFromString(ShipsFile.serializer(), source.read("ships.yaml"))
        val scenario = Yaml.default.decodeFromString(ScenarioConfig.serializer(), source.read("scenario.yaml"))
        val generation = Yaml.default.decodeFromString(GenerationConfig.serializer(), source.read("system-generation.yaml"))
        modules.modules.forEach { (id, m) -> checkModule(id, m) }
        ships.designs.forEach { (id, d) ->
            for (m in d.modules.keys) require(m in modules.modules) { "design $id: unknown module $m" }
            require(d.modules.keys.count { modules.modules.getValue(it).kind == ModuleKind.COMPUTER } == 1) { "design $id: needs exactly one computer" }
        }
        for ((id, m) in modules.modules) m.computer?.assumedLaser?.let {
            require(modules.modules[it]?.laser != null) { "module $id: assumedLaser $it is not a laser" }
        }
        checkScenario(ships.designs, scenario)
        return GameData(rules, modules.modules, modules.munitions, ships.designs, scenario, generation)
    }

    /** The sub-config for the module's kind must be set. */
    private fun checkModule(id: String, m: ModuleType) {
        val hasConfig = when (m.kind) {
            ModuleKind.HULL -> true
            ModuleKind.PROPULSION -> m.propulsion != null
            ModuleKind.GENERATOR -> m.generator != null
            ModuleKind.BATTERY -> m.battery != null
            ModuleKind.FUEL_TANK -> m.fuelTank != null
            ModuleKind.AMMO_CONTAINER -> m.ammoContainer != null
            ModuleKind.SENSOR -> m.sensor != null
            ModuleKind.LASER_TURRET -> m.laser != null
            ModuleKind.CANNON -> m.cannon != null
            ModuleKind.MISSILE_BAY -> m.missileBay != null
            ModuleKind.COMPUTER -> m.computer != null
        }
        require(hasConfig) { "module $id (${m.kind}) is missing its ${m.kind.name.lowercase()} settings" }
        val made = m.materials.values.sum()
        require(Math.abs(made - m.massT) <= 0.01 * m.massT) { "module $id: materials add up to $made t, not its ${m.massT} t" }
    }

    fun checkScenario(designs: Map<String, ShipDesign>, scenario: ScenarioConfig) {
        val seen = HashSet<String>()
        for (p in scenario.planets) {
            p.parent?.let { require(it in seen) { "${p.name}: parent $it must come before it" } }
            seen.add(p.name)
        }
        val bodies = scenario.planets.map { it.name }.toSet() + scenario.star.name
        require(scenario.player.design in designs) { "unknown design ${scenario.player.design}" }
        for (e in scenario.enemies) {
            require(e.design in designs) { "${e.name}: unknown design ${e.design}" }
            require(e.orbits in bodies) { "${e.name}: unknown body ${e.orbits}" }
            require(Vec3.of(e.offsetKm).length() > 0.0) { "${e.name}: offsetKm must not be zero" }
        }
    }
}
