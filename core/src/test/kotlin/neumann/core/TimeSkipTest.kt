package neumann.core

import neumann.core.game.AutoPauseMonitor
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.EventKind
import neumann.core.world.GameEvent
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `events of one kind arriving at nearly the same time are one pause`() {
        val cfg = Fixture.data.rules.autoPause
        val m = AutoPauseMonitor(cfg)
        fun inbound(t: Double, torpedo: String) = m.pausing(listOf(GameEvent(t, EventKind.TORPEDO_INBOUND, torpedo, "inbound"))).size
        assertEquals(1, inbound(0.0, "T-1"), "the first torpedo of a salvo pauses")
        assertEquals(0, inbound(20.0, "T-2"), "the rest of the salvo does not")
        assertEquals(0, inbound(cfg.groupSeconds - 1, "T-3"))
        assertEquals(1, inbound(cfg.groupSeconds + 100, "T-4"), "a later salvo is new")
    }

    @Test
    fun `continuing damage from the same source is not new and does not pause again`() {
        val gap = Fixture.data.rules.autoPause.repeatSuppressionSeconds
        val m = AutoPauseMonitor(Fixture.data.rules.autoPause)
        fun hit(t: Double, source: String) = m.pausing(listOf(GameEvent(t, EventKind.DAMAGE_TAKEN, source, "hit"))).size
        assertEquals(1, hit(0.0, "E-1"), "first hit pauses")
        assertEquals(0, hit(10.0, "E-2"), "a different enemy's hit a moment later is the same news")
        assertEquals(1, hit(Fixture.data.rules.autoPause.groupSeconds + 20.0, "E-3"), "a different enemy's hit later on is new")
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
