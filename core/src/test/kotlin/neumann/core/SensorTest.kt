package neumann.core

import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.game.Coaster
import neumann.core.game.BogeyEstimator
import neumann.core.game.KnownFixtures
import neumann.core.game.RemainingCount
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

    @Test
    fun `a ship that drops to bearing only keeps its last position fix`() {
        val w = setup(10_000.0)
        val d = Fixture.dummy(w)
        sweep(w)
        assertTrue(!w.player.contacts[d.id]!!.bearingOnly)
        val fix = assertNotNull(w.player.contacts.lastFix[d.id])

        // Far beyond telescope and radar, its radar still on: only its bearing is known now.
        Fixture.place(w, d, Fixture.openSpace + Vec3(100_000_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        d.sensorRules.radarActive = true
        sweep(w)
        assertTrue(w.player.contacts[d.id]!!.bearingOnly)
        assertTrue(w.player.contacts.lastFix[d.id] === fix, "a bearing alone does not replace the fix")
    }
}

class DeadReckoningTest {
    @Test
    fun `a coasting ship's estimate now follows gravity, not a straight line`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        // Close to the star, where its pull bends a path quickly.
        val at = Vec3(0.3 * Units.AU, 0.0, 0.0)
        Fixture.place(w, d, at, Vec3(0.0, 40_000.0, 0.0))
        val coaster = Coaster(w.star, w.planets)
        val start = w.time
        val step = neumann.core.sim.SimulationStep(w.data)
        while (w.time < start + 86_400.0) step.run(w, 60.0)
        val estimate = coaster.coast(at, Vec3(0.0, 40_000.0, 0.0), start, w.time).position
        val straight = at + Vec3(0.0, 40_000.0, 0.0) * (w.time - start)
        val miss = (estimate - d.position).length()
        assertTrue(miss * 50 < (straight - d.position).length(), "gravity makes the estimate far closer than a straight line")
    }

    @Test
    fun `every seen ship is shown where it was seen and where it probably is, within a sphere`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(1_000_000.0 * Units.KM, 0.0, 0.0), Vec3(0.0, 20_000.0, 0.0))
        w.player.sensorRules.radarActive = true
        w.player.sensorsOn = neumann.core.sim.PowerPhase.wanted(w.player)
        neumann.core.sim.SimulationStep(w.data).sensors.run(w)
        val estimator = BogeyEstimator(w)
        val e = estimator.estimate().single { it.contact.id == d.id }
        assertTrue(w.time - e.seen.time > 3.0, "seen as its light left it, over 3 s ago")
        assertTrue((e.now!! - d.position).length() < 3 * e.radiusM, "the estimate now is near where it really is")
        assertTrue((e.now!! - e.seen.position).length() > 50.0 * Units.KM, "and apart from where it was seen")
        assertTrue(e.radiusM >= e.contact.uncertaintyM)
    }

    /** A dummy seen by radar 1,000,000 km off, moving at 20 km/s. */
    private fun seenDummy(): neumann.core.world.World {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(1_000_000.0 * Units.KM, 0.0, 0.0), Vec3(0.0, 20_000.0, 0.0))
        w.player.sensorRules.radarActive = true
        w.player.sensorsOn = neumann.core.sim.PowerPhase.wanted(w.player)
        neumann.core.sim.SimulationStep(w.data).sensors.run(w)
        return w
    }

    @Test
    fun `a lost bogey is still estimated, its sphere growing, until the fix is too old, then a ghost, then nothing`() {
        val w = seenDummy()
        val d = Fixture.dummy(w)
        val first = BogeyEstimator(w).estimate().single()
        // Out of sight: the player goes dark and the dummy is cold and silent.
        w.player.sensorRules.radarActive = false
        w.player.sensorsOn = neumann.core.sim.PowerPhase.wanted(w.player)
        d.sensorRules.radarActive = false
        d.sensorRules.lidarActive = false
        d.sensorsOn = neumann.core.sim.PowerPhase.wanted(d)
        d.emitting = false
        val maxAge = w.data.rules.map.estimateMaxAgeSeconds
        w.time += maxAge / 2
        neumann.core.sim.SimulationStep(w.data).sensors.run(w)
        val lost = BogeyEstimator(w).estimate().single()
        assertTrue(!lost.tracked && !lost.ghost, "lost, but still estimated")
        val since = w.time - lost.seen.time
        val drift = lost.contact.velocityUncertaintyMS * since
        assertEquals(Math.sqrt(lost.contact.uncertaintyM.let { it * it } + drift * drift), lost.radiusM, 1.0,
            "the sphere grows with the velocity error times the time unseen")
        assertTrue(lost.radiusM > first.radiusM)
        w.time += maxAge
        val old = BogeyEstimator(w).estimate().single()
        assertTrue(old.ghost, "too old: a ghost where it was seen")
        assertEquals(first.seen.position, old.seen.position)
        w.time = first.seen.time + w.data.rules.map.ghostMaxAgeSeconds + 1.0
        assertTrue(BogeyEstimator(w).estimate().isEmpty(), "the ghost disappears")
        assertEquals(1, RemainingCount(w).count().ships, "but it still counts as remaining")
    }

    @Test
    fun `a ship seen being destroyed leaves no ghost`() {
        val w = seenDummy()
        Fixture.dummy(w).alive = false
        neumann.core.sim.SimulationStep(w.data).sensors.run(w)
        assertTrue(BogeyEstimator(w).estimate().isEmpty())
    }
}

class RemainingTest {
    @Test
    fun `remaining counts the enemy ships, defences, and bases seen and not destroyed, but not torpedoes`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(10_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        val t = Torpedo("T-x", "Torpedo T-x", d.id, d.faction, w.player.id, w.data.munitions.torpedo, d.position, Vec3.ZERO, w.time)
        w.torpedoes.add(t)
        val at = d.position + Vec3(1_000.0 * Units.KM, 0.0, 0.0)
        Fixture.place(w, t, at, Vec3.ZERO)
        t.history.samples.add(0, neumann.core.world.HistorySample(w.time - 60.0, at, Vec3.ZERO, true)) // in flight already
        val count = RemainingCount(w)
        assertEquals(0, count.count().total, "nothing seen yet")
        w.player.sensorRules.radarActive = true
        w.player.sensorsOn = neumann.core.sim.PowerPhase.wanted(w.player)
        SimulationStep(w.data).sensors.run(w)
        assertNotNull(w.player.contacts[t.id], "the torpedo is seen")
        assertEquals(1, count.count().total, "only the ship counts")
        neumann.core.sim.DamageResolver().destroy(w, d, "by test")
        assertEquals(0, count.count().total, "reported destroyed")
    }

    @Test
    fun `a fixed defence or base, once detected, is shown where it is now even when no longer seen`() {
        val w = neumann.core.game.ScenarioBuilder(Fixture.data).build(neumann.core.world.ApproachVector(null, 15.0))
        val base = w.vessels.first { it.contactKind == neumann.core.world.ContactKind.BASE }
        val fixtures = KnownFixtures(w)
        assertTrue(fixtures.list().isEmpty(), "not yet detected")
        w.player.contacts.everSeen[base.id] = base.contactKind
        assertTrue(w.player.contacts[base.id] == null, "not seen now")
        assertEquals(listOf(base), fixtures.list())
    }
}
