package neumann.core

import neumann.core.game.ScenarioBuilder
import neumann.core.game.SystemGenerator
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.Faction
import neumann.core.world.VesselRole
import kotlin.test.Test
import kotlin.test.assertTrue

class PatrolTest {
    private val data = Fixture.data

    @Test
    fun `guarding raiders patrol the base's planet with radar on, and none crashes in a month`() {
        for (seed in 1L..10L) {
            val s = SystemGenerator(data).generate(seed)
            val w = ScenarioBuilder(data).arrive(s) // the probe far out: no raider sees it
            val raiders = w.vessels.filter { it.faction == Faction.ENEMY && it.design.role == VesselRole.SHIP }
            val step = SimulationStep(data)
            repeat(30 * 24 * 3) { step.run(w, 1200.0) }
            assertTrue(raiders.all { it.alive }, "seed $seed: " + w.log.events.filter { it.message.contains("crashing") }.joinToString { it.message })
            assertTrue(raiders.all { it.emitting }, "seed $seed: radar on while patrolling")
            for (r in raiders) {
                val home = w.planets.first { it.name == r.patrol!!.current }
                val d = (r.position - home.position).length() / (r.patrol!!.orbitKm * Units.KM)
                assertTrue(d in 0.5..2.0, "seed $seed: ${r.name} still round the base's planet, near its own patrol distance (x$d)")
            }
        }
    }

    @Test
    fun `a raider far from its patrol target flies there and settles into orbit without crashing`() {
        val s = SystemGenerator(data).generate(3)
        val w = ScenarioBuilder(data).arrive(s)
        val r = w.vessels.first { it.name == "Raider 1" }
        val home = w.planets.first { it.name == r.patrol!!.current }
        // Two million km off, at rest relative to the planet.
        Fixture.place(w, r, home.position + Vec3(2_000_000.0 * Units.KM, 0.0, 0.0), home.velocity)
        val step = SimulationStep(data)
        repeat(20 * 24 * 6) { step.run(w, 600.0) }
        val d = (r.position - home.position).length()
        assertTrue(r.alive, "did not crash")
        assertTrue(d in 50_000.0 * Units.KM..200_000.0 * Units.KM, "in orbit at about the patrol distance: ${d / Units.KM} km")
    }

    @Test
    fun `a raider near its base and moving with it refuels slowly`() {
        val w = ScenarioBuilder(data).arrive(SystemGenerator(data).generate(5))
        val base = w.vessels.first { it.design.role == VesselRole.BASE }
        val r = w.vessels.first { it.name == "Raider 1" }
        r.patrol = null
        Fixture.place(w, r, base.position + Vec3(50_000.0 * Units.KM, 0.0, 0.0), base.velocity)
        r.fuel = 1.0
        SimulationStep(data).run(w, 3600.0)
        assertTrue(r.fuel in 1.9..2.1, "about a tonne an hour: ${r.fuel}")
    }
}
