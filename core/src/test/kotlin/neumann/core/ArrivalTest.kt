package neumann.core

import neumann.core.game.ArrivalPlanner
import neumann.core.game.GameSession
import neumann.core.game.ScenarioBuilder
import neumann.core.game.SystemGenerator
import neumann.core.math.Units
import neumann.core.sim.ShipPerformance
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrivalTest {
    private val data = Fixture.data
    private val perf = ShipPerformance(data.munitions)
    private val scenario = SystemGenerator(data).generate(42)

    @Test
    fun `the probe arrives heading for the star at cruise speed, with enough fuel to stop and keep a quarter tank`() {
        val w = ScenarioBuilder(data).arrive(scenario)
        val p = w.player
        val a = w.arrival!!
        assertEquals(scenario.originLy, a.originLy)
        assertTrue(a.originLy in 3.0..15.0)
        assertEquals(a.speedMS, p.velocity.length(), 1e-6)
        assertTrue(p.velocity.normalized().dot(-p.position.normalized()) > 0.999, "heading for the star")
        // Braking to a stop at the economical end of the drive dial leaves a quarter of the tanks.
        val massAfterStop = perf.massKg(p) * exp(-a.speedMS / perf.exhaustMS(p, 0.0))
        val left = p.fuel - (perf.massKg(p) - massAfterStop) / 1000.0
        assertEquals(0.25 * perf.fuelCapacity(p), left, 0.01 * perf.fuelCapacity(p))
        assertTrue(a.tripYears > 1000.0, "thousands of years between stars")
    }

    @Test
    fun `a probe that must start braking before the game begins arrives slower`() {
        val p = ScenarioBuilder(data).arrive(scenario).player
        val planner = ArrivalPlanner(data)
        p.fuel = perf.fuelCapacity(p)
        val far = planner.plan(p, 5.0, 1e15)
        val near = planner.plan(p, 5.0, 1.0 * Units.AU)
        assertEquals(far.cruiseMS, far.speedMS, 1e-6, "plenty of room: at cruise speed")
        assertTrue(near.speedMS < far.speedMS / 2, "1 AU to stop in: much slower")
        assertTrue(near.fuelT < far.fuelT, "braking already spent fuel")
        assertEquals(far.cruiseMS, near.cruiseMS, 1e-6, "the star's distance does not change the cruise speed")
    }

    @Test
    fun `entering runs a matching burn to the chosen velocity, and the skip stops when it is done`() {
        val world = ScenarioBuilder(data).arrive(scenario)
        val inward = world.player.velocity.normalized()
        val wanted = inward * 40_000.0
        val session = GameSession.arriving(data, scenario, wanted, 0.0)
        var skips = 0
        while (session.world.player.burn != null && skips++ < 50) session.skipUntilBurnEnds()
        val p = session.world.player
        assertNull(p.burn, "the entry burn finished")
        // Gravity keeps pulling after the burn ends, so allow a little.
        assertEquals(40_000.0, p.velocity.length(), 500.0, "at the chosen entry speed")
        assertTrue(p.fuel >= 0.25 * perf.fuelCapacity(p), "a quarter tank kept")
    }
}
