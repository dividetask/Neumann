package neumann.core

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.FactoryPhase
import neumann.core.sim.SimulationStep
import neumann.core.world.PartKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiningTest {
    private fun nearHaven() = Fixture.world().also { w ->
        val haven = w.planets.first { it.name == "Haven" }
        Fixture.place(w, w.player, haven.position + Vec3(100_000 * Units.KM, 0.0, 0.0), haven.velocity)
    }

    @Test
    fun `mining does not start while a known enemy is near`() {
        val w = nearHaven()
        Fixture.place(w, Fixture.dummy(w), w.player.position + Vec3(0.0, 1e6 * Units.KM, 0.0), w.player.velocity)
        SimulationStep(w.data).run(w, 10.0)
        assertTrue(w.player.contacts.current.isNotEmpty())
        assertFalse(w.player.mining)
    }

    @Test
    fun `mining starts once the area is cleared near a planet`() {
        val w = nearHaven()
        SimulationStep(w.data).run(w, 10.0)
        assertTrue(w.player.mining)
        assertTrue(w.player.minerals > 0.0)
    }

    @Test
    fun `the factory repairs damage before making fuel`() {
        val w = Fixture.world()
        val p = w.player
        val hull = p.parts.getValue(PartKind.HULL)
        hull.hitPoints -= 100.0
        p.fuel = 100.0
        p.minerals = 1000.0
        FactoryPhase().run(w, 3600.0)
        val f = p.shipClass.factory!!
        assertEquals(hull.maxHitPoints - 100.0 + f.mineralsPerHour * f.repairPointsPerMineral, hull.hitPoints, 1e-6)
        assertEquals(100.0, p.fuel, 1e-9)
    }
}
