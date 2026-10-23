package neumann.core

import kotlin.test.Test
import kotlin.test.assertTrue

class DataTest {
    @Test
    fun `data files load and every reference resolves`() {
        val d = Fixture.data
        assertTrue(d.ships.isNotEmpty())
        assertTrue(d.scenario.enemies.isNotEmpty())
    }
}
