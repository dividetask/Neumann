package neumann.core

import neumann.core.game.OrderDesk
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.BurnStage
import neumann.core.world.EventKind
import neumann.core.world.HeadingMode
import neumann.core.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BurnTest {
    private val perf = Fixture.performance
    // North: at right angles to the star's pull on the test spot, so gravity does not add speed along the burn.
    private val north = Vec3(0.0, 0.0, 1.0)

    /** Player at rest in open space, facing north. */
    private fun still(): World = Fixture.world().also { Fixture.place(it, it.player, Fixture.openSpace, Vec3.ZERO, forward = north) }

    /** Speed and distance travelled northward, from the start. */
    private fun along(w: World) = Pair(w.player.velocity.dot(north), (w.player.position - Fixture.openSpace).dot(north))

    @Test
    fun `a desired speed burns until that speed, then coasts`() {
        val w = still()
        val p = w.player
        assertNull(OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, 0.2, null))
        Fixture.runFor(w, 3600.0)
        assertNull(p.burn)
        assertEquals(200.0, along(w).first, 2.0)
        val fuel = p.fuel
        Fixture.runFor(w, 3600.0)
        assertEquals(fuel, p.fuel, 1e-9, "coasting uses no fuel")
        assertTrue(w.log.events.any { it.kind == EventKind.BURN_COMPLETE && it.message.contains("reached") })
    }

    @Test
    fun `a desired distance speeds up to halfway, then brakes and stops there`() {
        val w = still()
        val p = w.player
        OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, null, 1_000.0)
        val step = SimulationStep(w.data)
        var braked = false
        while (p.burn != null && w.time < 86400.0) {
            if (p.burn?.stage == BurnStage.BRAKE) braked = true
            step.run(w, 1.0)
        }
        assertTrue(braked)
        val (speed, distance) = along(w)
        assertEquals(0.0, speed, 1.0, "stopped")
        assertEquals(1_000_000.0, distance, 60_000.0, "near the desired distance")
    }

    @Test
    fun `speed and distance together accelerate to the speed, coast, then stop at the distance`() {
        val w = still()
        val p = w.player
        OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, 0.2, 5_000.0)
        val step = SimulationStep(w.data)
        var coasted = false
        var top = 0.0
        while (p.burn != null && w.time < 5 * 86400.0) {
            if (p.burn?.stage == BurnStage.COAST) coasted = true
            top = maxOf(top, along(w).first)
            step.run(w, 2.0)
        }
        assertTrue(coasted)
        assertEquals(200.0, top, 5.0, "never faster than the desired speed")
        val (speed, distance) = along(w)
        assertEquals(0.0, speed, 1.0)
        assertEquals(5_000_000.0, distance, 150_000.0)
    }

    @Test
    fun `braking turns the ship around before it thrusts`() {
        val w = still()
        val p = w.player
        OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, null, 1_000.0)
        val step = SimulationStep(w.data)
        while (p.burn?.stage != BurnStage.BRAKE) step.run(w, 1.0)
        repeat(60) { step.run(w, 1.0) }
        assertTrue(p.forward.dot(north) < 0.0, "facing back the way it came")
    }

    @Test
    fun `a burn ends when the fuel runs out`() {
        val w = still()
        val p = w.player
        p.fuel = 0.001
        val accel = perf.accelerationMS2(p)
        OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, 100.0, null)
        Fixture.runFor(w, 3600.0)
        assertEquals(0.0, p.fuel, 1e-12)
        assertNull(p.burn)
        assertEquals(accel * (1.0 / perf.fuelKgS(p)), along(w).first, 5.0)
        assertTrue(w.log.events.any { it.message.contains("out of fuel") })
    }

    @Test
    fun `a burn needs a speed or a distance`() {
        val w = still()
        assertNotNull(OrderDesk(w).burn(HeadingMode.FIXED, north, 1.0, null, null))
    }

    @Test
    fun `a ship turns at its turn rate before it thrusts`() {
        val w = still()
        val p = w.player
        OrderDesk(w).burn(HeadingMode.FIXED, -north, 1.0, 1.0, null)
        val turnTime = Math.PI / perf.turnRate(p)
        val step = SimulationStep(w.data)
        while (w.time + 0.5 < turnTime * 0.9) step.run(w, 0.5)
        assertEquals(0.0, p.velocity.dot(-north), 1.0, "no thrust while still turning")
        repeat((turnTime * 0.2 / 0.5).toInt() + 20) { step.run(w, 0.5) }
        assertTrue(p.velocity.dot(-north) > 0.5, "thrusting once facing the burn direction")
    }

    @Test
    fun `turning uses a little fuel, in proportion to mass and angle`() {
        val w = still()
        val p = w.player
        val fuel = p.fuel
        val massT = perf.massKg(p) / 1000
        OrderDesk(w).face(HeadingMode.FIXED, Vec3(0.0, 1.0, 0.0))
        Fixture.runFor(w, 600.0)
        val expected = w.data.rules.ships.turnFuelKgPerTonneDeg * massT * 90.0 / 1000
        assertEquals(expected, fuel - p.fuel, expected * 0.01)
    }

    @Test
    fun `a ship gets lighter as it burns fuel, so it accelerates harder`() {
        val w = Fixture.world()
        val full = perf.accelerationMS2(w.player)
        w.player.fuel = 0.0
        assertTrue(perf.accelerationMS2(w.player) > full)
    }
}
