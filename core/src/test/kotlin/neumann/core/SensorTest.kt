package neumann.core

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.Burn
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.Torpedo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SensorTest {
    @Test
    fun `sensors show a distant contact where it was, not where it is`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        val lightSeconds100 = 100 * Units.LIGHT_SPEED
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(lightSeconds100, 0.0, 0.0), Vec3(0.0, 50_000.0, 0.0))
        // burning, so visible far off
        Fixture.place(w, d, d.position, d.velocity, forward = Vec3(0.0, 1.0, 0.0))
        d.heading = Heading(HeadingMode.FIXED, Vec3(0.0, 1.0, 0.0))
        d.burn = Burn(0.003, 1e9, null, w.time, d.position, d.velocity)
        Fixture.runFor(w, 1000.0)
        val c = assertNotNull(w.player.contacts[d.id])
        assertEquals(100.0, c.lightDelaySeconds, 1.0)
        assertTrue((d.position - c.observedPosition).length() > 4_000 * Units.KM)
        assertTrue((d.position - c.predictedPosition).length() < 100 * Units.KM)
    }

    @Test
    fun `an object is not seen until its light has arrived`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        val far = Fixture.openSpace + Vec3(100 * Units.LIGHT_SPEED, 0.0, 0.0)
        val t = Torpedo("T-x", "Torpedo T-x", d.id, d.faction, w.player.id, w.data.munitions.torpedo, far, Vec3.ZERO, w.time)
        w.torpedoes.add(t)
        Fixture.place(w, t, far, Vec3.ZERO)
        val step = SimulationStep(w.data)
        step.run(w, 10.0)
        assertNull(w.player.contacts[t.id])
        repeat(12) { step.run(w, 10.0) }
        assertNotNull(w.player.contacts[t.id])
    }
}
