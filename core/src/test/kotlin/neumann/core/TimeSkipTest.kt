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
    fun `the same event from the same source does not pause again within the suppression time`() {
        val config = Fixture.data.rules.autoPause
        val m = AutoPauseMonitor(config)
        fun hit(t: Double, source: String) = GameEvent(t, EventKind.DAMAGE_TAKEN, source, "hit")
        assertEquals(1, m.pausing(listOf(hit(0.0, "E-1"))).size)
        assertEquals(0, m.pausing(listOf(hit(100.0, "E-1"))).size)
        assertEquals(1, m.pausing(listOf(hit(100.0, "E-2"))).size)
        assertEquals(1, m.pausing(listOf(hit(config.repeatSuppressionSeconds + 1.0, "E-1"))).size)
    }
}
