package neumann.core.sim

import neumann.core.data.AutoPauseConfig
import neumann.core.math.ClosestApproach
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.EventKind
import neumann.core.world.World
import kotlin.math.max

/**
 * Warns when the player's ship is on course to hit the star, a planet, a moon,
 * a ship, or a base within `collisionWarningSeconds`, coasting from where it is
 * now under gravity (see docs/game-design.md#auto-pause-events). Warns once per
 * object, and again only after the course has changed and come back.
 */
class CollisionWatch(private val config: AutoPauseConfig) {
    private val mover = PlanetMover()

    fun run(world: World) {
        val p = world.player
        val w = world.playerWatch
        if (!p.alive || world.time - w.lastCollisionCheck < config.collisionCheckSeconds) return
        w.lastCollisionCheck = world.time
        val hits = onCourse(world)
        for ((id, name) in hits) if (id !in w.onCourseFor) {
            world.event(EventKind.COLLISION_WARNING, id, "Collision warning: on course to hit $name within ${hoursText(config.collisionWarningSeconds)}")
        }
        w.onCourseFor.clear()
        w.onCourseFor.addAll(hits.keys)
    }

    /** Everything the coasting path hits within the warning time: id to name. */
    private fun onCourse(world: World): Map<String, String> {
        val p = world.player
        val hits = LinkedHashMap<String, String>()
        // Ships and bases from the player's estimates, moving in straight lines.
        val vessels = p.contacts.current.values.filter { it.kind.isVessel && !it.bearingOnly }
        val gravity = GravityField()
        var pos = p.position
        var vel = p.velocity
        var t = 0.0
        val dt = max(STEP_S, config.collisionWarningSeconds / STEPS)
        while (t < config.collisionWarningSeconds) {
            val now = world.time + t
            val a = pull(-pos, world.star.mass, world.star.radius) +
                world.planets.fold(Vec3.ZERO) { acc, b -> acc + pull(mover.positionAt(b, now) - pos, b.mass, b.radius) }
            val next = pos + (vel + a * dt) * dt
            if (ClosestApproach.between(pos, next, Vec3.ZERO, Vec3.ZERO).distance <= world.star.radius) hits["star"] = world.star.name
            for (b in world.planets) {
                val at = mover.positionAt(b, now)
                val then = mover.positionAt(b, now + dt)
                if (ClosestApproach.between(pos, next, at, then).distance <= b.radius) hits[b.name] = b.name
            }
            for (c in vessels) {
                val size = world.vessel(c.id)?.design?.sizeM ?: 0.0
                val at = c.predictedPosition + c.observedVelocity * t
                if (ClosestApproach.between(pos, next, at, at + c.observedVelocity * dt).distance <= size + Units.KM) hits[c.id] = c.name
            }
            vel += a * dt
            pos = next
            t += dt
        }
        return hits
    }

    private fun pull(toBody: Vec3, mass: Double, radius: Double): Vec3 {
        val r = max(toBody.length(), radius)
        return toBody.normalized() * (Units.G * mass / (r * r))
    }

    private fun hoursText(s: Double) = if (s >= 86400) "%.0f day%s".format(s / 86400, if (s >= 2 * 86400) "s" else "") else "%.0f h".format(s / 3600)

    companion object {
        const val STEPS = 288
        const val STEP_S = 10.0
    }
}
