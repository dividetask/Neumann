package neumann.core

import neumann.core.data.PartConfig
import neumann.core.math.Vec3
import neumann.core.sim.DamageResolver
import neumann.core.world.Faction
import neumann.core.world.PartKind
import neumann.core.world.Vessel
import kotlin.test.Test
import kotlin.test.assertEquals

class DamageTest {
    @Test
    fun `a destroyed fuel tank loses all fuel and extra damage goes to the hull`() {
        val w = Fixture.world()
        val probe = Fixture.data.ships.getValue("probe")
        val onlyTank = probe.copy(parts = listOf(PartConfig(PartKind.HULL, 1000.0, 0.0), PartConfig(PartKind.FUEL_TANK, 10.0, 1.0)))
        val v = Vessel("X", "Test", Faction.PLAYER, onlyTank, Vec3.ZERO, Vec3.ZERO, null)
        DamageResolver().hit(w, v, 30.0, "E-0", "test")
        assertEquals(0.0, v.fuel)
        assertEquals(980.0, v.parts.getValue(PartKind.HULL).hitPoints, 1e-9)
    }
}
