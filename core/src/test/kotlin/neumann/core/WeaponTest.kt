package neumann.core

import neumann.core.game.OrderDesk
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.Torpedo
import neumann.core.world.Vessel
import neumann.core.world.WeaponKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeaponTest {
    private fun damageTaken(v: Vessel) = v.parts.values.sumOf { it.maxHitPoints - it.hitPoints }

    /** Player in open space with the dummy at [offsetKm], both known to each other. */
    private fun duel(offsetKm: Double, dummyVelocity: Vec3 = Vec3.ZERO) = Fixture.world().also { w ->
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, Fixture.dummy(w), Fixture.openSpace + Vec3(offsetKm * Units.KM, 0.0, 0.0), dummyVelocity)
        SimulationStep(w.data).sensors.run(w)
    }

    @Test
    fun `a torpedo hits a target that does not manoeuvre`() {
        val w = duel(50_000.0)
        val d = Fixture.dummy(w)
        d.rules.laserPointDefence = false
        val orders = OrderDesk(w)
        assertNull(orders.setTarget(d.id))
        assertNull(orders.fireNow(WeaponKind.TORPEDO))
        Fixture.runFor(w, 3600.0)
        assertEquals(d.shipClass.torpedo!!.damage, damageTaken(d), 1e-6)
        assertTrue(w.torpedoes.isEmpty())
    }

    @Test
    fun `laser point defence shoots down an incoming torpedo`() {
        val w = duel(50_000.0)
        val d = Fixture.dummy(w)
        val start = d.position
        val t = Torpedo("T-x", "Torpedo T-x", d.id, d.faction, w.player.id, d.shipClass.torpedo!!, start, Vec3.ZERO, w.time)
        w.torpedoes.add(t)
        Fixture.place(w, t, start, Vec3.ZERO)
        Fixture.runFor(w, 3600.0)
        assertFalse(t.alive)
        assertEquals(0.0, damageTaken(w.player), 1e-6)
        assertTrue(w.log.events.any { it.message.contains("shot down") })
    }

    @Test
    fun `a cannon slug hits a target moving at constant velocity`() {
        val w = duel(2_000.0, Vec3(0.0, 3_000.0, 0.0))
        val d = Fixture.dummy(w)
        val orders = OrderDesk(w)
        orders.setTarget(d.id)
        assertNull(orders.fireNow(WeaponKind.CANNON))
        Fixture.runFor(w, 600.0)
        assertEquals(d.shipClass.cannon!!.damage, damageTaken(d), 1e-6)
    }
}
