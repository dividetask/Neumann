package neumann.core.sim

import neumann.core.data.SimConfig
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.World
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Chooses the next step length: as long as nothing important would be missed
 * (up to `maxStepSeconds`), and short near projectiles, weapon ranges,
 * orbits, and the end of a burn (see docs/game-design.md#physics).
 */
class StepSizer(private val config: SimConfig, private val performance: ShipPerformance) {
    private val clock = BurnClock(performance, HeadingResolver())

    fun next(world: World, remaining: Double): Double {
        val dt = minOf(projectiles(world), approaches(world), orbits(world), burns(world))
        return min(dt.coerceAtLeast(config.fineStepSeconds), remaining)
    }

    /** Short steps when a torpedo or slug is about to reach something. */
    private fun projectiles(world: World): Double {
        var soonest = Double.MAX_VALUE
        for (t in world.torpedoes) {
            if (!t.alive) continue
            val target = world.vessel(t.targetId) ?: continue
            soonest = min(soonest, timeToReach(t.position, t.velocity, target.position, target.velocity))
        }
        for (s in world.slugs) {
            if (!s.alive) continue
            for (v in world.vessels) {
                if (!v.alive || v.faction == s.faction) continue
                soonest = min(soonest, timeToReach(s.position, s.velocity, v.position, v.velocity))
            }
            for (t in world.torpedoes) {
                if (!t.alive || t.faction == s.faction) continue
                soonest = min(soonest, timeToReach(s.position, s.velocity, t.position, t.velocity))
            }
        }
        if (soonest == Double.MAX_VALUE) return config.maxStepSeconds
        return (soonest / config.fineStepDivisions).coerceIn(config.fineStepSeconds, config.baseStepSeconds)
    }

    /** Base steps inside weapon range; outside, a tenth of the time until the player and an enemy could get there. */
    private fun approaches(world: World): Double {
        val p = world.player
        if (!p.alive) return config.maxStepSeconds
        var dt = config.maxStepSeconds
        for (e in world.vessels) {
            if (!e.alive || e.faction == p.faction) continue
            val gap = (e.position - p.position).length() - max(performance.threatRange(e), performance.threatRange(p))
            if (gap <= 0.0) return config.baseStepSeconds
            dt = min(dt, max(config.baseStepSeconds, gap / max((e.velocity - p.velocity).length(), 1.0) / config.fineStepDivisions))
        }
        return dt
    }

    /** Every ship takes at least `stepsPerOrbit` steps per orbit around each body it is near, so orbits stay true. */
    private fun orbits(world: World): Double {
        var dt = config.maxStepSeconds
        for (v in world.vessels) {
            if (!v.alive) continue
            dt = min(dt, perOrbit(v.position.length(), world.star.mass))
            for (b in world.planets) dt = min(dt, perOrbit((v.position - b.position).length(), b.mass))
        }
        return max(dt, config.baseStepSeconds)
    }

    private fun perOrbit(r: Double, mass: Double): Double =
        2 * Math.PI * sqrt(r * r * r / (Units.G * mass)) / config.stepsPerOrbit

    /** A tenth of the time each burn has left. */
    private fun burns(world: World): Double {
        var dt = config.maxStepSeconds
        for (v in world.vessels) {
            if (!v.active) continue
            val left = clock.secondsLeft(v, world.time) ?: continue
            dt = min(dt, max(config.baseStepSeconds, left / config.fineStepDivisions))
        }
        return dt
    }

    private fun timeToReach(p: Vec3, v: Vec3, targetP: Vec3, targetV: Vec3): Double =
        (targetP - p).length() / max((v - targetV).length(), 1.0)
}
