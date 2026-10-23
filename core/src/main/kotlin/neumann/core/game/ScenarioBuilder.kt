package neumann.core.game

import neumann.core.data.GameData
import neumann.core.math.Frame
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

        val bearing = bearing(s.entry.bearingDeg)
        val speed = approach.speedKmS.coerceIn(s.entry.minSpeedKmS, s.entry.maxSpeedKmS) * Units.KM
        val lookingAtStar = -bearing
        val aim = DirectionDials.direction(lookingAtStar, Frame.upFor(lookingAtStar),
            DirectionDials.Angles(approach.yawDeg, approach.pitchDeg))
        val factory = VesselFactory(data)
        val player = factory.build("P", s.player.name, Faction.PLAYER, s.player.design,
            bearing * (s.entry.radiusAu * Units.AU), aim * speed, aim)
        val world = World(data, star, planets, player)
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
