package neumann.core

import neumann.core.game.DirectionDials
import neumann.core.game.DirectionDials.Angles
import neumann.core.math.Frame
import neumann.core.math.Vec3
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectionDialsTest {
    private val forward = Vec3(0.0, 1.0, 0.0)
    private val up = Vec3(0.0, 0.0, 1.0)
    private val right = forward.cross(up)

    private fun near(a: Vec3, b: Vec3) = assertTrue((a - b).length() < 1e-9, "$a != $b")

    @Test
    fun `yaw turns the front right and pitch turns it up`() {
        near(right, DirectionDials.orientation(forward, up, Angles(90.0, 0.0, 0.0)).forward)
        near(up, DirectionDials.orientation(forward, up, Angles(0.0, 90.0, 0.0)).forward)
        near((forward + up).normalized(), DirectionDials.orientation(forward, up, Angles(0.0, 45.0, 0.0)).forward)
    }

    @Test
    fun `roll right moves the ship's up toward its right`() {
        val o = DirectionDials.orientation(forward, up, Angles(0.0, 0.0, 90.0))
        near(forward, o.forward)
        near(right, o.up)
    }

    @Test
    fun `the turn to go is the dial angles that were set`() {
        val set = Angles(-35.0, 20.0, 60.0)
        val o = DirectionDials.orientation(forward, up, set)
        val left = DirectionDials.remaining(forward, up, o.forward, o.up)
        assertEquals(set.yaw, left.yaw, 1e-6)
        assertEquals(set.pitch, left.pitch, 1e-6)
        assertEquals(set.roll, left.roll, 1e-6)
    }

    @Test
    fun `once the ship faces the heading the turn to go is zero`() {
        val o = DirectionDials.orientation(forward, up, Angles(50.0, -10.0, 30.0))
        val left = DirectionDials.remaining(o.forward, o.up, o.forward, o.up)
        assertEquals(0.0, left.yaw, 1e-6)
        assertEquals(0.0, left.pitch, 1e-6)
        assertEquals(0.0, left.roll, 1e-6)
    }

    @Test
    fun `ship-relative directions follow the ship's facing`() {
        near(forward, Frame.toWorld(forward, up, Vec3(1.0, 0.0, 0.0)))
        near(right, Frame.toWorld(forward, up, Vec3(0.0, 1.0, 0.0)))
        val v = Vec3(0.3, 0.5, 0.8).normalized()
        near(v, Frame.toShip(forward, up, Frame.toWorld(forward, up, v)))
        assertEquals(1.0, sqrt(v.dot(v)), 1e-12)
    }
}
