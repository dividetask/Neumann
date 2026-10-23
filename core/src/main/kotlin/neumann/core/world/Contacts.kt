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

/** A vessel's current contacts, every id it has ever seen, and when each current track began. */
class ContactBook {
    val current = LinkedHashMap<String, Contact>()
    val everSeen = HashSet<String>()
    val trackedSince = HashMap<String, Double>()

    operator fun get(id: String?): Contact? = id?.let { current[it] }
}
