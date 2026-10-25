package neumann.core.world

import neumann.core.math.Vec3

/**
 * What one vessel's sensors show of another object, as of its light delay.
 * Position and velocity are estimates, with [uncertaintyM] the size of the
 * position error. A [bearingOnly] contact's distance is not known.
 */
class Contact(
    val id: String,
    val name: String,
    val kind: ContactKind,
    val faction: Faction,
    val observedPosition: Vec3,
    val observedVelocity: Vec3,
    val lightDelaySeconds: Double,
    val distance: Double,
    val closingSpeed: Double,
    val targetsObserver: Boolean,
    val sensor: SensorType,
    val uncertaintyM: Double,
    val bearingOnly: Boolean,
) {
    /** Where it is now if it kept its observed velocity. */
    val predictedPosition: Vec3 get() = observedPosition + observedVelocity * lightDelaySeconds
}

/** Where an object was at [time], and its velocity then: the last time its position was known. */
class Fix(val position: Vec3, val velocity: Vec3, val time: Double) {
    /** Dead reckoning: where it would be at [now] had it kept that velocity (straight line). */
    fun positionAt(now: Double): Vec3 = position + velocity * (now - time)
}

/**
 * A vessel's current contacts, every id it has ever seen, when each current
 * track began, and the last position fix of each (kept after the position
 * is lost, for dead reckoning).
 */
class ContactBook {
    val current = LinkedHashMap<String, Contact>()
    val everSeen = HashSet<String>()
    val trackedSince = HashMap<String, Double>()
    val lastFix = HashMap<String, Fix>()

    operator fun get(id: String?): Contact? = id?.let { current[it] }

    /**
     * Where to show [c] at [now]: its estimate, or for a bearing-only contact
     * its dead-reckoned position; null if it has never had a position fix.
     */
    fun placeOf(c: Contact, now: Double): Vec3? =
        if (!c.bearingOnly) c.observedPosition else lastFix[c.id]?.positionAt(now)
}
