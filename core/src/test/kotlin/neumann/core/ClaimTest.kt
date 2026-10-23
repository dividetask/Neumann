package neumann.core

import neumann.core.math.Vec3
import neumann.core.sim.DamageResolver
import neumann.core.world.EventKind
import kotlin.test.Test
import kotlin.test.assertTrue

class ClaimTest {
    @Test
    fun `the system is claimed when every enemy is destroyed`() {
        val w = Fixture.world()
        Fixture.place(w, w.player, Fixture.openSpace, Vec3.ZERO)
        DamageResolver().destroy(w, Fixture.dummy(w), "by test")
        val r = Fixture.timeSkip(w).run(w, 3600.0)
        assertTrue(w.claimed)
        assertTrue(r.pausedBy.any { it.kind == EventKind.SYSTEM_CLAIMED })
    }
}
