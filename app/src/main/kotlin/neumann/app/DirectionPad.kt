package neumann.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import neumann.core.game.DirectionCircles
import neumann.core.math.Vec3
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * The FRONT and REAR direction circles. Drag or tap to place the dot.
 * [dot] is the chosen direction; [heading] marks where the ship is turning
 * to now (hollow ring). Both are relative to the ship.
 */
@Composable
fun DirectionPad(dot: DirectionCircles.Dot?, heading: DirectionCircles.Dot?, onDot: (DirectionCircles.Dot) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        for (rear in listOf(false, true)) {
            Column(Modifier.weight(1f).padding(4.dp)) {
                Line(if (rear) "REAR" else "FRONT", Theme.dim)
                Circle(rear, dot?.takeIf { it.rear == rear }, heading?.takeIf { it.rear == rear }, onDot)
            }
        }
    }
    Line(dot?.let { describe(DirectionCircles.toShip(it)) } ?: "Drag in a circle to choose a direction.", Theme.dim)
}

@Composable
private fun Circle(rear: Boolean, dot: DirectionCircles.Dot?, heading: DirectionCircles.Dot?, onDot: (DirectionCircles.Dot) -> Unit) {
    val callback by rememberUpdatedState(onDot)
    fun place(at: Offset, size: IntSize) = callback(toDot(rear, at, size))
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1f).background(Theme.panel)
            .pointerInput(rear) { detectTapGestures { place(it, size) } }
            .pointerInput(rear) {
                detectDragGestures(onDragStart = { place(it, size) }) { change, _ -> place(change.position, size) }
            },
    ) {
        val c = center
        val r = radius()
        drawCircle(Theme.text, r, c, style = Stroke(2f))
        drawCircle(Theme.dim, r * 2 / 3, c, style = Stroke(1f))
        drawCircle(Theme.dim, r / 3, c, style = Stroke(1f))
        drawLine(Theme.dim, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1f)
        drawLine(Theme.dim, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1f)
        heading?.let { drawCircle(Theme.text, 10f, point(it), style = Stroke(2f)) }
        dot?.let { drawCircle(Theme.alert, 9f, point(it)) }
    }
}

private fun DrawScope.radius() = size.minDimension / 2 * 0.9f

private fun DrawScope.point(d: DirectionCircles.Dot) =
    Offset(center.x + d.x.toFloat() * radius(), center.y - d.y.toFloat() * radius())

private fun toDot(rear: Boolean, at: Offset, size: IntSize): DirectionCircles.Dot {
    val r = minOf(size.width, size.height) / 2f * 0.9f
    var x = ((at.x - size.width / 2f) / r).toDouble()
    var y = (-(at.y - size.height / 2f) / r).toDouble()
    val len = sqrt(x * x + y * y)
    if (len > 1.0) {
        x /= len
        y /= len
    }
    return DirectionCircles.Dot(rear, x, y)
}

/** A ship-relative direction as yaw (right +) and pitch (up +) from the front. */
private fun describe(rel: Vec3): String {
    val yaw = Math.toDegrees(atan2(rel.y, rel.x))
    val pitch = Math.toDegrees(asin(rel.z.coerceIn(-1.0, 1.0)))
    return "%.0f° %s, %.0f° %s".format(kotlin.math.abs(yaw), if (yaw >= 0) "right" else "left",
        kotlin.math.abs(pitch), if (pitch >= 0) "up" else "down")
}
