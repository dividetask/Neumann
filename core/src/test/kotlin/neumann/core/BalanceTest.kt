package neumann.core

import neumann.core.data.EnemyPlacement
import neumann.core.game.GameSession
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.ApproachVector
import neumann.core.world.ContactKind
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import kotlin.test.Test
import kotlin.test.assertTrue

/** Balance rules from docs/game-design.md. These guard the numbers in data/. */
class BalanceTest {
    @Test
    fun `the probe beats four raiders at once without significant strategy, at medium difficulty`() {
        val raiders = (1..4).map { EnemyPlacement("Raider $it", "raider", orbits = "Shale", offsetKm = listOf(0.0, 200000.0 + it, 0.0)) }
        val data = Fixture.data.copy(scenario = Fixture.data.scenario.copy(enemies = raiders))
        val s = GameSession(data, ApproachVector(0.0, 0.0, 50.0))
        val w = s.world
        val p = w.player
        // Four raiders together, 300,000 km away, closing; the probe sits still.
        Fixture.place(w, p, Fixture.openSpace, Vec3.ZERO)
        w.vessels.filter { it !== p }.forEachIndexed { i, r ->
            val at = Fixture.openSpace + Vec3(300_000.0, (i - 1.5) * 20_000.0, 0.0) * Units.KM
            Fixture.place(w, r, at, Vec3(-5_000.0, 0.0, 0.0), forward = Vec3(-1.0, 0.0, 0.0))
        }
        w.player.contacts.current.clear() // forget the scan from before the raiders were placed
        w.player.contacts.everSeen.clear()
        neumann.core.sim.SimulationStep(data).sensors.run(w)
        // No strategy: every weapon rule on; target the nearest enemy and face it.
        p.rules.apply { laserAutoFire = true; cannonAutoFire = true; torpedoAutoLaunch = true }
        while (!w.over && w.time < 3 * 86400.0) {
            p.contacts.current.values.filter { it.kind != ContactKind.TORPEDO }.minByOrNull { it.distance }?.let {
                if (p.targetId != it.id) s.orders.setTarget(it.id)
                if (p.heading?.mode != HeadingMode.TOWARD_TARGET) s.orders.face(HeadingMode.TOWARD_TARGET)
            }
            s.skip(3600.0)
        }
        if (System.getenv("BALANCE_LOG") != null) for (e in w.log.events) println("LOG %7.0f %-16s %s".format(e.time, e.kind, e.message))
        for (r in w.vessels) println("VESSEL ${r.name} alive=${r.alive} dmg=${Fixture.damageTaken(r).toInt()} torps=${r.torpedoes} slugs=${r.slugs} energy=${r.energy.toInt()}")
        val hull = p.modules.filter { it.kind == ModuleKind.HULL }
        val hullLeft = hull.sumOf { it.hitPoints } / hull.sumOf { it.type.hitPoints }
        val integrity = p.modules.sumOf { it.hitPoints } / p.modules.sumOf { it.type.hitPoints }
        println("balance: claimed=${w.claimed} after ${w.time / 3600} h, integrity ${(integrity * 100).toInt()}%, hull left ${(hullLeft * 100).toInt()}%, " +
            "damage taken ${Fixture.damageTaken(p).toInt()}, torpedoes left ${p.torpedoes}, slugs left ${p.slugs}")
        val byWeapon = w.damage.by(p.id)
        val total = byWeapon.values.sum()
        println("balance: probe damage by weapon " + byWeapon.mapValues { "${(it.value / total * 100).toInt()}%" })
        assertTrue(w.claimed, "the probe should destroy all four raiders")
        for (weapon in listOf("laser", "cannon", "torpedo")) {
            val share = (byWeapon[weapon] ?: 0.0) / total
            assertTrue(share >= 0.1, "all three weapons matter: $weapon dealt ${(share * 100).toInt()}% of the probe's damage")
        }
        assertTrue(integrity in 0.4..0.8, "medium difficulty: 40-80% integrity left, was ${(integrity * 100).toInt()}%")
    }
}
