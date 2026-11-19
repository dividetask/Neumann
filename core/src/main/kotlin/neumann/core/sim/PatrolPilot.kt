package neumann.core.sim

import neumann.core.data.AiConfig
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.Burn
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.Patrol
import neumann.core.world.Vessel
import neumann.core.world.VesselRole
import neumann.core.world.World
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Flies a raider's patrol while it has not seen the player (see
 * docs/game-design.md#raider-patrols): one orbit of each planet, moon, or the
 * star in turn, a flyby of each base, a trip to its base to refuel when low.
 * Dumb, but it never steers into a body.
 */
class PatrolPilot(private val config: AiConfig, private val performance: ShipPerformance) {
    private val orbitR = config.patrolOrbitKm * Units.KM

    fun fly(world: World, v: Vessel, dt: Double) {
        val patrol = v.patrol ?: return
        if (!v.has(ModuleKind.PROPULSION) || v.fuel <= 0.0) return coast(v)
        if (avoidCrash(world, v, dt)) return
        val capacity = performance.fuelCapacity(v)
        val base = world.vessels.firstOrNull { it.active && it.faction == v.faction && it.design.role == VesselRole.BASE }
        if (base != null && v.fuel < capacity * config.refuelBelowFraction) patrol.refuelling = true
        if (patrol.refuelling) {
            if (base == null || v.fuel >= capacity * REFUELLED) patrol.refuelling = false
            else return approach(world, v, base.position, base.velocity, config.refuelRangeKm * Units.KM / 2, dt)
        }
        val name = patrol.current ?: return coast(v)
        val body = world.planets.firstOrNull { it.name == name }
        val star = name == world.star.name
        val baseTarget = world.vessels.firstOrNull { it.name == name && it.active }
        when {
            body != null -> orbit(world, v, patrol, body.position, body.velocity, body.mass, dt)
            star -> orbit(world, v, patrol, Vec3.ZERO, Vec3.ZERO, world.star.mass, dt)
            baseTarget != null -> {
                // A base has too little gravity to orbit: fly by within the patrol distance, then move on.
                if ((baseTarget.position - v.position).length() <= orbitR) patrol.advance()
                else approach(world, v, baseTarget.position, baseTarget.velocity, orbitR * 0.5, dt)
            }
            else -> patrol.advance() // gone (destroyed): skip it
        }
    }

    /** In orbit round the target: count the angle swept, and move on after a full orbit; otherwise get there. */
    private fun orbit(world: World, v: Vessel, patrol: Patrol, at: Vec3, vel: Vec3, mass: Double, dt: Double) {
        val r = v.position - at
        val d = r.length()
        if (d in orbitR * IN_ORBIT_MIN..orbitR * IN_ORBIT_MAX) {
            val circular = sqrt(Units.G * mass / d)
            val rel = v.velocity - vel
            val tangent = (rel - r.normalized() * rel.dot(r.normalized()))
            val wanted = if (tangent.lengthSquared() > 0.0) tangent.normalized() * circular else Vec3.ZERO
            patrol.lastOffset?.let { patrol.swept += it.angleTo(r) }
            patrol.lastOffset = r
            if (patrol.swept >= 2 * Math.PI) patrol.advance()
            // Keep it near circular: correct only clear errors.
            val dv = vel + wanted - v.velocity
            if (dv.length() > circular * ORBIT_TOLERANCE) return burn(world, v, dv)
            return coast(v)
        }
        patrol.lastOffset = null
        approach(world, v, at, vel, orbitR, dt)
    }

    /**
     * Head for a point [offset] beside [at] (never at its centre), matching its velocity
     * [vel] on arrival: close no faster than it can brake.
     */
    private fun approach(world: World, v: Vessel, at: Vec3, vel: Vec3, offset: Double, dt: Double) {
        val toward = at - v.position
        val side = toward.cross(Vec3(0.0, 0.0, 1.0)).let { if (it.lengthSquared() > 0.0) it.normalized() else Vec3(1.0, 0.0, 0.0) }
        val aim = at + side * offset - v.position
        val accel = performance.accelerationMS2(v)
        val closing = min(sqrt(accel * aim.length()), config.maxClosingSpeedKmS * Units.KM)
        val dv = vel + aim.normalized() * closing - v.velocity
        if (dv.length() < accel * dt * 0.5) return coast(v)
        burn(world, v, dv)
    }

    /**
     * On course to hit a body within a day (its straight-line closest approach comes within
     * the body's size, plus a margin): burn straight away from it first.
     */
    private fun avoidCrash(world: World, v: Vessel, dt: Double): Boolean {
        val bodies = world.planets.map { Triple(it.position, it.velocity, it.radius) } + Triple(Vec3.ZERO, Vec3.ZERO, world.star.radius)
        for ((at, vel, radius) in bodies) {
            val r = v.position - at
            val u = v.velocity - vel
            val uu = u.lengthSquared()
            if (uu == 0.0) continue
            val tca = -r.dot(u) / uu
            if (tca <= 0.0 || tca > DANGER_SECONDS) continue
            if ((r + u * tca).length() > radius * CRASH_MARGIN + CRASH_MARGIN_M) continue
            burn(world, v, r.normalized() * (sqrt(uu) * 2.0))
            return true
        }
        return false
    }

    private fun burn(world: World, v: Vessel, dv: Vec3) {
        v.heading = Heading(HeadingMode.FIXED, dv)
        v.burn = Burn(1.0, dv.length(), null, world.time, v.position, v.velocity)
    }

    private fun coast(v: Vessel) {
        v.burn = null
    }

    companion object {
        const val IN_ORBIT_MIN = 0.5
        const val IN_ORBIT_MAX = 2.0
        /** Orbit speed errors under this fraction of the circular speed are left alone. */
        const val ORBIT_TOLERANCE = 0.2
        /** Refuelling ends once the tanks are this full. */
        const val REFUELLED = 0.95
        const val DANGER_SECONDS = 86400.0
        /** A pass this close counts as a crash course: the body's radius times this, plus the metres below. */
        const val CRASH_MARGIN = 1.5
        const val CRASH_MARGIN_M = 1_000_000.0
    }
}
