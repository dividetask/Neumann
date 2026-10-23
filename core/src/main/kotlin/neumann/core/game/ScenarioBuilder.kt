package neumann.core.game

import neumann.core.data.GameData
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.PlanetMover
import neumann.core.world.ApproachVector
import neumann.core.world.Faction
import neumann.core.world.HistorySample
import neumann.core.world.Planet
import neumann.core.world.Star
import neumann.core.world.Station
import neumann.core.world.VesselRole
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Builds the battle's World from the scenario and the player's approach vector. */
class ScenarioBuilder(private val data: GameData) {
    fun build(approach: ApproachVector): World {
        val s = data.scenario
        val star = Star(s.star.name, s.star.massSolar * Units.SOLAR_MASS, s.star.radiusKm * Units.KM)
        val mover = PlanetMover()
        val planets = s.planets.map {
            val r = it.orbitAu * Units.AU
            Planet(it.name, it.massEarth * Units.EARTH_MASS, it.radiusKm * Units.KM, r,
                it.inclinationDeg * Units.DEGREE, it.phaseDeg * Units.DEGREE,
                2 * Math.PI * sqrt(r * r * r / (Units.G * star.mass))).also { p -> mover.place(p, 0.0) }
        }

        val bearing = bearing(approach)
        val speed = approach.speedKmS.coerceIn(s.entry.minSpeedKmS, s.entry.maxSpeedKmS) * Units.KM
        val factory = VesselFactory(data)
        val player = factory.build("P", s.player.name, Faction.PLAYER, s.player.design,
            bearing * (s.entry.radiusAu * Units.AU), -bearing * speed, -bearing)
        val world = World(data, star, planets, player)
        world.vessels.add(player)

        for (e in s.enemies) {
            val design = data.designs.getValue(e.design)
            val planet = planets.first { it.name == e.planet }
            val offset = Vec3.of(e.offsetKm) * Units.KM
            val v = if (design.role == VesselRole.SHIP) {
                val orbit = orbitVelocity(planet, offset)
                factory.build(world.newId("E"), e.name, Faction.ENEMY, e.design, planet.position + offset,
                    planet.velocity + orbit, orbit)
            } else {
                factory.build(world.newId("E"), e.name, Faction.ENEMY, e.design, planet.position + offset,
                    planet.velocity, offset, Station(planet, offset))
            }
            v.rules.laserPointDefence = true
            v.rules.laserAutoFire = true
            v.rules.cannonAutoFire = true
            v.rules.torpedoAutoLaunch = true
            v.ambush = e.ambush
            v.ambushRange = e.ambushRangeKm * Units.KM
            world.vessels.add(v)
        }

        for (v in world.vessels) v.history.record(HistorySample(0.0, v.position, v.velocity, false))
        world.lastHistorySample = 0.0
        return world
    }

    /** Unit vector from the star toward the entry point. */
    private fun bearing(a: ApproachVector): Vec3 {
        val az = a.azimuthDeg * Units.DEGREE
        val el = a.elevationDeg * Units.DEGREE
        return Vec3(cos(el) * cos(az), cos(el) * sin(az), sin(el))
    }

    /** Velocity, relative to the planet, of a circular orbit at [offset]. */
    private fun orbitVelocity(planet: Planet, offset: Vec3): Vec3 {
        val normal = Vec3(0.0, -sin(planet.inclination), cos(planet.inclination))
        var along = normal.cross(offset)
        if (along.lengthSquared() == 0.0) along = Vec3(1.0, 0.0, 0.0).cross(offset)
        return along.normalized() * sqrt(Units.G * planet.mass / offset.length())
    }
}
