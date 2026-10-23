package neumann.core

import neumann.core.world.ModuleKind
import kotlin.test.Test
import kotlin.test.assertTrue

/** Balance rules from docs/game-design.md. These guard the numbers in data/. */
class BalanceTest {
    @Test
    fun `the probe beats four raiders at once without significant strategy, at medium difficulty`() {
        val s = FourRaiderFight().run()
        val w = s.world
        val p = w.player
        val hull = p.modules.filter { it.kind == ModuleKind.HULL }
        val hullLeft = hull.sumOf { it.hitPoints } / hull.sumOf { it.type.hitPoints }
        val integrity = p.modules.sumOf { it.hitPoints } / p.modules.sumOf { it.type.hitPoints }
        val byWeapon = w.damage.by(p.id)
        val total = byWeapon.values.sum()
        println("balance: claimed=${w.claimed} after ${w.time / 3600} h, integrity ${(integrity * 100).toInt()}%, " +
            "hull left ${(hullLeft * 100).toInt()}%, damage taken ${Fixture.damageTaken(p).toInt()}, " +
            "torpedoes left ${p.torpedoes}, slugs left ${p.slugs}")
        println("balance: probe damage by weapon " + byWeapon.mapValues { "${(it.value / total * 100).toInt()}%" })
        assertTrue(w.claimed, "the probe should destroy all four raiders")
        assertTrue(integrity in 0.4..0.8, "medium difficulty: 40-80% integrity left, was ${(integrity * 100).toInt()}%")
        for (weapon in listOf("laser", "cannon", "torpedo")) {
            val share = (byWeapon[weapon] ?: 0.0) / total
            assertTrue(share >= 0.1, "all three weapons matter: $weapon dealt ${(share * 100).toInt()}% of the probe's damage")
        }
    }
}
