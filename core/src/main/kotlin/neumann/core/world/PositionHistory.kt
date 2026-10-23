package neumann.core.world

import neumann.core.math.Vec3

class HistorySample(val time: Double, val position: Vec3, val velocity: Vec3, val burning: Boolean)

/**
 * Past positions of one object, for light delay.
 * [existedBefore]: the object was already there before the first sample
 * (placed by the scenario), so earlier times are extrapolated backwards.
 */
class PositionHistory(val existedBefore: Boolean) {
    val samples = ArrayList<HistorySample>()

    fun record(sample: HistorySample) {
        samples.add(sample)
    }

    fun prune(before: Double) {
        var drop = 0
        while (drop + 1 < samples.size && samples[drop + 1].time <= before) drop++
        if (drop > 0) samples.subList(0, drop).clear()
    }

    /** The state at [time], or null if the object did not exist yet. */
    fun at(time: Double): HistorySample? {
        if (samples.isEmpty()) return null
        val first = samples.first()
        if (time < first.time) {
            if (!existedBefore) return null
            return HistorySample(time, first.position - first.velocity * (first.time - time), first.velocity, first.burning)
        }
        val last = samples.last()
        if (time >= last.time) return last
        var lo = 0
        var hi = samples.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (samples[mid].time <= time) lo = mid else hi = mid
        }
        val a = samples[lo]
        val b = samples[hi]
        val f = (time - a.time) / (b.time - a.time)
        return HistorySample(time, a.position + (b.position - a.position) * f, a.velocity + (b.velocity - a.velocity) * f, a.burning)
    }
}
