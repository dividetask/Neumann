package neumann.core

import neumann.core.game.DirectionCircles
import neumann.core.game.DirectionCircles.Dot
import neumann.core.math.Frame
import neumann.core.math.Vec3
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DirectionCirclesTest {
    private fun near(a: Vec3, b: Vec3) = assertTrue((a - b).length() < 1e-9, "$a != $b")

    @Test
    fun `the front circle's centre is straight ahead and the rear circle's centre straight back`() {
        near(Vec3(1.0, 0.0, 0.0), DirectionCircles.toShip(Dot(false, 0.0, 0.0)))
        near(Vec3(-1.0, 0.0, 0.0), DirectionCircles.toShip(Dot(true, 0.0, 0.0)))
    }

    @Test
    fun `a dot on a circle's edge is 90 degrees off, toward its side`() {
        near(Vec3(0.0, 1.0, 0.0), DirectionCircles.toShip(Dot(false, 1.0, 0.0)))
        near(Vec3(0.0, 0.0, 1.0), DirectionCircles.toShip(Dot(true, 0.0, 1.0)))
    }

    @Test
    fun `halfway up the front circle is 45 degrees above ahead`() {
        near(Vec3(sqrt(0.5), 0.0, sqrt(0.5)), DirectionCircles.toShip(Dot(false, 0.0, 0.5)))
    }

    @Test
    fun `a dot survives the trip to a direction and back`() {
        for (dot in listOf(Dot(false, 0.3, -0.4), Dot(true, -0.6, 0.2))) {
            val back = DirectionCircles.toDot(DirectionCircles.toShip(dot))
            assertEquals(dot.rear, back.rear)
            assertEquals(dot.x, back.x, 1e-9)
            assertEquals(dot.y, back.y, 1e-9)
        }
    }

    @Test
    fun `ship-relative directions follow the ship's facing`() {
        val forward = Vec3(0.0, 1.0, 0.0)
        val up = Vec3(0.0, 0.0, 1.0)
        near(forward, Frame.toWorld(forward, up, Vec3(1.0, 0.0, 0.0)))
        near(forward.cross(up), Frame.toWorld(forward, up, Vec3(0.0, 1.0, 0.0)))
        near(Vec3(0.3, 0.5, 0.8).normalized(), Frame.toShip(forward, up, Frame.toWorld(forward, up, Vec3(0.3, 0.5, 0.8))))
    }
}
