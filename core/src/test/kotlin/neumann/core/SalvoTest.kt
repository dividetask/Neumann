package neumann.core

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SalvoPlanner
import neumann.core.sim.SimulationStep
import neumann.core.world.Contact
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.Faction
import neumann.core.world.ModuleKind
import neumann.core.world.SensorType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalvoTest {
    private val planner = SalvoPlanner(Fixture.data)
    private val probe = Fixture.data.designs.getValue("probe")
    private val raider = Fixture.data.designs.getValue("raider")

    /** A well-tracked target closing at [closingKmS]. */
    private fun contact(closingKmS: Double, velocityErrorMS: Double = 0.0) = Contact("x", "x", ContactKind.SHIP, Faction.PLAYER,
        Vec3.ZERO, Vec3.ZERO, 0.0, 1e8, closingKmS * Units.KM, false, SensorType.RADAR, 1.0, velocityErrorMS, false)

    @Test
    fun `point defence estimates match a measured salvo from a standing start`() {
        // Measured by simulation: salvos launched together from 100,000 km at a ship standing still.
        assertEquals(8, planner.shotDown(probe, contact(0.0)), "the probe's two turrets stop 8")
        assertEquals(3, planner.shotDown(raider, contact(0.0)), "a raider's one light laser stops 3")
    }

    @Test
    fun `point defence stops fewer torpedoes the faster they close`() {
        assertTrue(planner.shotDown(probe, contact(4.0)) < planner.shotDown(probe, contact(0.0)))
    }

    @Test
    fun `a salvo that cannot get through is not fired, and one that can is sized to destroy or fires everything`() {
        val c = contact(0.0)
        val k = planner.shotDown(raider, c)
        val h = planner.hitsToDestroy(raider)
        assertEquals(0, planner.size(k, raider, c), "all would be shot down")
        assertEquals(k + 1, planner.size(k + 1, raider, c), "too few to destroy: fire them all, one should hit")
        assertEquals(k + h, planner.size(k + h + 5, raider, c), "enough to destroy it, and no more")
    }

    @Test
    fun `a poor track makes point defence look stronger, so the shooter waits for a better one`() {
        assertTrue(planner.shotDown(probe, contact(4.0, velocityErrorMS = 1_000.0)) > planner.shotDown(probe, contact(4.0)))
    }

    @Test
    fun `a lone raider holds its torpedoes against the probe, but raiders together fire as one salvo`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(50_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO, forward = Vec3(-1.0, 0.0, 0.0))
        val step = SimulationStep(w.data)
        step.sensors.run(w)
        d.targetId = w.player.id
        step.run(w, 1.0)
        assertTrue(w.torpedoes.isEmpty(), "two torpedoes cannot beat the probe's point defence")

        // Five raiders' worth of torpedoes in the dummy's bays (10 > the 8 the probe stops).
        d.working(ModuleKind.MISSILE_BAY).first().torpedoes = 10
        step.run(w, 1.0)
        assertTrue(w.torpedoes.size >= 9, "enough to get through, launched together")
        assertTrue(w.torpedoes.all { it.launchTime == w.torpedoes.first().launchTime })
    }
}

class FuelWarningTest {
    @Test
    fun `fuel running low from burns is logged without pausing, but losing a fuel tank pauses`() {
        val w = Fixture.world()
        val p = w.player
        val watch = neumann.core.sim.SupplyWatch(w.data.rules.autoPause, SimulationStep(w.data).performance)
        watch.run(w)
        p.fuel = 1.0
        watch.run(w)
        assertTrue(w.log.events.any { it.kind == EventKind.INFO && it.message == "Fuel low" })
        assertTrue(w.log.events.none { it.kind == EventKind.LOW_FUEL })

        val w2 = Fixture.world()
        val watch2 = neumann.core.sim.SupplyWatch(w2.data.rules.autoPause, SimulationStep(w2.data).performance)
        watch2.run(w2)
        w2.player.working(ModuleKind.FUEL_TANK).first().hitPoints = 0.0 // one tank hit, its fuel lost with it
        w2.player.fuel = 1.0
        watch2.run(w2)
        assertTrue(w2.log.events.any { it.kind == EventKind.LOW_FUEL }, "a lost tank is unexpected: it pauses")
    }
}
