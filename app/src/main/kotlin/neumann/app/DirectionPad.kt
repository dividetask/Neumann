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
import neumann.core.game.DirectionDials.Angles
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class Axis(val label: String) { YAW("YAW"), PITCH("PITCH") }

/**
 * The YAW and PITCH dials. The top of each dial is the ship's current
 * front. [pending] (filled dot) is the turn being set; [toGo] (ring) is the
 * turn the ship still has to make, which moves to the top as it turns.
 */
@Composable
fun DirectionPad(pending: Angles?, toGo: Angles?, toGoLabel: String = "To go: ", onChange: (Angles) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        for (axis in Axis.entries) {
            Column(Modifier.weight(1f).padding(3.dp)) {
                Line(axis.label, Theme.dim)
                Dial(axis, pending?.let { angleOf(it, axis) }, toGo?.let { angleOf(it, axis) }) { deg ->
                    onChange(withAngle(pending ?: Angles(0.0, 0.0), axis, deg))
                }
            }
        }
    }
    Line("Set:   " + (pending?.let(::describe) ?: "drag a dial"), if (pending == null) Theme.dim else Theme.alert)
    if (toGoLabel.isNotEmpty()) Line(toGoLabel + (toGo?.let(::describe) ?: "no heading"), Theme.dim)
}

@Composable
private fun Dial(axis: Axis, pending: Double?, toGo: Double?, onAngle: (Double) -> Unit) {
    val callback by rememberUpdatedState(onAngle)
    fun place(at: Offset, size: IntSize) {
        val deg = Math.toDegrees(atan2((at.x - size.width / 2f).toDouble(), -(at.y - size.height / 2f).toDouble()))
        callback(if (axis == Axis.PITCH) deg.coerceIn(-90.0, 90.0) else deg)
    }
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1f).background(Theme.panel)
            .pointerInput(axis) { detectTapGestures { place(it, size) } }
            .pointerInput(axis) {
                detectDragGestures(onDragStart = { place(it, size) }) { change, _ -> place(change.position, size) }
            },
    ) {
        val r = radius()
        drawCircle(Theme.text, r, center, style = Stroke(2f))
        drawLine(Theme.text, onRim(0.0, 0.8f), onRim(0.0, 1.1f), 3f)
        for (tick in listOf(90.0, 180.0, -90.0)) drawLine(Theme.dim, onRim(tick, 0.85f), onRim(tick, 1f), 1f)
        toGo?.let { drawCircle(Theme.text, 10f, onRim(it, 1f), style = Stroke(2f)) }
        pending?.let {
            drawLine(Theme.alert, center, onRim(it, 1f), 1f)
            drawCircle(Theme.alert, 8f, onRim(it, 1f))
        }
    }
}

private fun DrawScope.radius() = size.minDimension / 2 * 0.8f

/** A point at [deg] clockwise from the top, [f] × radius from the centre. */
private fun DrawScope.onRim(deg: Double, f: Float): Offset {
    val a = Math.toRadians(deg)
    val r = radius() * f
    return Offset(center.x + (sin(a) * r).toFloat(), center.y - (cos(a) * r).toFloat())
}

private fun angleOf(a: Angles, axis: Axis) = when (axis) {
    Axis.YAW -> a.yaw
    Axis.PITCH -> a.pitch
}

private fun withAngle(a: Angles, axis: Axis, deg: Double) = when (axis) {
    Axis.YAW -> a.copy(yaw = deg)
    Axis.PITCH -> a.copy(pitch = deg)
}

private fun describe(a: Angles): String =
    "yaw %.0f° %s, pitch %.0f° %s".format(
        abs(a.yaw), if (a.yaw >= 0) "R" else "L",
        abs(a.pitch), if (a.pitch >= 0) "U" else "D",
    )
