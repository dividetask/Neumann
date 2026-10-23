package neumann.core

import neumann.core.game.OrderDesk
import neumann.core.math.Vec3
import neumann.core.world.BurnDirection
import neumann.core.world.EventKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BurnTest {
    @Test
    fun `a retrograde burn slows the ship and uses fuel`() {
        val w = Fixture.world()
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3(0.0, 10_000.0, 0.0))
        val fuel = p.fuel
        assertNull(OrderDesk(w).setBurn(BurnDirection.RETROGRADE, 1.0, 100.0))
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertEquals(EventKind.BURN_COMPLETE, r.pausedBy.single().kind)
        assertEquals(9_500.0, p.velocity.length(), 5.0)
        assertEquals(fuel - 0.5, p.fuel, 1e-6)
    }

    @Test
    fun `a burn ends when the fuel runs out`() {
        val w = Fixture.world()
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3(0.0, 10_000.0, 0.0))
        p.fuel = 0.1
        OrderDesk(w).setBurn(BurnDirection.RETROGRADE, 1.0, 1000.0)
        Fixture.runFor(w, 3600.0)
        assertEquals(0.0, p.fuel, 1e-9)
        assertNull(p.burn)
        assertEquals(9_900.0, p.velocity.length(), 5.0)
        assertTrue(w.log.events.any { it.message.contains("out of fuel") })
    }
}
