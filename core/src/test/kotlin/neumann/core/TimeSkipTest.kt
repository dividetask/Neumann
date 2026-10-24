package neumann.core

import neumann.core.game.AutoPauseMonitor
import neumann.core.game.PauseCollector
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.EventKind
import neumann.core.world.GameEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimeSkipTest {
    @Test
    fun `a time skip runs its full time when nothing happens`() {
        val w = Fixture.world()
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertEquals(3600.0, r.elapsedSeconds, 1e-6)
        assertTrue(r.pausedBy.isEmpty())
    }

    @Test
    fun `a time skip stops early when a new contact is detected`() {
        val w = Fixture.world()
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, Fixture.dummy(w), Fixture.openSpace + Vec3(1e6 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertTrue(r.elapsedSeconds < 3600.0)
        assertEquals(EventKind.NEW_CONTACT, r.pausedBy.single().kind)
    }

    @Test
    fun `a pause waits for everything arriving at nearly the same time, and drops nothing`() {
        val group = Fixture.data.rules.autoPause.groupSeconds
        val c = PauseCollector(group)
        fun inbound(t: Double, torpedo: String) = listOf(GameEvent(t, EventKind.TORPEDO_INBOUND, torpedo, "inbound"))
        assertNull(c.offer(inbound(0.0, "T-1"), 0.0), "the first torpedo opens the window, no pause yet")
        assertNull(c.offer(inbound(20.0, "T-2"), 20.0))
        assertNull(c.offer(inbound(group - 1, "T-3"), group - 1))
        val pause = assertNotNull(c.offer(emptyList(), group), "the pause comes when the window closes")
        assertEquals(listOf("T-1", "T-2", "T-3"), pause.map { it.sourceId }, "every torpedo is in it")
    }

    @Test
    fun `claiming the system or losing the ship pauses at once`() {
        val c = PauseCollector(Fixture.data.rules.autoPause.groupSeconds)
        c.offer(listOf(GameEvent(0.0, EventKind.TORPEDO_INBOUND, "T-1", "inbound")), 0.0)
        val pause = assertNotNull(c.offer(listOf(GameEvent(5.0, EventKind.SHIP_LOST, "P", "lost")), 5.0))
        assertEquals(2, pause.size)
    }

    @Test
    fun `a time skip gathers a torpedo salvo into one pause`() {
        val w = Fixture.world()
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        val d = Fixture.dummy(w)
        Fixture.place(w, d, Fixture.openSpace + Vec3(50_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        neumann.core.sim.SimulationStep(w.data).sensors.run(w)
        // The dummy fires its whole missile bay at the player: one torpedo, then the next 20 s later.
        d.targetId = w.player.id
        val salvo = d.torpedoes
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertTrue(salvo >= 2)
        assertEquals(salvo, r.pausedBy.count { it.kind == EventKind.TORPEDO_INBOUND }, "every torpedo of the salvo in one pause")
    }

    @Test
    fun `continuing damage from the same source is not new and does not pause again`() {
        val gap = Fixture.data.rules.autoPause.repeatSuppressionSeconds
        val m = AutoPauseMonitor(Fixture.data.rules.autoPause)
        fun hit(t: Double, source: String) = m.pausing(listOf(GameEvent(t, EventKind.DAMAGE_TAKEN, source, "hit"))).size
        assertEquals(1, hit(0.0, "E-1"), "first hit pauses")
        assertEquals(1, hit(10.0, "E-2"), "a different enemy's hit is new")
        for (i in 1..30) assertEquals(0, hit(i * gap / 2, "E-1"), "steady hits never pause")
        assertEquals(1, hit(15 * gap + gap, "E-1"), "a hit after a quiet gap is new")
    }
}

class EventLogTest {
    @Test
    fun `repeated damage merges only with the same weapon`() {
        val log = neumann.core.world.EventLog()
        fun hit(t: Double, weapon: String) = log.addMerging(GameEvent(t, EventKind.DAMAGE_DEALT, "P", "Raider hit by $weapon", "E-1", 10.0))
        hit(0.0, "laser"); hit(5.0, "laser"); hit(6.0, "cannon")
        assertEquals(2, log.events.size)
        assertEquals(20.0, log.events[0].amount, 1e-9)
    }
}
