package neumann.core

import neumann.core.game.OrderDesk
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.EventKind
import neumann.core.world.HeadingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BurnTest {
    private val perf = Fixture.performance

    @Test
    fun `a retrograde burn slows the ship by thrust over mass and uses fuel at the engines' rate`() {
        val w = Fixture.world()
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3(0.0, 10_000.0, 0.0), forward = Vec3(0.0, -1.0, 0.0))
        val fuel = p.fuel
        val accel = perf.accelerationMS2(p)
        assertNull(OrderDesk(w).burn(HeadingMode.RETROGRADE, Vec3.ZERO, 1.0, 100.0))
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertEquals(3600.0, r.elapsedSeconds, 1e-6, "the end of a burn does not pause")
        assertTrue(w.log.events.any { it.kind == EventKind.BURN_COMPLETE })
        assertEquals(10_000.0 - accel * 100, p.velocity.length(), 25.0)
        assertEquals(fuel - perf.fuelKgS(p) * 100 / 1000, p.fuel, 1e-6)
    }

    @Test
    fun `a ship turns at its turn rate before it thrusts`() {
        val w = Fixture.world()
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3(0.0, 10_000.0, 0.0), forward = Vec3(0.0, 1.0, 0.0))
        OrderDesk(w).burn(HeadingMode.RETROGRADE, Vec3.ZERO, 1.0, 100.0)
        val turnTime = Math.PI / perf.turnRate(p)
        val step = SimulationStep(w.data)
        val dt = 0.5
        while (w.time + dt < turnTime * 0.9) step.run(w, dt)
        assertEquals(10_000.0, p.velocity.length(), 1.0, "no thrust while still turning")
        repeat((turnTime * 0.2 / dt).toInt() + 20) { step.run(w, dt) }
        assertTrue(p.velocity.length() < 9_990.0, "thrusting once facing the burn direction")
    }

    @Test
    fun `a burn ends when the fuel runs out`() {
        val w = Fixture.world()
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3(0.0, 10_000.0, 0.0), forward = Vec3(0.0, -1.0, 0.0))
        p.fuel = 0.001
        val accel = perf.accelerationMS2(p)
        OrderDesk(w).burn(HeadingMode.RETROGRADE, Vec3.ZERO, 1.0, 1000.0)
        Fixture.runFor(w, 3600.0)
        assertEquals(0.0, p.fuel, 1e-12)
        assertNull(p.burn)
        assertEquals(10_000.0 - accel * (1.0 / perf.fuelKgS(p)), p.velocity.length(), 5.0)
        assertTrue(w.log.events.any { it.message.contains("out of fuel") })
    }

    @Test
    fun `a ship gets lighter as it burns fuel, so it accelerates harder`() {
        val w = Fixture.world()
        val full = perf.accelerationMS2(w.player)
        w.player.fuel = 0.0
        assertTrue(perf.accelerationMS2(w.player) > full)
    }
}
