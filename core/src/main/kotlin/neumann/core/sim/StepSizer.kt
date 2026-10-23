package neumann.core.sim

import neumann.core.data.SimConfig
import neumann.core.math.Vec3
import neumann.core.world.World
import kotlin.math.max
import kotlin.math.min

/** Chooses the next step length: short when a projectile is near a target. */
class StepSizer(private val config: SimConfig) {
    fun next(world: World, remaining: Double): Double {
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
        }
        val dt = (soonest / config.fineStepDivisions).coerceIn(config.fineStepSeconds, config.baseStepSeconds)
        return min(dt, remaining)
    }

    private fun timeToReach(p: Vec3, v: Vec3, targetP: Vec3, targetV: Vec3): Double =
        (targetP - p).length() / max((v - targetV).length(), 1.0)
}
