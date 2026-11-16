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
import neumann.core.sim.ShipPerformance
import neumann.core.world.Vessel
import kotlin.math.exp
import kotlin.math.ln

/**
 * Arrival: the probe is at the start point, moving at its arrival speed. The
 * player drags the arrow out from the ship to choose the entry velocity: its
 * direction is the approach, its length the entry speed, anything from
 * stopped up to what the fuel allows. The screen shows the entry burn's
 * speed change, fuel, and time at the drive dial's setting, and the
 * coasting path at that velocity. ENTER starts the entry burn.
 */
@Composable
fun ApproachScreen(data: GameData, seed: Long, version: String, onNewSystem: () -> Unit, onEnter: (ScenarioConfig, Vec3, Double) -> Unit) {
    val scenario = remember(seed) { SystemGenerator(data).generate(seed) }
    val world = remember(scenario) { ScenarioBuilder(data).arrive(scenario) }
    val arrival = world.arrival!!
    val probe = world.player
    val performance = remember { ShipPerformance(data.munitions) }
    val predictor = remember(world) { TrajectoryPredictor(world.star, world.planets) }
    // The fastest entry the fuel allows: the arrival speed plus all its delta-v at the most economical setting.
    val topSpeed = remember(world) {
        val mass = performance.massKg(probe)
        arrival.speedMS + performance.exhaustMS(probe, 0.0) * ln(mass / (mass - probe.fuel * 1000.0))
    }
    var direction by remember(scenario) { mutableStateOf(probe.velocity.normalized()) }
    var speed by remember(scenario) { mutableStateOf(minOf(DEFAULT_ENTRY_MS, topSpeed)) }
    var drive by remember(scenario) { mutableStateOf(0.0) }
    val prediction = remember(direction, speed) {
        // Long enough to cross the system from the start point and out the other side.
        val span = PREDICT_CROSSINGS * probe.position.length() / maxOf(speed, 1_000.0)
        predictor.predict(probe.position, direction * speed, span, span / PREDICT_STEPS)
    }
    val burn = EntryBurn(probe, performance, direction * speed, drive)

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.background)) {
        val area = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val maxArrow = minOf(area.width, area.height) * 0.35f
        // The default view: the star, every planet, and the ship.
        val startView = remember(scenario, area) { MapCamera.framing(world.planets.filter { !it.isMoon }.map { it.position } + probe.position + Vec3.ZERO, area) }
        var camera by remember(startView) { mutableStateOf(startView) }
        val fraction = (speed / topSpeed).toFloat()
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
                    speed = ((len - 20f) / (maxArrow - 20f)).coerceIn(0f, 1f) * topSpeed
                }
            },
        )

        Column(Modifier.align(Alignment.TopStart).fillMaxWidth().background(Theme.panel.copy(alpha = 0.85f)).padding(8.dp)) {
            Line("NEUMANN  $version", Theme.dim)
            Line("ARRIVING: ${scenario.name.uppercase()}   seed $seed", Theme.alert)
            Line("From %.1f ly: %,.0f years at %.0f km/s".format(arrival.originLy, arrival.tripYears, arrival.cruiseMS / Units.KM), Theme.dim)
            Line("Now %.0f km/s, %.1f t fuel".format(arrival.speedMS / Units.KM, probe.fuel))
        }

        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Theme.panel.copy(alpha = 0.85f)).padding(8.dp)) {
            Line("Entry speed %.1f km/s".format(speed / Units.KM))
            if (burn.possible) {
                Line("Entry burn %.0f km/s: %.1f t fuel, about %s".format(burn.deltaV / Units.KM, burn.fuelT, DisplayFormat.duration(burn.seconds)))
            } else {
                Line("Not enough fuel: needs %.1f t, have %.1f t".format(burn.fuelT, probe.fuel), Theme.alert)
            }
            DriveDial(drive, probe, performance) { drive = it }
            val closest = prediction.closest
            Line(when {
                prediction.hits && closest != null -> "Path hits ${closest.name} in ${DisplayFormat.duration(prediction.closestTime)}!"
                prediction.hits -> "Path falls into ${world.star.name}!"
                closest != null -> "Passes ${closest.name} at ${DisplayFormat.distance(prediction.closestDistance)} in ${DisplayFormat.duration(prediction.closestTime)}"
                else -> "Coasting path"
            }, if (prediction.hits) Theme.danger else Theme.text)
            Row(Modifier.fillMaxWidth()) {
                ConsoleButton("NEW SYSTEM") { onNewSystem() }
                Box(Modifier.weight(1f))
                ConsoleButton("ENTER SYSTEM", enabled = burn.possible) { onEnter(scenario, direction * speed, drive) }
            }
        }
    }
}

/** The entry burn to [velocity]: its speed change, the fuel it needs, roughly how long it takes, and whether the fuel allows it. */
private class EntryBurn(probe: Vessel, performance: ShipPerformance, velocity: Vec3, drive: Double) {
    val deltaV = (velocity - probe.velocity).length()
    val fuelT = performance.massKg(probe) * (1 - exp(-deltaV / performance.exhaustMS(probe, drive))) / 1000.0
    val possible = fuelT <= probe.fuel
    val seconds = deltaV / performance.accelerationMS2(probe, drive)
}

private const val DEFAULT_ENTRY_MS = 40_000.0
private const val PREDICT_CROSSINGS = 2.0
private const val PREDICT_STEPS = 3000
