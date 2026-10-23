package neumann.core.sim

import neumann.core.data.SensorConfig
import neumann.core.math.Noise
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.SensorType
import neumann.core.world.Torpedo
import neumann.core.world.Tracked
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Updates every vessel's contacts from its sensors that are on, with light
 * delay. Each contact uses the most accurate detection, with random errors.
 */
class SensorPhase(private val lightDelay: LightDelay) {
    fun run(world: World) {
        val tracked = world.tracked()
        for (v in world.vessels) if (v.alive) sweep(world, v, tracked)
    }

    private fun sweep(world: World, observer: Vessel, tracked: List<Tracked>) {
        val seen = LinkedHashMap<String, Contact>()
        for (t in tracked) {
            if (t.faction == observer.faction) continue
            val obs = lightDelay.observe(observer.position, t, world.time) ?: continue
            val rel = obs.sample.position - observer.position
            val distance = rel.length()
            val best = observer.sensorsOn.map { it.type.sensor!! }
                .filter { detects(it, observer, t, distance, obs.sample.burning) }
                .minByOrNull { uncertainty(it, distance) } ?: continue
            val contact = estimate(world, observer, t, best, obs, rel)
            seen[t.id] = contact
            if (observer.contacts.everSeen.add(t.id) && observer === world.player) announce(world, contact)
        }
        if (observer === world.player) {
            for (id in observer.contacts.current.keys - seen.keys) {
                val lost = tracked.firstOrNull { it.id == id } ?: continue
                world.event(EventKind.INFO, id, "Lost contact: ${lost.name}")
            }
        }
        observer.contacts.current.clear()
        observer.contacts.current.putAll(seen)
    }

    private fun detects(s: SensorConfig, observer: Vessel, t: Tracked, distance: Double, burning: Boolean): Boolean {
        val range = s.rangeAu * Units.AU
        return when (s.type) {
            SensorType.TELESCOPE -> distance <= range * t.signature(burning)
            SensorType.RADAR -> distance <= range * (t.sizeM / RADAR_REFERENCE_SIZE_M).pow(0.25)
            SensorType.LIDAR -> t.id == observer.targetId && observer.contacts.current.containsKey(t.id) && distance <= range
            SensorType.EMISSION_RECEIVER -> t.emitting && distance <= range
        }
    }

    /** Position error size; an emission receiver cannot tell distance at all. */
    private fun uncertainty(s: SensorConfig, distance: Double): Double {
        if (s.type == SensorType.EMISSION_RECEIVER) return distance
        val cross = s.bearingErrorDeg * Units.DEGREE * distance
        val along = s.rangeErrorFraction * distance
        return sqrt(cross * cross + along * along)
    }

    private fun estimate(world: World, observer: Vessel, t: Tracked, s: SensorConfig, obs: LightDelay.Observation, rel: Vec3): Contact {
        val distance = rel.length()
        val los = rel.normalized()
        val bearingOnly = s.type == SensorType.EMISSION_RECEIVER
        // Error across the line of sight from the bearing error, along it from the range error.
        val noise = Noise.vector(world.random, s.bearingErrorDeg * Units.DEGREE * distance)
        val across = noise - los * noise.dot(los)
        val along = if (bearingOnly) 0.0 else Noise.gaussian(world.random) * s.rangeErrorFraction * distance
        val position = obs.sample.position + across + los * along
        val velocity = obs.sample.velocity + Noise.vector(world.random, s.velocityErrorKmS * Units.KM)
        val estRel = position - observer.position
        val estDistance = estRel.length()
        val closing = -estRel.dot(velocity - observer.velocity) / estDistance
        return Contact(t.id, t.name, t.contactKind, t.faction, position, velocity, obs.delaySeconds, estDistance, closing,
            t is Torpedo && t.targetId == observer.id, s.type, uncertainty(s, distance), bearingOnly)
    }

    private fun announce(world: World, c: Contact) {
        if (c.targetsObserver) {
            world.event(EventKind.TORPEDO_INBOUND, c.id, "Torpedo inbound: ${c.name}")
        } else if (c.kind != ContactKind.TORPEDO) {
            val how = if (c.bearingOnly) ", bearing only" else ""
            world.event(EventKind.NEW_CONTACT, c.id, "New contact: ${c.name} (${c.kind.name.lowercase()}, ${sensorName(c.sensor)}$how)")
        }
    }

    companion object {
        /** Radar range is quoted for a target this size. */
        const val RADAR_REFERENCE_SIZE_M = 100.0

        fun sensorName(t: SensorType) = t.name.lowercase().replace('_', ' ')
    }
}
