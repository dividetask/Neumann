package neumann.core.data

import com.charleskorn.kaml.Yaml

/** Parses the YAML data files into GameData. */
class GameDataLoader(private val source: DataSource) {
    fun load(): GameData {
        val rules = Yaml.default.decodeFromString(RulesFile.serializer(), source.read("rules.yaml"))
        val ships = Yaml.default.decodeFromString(ShipsFile.serializer(), source.read("ships.yaml"))
        val scenario = Yaml.default.decodeFromString(ScenarioConfig.serializer(), source.read("scenario.yaml"))
        checkReferences(ships.ships, scenario)
        return GameData(rules, ships.ships, scenario)
    }

    private fun checkReferences(ships: Map<String, ShipClass>, scenario: ScenarioConfig) {
        val planets = scenario.planets.map { it.name }.toSet()
        require(scenario.player.shipClass in ships) { "unknown ship class ${scenario.player.shipClass}" }
        for (e in scenario.enemies) {
            require(e.shipClass in ships) { "${e.name}: unknown ship class ${e.shipClass}" }
            require(e.planet in planets) { "${e.name}: unknown planet ${e.planet}" }
        }
    }
}
