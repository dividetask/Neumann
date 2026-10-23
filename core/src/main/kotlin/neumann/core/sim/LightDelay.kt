package neumann.core.sim

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.HistorySample
import neumann.core.world.Tracked

/**
 * What an observer sees of an object now: the object as it was when the
 * light now arriving left it. Null if that light has not arrived yet.
 */
class LightDelay {
    class Observation(val sample: HistorySample, val delaySeconds: Double)

    fun observe(observer: Vec3, target: Tracked, now: Double): Observation? {
        val current = HistorySample(now, target.position, target.velocity, target.burning)
        var delay = (target.position - observer).length() / Units.LIGHT_SPEED
        repeat(ITERATIONS) {
            val s = sampleAt(target, now - delay, current) ?: return null
            delay = (s.position - observer).length() / Units.LIGHT_SPEED
        }
        val s = sampleAt(target, now - delay, current) ?: return null
        return Observation(s, delay)
    }

    /** History lookup that also interpolates between the last sample and now. */
    private fun sampleAt(target: Tracked, time: Double, current: HistorySample): HistorySample? {
        if (time >= current.time) return current
        val last = target.history.samples.lastOrNull()
        if (last == null) {
            if (!target.history.existedBefore) return null
            return HistorySample(time, current.position - current.velocity * (current.time - time), current.velocity, current.burning)
        }
        if (time < last.time) return target.history.at(time)
        val f = (time - last.time) / (current.time - last.time)
        return HistorySample(time, last.position + (current.position - last.position) * f,
            last.velocity + (current.velocity - last.velocity) * f, last.burning)
    }

    companion object {
        const val ITERATIONS = 3
    }
}
