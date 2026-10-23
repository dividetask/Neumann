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
        val then = d.history.at(w.time - c.lightDelaySeconds)!!.position
        assertTrue((then - c.observedPosition).length() < 5 * c.uncertaintyM, "the estimate is of where it was")
        assertTrue((d.position - then).length() > 4_000 * Units.KM, "and it has moved since")
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

class SensorTypesTest {
    private fun setup(offsetKm: Double): neumann.core.world.World {
        val w = Fixture.world()
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, Fixture.dummy(w), Fixture.openSpace + Vec3(offsetKm * Units.KM, 0.0, 0.0), Vec3.ZERO)
        return w
    }

    private fun sweep(w: neumann.core.world.World) {
        for (v in w.vessels) v.sensorsOn = neumann.core.sim.PowerPhase.wanted(v)
        for (v in w.vessels) v.emitting = v.sensorsOn.any { it.type.sensor!!.type.active }
        SimulationStep(w.data).sensors.run(w)
    }

    @Test
    fun `radar finds a cold ship that a telescope cannot`() {
        val w = setup(1_000_000.0) // beyond a telescope's reach for a cold raider, inside radar's
        val d = Fixture.dummy(w)
        d.sensorRules.radarActive = false // silent, so the emission receiver cannot find it either
        d.sensorRules.lidarActive = false
        w.player.sensorRules.radarActive = false
        sweep(w)
        assertNull(w.player.contacts[d.id], "telescope alone misses it")
        w.player.sensorRules.radarActive = true
        sweep(w)
        assertEquals(neumann.core.world.SensorType.RADAR, w.player.contacts[d.id]?.sensor)
    }

    @Test
    fun `an emission receiver finds a ship using radar, bearing only, and not one that is silent`() {
        val w = setup(100_000_000.0) // far beyond telescope and radar reach
        val d = Fixture.dummy(w)
        d.sensorRules.radarActive = true
        sweep(w)
        val c = assertNotNull(w.player.contacts[d.id])
        assertTrue(c.bearingOnly)
        d.sensorRules.radarActive = false
        d.sensorRules.lidarActive = false
        sweep(w)
        assertNull(w.player.contacts[d.id])
    }

    @Test
    fun `lidar tracks the target far more accurately than radar`() {
        val w = setup(10_000.0)
        val d = Fixture.dummy(w)
        sweep(w)
        val radar = w.player.contacts[d.id]!!.uncertaintyM
        w.player.targetId = d.id
        sweep(w)
        val c = w.player.contacts[d.id]!!
        assertEquals(neumann.core.world.SensorType.LIDAR, c.sensor)
        assertTrue(c.uncertaintyM * 100 < radar)
    }
}
