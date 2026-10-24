package neumann.core

import neumann.core.data.EnemyPlacement
import neumann.core.game.OrderDesk
import neumann.core.game.ScenarioBuilder
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.DamageResolver
import neumann.core.sim.ImpactPhase
import neumann.core.sim.JinkPhase
import neumann.core.sim.MotionPhase
import neumann.core.sim.GravityField
import neumann.core.sim.PowerPhase
import neumann.core.sim.SimulationStep
import neumann.core.world.ApproachVector
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.Faction
import neumann.core.world.HistorySample
import neumann.core.world.SensorType
import neumann.core.world.Slug
import neumann.core.world.WeaponKind
import neumann.core.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StealthTest {
    private val east = Vec3(1.0, 0.0, 0.0)

    /** Player in open space facing the dummy [km] away to the east; player's radar and lidar as given. */
    private fun setup(km: Double, active: Boolean): World = Fixture.world().also { w ->
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO, forward = east)
        Fixture.place(w, Fixture.dummy(w), Fixture.openSpace + east * (km * Units.KM), Vec3.ZERO, forward = -east)
        w.player.sensorRules.radarActive = active
        w.player.sensorRules.lidarActive = active
        w.player.sensorsOn = PowerPhase.wanted(w.player)
        SimulationStep(w.data).sensors.run(w)
    }

    @Test
    fun `a passive telescope track gets more accurate the longer it is held`() {
        val w = setup(200_000.0, active = false)
        val d = Fixture.dummy(w)
        val first = w.player.contacts[d.id]!!
        assertEquals(SensorType.TELESCOPE, first.sensor)
        Fixture.runFor(w, 3600.0)
        assertTrue(w.player.contacts[d.id]!!.uncertaintyM * 50 < first.uncertaintyM)
    }

    @Test
    fun `cannons can hit from a long passive track without ever emitting`() {
        val w = setup(2_000.0, active = false)
        val d = Fixture.dummy(w)
        val orders = OrderDesk(w)
        orders.setTarget(d.id)
        val step = SimulationStep(w.data)
        var emitted = false
        while (w.time < 3600.0) { step.run(w, 10.0); emitted = emitted || w.player.emitting }
        assertEquals(SensorType.TELESCOPE, w.player.contacts[d.id]!!.sensor)
        orders.fireNow(WeaponKind.CANNON)
        while (w.time < 3600.0 + 600.0) { step.run(w, 1.0); emitted = emitted || w.player.emitting }
        assertTrue(emitted.not(), "never emitted")
        assertTrue((w.damage.by(w.player.id)["cannon"] ?: 0.0) > 0.0, "the slug hit")
    }

    @Test
    fun `slugs are invisible to telescopes, but radar sees one close in and warns that it is inbound`() {
        val w = setup(1_000_000.0, active = false)
        val d = Fixture.dummy(w)
        val at = w.player.position + east * (20_000.0 * Units.KM)
        val slug = Slug("S-x", d.id, Faction.ENEMY, at, -east * 20_000.0, 2.0, 0.1, w.time)
        slug.history.record(HistorySample(w.time - 10.0, at - slug.velocity * 10.0, slug.velocity, false)) // in flight already
        w.slugs.add(slug)
        SimulationStep(w.data).sensors.run(w)
        assertNull(w.player.contacts[slug.id], "telescope cannot see a cold slug")
        w.player.sensorRules.radarActive = true
        w.player.sensorsOn = PowerPhase.wanted(w.player)
        SimulationStep(w.data).sensors.run(w)
        val c = assertNotNull(w.player.contacts[slug.id])
        assertEquals(ContactKind.SLUG, c.kind)
        assertTrue(w.log.events.any { it.kind == EventKind.SLUG_INBOUND })
    }

    /** One slug fired from 10,000 km at a stationary-aimed target, which jinks or not; returns cannon damage dealt. */
    private fun slugAtTarget(jink: Boolean): Double {
        // The target is a probe (it has thrusters to jink with), placed by hand; no AI step runs here.
        val target = EnemyPlacement("Target", "probe", orbits = "Shale", offsetKm = listOf(0.0, 200000.0, 0.0), ambush = true)
        val data = Fixture.data.copy(scenario = Fixture.data.scenario.copy(enemies = listOf(target)))
        val w = ScenarioBuilder(data).build(ApproachVector(null, 15.0))
        val t = w.vessels.first { it.name == "Target" }
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, t, Fixture.openSpace + east * (10_000.0 * Units.KM), Vec3.ZERO)
        t.evasive = jink
        w.slugs.add(Slug("S-x", w.player.id, Faction.PLAYER, w.player.position, east * 20_000.0, 2.0, 0.1, w.time))
        val perf = SimulationStep(data).performance
        val jinks = JinkPhase(perf, data.rules.ships)
        val motion = MotionPhase(GravityField(), data.rules.sim)
        val impacts = ImpactPhase(DamageResolver())
        repeat(700) {
            jinks.run(w, 1.0)
            motion.run(w, 1.0)
            w.time += 1.0
            impacts.run(w)
        }
        return w.damage.by(w.player.id)["cannon"] ?: 0.0
    }

    @Test
    fun `jinking spoils a long-range cannon shot`() {
        assertTrue(slugAtTarget(jink = false) > 0.0, "a steady target is hit")
        assertEquals(0.0, slugAtTarget(jink = true), "a jinking target is missed")
    }
}
