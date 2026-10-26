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
    private val sentient = Fixture.data.modules.getValue("sentientCore").computer!!
    private val daemon = Fixture.data.modules.getValue("daemonCore").computer!!

    /** A well-tracked target closing at [closingKmS]. */
    private fun contact(closingKmS: Double, velocityErrorMS: Double = 0.0) = Contact("x", "x", ContactKind.SHIP, Faction.PLAYER,
        Vec3.ZERO, Vec3.ZERO, 0.0, 1e8, closingKmS * Units.KM, false, SensorType.RADAR, 1.0, velocityErrorMS, false)

    @Test
    fun `point defence estimates match a measured salvo from a standing start`() {
        // Measured by simulation: salvos launched together from 100,000 km at a ship standing still.
        assertEquals(8, planner.shotDown(probe, contact(0.0), sentient), "the probe's two turrets stop 8")
        assertEquals(3, planner.shotDown(raider, contact(0.0), sentient), "a raider's one light laser stops 3")
    }

    @Test
    fun `point defence stops fewer torpedoes the faster they close`() {
        assertTrue(planner.shotDown(probe, contact(4.0), sentient) < planner.shotDown(probe, contact(0.0), sentient))
    }

    @Test
    fun `a salvo that cannot get through is not fired, and one that can is sized to destroy or fires everything`() {
        val c = contact(0.0)
        val k = planner.shotDown(raider, c, sentient)
        val h = planner.hitsToDestroy(raider, sentient)
        assertEquals(0, planner.size(k, raider, c, sentient), "all would be shot down")
        assertEquals(k + 1, planner.size(k + 1, raider, c, sentient), "too few to destroy: fire them all, one should hit")
        assertEquals(k + h, planner.size(k + h + 5, raider, c, sentient), "enough to destroy it, and no more")
    }

    @Test
    fun `a daemon core assumes every target is as tough as the probe`() {
        assertEquals(planner.hitsToDestroy(probe, daemon), planner.hitsToDestroy(raider, daemon))
        assertTrue(planner.hitsToDestroy(raider, sentient) < planner.hitsToDestroy(raider, daemon), "the sentient core knows a raider is weaker")
    }

    @Test
    fun `a daemon counts every ally's torpedoes, wherever they are, and launches its own once the total is enough`() {
        val w = neumann.core.game.ScenarioBuilder(Fixture.data).build(neumann.core.world.ApproachVector(null, 15.0))
        val raiders = w.vessels.filter { it.design.name == "Raider" }
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        val near = raiders.first()
        Fixture.place(w, near, Fixture.openSpace + Vec3(50_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO, forward = Vec3(-1.0, 0.0, 0.0))
        for (r in raiders) { r.ambush = false; r.targetId = w.player.id }
        val step = SimulationStep(w.data)
        step.sensors.run(w)
        val c = near.contacts[w.player.id]!!
        val needed = planner.shotDown(probe, c, daemon) + planner.hitsToDestroy(probe, daemon)
        // The far raiders hold most of the torpedoes; the near one only its own two.
        for (r in raiders) r.working(ModuleKind.MISSILE_BAY).first().torpedoes = if (r === near) 2 else needed
        step.run(w, 1.0)
        assertEquals(2, w.torpedoes.count { it.ownerId == near.id }, "allies out of range count, so the near raider fires its share")

        // A far ally keeps its promise once in range, though its torpedoes alone would not get through.
        val far = raiders.first { it.salvoShare != null }
        val promised = far.salvoShare!!.torpedoes
        far.working(ModuleKind.MISSILE_BAY).first().torpedoes = 2
        far.salvoShare!!.torpedoes = 2
        Fixture.place(w, far, Fixture.openSpace + Vec3(60_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO, forward = Vec3(-1.0, 0.0, 0.0))
        step.sensors.run(w)
        step.run(w, 1.0)
        assertTrue(promised > 0)
        assertEquals(2, w.torpedoes.count { it.ownerId == far.id }, "promised share launched")
        assertTrue(far.salvoShare == null)
    }

    @Test
    fun `a salvo needs at least one hit and is never bigger than needed to destroy`() {
        val c = contact(0.0)
        val k = planner.shotDown(raider, c, daemon)
        val h = planner.hitsToDestroy(raider, daemon)
        assertEquals(planner.shotDown(probe, c, sentient), k, "a daemon assumes a raider is as strong as the probe")
        assertEquals(0, planner.size(k, raider, c, daemon), "none would get through: hold")
        assertEquals(k + 1, planner.size(k + 1, raider, c, daemon), "one should hit: fire them all")
        assertEquals(k + h, planner.size(k + h + 3, raider, c, daemon), "more than enough: only enough to destroy")
    }

    @Test
    fun `a poor track makes point defence look stronger, so the shooter waits for a better one`() {
        assertTrue(planner.shotDown(probe, contact(4.0, velocityErrorMS = 1_000.0), sentient) > planner.shotDown(probe, contact(4.0), sentient))
    }

    @Test
    fun `a lone raider holds its torpedoes against the probe until enough to get through`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(50_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO, forward = Vec3(-1.0, 0.0, 0.0))
        val step = SimulationStep(w.data)
        step.sensors.run(w)
        d.targetId = w.player.id
        step.run(w, 1.0)
        assertTrue(w.torpedoes.isEmpty(), "two torpedoes cannot beat the probe's point defence")

        // More torpedoes than needed to destroy the probe through its point defence.
        val daemon = Fixture.data.modules.getValue("daemonCore").computer!!
        val c = d.contacts[w.player.id]!!
        val needed = planner.shotDown(probe, c, daemon) + planner.hitsToDestroy(probe, daemon)
        d.working(ModuleKind.MISSILE_BAY).first().torpedoes = needed + 3
        step.run(w, 1.0)
        assertEquals(needed, w.torpedoes.size, "a salvo big enough to destroy, launched together, and no more")
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

class ComputerTest {
    private fun hitComputer(w: neumann.core.world.World, v: neumann.core.world.Vessel) {
        v.computer!!.hitPoints = 0.0
        // Any further hit notices it: here, a tiny one.
        neumann.core.sim.DamageResolver().hit(w, v, 0.001, w.player.id, "laser")
    }

    @Test
    fun `a ship whose computer is destroyed is dead - it drifts and does nothing - and counts as destroyed`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(10_000.0 * Units.KM, 0.0, 0.0), Vec3(0.0, 1_000.0, 0.0))
        val step = SimulationStep(w.data)
        step.sensors.run(w)
        assertEquals(1, neumann.core.game.RemainingCount(w).count().ships)
        // A fresh hit that lands on the computer: simulate by destroying it, then any hit.
        d.computer!!.hitPoints = 0.0
        d.computer // gone
        assertTrue(!d.active && d.alive, "dead, but still there")
        val before = d.position
        step.run(w, 10.0)
        assertTrue(d.contacts.current.isEmpty(), "its sensors are off")
        assertTrue((d.position - before).length() > 5_000.0, "it drifts on")
        assertTrue(w.claimed, "a dead enemy is as good as destroyed")
    }

    @Test
    fun `the computer dying from a hit is reported, and no longer remaining`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(10_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        SimulationStep(w.data).sensors.run(w)
        val core = d.computer!!
        core.hitPoints = 0.0001
        // Hits land by mass at random; push hits until one finishes the computer.
        var tries = 0
        while (core.hitPoints > 0.0 && tries++ < 10_000) neumann.core.sim.DamageResolver().hit(w, d, 0.01, w.player.id, "laser")
        assertTrue(w.log.events.any { it.kind == EventKind.DESTROYED && it.message.contains("computer destroyed") })
        assertEquals(0, neumann.core.game.RemainingCount(w).count().total)
    }

    @Test
    fun `losing the sentient core loses the game`() {
        val w = Fixture.world()
        w.player.computer!!.hitPoints = 0.0
        SimulationStep(w.data).run(w, 1.0)
        assertTrue(w.lost)
    }

    @Test
    fun `a daemon core forgets a contact it has not seen for a day`() {
        val w = Fixture.world()
        val d = Fixture.dummy(w)
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        Fixture.place(w, d, Fixture.openSpace + Vec3(10_000.0 * Units.KM, 0.0, 0.0), Vec3.ZERO)
        val step = SimulationStep(w.data)
        step.sensors.run(w)
        assertTrue(w.player.id in d.contacts.everSeen)
        // The player moves far away, out of every sensor's reach.
        Fixture.place(w, w.player, Fixture.openSpace + Vec3(0.0, 0.0, 3.0 * Units.AU), Vec3.ZERO)
        w.player.sensorRules.radarActive = false
        w.time += 3600.0
        step.sensors.run(w)
        assertTrue(w.player.id in d.contacts.everSeen, "an hour: still remembered")
        w.time += Fixture.data.modules.getValue("daemonCore").computer!!.forgetAfterSeconds
        step.sensors.run(w)
        assertTrue(w.player.id !in d.contacts.everSeen, "a day: forgotten")
        assertTrue(d.id in w.player.contacts.everSeen, "the sentient core never forgets")
    }
}
