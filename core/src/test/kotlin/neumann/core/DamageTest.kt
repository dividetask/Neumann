package neumann.core

import neumann.core.data.ShipDesign
import neumann.core.math.Vec3
import neumann.core.sim.DamageResolver
import neumann.core.world.Faction
import neumann.core.world.ModuleKind
import neumann.core.world.ModuleState
import neumann.core.world.Vessel
import neumann.core.world.VesselRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageTest {
    private val modules = Fixture.data.modules

    /** A vessel of the given modules; [weightless] modules are never hit directly. */
    private fun vessel(ids: List<String>, weightless: Set<String> = emptySet()): Vessel {
        val states = ids.map { id ->
            val t = modules.getValue(id)
            ModuleState(id, if (id in weightless) t.copy(massT = 0.0) else t)
        }
        val design = ShipDesign("Test", VesselRole.SHIP, 10.0, 1.0, 1.0, emptyMap())
        return Vessel("X", "Test", Faction.PLAYER, design, states, Vec3.ZERO, Vec3.ZERO, Vec3(1.0, 0.0, 0.0), Vec3(0.0, 0.0, 1.0), null)
    }

    @Test
    fun `a destroyed fuel tank loses its fuel and extra damage goes to the hull`() {
        val w = Fixture.world()
        val v = vessel(listOf("frigateHull", "fuelTank"), weightless = setOf("frigateHull"))
        v.fuel = 20.0
        val tank = v.modules.first { it.kind == ModuleKind.FUEL_TANK }
        DamageResolver().hit(w, v, tank.type.hitPoints + 30.0, "E-0", "test")
        assertEquals(0.0, v.fuel, 1e-9)
        val hull = v.modules.first { it.kind == ModuleKind.HULL }
        assertEquals(hull.type.hitPoints - 30.0, hull.hitPoints, 1e-9)
    }

    @Test
    fun `a destroyed fuel tank loses only its share of the fuel`() {
        val w = Fixture.world()
        val v = vessel(listOf("frigateHull", "fuelTank", "fuelTank"), weightless = setOf("frigateHull"))
        v.fuel = 40.0
        DamageResolver().hit(w, v, modules.getValue("fuelTank").hitPoints, "E-0", "test")
        assertEquals(20.0, v.fuel, 1e-9)
    }

    @Test
    fun `a destroyed missile bay loses its torpedoes`() {
        val w = Fixture.world()
        val v = vessel(listOf("frigateHull", "missileBay"), weightless = setOf("frigateHull"))
        v.modules[1].torpedoes = 6
        DamageResolver().hit(w, v, modules.getValue("missileBay").hitPoints, "E-0", "test")
        assertEquals(0, v.torpedoes)
    }

    @Test
    fun `a ship is destroyed only when every hull module is destroyed`() {
        val w = Fixture.world()
        val v = vessel(listOf("frigateHull", "frigateHull"))
        val hp = modules.getValue("frigateHull").hitPoints
        DamageResolver().hit(w, v, hp * 1.5, "E-0", "test")
        assertTrue(v.alive)
        DamageResolver().hit(w, v, hp, "E-0", "test")
        assertFalse(v.alive)
    }
}
