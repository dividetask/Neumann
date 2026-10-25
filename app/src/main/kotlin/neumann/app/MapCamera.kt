package neumann.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import neumann.core.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * How the map looks at the system: the point at the screen centre, the
 * scale (pixels per metre), the rotation around the system's axis ([yaw]),
 * and the [tilt] from looking down from above (0) to edge-on (π/2).
 */
data class MapCamera(val centre: Vec3, val scale: Double, val yaw: Double = 0.0, val tilt: Double = 0.0) {
    /** Screen position of a point in space. */
    fun project(p: Vec3, size: Size): Offset {
        val d = p - centre
        val x1 = d.x * cos(yaw) + d.y * sin(yaw)
        val y1 = -d.x * sin(yaw) + d.y * cos(yaw)
        val up = y1 * cos(tilt) + d.z * sin(tilt)
        return Offset((size.width / 2 + x1 * scale).toFloat(), (size.height / 2 - up * scale).toFloat())
    }

    /** The direction in space (in the screen's plane) that a screen movement of (dx, dy) pixels shows. */
    fun screenToSpace(dx: Float, dy: Float): Vec3 {
        val x1 = dx / scale
        val up = -dy / scale
        val y1 = up * cos(tilt)
        val z = up * sin(tilt)
        return Vec3(x1 * cos(yaw) - y1 * sin(yaw), x1 * sin(yaw) + y1 * cos(yaw), z)
    }

    fun panned(dx: Float, dy: Float) = copy(centre = centre - screenToSpace(dx, dy))

    /** Zoom by [factor], keeping the point under [focus] still. */
    fun zoomed(factor: Float, focus: Offset, size: Size): MapCamera {
        val fromCentre = focus - Offset(size.width / 2, size.height / 2)
        val under = centre + screenToSpace(fromCentre.x, fromCentre.y)
        val z = copy(scale = scale * factor)
        return z.copy(centre = under - z.screenToSpace(fromCentre.x, fromCentre.y))
    }

    fun rotated(degrees: Float) = copy(yaw = yaw - degrees * PI / 180)

    fun tilted(dyPixels: Float) = copy(tilt = (tilt + dyPixels * TILT_PER_PIXEL).coerceIn(0.0, PI / 2))

    companion object {
        const val TILT_PER_PIXEL = 0.005

        /** A camera centred on [centre] that fits [radius] metres into the smaller screen side. */
        fun fitting(centre: Vec3, radius: Double, size: Size) =
            MapCamera(centre, minOf(size.width, size.height) / 2.0 / radius, tilt = 0.35)

        /** A camera that fits every one of [points] on screen, with a margin. */
        fun framing(points: List<Vec3>, size: Size): MapCamera {
            val min = Vec3(points.minOf { it.x }, points.minOf { it.y }, points.minOf { it.z })
            val max = Vec3(points.maxOf { it.x }, points.maxOf { it.y }, points.maxOf { it.z })
            val centre = (min + max) * 0.5
            val radius = points.maxOf { (it - centre).length() }
            return fitting(centre, maxOf(radius * FRAME_MARGIN, MIN_FRAME_RADIUS_M), size)
        }

        private const val FRAME_MARGIN = 1.2
        private const val MIN_FRAME_RADIUS_M = 1e7
    }
}
