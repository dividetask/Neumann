package neumann.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.ContactKind
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin

/** How much of the system the plot shows. SYSTEM is centred on the star; the rest on the ship. */
enum class PlotZoom(val label: String, val radius: Double) {
    SYSTEM("SYS", 2.0 * Units.AU),
    M10("10Mkm", 1e10),
    M1("1Mkm", 1e9),
    K100("100k", 1e8),
    K10("10k", 1e7),
}

/** Top view (looking down on the orbital plane) and side view of the system. */
@Composable
fun SystemPlot(world: World, zoom: PlotZoom, showContacts: Boolean, tick: Int) {
    val measurer = rememberTextMeasurer()
    val centre = if (zoom == PlotZoom.SYSTEM) Vec3.ZERO else world.player.position
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        // The world is mutable, so the draw lambdas take [tick] to redraw after each change.
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).background(Theme.panel).border(1.dp, Theme.dim)) {
            draw(world, View(centre, zoom.radius, size, top = true), showContacts, measurer, "TOP", tick)
        }
        Canvas(Modifier.fillMaxWidth().aspectRatio(3f).background(Theme.panel).border(1.dp, Theme.dim)) {
            draw(world, View(centre, zoom.radius, size, top = false), showContacts, measurer, "SIDE", tick)
        }
    }
}

/** Maps world positions to screen points for one view. */
private class View(val centre: Vec3, val radius: Double, val size: Size, val top: Boolean) {
    private val scale = (size.width / 2) / radius

    fun point(p: Vec3): Offset {
        val d = p - centre
        val v = if (top) d.y else d.z
        return Offset((size.width / 2 + d.x * scale).toFloat(), (size.height / 2 - v * scale).toFloat())
    }

    fun visible(o: Offset) = o.x in 0f..size.width && o.y in 0f..size.height

    fun pixels(metres: Double) = (metres * scale).toFloat().coerceAtMost(size.width)
}

@Suppress("UNUSED_PARAMETER")
private fun DrawScope.draw(world: World, view: View, showContacts: Boolean, measurer: TextMeasurer, title: String, tick: Int) {
    label(measurer, title, Offset(6f, 4f), Theme.dim)
    label(measurer, DisplayFormat.distance(view.radius), Offset(size.width - 120f, 4f), Theme.dim)

    for (p in world.planets) {
        val path = Path()
        for (i in 0..96) {
            val th = 2 * Math.PI * i / 96
            val y = p.orbitRadius * sin(th)
            val o = view.point(Vec3(p.orbitRadius * cos(th), y * cos(p.inclination), y * sin(p.inclination)))
            if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
        }
        drawPath(path, Theme.dim.copy(alpha = 0.5f), style = Stroke(1f))
        val o = view.point(p.position)
        if (view.visible(o)) {
            drawCircle(Theme.planet, 5f, o)
            label(measurer, p.name, o + Offset(7f, -7f), Theme.planet)
        }
    }

    val star = view.point(Vec3.ZERO)
    if (view.visible(star)) drawCircle(Theme.star, 7f, star)

    if (showContacts) {
        for (c in world.player.contacts.current.values) {
            val o = view.point(c.observedPosition)
            if (!view.visible(o)) continue
            val color = if (c.kind == ContactKind.TORPEDO && c.targetsObserver) Theme.alert else Theme.enemy
            when (c.kind) {
                ContactKind.TORPEDO -> {
                    drawLine(color, o + Offset(-4f, -4f), o + Offset(4f, 4f), 2f)
                    drawLine(color, o + Offset(-4f, 4f), o + Offset(4f, -4f), 2f)
                }
                ContactKind.SHIP -> drawCircle(color, 5f, o)
                ContactKind.DEFENCE -> drawRect(color, o - Offset(4f, 4f), Size(8f, 8f))
                ContactKind.BASE -> drawRect(color, o - Offset(7f, 7f), Size(14f, 14f))
            }
            if (c.id == world.player.targetId) drawCircle(Theme.alert, 12f, o, style = Stroke(2f))
            // Error circle: the size of the position uncertainty, when big enough to see.
            val err = view.pixels(c.uncertaintyM)
            if (!c.bearingOnly && err > 3f) drawCircle(color.copy(alpha = 0.6f), err, o, style = Stroke(1f))
            if (c.bearingOnly) {
                drawLine(color.copy(alpha = 0.6f), view.point(world.player.position), o, 1f)
            }
            if (c.kind != ContactKind.TORPEDO) label(measurer, c.name, o + Offset(8f, 4f), color)
        }
    }

    val p = world.player
    if (p.alive) {
        val o = view.point(p.position)
        drawCircle(Theme.player, 5f, o)
        // Velocity (green) and facing (amber).
        val tip = view.point(p.position + p.velocity.normalized() * (view.radius * 0.15))
        drawLine(Theme.player, o, tip, 2f)
        val front = view.point(p.position + p.forward * (view.radius * 0.08))
        drawLine(Theme.alert, o, front, 3f)
    }
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, at: Offset, color: Color) {
    drawText(measurer, text, at, Theme.small.copy(color = color))
}
