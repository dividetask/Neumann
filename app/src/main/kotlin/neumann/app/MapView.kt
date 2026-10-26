package neumann.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import neumann.core.game.BogeyEstimate
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.ContactKind
import neumann.core.world.Planet
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.cos
import kotlin.math.sin

/** The approach arrow: from the ship, a direction in space, drawn [lengthPx] long. */
class ApproachArrow(val direction: Vec3, val lengthPx: Float)

/**
 * The full-screen 3D map. One-finger drag pans, pinch zooms, two-finger
 * twist rotates around the system's axis, two-finger drag up/down tilts,
 * double-tap resets the view ([onReset]). With [onArrow] set, a one-finger drag that starts on
 * the ship drags the approach arrow instead of panning.
 */
@Composable
fun MapView(
    world: World,
    camera: MapCamera,
    onCamera: (MapCamera) -> Unit,
    onReset: () -> Unit,
    tick: Int,
    showContacts: Boolean,
    modifier: Modifier = Modifier,
    path: List<Vec3>? = null,
    highlight: Planet? = null,
    arrow: ApproachArrow? = null,
    onArrow: ((Offset) -> Unit)? = null,
    bogeys: List<BogeyEstimate> = emptyList(),
    fixtures: List<Vessel> = emptyList(),
) {
    val measurer = rememberTextMeasurer()
    val cam by rememberUpdatedState(camera)
    val setCam by rememberUpdatedState(onCamera)
    val arrowDrag by rememberUpdatedState(onArrow)
    val reset by rememberUpdatedState(onReset)
    val shipAt by rememberUpdatedState(world.player.position)
    Canvas(
        modifier.fillMaxSize().background(Theme.background)
            .pointerInput(Unit) {
                // The previous tap, to spot a double tap. Taps are found here, not by a
                // separate tap detector, because this handler consumes every movement.
                var lastTapTime = 0L
                var lastTapAt = Offset.Zero
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val area = Size(size.width.toFloat(), size.height.toFloat())
                    val ship = cam.project(shipAt, area)
                    // A drag that starts on the ship moves the approach arrow (approach screen only).
                    var dragging = arrowDrag != null && (down.position - ship).getDistance() < 64.dp.toPx()
                    var tap = true
                    var upTime = down.uptimeMillis
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size > 1 || event.changes.any { (it.position - down.position).getDistance() > viewConfiguration.touchSlop }) tap = false
                        when {
                            pressed.size == 1 && dragging -> arrowDrag?.invoke(pressed[0].position - cam.project(shipAt, area))
                            pressed.size == 1 -> {
                                val c = pressed[0]
                                val d = c.position - c.previousPosition
                                setCam(cam.panned(d.x, d.y))
                            }
                            pressed.size >= 2 -> {
                                dragging = false
                                var c = cam.zoomed(event.calculateZoom(), event.calculateCentroid(), area)
                                c = c.rotated(event.calculateRotation())
                                c = c.tilted(event.calculatePan().y)
                                setCam(c)
                            }
                        }
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                        upTime = event.changes.first().uptimeMillis
                    } while (event.changes.any { it.pressed })
                    if (tap && upTime - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) {
                        val double = down.uptimeMillis - lastTapTime < viewConfiguration.doubleTapTimeoutMillis &&
                            (down.position - lastTapAt).getDistance() < 48.dp.toPx()
                        if (double) {
                            reset()
                            lastTapTime = 0L
                        } else {
                            lastTapTime = upTime
                            lastTapAt = down.position
                        }
                    }
                }
            },
    ) {
        // The world is mutable, so this lambda reads [tick] to redraw after each change.
        draw(world, camera, showContacts, measurer, path, highlight, arrow, bogeys, fixtures, tick)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun DrawScope.draw(
    world: World, cam: MapCamera, showContacts: Boolean, measurer: TextMeasurer,
    path: List<Vec3>?, highlight: Planet?, arrow: ApproachArrow?, bogeys: List<BogeyEstimate>, fixtures: List<Vessel>, tick: Int,
) {
    fun at(p: Vec3) = cam.project(p, size)
    fun visible(o: Offset) = o.x in -20f..size.width + 20f && o.y in -20f..size.height + 20f

    world.belt?.let { belt ->
        val steps = 6
        for (i in 0..steps) {
            val r = (belt.innerAu + (belt.outerAu - belt.innerAu) * i / steps) * Units.AU
            ring(cam, Vec3.ZERO, r, 0.0, Theme.dim.copy(alpha = 0.25f))
        }
    }
    for (p in world.planets) {
        val centre = p.parent?.position ?: Vec3.ZERO
        if (p.orbitRadius * cam.scale < 8) continue // too small to see at this zoom
        ring(cam, centre, p.orbitRadius, p.inclination, Theme.dim.copy(alpha = if (p.isMoon) 0.3f else 0.5f))
    }
    path?.let { pts ->
        val line = Path()
        pts.forEachIndexed { i, q -> val o = at(q); if (i == 0) line.moveTo(o.x, o.y) else line.lineTo(o.x, o.y) }
        drawPath(line, Theme.alert.copy(alpha = 0.7f), style = Stroke(2f))
    }
    val star = at(Vec3.ZERO)
    if (visible(star)) {
        drawCircle(Theme.star, 7f, star)
        label(measurer, world.star.name, star + Offset(9f, -9f), Theme.star)
    }
    for (p in world.planets) {
        val o = at(p.position)
        if (!visible(o)) continue
        val radius = if (p.isMoon) 2.5f else if (p.mass > 10 * Units.EARTH_MASS) 6f else 4f
        drawCircle(if (p.isMoon) Theme.dim else Theme.planet, radius, o)
        if (p === highlight) drawCircle(Theme.alert, radius + 6f, o, style = Stroke(2f))
        // Moons are labelled only when zoomed in enough to tell them apart.
        val labelled = !p.isMoon || p.orbitRadius * cam.scale > 40
        if (labelled) label(measurer, p.name, o + Offset(7f, -7f), if (p.isMoon) Theme.dim else Theme.planet)
    }

    if (showContacts) {
        val ship = at(world.player.position)
        for (c in world.player.contacts.current.values) {
            val color = if (c.targetsObserver) Theme.alert else Theme.enemy
            // Its distance is unknown, so a bearing-only contact is a line from the ship off the screen.
            if (c.bearingOnly) {
                val reach = size.maxDimension * 2
                drawLine(color.copy(alpha = 0.6f), ship, ship + direction(cam, world.player.position, c.observedPosition - world.player.position) * reach, 1f)
            }
            // Bogeys are drawn from their estimates, defences and bases where known, below; slugs once, where seen.
            if (c.kind != ContactKind.SLUG || c.bearingOnly) continue
            val o = at(c.observedPosition)
            if (!visible(o)) continue
            drawCircle(color, 2f, o)
            val err = (c.uncertaintyM * cam.scale).toFloat()
            if (err > 3f) drawCircle(color.copy(alpha = 0.6f), minOf(err, size.width), o, style = Stroke(1f))
        }
        for (v in fixtures) {
            val o = at(v.position)
            if (!visible(o)) continue
            val half = if (v.contactKind == ContactKind.BASE) 7f else 4f
            drawRect(Theme.enemy, o - Offset(half, half), Size(2 * half, 2 * half))
            if (v.id == world.player.targetId) drawCircle(Theme.alert, 12f, o, style = Stroke(2f))
            label(measurer, v.name, o + Offset(9f, 4f), Theme.enemy)
        }
        for (e in bogeys) drawBogey(world, cam, measurer, e)
        label(measurer, "○ seen   ● now", Offset(8f, size.height - 46f), Theme.seen)
    }

    val p = world.player
    if (p.alive) {
        val o = at(p.position)
        val reach = minOf(size.width, size.height) * 0.08f
        drawCircle(Theme.player, 5f, o)
        drawLine(Theme.player, o, o + direction(cam, p.position, p.velocity) * reach * 1.5f, 2f) // velocity
        drawLine(Theme.alert, o, o + direction(cam, p.position, p.forward) * reach, 3f) // facing
        arrow?.let { a ->
            val tip = o + direction(cam, p.position, a.direction) * a.lengthPx
            drawLine(Theme.alert, o, tip, 4f)
            drawCircle(Theme.alert, 9f, tip)
        }
    }
    val hundredPx = 100 / cam.scale
    label(measurer, "100 px = ${DisplayFormat.distance(hundredPx)}", Offset(8f, size.height - 28f), Theme.dim)
}

/**
 * A bogey drawn twice: hollow and grey where the instruments saw
 * it, filled where it probably is now inside a mostly translucent sphere of
 * where it could be, and a faint line between. A ghost (fix too old) is
 * only a faint hollow marker where it was seen.
 */
private fun DrawScope.drawBogey(world: World, cam: MapCamera, measurer: TextMeasurer, e: BogeyEstimate) {
    val c = e.contact
    val seen = cam.project(e.seen.position, size)
    val name = if (e.tracked) c.name else "${c.name} (lost)"
    val now = e.now?.let { cam.project(it, size) }
    if (now == null) {
        marker(c.kind, seen, Theme.seen.copy(alpha = 0.35f), filled = false)
        label(measurer, name, seen + Offset(8f, 4f), Theme.seen.copy(alpha = 0.35f))
        return
    }
    val targeted = c.id == world.player.targetId
    val color = if (targeted || c.targetsObserver) Theme.alert else Theme.enemy
    drawLine(Theme.seen.copy(alpha = 0.4f), seen, now, 1f)
    val sphere = minOf((e.radiusM * cam.scale).toFloat(), size.maxDimension * 2)
    if (sphere > 3f) {
        drawCircle(color.copy(alpha = 0.10f), sphere, now)
        drawCircle(color.copy(alpha = 0.30f), sphere, now, style = Stroke(1f))
    }
    marker(c.kind, seen, Theme.seen, filled = false)
    marker(c.kind, now, color, filled = true)
    if (targeted) drawCircle(Theme.alert, 12f, now, style = Stroke(2f))
    if (c.kind.isVessel && now.x in 0f..size.width && now.y in 0f..size.height) label(measurer, name, now + Offset(8f, 4f), color)
}

/** A torpedo is a cross; a ship a circle. */
private fun DrawScope.marker(kind: ContactKind, o: Offset, color: Color, filled: Boolean) {
    if (kind == ContactKind.TORPEDO) {
        val w = if (filled) 2.5f else 1.2f
        drawLine(color, o + Offset(-4f, -4f), o + Offset(4f, 4f), w)
        drawLine(color, o + Offset(-4f, 4f), o + Offset(4f, -4f), w)
        return
    }
    if (filled) drawCircle(color, 5f, o) else drawCircle(color, 5f, o, style = Stroke(1.5f))
}

/** Unit screen direction of a direction in space, as seen from [from]; zero if it points straight at the viewer. */
private fun DrawScope.direction(cam: MapCamera, from: Vec3, dir: Vec3): Offset {
    if (dir.lengthSquared() == 0.0) return Offset.Zero
    val a = cam.project(from, size)
    val b = cam.project(from + dir.normalized() * (1.0 / cam.scale * 100), size)
    val d = b - a
    val len = d.getDistance()
    return if (len < 1e-3f) Offset.Zero else d / len
}

/** A circular orbit of [radius] around [centre], tilted by [inclination] (same tilt as PlanetMover). */
private fun DrawScope.ring(cam: MapCamera, centre: Vec3, radius: Double, inclination: Double, color: Color) {
    val path = Path()
    for (i in 0..96) {
        val th = 2 * Math.PI * i / 96
        val y = radius * sin(th)
        val o = cam.project(centre + Vec3(radius * cos(th), y * cos(inclination), y * sin(inclination)), size)
        if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
    }
    drawPath(path, color, style = Stroke(1f))
}

/**
 * Measured first, then drawn: drawing a string directly limits it to the
 * space left on the canvas, which throws once [at] is past the right or
 * bottom edge.
 */
private fun DrawScope.label(measurer: TextMeasurer, text: String, at: Offset, color: Color) {
    if (at.x > size.width || at.y > size.height) return
    drawText(measurer.measure(text, Theme.small.copy(color = color)), topLeft = at)
}
