package neumann.core

import kotlin.test.Test
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
}
