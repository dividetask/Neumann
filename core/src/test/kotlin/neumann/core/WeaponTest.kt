package neumann.core

import neumann.core.Fixture.damageTaken
import neumann.core.game.OrderDesk
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.LaserFalloff
import neumann.core.sim.SimulationStep
import neumann.core.world.ModuleKind
import neumann.core.world.SensorType
import neumann.core.world.Torpedo
import neumann.core.world.WeaponKind
import neumann.core.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeaponTest {
    private val munitions = Fixture.data.munitions
    private val toDummy = Vec3(1.0, 0.0, 0.0)

    /** Player in open space facing [facing], the dummy at [offsetKm] along +x, both known to each other. */
    private fun duel(offsetKm: Double, dummyVelocity: Vec3 = Vec3.ZERO, facing: Vec3 = toDummy) = Fixture.world().also { w ->
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO, forward = facing)
        Fixture.place(w, Fixture.dummy(w), Fixture.openSpace + Vec3(offsetKm * Units.KM, 0.0, 0.0), dummyVelocity, forward = -toDummy)
        SimulationStep(w.data).sensors.run(w)
    }

    /** Target the dummy and let the lidar lock on (it needs a known contact first). */
    private fun lockOn(w: World): OrderDesk {
        val orders = OrderDesk(w)
        assertNull(orders.setTarget(Fixture.dummy(w).id))
        SimulationStep(w.data).sensors.run(w)
        return orders
    }

    private fun incomingTorpedo(w: World): Torpedo {
        val d = Fixture.dummy(w)
        val t = Torpedo("T-x", "Torpedo T-x", d.id, d.faction, w.player.id, munitions.torpedo, d.position, Vec3.ZERO, w.time)
        w.torpedoes.add(t)
        Fixture.place(w, t, d.position, Vec3.ZERO)
        return t
    }

    @Test
    fun `a torpedo hits a target that does not manoeuvre`() {
        val w = duel(50_000.0)
        val d = Fixture.dummy(w)
        d.rules.laserPointDefence = false
        val orders = OrderDesk(w)
        assertNull(orders.setTarget(d.id))
        assertNull(orders.fireNow(WeaponKind.TORPEDO))
        Fixture.runFor(w, 3 * 3600.0) // a torpedo takes over an hour to cross 50,000 km
        assertEquals(munitions.torpedo.damage, damageTaken(d), 1e-6)
        assertTrue(w.torpedoes.isEmpty())
    }

    @Test
    fun `laser point defence shoots down an incoming torpedo`() {
        val w = duel(50_000.0)
        val t = incomingTorpedo(w)
        Fixture.runFor(w, 3 * 3600.0) // a torpedo takes over an hour to cross 50,000 km
        assertFalse(t.alive)
        assertEquals(0.0, damageTaken(w.player), 1e-6)
        assertTrue(w.log.events.any { it.message.contains("shot down") })
    }

    @Test
    fun `a laser with no stored power does not fire`() {
        val w = duel(50_000.0)
        val p = w.player
        p.working(ModuleKind.GENERATOR).forEach { it.hitPoints = 0.0 }
        p.energy = 0.0
        incomingTorpedo(w)
        Fixture.runFor(w, 3 * 3600.0) // a torpedo takes over an hour to cross 50,000 km
        assertEquals(munitions.torpedo.damage, damageTaken(p) - p.modules.filter { it.kind == ModuleKind.GENERATOR }.sumOf { it.type.hitPoints }, 1e-6)
    }

    @Test
    fun `a cannon slug hits a target moving at constant velocity`() {
        val w = duel(2_000.0, Vec3(0.0, 3_000.0, 0.0))
        val d = Fixture.dummy(w)
        val orders = lockOn(w)
        assertEquals(SensorType.LIDAR, w.player.contacts[d.id]!!.sensor)
        assertNull(orders.fireNow(WeaponKind.CANNON))
        Fixture.runFor(w, 600.0)
        // Damage is the slug's kinetic energy relative to the target (MJ): muzzle 20 km/s, target crossing at 3 km/s.
        val ke = { v: Double -> 0.5 * munitions.slug.massKg * v * v / 1e6 }
        val muzzle = w.player.working(ModuleKind.CANNON).first().type.cannon!!.muzzleSpeedKmS * 1000
        assertTrue(damageTaken(d) in ke(muzzle - 3_000.0)..ke(muzzle + 3_000.0), "one hit of ${damageTaken(d)} MJ")
    }

    @Test
    fun `a laser fires continuously while it has a target, and draws no power without one`() {
        val w = duel(2_000.0)
        val p = w.player
        lockOn(w)
        p.rules.laserAutoFire = true
        val d = Fixture.dummy(w)
        val step = SimulationStep(w.data)
        val before = damageTaken(d)
        repeat(60) { step.run(w, 1.0) }
        val laser = p.working(ModuleKind.LASER_TURRET).map { it.type.laser!! }
        val expected = laser.sumOf { LaserFalloff.damagePerSecond(it, 2_000_000.0) } * 60
        assertEquals(expected, damageTaken(d) - before, expected * 0.05, "a full minute of fire from every turret")
        p.rules.laserAutoFire = false
        val energy = p.energy
        repeat(60) { step.run(w, 1.0) }
        assertTrue(p.energy >= energy, "no target, no laser draw: the batteries recharge")
    }

    @Test
    fun `laser damage is full out to the focus range, then falls with the square of distance`() {
        val laser = Fixture.data.modules.getValue("laserTurret").laser!!
        val focus = laser.focusRangeKm * Units.KM
        assertEquals(laser.beamMW, LaserFalloff.damagePerSecond(laser, focus * 0.5), 1e-9)
        assertEquals(laser.beamMW / 4, LaserFalloff.damagePerSecond(laser, focus * 2), 1e-9)
        assertEquals(0.0, LaserFalloff.damagePerSecond(laser, laser.rangeKm * Units.KM * 1.01), 1e-9)
    }

    @Test
    fun `a cannon does not fire at a target outside its arc`() {
        val w = duel(2_000.0, facing = -toDummy)
        val d = Fixture.dummy(w)
        val slugs = w.player.slugs
        val orders = OrderDesk(w)
        orders.setTarget(d.id)
        orders.fireNow(WeaponKind.CANNON)
        Fixture.runFor(w, 600.0)
        assertEquals(slugs, w.player.slugs)
        assertEquals(0.0, damageTaken(d), 1e-6)
    }
}
