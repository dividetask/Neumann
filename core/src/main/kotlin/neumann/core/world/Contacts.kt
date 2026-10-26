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
    val velocityUncertaintyMS: Double,
    val bearingOnly: Boolean,
) {
    /** Where it is now if it kept its observed velocity. */
    val predictedPosition: Vec3 get() = observedPosition + observedVelocity * lightDelaySeconds
}

/** The last time an object's position was known: what was seen, as of [time] (when that light left it). */
class Fix(val contact: Contact, val time: Double) {
    val position: Vec3 get() = contact.observedPosition
    val velocity: Vec3 get() = contact.observedVelocity
}

/**
 * A vessel's current contacts, every id it has ever seen (with its kind and
 * name), which of those it knows were destroyed, when each current track
 * began, and the last position fix of each bogey (kept after the position
 * or the contact is lost, for dead reckoning).
 */
class ContactBook {
    val current = LinkedHashMap<String, Contact>()
    val everSeen = LinkedHashMap<String, ContactKind>()
    val destroyed = HashSet<String>()
    val trackedSince = HashMap<String, Double>()
    val lastFix = HashMap<String, Fix>()
    /** When each object was last detected at all. */
    val lastSeen = HashMap<String, Double>()

    operator fun get(id: String?): Contact? = id?.let { current[it] }

    /** Forget [id] entirely, as if it had never been seen. */
    fun forget(id: String) {
        everSeen.remove(id)
        lastFix.remove(id)
        lastSeen.remove(id)
    }

    /** Known destroyed: no longer remaining, and no ghost. */
    fun markDestroyed(id: String) {
        destroyed.add(id)
        lastFix.remove(id)
    }
}
