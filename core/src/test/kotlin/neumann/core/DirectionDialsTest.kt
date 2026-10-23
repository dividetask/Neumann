package neumann.core

import neumann.core.game.DirectionDials
import neumann.core.game.DirectionDials.Angles
import neumann.core.math.Frame
import neumann.core.math.Vec3
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
        near(right, DirectionDials.direction(forward, up, Angles(90.0, 0.0)))
        near(-right, DirectionDials.direction(forward, up, Angles(-90.0, 0.0)))
        near(up, DirectionDials.direction(forward, up, Angles(0.0, 90.0)))
        near((forward + up).normalized(), DirectionDials.direction(forward, up, Angles(0.0, 45.0)))
    }

    @Test
    fun `the turn to go is the dial angles that were set`() {
        for (set in listOf(Angles(-35.0, 20.0), Angles(150.0, -60.0))) {
            val left = DirectionDials.remaining(forward, up, DirectionDials.direction(forward, up, set))
            assertEquals(set.yaw, left.yaw, 1e-6)
            assertEquals(set.pitch, left.pitch, 1e-6)
        }
    }

    @Test
    fun `once the ship faces the heading the turn to go is zero`() {
        val left = DirectionDials.remaining(forward, up, forward)
        assertEquals(0.0, left.yaw, 1e-9)
        assertEquals(0.0, left.pitch, 1e-9)
    }

    @Test
    fun `ship-relative directions follow the ship's facing`() {
        near(forward, Frame.toWorld(forward, up, Vec3(1.0, 0.0, 0.0)))
        near(right, Frame.toWorld(forward, up, Vec3(0.0, 1.0, 0.0)))
        val v = Vec3(0.3, 0.5, 0.8).normalized()
        near(v, Frame.toShip(forward, up, Frame.toWorld(forward, up, v)))
    }
}
