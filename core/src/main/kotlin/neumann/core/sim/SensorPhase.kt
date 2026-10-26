package neumann.core.sim

import neumann.core.data.SensorConfig
import neumann.core.data.ShipRules
import neumann.core.world.Slug
import neumann.core.math.Noise
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.Fix
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
class SensorPhase(private val lightDelay: LightDelay, private val rules: ShipRules) {
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
            val tracked = world.time - (observer.contacts.trackedSince[t.id] ?: world.time)
            val best = observer.sensorsOn.map { it.type.sensor!! }
                .filter { detects(it, observer, t, distance, obs.sample.burning) }
                .minByOrNull { uncertainty(it, distance, tracked) } ?: continue
            val contact = estimate(world, observer, t, best, obs, rel, tracked)
            seen[t.id] = contact
            if (observer.contacts.everSeen.add(t.id) && observer === world.player) announce(world, contact)
        }
        if (observer === world.player) {
            for (id in observer.contacts.current.keys - seen.keys) {
                val lost = tracked.firstOrNull { it.id == id } ?: continue
                if (lost.contactKind.isVessel) world.event(EventKind.INFO, id, "Lost contact: ${lost.name}")
            }
        }
        val since = observer.contacts.trackedSince
        since.keys.retainAll(seen.keys)
        for (id in seen.keys) since.putIfAbsent(id, world.time)
        val wasSeen = observer.contacts.current.keys.toSet()
        observer.contacts.current.clear()
        observer.contacts.current.putAll(seen)
        val fixes = observer.contacts.lastFix
        for (c in seen.values) {
            if (!c.bearingOnly && c.kind != ContactKind.SLUG) fixes[c.id] = Fix(c, world.time - c.lightDelaySeconds)
        }
        // Seen right up until it no longer exists: destroyed, so no ghost. (interpretation)
        val exists = tracked.mapTo(HashSet()) { it.id }
        fixes.keys.removeAll { it in wasSeen && it !in seen && it !in exists }
    }

    private fun detects(s: SensorConfig, observer: Vessel, t: Tracked, distance: Double, burning: Boolean): Boolean {
        val range = s.rangeAu * Units.AU
        return when (s.type) {
            SensorType.TELESCOPE -> distance <= range * t.signature(burning)
            // Radar range goes as cross-section^(1/4), and cross-section as size^2.
            SensorType.RADAR -> distance <= range * sqrt(t.sizeM / RADAR_REFERENCE_SIZE_M)
            SensorType.LIDAR -> t.id == observer.targetId && observer.contacts.current.containsKey(t.id) && distance <= range
            SensorType.EMISSION_RECEIVER -> t.emitting && distance <= range
        }
    }

    /** Position error size; an emission receiver cannot tell distance at all. */
    private fun uncertainty(s: SensorConfig, distance: Double, tracked: Double): Double {
        if (s.type == SensorType.EMISSION_RECEIVER) return distance
        val cross = s.bearingErrorDeg * Units.DEGREE * distance
        val along = rangeError(s, tracked) * distance
        return sqrt(cross * cross + along * along)
    }

    /** Passive tracking: errors shrink as (1 + t/τ)^-1.5 with tracking time, down to the sensor's floor. */
    private fun improvement(s: SensorConfig, tracked: Double): Double =
        if (s.trackTimeConstantSeconds <= 0.0) 1.0 else (1.0 + tracked / s.trackTimeConstantSeconds).pow(-1.5)

    private fun rangeError(s: SensorConfig, tracked: Double) =
        maxOf(s.rangeErrorFraction * improvement(s, tracked), minOf(s.minRangeErrorFraction, s.rangeErrorFraction))

    private fun velocityError(s: SensorConfig, tracked: Double) =
        maxOf(s.velocityErrorKmS * improvement(s, tracked), minOf(s.minVelocityErrorKmS, s.velocityErrorKmS))

    private fun estimate(world: World, observer: Vessel, t: Tracked, s: SensorConfig, obs: LightDelay.Observation, rel: Vec3, tracked: Double): Contact {
        val distance = rel.length()
        val los = rel.normalized()
        val bearingOnly = s.type == SensorType.EMISSION_RECEIVER
        // Error across the line of sight from the bearing error, along it from the range error.
        val noise = Noise.vector(world.random, s.bearingErrorDeg * Units.DEGREE * distance)
        val across = noise - los * noise.dot(los)
        val along = if (bearingOnly) 0.0 else Noise.gaussian(world.random) * rangeError(s, tracked) * distance
        val position = obs.sample.position + across + los * along
        val velocity = obs.sample.velocity + Noise.vector(world.random, velocityError(s, tracked) * Units.KM)
        val estRel = position - observer.position
        val estDistance = estRel.length()
        val closing = -estRel.dot(velocity - observer.velocity) / estDistance
        val aimedAtObserver = when (t) {
            is Torpedo -> t.targetId == observer.id
            is Slug -> passesWithin(position - observer.position, velocity - observer.velocity, rules.slugAlertKm * Units.KM,
                uncertainty(s, distance, tracked), velocityError(s, tracked) * Units.KM)
            else -> false
        }
        return Contact(t.id, t.name, t.contactKind, t.faction, position, velocity, obs.delaySeconds, estDistance, closing,
            aimedAtObserver, s.type, uncertainty(s, distance, tracked), velocityError(s, tracked) * Units.KM, bearingOnly)
    }

    /**
     * Whether something at relative position [r], moving at [u], could pass
     * within [radius] of the observer, allowing three times the estimate's
     * position and velocity errors.
     */
    private fun passesWithin(r: Vec3, u: Vec3, radius: Double, positionError: Double, velocityError: Double): Boolean {
        val uu = u.lengthSquared()
        if (uu == 0.0) return false
        val tMin = -r.dot(u) / uu
        return tMin > 0.0 && (r + u * tMin).length() <= radius + 3 * (positionError + velocityError * tMin)
    }

    private fun announce(world: World, c: Contact) {
        if (c.kind == ContactKind.SLUG) {
            if (c.targetsObserver) world.event(EventKind.SLUG_INBOUND, c.id, "Slug inbound: ${c.name}")
        } else if (c.targetsObserver) {
            world.event(EventKind.TORPEDO_INBOUND, c.id, "Torpedo inbound: ${c.name}")
        } else if (c.kind.isVessel) {
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
