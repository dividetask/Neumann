package neumann.core.game

import neumann.core.data.BeltConfig
import neumann.core.data.EnemyPlacement
import neumann.core.data.EntryConfig
import neumann.core.data.GameData
import neumann.core.data.PlanetConfig
import neumann.core.data.ScenarioConfig
import neumann.core.data.StarConfig
import neumann.core.math.Units
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates a random star system from a seed, within the ranges in
 * data/system-generation.yaml (see docs/game-design.md#random-systems).
 * The same seed always gives the same system.
 */
class SystemGenerator(private val data: GameData) {
    private val g = data.generation
    private val reach = DetectionReach(data).au()

    fun generate(seed: Long): ScenarioConfig {
        val rnd = Random(seed)
        val starName = g.starNames[rnd.nextInt(g.starNames.size)]
        val starMass = rnd.range(g.star.massSolarMin, g.star.massSolarMax)
        val star = StarConfig(starName, starMass, g.star.radiusKmPerSolarMass * starMass)

        val planets = ArrayList<PlanetConfig>()
        val count = rnd.nextInt(g.planets.countMin, g.planets.countMax + 1)
        var orbit = rnd.range(g.planets.firstOrbitAuMin, g.planets.firstOrbitAuMax)
        for (i in 0 until count) {
            if (orbit > g.planets.maxOrbitAu) break // real systems are no wider
            val rocky = orbit < g.planets.rockyLimitAu
            val kind = if (rocky) g.planets.rocky else g.planets.giant
            val mass = rnd.logRange(kind.massEarthMin, kind.massEarthMax)
            val planet = PlanetConfig(
                name = "$starName ${roman(i + 1)}",
                orbitAu = orbit,
                inclinationDeg = rnd.range(-g.planets.inclinationDegMax, g.planets.inclinationDegMax),
                phaseDeg = rnd.range(0.0, 360.0),
                massEarth = mass,
                radiusKm = if (rocky) rockyRadiusKm(mass) else giantRadiusKm(mass),
            )
            planets.add(planet)
            planets.addAll(moons(rnd, planet, starMass, rnd.nextInt(kind.moonsMin, kind.moonsMax + 1)))
            orbit *= rnd.range(g.planets.spacingMin, g.planets.spacingMax)
        }

        val worlds = planets.filter { it.parent == null }
        val rockyWorlds = worlds.filter { it.orbitAu < g.planets.rockyLimitAu }.ifEmpty { listOf(worlds.first()) }
        val giants = worlds.filter { it.orbitAu >= g.planets.rockyLimitAu }
        val belt = belt(rnd, rockyWorlds, giants)

        // Far out: beyond the outermost orbit by several enemy detection ranges.
        val entryRadius = worlds.maxOf { it.orbitAu } + g.entry.detectionRangeMultiple * reach
        val f = data.scenario.entry
        val entry = EntryConfig(entryRadius, listOf(rnd.range(0.0, 360.0), rnd.range(-g.entry.elevationDegMax, g.entry.elevationDegMax)),
            f.minSpeedKmS, f.maxSpeedKmS, f.defaultSpeedKmS)

        return ScenarioConfig("$starName system", star, planets, entry, data.scenario.player,
            enemies(rnd, rockyWorlds, giants, planets, starMass), belt, seed)
    }

    private fun moons(rnd: Random, planet: PlanetConfig, starMass: Double, count: Int): List<PlanetConfig> {
        val m = g.moons
        val hill = hillKm(planet, starMass)
        var orbit = maxOf(planet.radiusKm * rnd.range(m.firstOrbitRadiiMin, m.firstOrbitRadiiMax), m.minOrbitKm)
        val moons = ArrayList<PlanetConfig>()
        for (i in 0 until count) {
            if (orbit > hill * g.enemies.hillFraction) break // further out would not stay bound
            val mass = minOf(rnd.logRange(m.massEarthMin, m.massEarthMax), planet.massEarth * m.maxMassFraction)
            moons.add(PlanetConfig(
                name = "${planet.name}${'a' + i}",
                inclinationDeg = rnd.range(-g.planets.inclinationDegMax, g.planets.inclinationDegMax),
                phaseDeg = rnd.range(0.0, 360.0),
                massEarth = mass,
                radiusKm = moonRadiusKm(mass),
                parent = planet.name,
                orbitKm = orbit,
            ))
            orbit *= rnd.range(m.spacingMin, m.spacingMax)
        }
        return moons
    }

    /** Sometimes a belt in the gap between the rocky planets and the giants. */
    private fun belt(rnd: Random, rocky: List<PlanetConfig>, giants: List<PlanetConfig>): BeltConfig? {
        if (giants.isEmpty() || rnd.nextDouble() >= g.belt.chance) return null
        val inner = rocky.maxOf { it.orbitAu } * 1.2
        val outer = giants.minOf { it.orbitAu } / 1.3
        return if (outer - inner >= g.belt.minGapAu) BeltConfig(inner, outer) else null
    }

    /**
     * The base and defence platform orbit a random rocky planet; each raider
     * patrols near the base or among a giant's moons; one may lie in ambush.
     */
    private fun enemies(rnd: Random, rocky: List<PlanetConfig>, giants: List<PlanetConfig>, all: List<PlanetConfig>, starMass: Double): List<EnemyPlacement> {
        val e = g.enemies
        val home = rocky[rnd.nextInt(rocky.size)]
        val station = minOf(e.stationOffsetKm, hillKm(home, starMass) * e.hillFraction / 2)
        val list = arrayListOf(
            EnemyPlacement("${home.name} Base", "base", home.name, listOf(station, 0.0, 0.0)),
            EnemyPlacement("${home.name} Platform", "defencePlatform", home.name, listOf(-station, 0.0, station / 8)),
        )
        val ambusher = if (rnd.nextDouble() < e.ambushChance) rnd.nextInt(e.raiderCount) else -1
        for (i in 0 until e.raiderCount) {
            val nearBase = giants.isEmpty() || rnd.nextDouble() < e.nearBaseChance
            val (body, distance) = if (nearBase) {
                home to minOf(rnd.range(e.patrolKmMin, e.patrolKmMax), hillKm(home, starMass) * e.hillFraction)
            } else {
                // Among a giant's moons: orbit the giant just outside one of its moons.
                val giant = giants[rnd.nextInt(giants.size)]
                val moons = all.filter { it.parent == giant.name }
                val r = moons.randomOrNull(rnd)?.orbitKm?.times(1.1) ?: (giant.radiusKm * 20)
                giant to minOf(r, hillKm(giant, starMass) * e.hillFraction)
            }
            val a = rnd.range(0.0, 2 * Math.PI)
            val ambush = i == ambusher
            list.add(EnemyPlacement("Raider ${i + 1}", "raider", body.name, listOf(distance * cos(a), distance * sin(a), 0.0),
                ambush = ambush, ambushRangeKm = if (ambush) e.ambushRangeKm else 0.0))
        }
        return list
    }

    /** Hill-sphere radius of a planet around the star, in km: inside it, the planet's gravity holds orbits. */
    private fun hillKm(p: PlanetConfig, starMassSolar: Double): Double =
        p.orbitAu * Units.AU / Units.KM * (p.massEarth * Units.EARTH_MASS / (3 * starMassSolar * Units.SOLAR_MASS)).pow(1.0 / 3)

    companion object {
        // Approximate mass-radius relations for real planets and moons.
        fun rockyRadiusKm(massEarth: Double) = 6371.0 * massEarth.pow(0.27)
        fun giantRadiusKm(massEarth: Double) = 25_000.0 + 45_000.0 * minOf(1.0, massEarth / 318.0)
        fun moonRadiusKm(massEarth: Double) = 1737.0 * (massEarth / 0.0123).pow(0.3)

        fun roman(n: Int): String = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII")[n - 1]
    }
}

private fun Random.range(min: Double, max: Double) = if (max <= min) min else nextDouble(min, max)

/** Uniform in log space: as many small worlds as large ones. */
private fun Random.logRange(min: Double, max: Double) = Math.exp(range(Math.log(min), Math.log(max)))
