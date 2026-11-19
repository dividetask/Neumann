package neumann.core.game

import neumann.core.data.GameData
import neumann.core.data.ScenarioConfig
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.PlanetMover
import neumann.core.world.ApproachVector
import neumann.core.world.Faction
import neumann.core.world.HistorySample
import neumann.core.world.Planet
import neumann.core.world.Star
import neumann.core.world.VesselRole
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds the battle's World from a scenario (fixed or generated): either
 * after entry, moving at the player's approach vector ([build]), or on
 * arrival from the last star ([arrive]; see docs/game-design.md#arrival).
 */
class ScenarioBuilder(private val data: GameData) {
    fun build(approach: ApproachVector): World = build(data.scenario, approach)

    /** After entry: the probe already moving at the approach vector, with full tanks. */
    fun build(s: ScenarioConfig, approach: ApproachVector): World {
        val bearing = arrivalBearing(s)
        val speed = approach.speedKmS.coerceIn(s.entry.minSpeedKmS, s.entry.maxSpeedKmS) * Units.KM
        val aim = approach.direction?.normalized()?.takeIf { it.lengthSquared() > 0.0 } ?: -bearing
        return assemble(s, aim * speed, aim) {}
    }

    /** On arrival: heading for the star at the arrival speed, with the fuel the crossing left. */
    fun arrive(s: ScenarioConfig): World {
        val inward = -arrivalBearing(s)
        return assemble(s, Vec3.ZERO, inward) { world ->
            val p = world.player
            val arrival = ArrivalPlanner(data).plan(p, s.originLy, p.position.length())
            p.velocity = inward * arrival.speedMS
            p.fuel = arrival.fuelT
            world.arrival = arrival
        }
    }

    private fun assemble(s: ScenarioConfig, velocity: Vec3, facing: Vec3, prepare: (World) -> Unit): World {
        val star = Star(s.star.name, s.star.massSolar * Units.SOLAR_MASS, s.star.radiusKm * Units.KM)
        val planets = bodies(s, star)
        val factory = VesselFactory(data)
        val player = factory.build("P", s.player.name, Faction.PLAYER, s.player.design,
            arrivalBearing(s) * (s.entry.radiusAu * Units.AU), velocity, facing)
        val world = World(data, star, planets, player, s.belt, s.seed)
        world.vessels.add(player)

        for (e in s.enemies) {
            val body = planets.firstOrNull { it.name == e.orbits }
            val mass = body?.mass ?: star.mass
            val centre = body?.position ?: Vec3.ZERO
            val centreVelocity = body?.velocity ?: Vec3.ZERO
            val offset = Vec3.of(e.offsetKm) * Units.KM
            val orbit = orbitVelocity(mass, body?.inclination ?: 0.0, offset)
            // Ships face along their orbit; fixed defences and bases face away from what they orbit.
            val facing = if (data.designs.getValue(e.design).role == VesselRole.SHIP) orbit else offset
            val v = factory.build(world.newId("E"), e.name, Faction.ENEMY, e.design, centre + offset,
                centreVelocity + orbit, facing)
            v.rules.pointDefenceFirst = true
            v.rules.laserAutoFire = true
            v.rules.cannonAutoFire = true
            v.rules.torpedoAutoLaunch = true
            v.ambush = e.ambush
            if (e.patrol.isNotEmpty()) v.patrol = neumann.core.world.Patrol(e.patrol)
            v.sensorPosture = e.sensors
            v.ambushRange = e.ambushRangeKm * Units.KM
            world.vessels.add(v)
        }

        prepare(world)
        for (v in world.vessels) v.history.record(HistorySample(0.0, v.position, v.velocity, false))
        world.lastHistorySample = 0.0
        return world
    }

    /** The star's planets and moons, placed at time 0. Moons orbit their parent planet. */
    fun bodies(s: ScenarioConfig, star: Star): List<Planet> {
        val mover = PlanetMover()
        val built = LinkedHashMap<String, Planet>()
        for (c in s.planets) {
            val parent = c.parent?.let { built.getValue(it) }
            val r = if (parent == null) c.orbitAu * Units.AU else c.orbitKm * Units.KM
            val centralMass = parent?.mass ?: star.mass
            val p = Planet(c.name, c.massEarth * Units.EARTH_MASS, c.radiusKm * Units.KM, r,
                c.inclinationDeg * Units.DEGREE, c.phaseDeg * Units.DEGREE,
                2 * Math.PI * sqrt(r * r * r / (Units.G * centralMass)), parent)
            mover.place(p, 0.0)
            built[c.name] = p
        }
        return built.values.toList()
    }

    /** Where the ship arrives: on the entry sphere at the scenario's bearing. */
    fun arrivalPoint(s: ScenarioConfig): Vec3 = arrivalBearing(s) * (s.entry.radiusAu * Units.AU)

    private fun arrivalBearing(s: ScenarioConfig) = bearing(s.entry.bearingDeg)

    /** Unit vector from the star toward the arrival point, from [azimuth, elevation] in degrees. */
    private fun bearing(deg: List<Double>): Vec3 {
        val az = deg[0] * Units.DEGREE
        val el = deg[1] * Units.DEGREE
        return Vec3(cos(el) * cos(az), cos(el) * sin(az), sin(el))
    }

    /** Velocity, relative to a body of [mass], of a circular orbit at [offset] in a plane tilted by [inclination]. */
    private fun orbitVelocity(mass: Double, inclination: Double, offset: Vec3): Vec3 {
        val normal = Vec3(0.0, -sin(inclination), cos(inclination))
        var along = normal.cross(offset)
        if (along.lengthSquared() == 0.0) along = Vec3(1.0, 0.0, 0.0).cross(offset)
        return along.normalized() * sqrt(Units.G * mass / offset.length())
    }
}
