package neumann.core.game

import neumann.core.data.GameData
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.ShipPerformance
import neumann.core.world.World
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The default entry (see docs/game-design.md#space): level with the planets,
 * at the default speed and the most economical drive setting, aimed so the
 * predicted path (entry burn included) passes the star at about half the
 * outermost planet's orbit and never within `planetClearanceKm` of any
 * planet or moon; if no aim does, the one passing furthest from them.
 */
class DefaultEntry(private val data: GameData) {
    private val performance = ShipPerformance(data.munitions)

    /** The entry velocity (m/s, relative to the star) for an arrived [world]. */
    fun velocity(world: World): Vec3 {
        val p = world.player
        val speed = data.generation.entry.defaultEntryKmS * Units.KM
        val inward = Vec3(-p.position.x, -p.position.y, 0.0).normalized()
        val side = Vec3(-inward.y, inward.x, 0.0)
        val goal = world.planets.filter { !it.isMoon }.maxOf { it.orbitRadius } / 2
        val clearance = data.generation.entry.planetClearanceKm * Units.KM
        val predictor = TrajectoryPredictor(world.star, world.planets)
        fun aim(angle: Double) = (inward * cos(angle) + side * sin(angle)).normalized()
        fun predict(angle: Double): TrajectoryPredictor.Prediction {
            val target = aim(angle) * speed
            val plan = TrajectoryPredictor.BurnPlan(target, performance.thrustN(p, 0.0), performance.massKg(p), performance.fuelKgS(p, 0.0))
            val gap = (target - p.velocity).length()
            val span = gap / performance.accelerationMS2(p, 0.0) + 2 * p.position.length() / speed
            return predictor.predict(p.position, p.velocity, span, span / STEPS, burn = plan)
        }
        // Each side of the star: the angle whose path passes it at the goal distance (bisection: further off, wider pass).
        val candidates = listOf(1.0, -1.0).map { sign ->
            var lo = 0.0
            var hi = MAX_ANGLE
            repeat(ITERATIONS) {
                val mid = (lo + hi) / 2
                if (predict(sign * mid).nearestStar < goal) lo = mid else hi = mid
            }
            sign * (lo + hi) / 2
        }
        // Nudge each until it clears every planet and moon; prefer the one that does, nearest the goal.
        val options = candidates.flatMap { a -> NUDGES.map { a + it } }.map { it to predict(it) }
        val clear = options.filter { it.second.nearestBody >= clearance && !it.second.hits }
        val best = clear.minByOrNull { abs(it.second.nearestStar - goal) } ?: options.maxBy { it.second.nearestBody }
        return aim(best.first) * speed
    }

    companion object {
        const val STEPS = 2000
        const val ITERATIONS = 14
        const val MAX_ANGLE = Math.PI / 2
        /** Small turns tried around each aim to clear the planets, in radians. */
        val NUDGES = listOf(0.0, 0.01, -0.01, 0.02, -0.02, 0.04, -0.04)
    }
}
