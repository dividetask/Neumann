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
