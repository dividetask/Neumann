package neumann.core.sim

import neumann.core.world.Contact
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.Torpedo
import neumann.core.world.Tracked
import neumann.core.world.Vessel
import neumann.core.world.World

/** Updates every vessel's contacts, with light delay. */
class SensorPhase(private val lightDelay: LightDelay, private val performance: ShipPerformance) {
    fun run(world: World) {
        val tracked = world.tracked()
        for (v in world.vessels) if (v.alive) sweep(world, v, tracked)
    }

    private fun sweep(world: World, observer: Vessel, tracked: List<Tracked>) {
        val range = performance.sensorRange(observer)
        val seen = LinkedHashMap<String, Contact>()
        for (t in tracked) {
            if (t.faction == observer.faction) continue
            val obs = lightDelay.observe(observer.position, t, world.time) ?: continue
            val rel = obs.sample.position - observer.position
            val distance = rel.length()
            if (distance > range * t.signature(obs.sample.burning)) continue
            val relV = obs.sample.velocity - observer.velocity
            val targetsObserver = t is Torpedo && t.targetId == observer.id
            val contact = Contact(t.id, t.name, t.contactKind, t.faction, obs.sample.position, obs.sample.velocity,
                obs.delaySeconds, distance, -rel.dot(relV) / distance, targetsObserver)
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

    private fun announce(world: World, c: Contact) {
        if (c.targetsObserver) {
            world.event(EventKind.TORPEDO_INBOUND, c.id, "Torpedo inbound: ${c.name}")
        } else if (c.kind != ContactKind.TORPEDO) {
            world.event(EventKind.NEW_CONTACT, c.id, "New contact: ${c.name} (${c.kind.name.lowercase()})")
        }
    }
}
