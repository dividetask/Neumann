package neumann.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import neumann.core.data.GameData
import neumann.core.data.ScenarioConfig
import neumann.core.game.ScenarioBuilder
import neumann.core.game.SystemGenerator
import neumann.core.game.TrajectoryPredictor
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.ApproachVector

/**
 * Choose the approach vector on the map: drag the arrow out from the ship.
 * Its direction is the approach, its length the entry speed. Rotate and tilt
 * the map to aim anywhere in 3D. The predicted coasting path is drawn and
 * the planet it passes closest to is highlighted.
 */
@Composable
fun ApproachScreen(data: GameData, seed: Long, version: String, onNewSystem: () -> Unit, onEnter: (ScenarioConfig, ApproachVector) -> Unit) {
    val scenario = remember(seed) { SystemGenerator(data).generate(seed) }
    val entry = scenario.entry
    // A world to draw: the system at arrival, with the ship at its arrival point.
    val world = remember(scenario) { ScenarioBuilder(data).build(scenario, ApproachVector(null, entry.defaultSpeedKmS)) }
    val predictor = remember(world) { TrajectoryPredictor(world.star, world.planets) }
    var direction by remember(scenario) { mutableStateOf(-world.player.position.normalized()) }
    var speed by remember(scenario) { mutableStateOf(entry.defaultSpeedKmS) }
    val prediction = remember(direction, speed) {
        predictor.predict(world.player.position, direction * (speed * Units.KM), PREDICT_DAYS * 86400.0, 3 * 3600.0)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.background)) {
        val area = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val maxArrow = minOf(area.width, area.height) * 0.35f
        // The default view: the star, every planet, and the ship.
        val startView = remember(scenario, area) { MapCamera.framing(world.planets.filter { !it.isMoon }.map { it.position } + world.player.position + Vec3.ZERO, area) }
        var camera by remember(startView) { mutableStateOf(startView) }
        val fraction = ((speed - entry.minSpeedKmS) / (entry.maxSpeedKmS - entry.minSpeedKmS)).toFloat()
        MapView(
            world, camera, { camera = it },
            onReset = { camera = startView },
            tick = 0, showContacts = false,
            path = prediction.points, highlight = prediction.closest,
            arrow = ApproachArrow(direction, 20f + fraction * (maxArrow - 20f)),
            onArrow = { drag ->
                val len = drag.getDistance()
                if (len > 8f) {
                    direction = camera.screenToSpace(drag.x, drag.y).normalized()
                    val f = ((len - 20f) / (maxArrow - 20f)).coerceIn(0f, 1f)
                    speed = entry.minSpeedKmS + f * (entry.maxSpeedKmS - entry.minSpeedKmS)
                }
            },
        )

        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().background(Theme.panel.copy(alpha = 0.85f)).padding(8.dp)) {
            Line("NEUMANN  $version", Theme.dim)
            Line("ARRIVING: ${scenario.name.uppercase()}   seed $seed", Theme.alert)
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Theme.panel.copy(alpha = 0.85f)).padding(8.dp)) {
            Line("Entry speed %.1f km/s (%.0f–%.0f)".format(speed, entry.minSpeedKmS, entry.maxSpeedKmS))
            val closest = prediction.closest
            Line(when {
                prediction.hits && closest != null -> "Path hits ${closest.name} in ${DisplayFormat.duration(prediction.closestTime)}!"
                prediction.hits -> "Path falls into ${world.star.name}!"
                closest != null -> "Passes ${closest.name} at ${DisplayFormat.distance(prediction.closestDistance)} in ${DisplayFormat.duration(prediction.closestTime)}"
                else -> "Coasting path for $PREDICT_DAYS days"
            }, if (prediction.hits) Theme.danger else Theme.text)
            Row(Modifier.fillMaxWidth()) {
                ConsoleButton("NEW SYSTEM") { onNewSystem() }
                Box(Modifier.weight(1f))
                ConsoleButton("ENTER SYSTEM") { onEnter(scenario, ApproachVector(direction, speed)) }
            }
        }
    }
}

private const val PREDICT_DAYS = 180
