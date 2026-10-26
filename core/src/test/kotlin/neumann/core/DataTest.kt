package neumann.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataTest {
    @Test
    fun `data files load and every reference resolves`() {
        val d = Fixture.data
        assertTrue(d.designs.isNotEmpty() && d.modules.isNotEmpty())
        assertTrue(d.scenario.enemies.isNotEmpty())
    }

    @Test
    fun `ammunition running low is never a reason to pause, since the player sees it coming when firing`() {
        assertTrue(neumann.core.world.EventKind.LOW_AMMO !in Fixture.data.rules.autoPause.pauseOn)
    }

    @Test
    fun `every module's materials add up to its mass, and every design has exactly one computer`() {
        for ((id, m) in Fixture.data.modules) assertTrue(Math.abs(m.materials.values.sum() - m.massT) <= 0.01 * m.massT, id)
        for ((id, d) in Fixture.data.designs) {
            assertTrue(d.modules.keys.count { Fixture.data.modules.getValue(it).kind == neumann.core.world.ModuleKind.COMPUTER } == 1, id)
        }
    }

    @Test
    fun `a daemon assumes every target is exactly as tough as the probe`() {
        val d = Fixture.data
        val probe = d.designs.getValue("probe").modules.entries.sumOf { (id, n) -> d.modules.getValue(id).hitPoints * n }
        assertEquals(probe, d.modules.getValue("daemonCore").computer!!.assumedTargetHitPoints)
    }
}
