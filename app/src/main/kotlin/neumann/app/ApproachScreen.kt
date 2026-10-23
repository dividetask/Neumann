package neumann.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neumann.core.data.GameData
import neumann.core.game.ScenarioBuilder
import neumann.core.world.ApproachVector

/** Choose the approach vector: bearing from the star, and entry speed. */
@Composable
fun ApproachScreen(data: GameData, version: String, onEnter: (ApproachVector) -> Unit) {
    val entry = data.scenario.entry
    var azimuth by remember { mutableFloatStateOf(0f) }
    var elevation by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(entry.defaultSpeedKmS.toFloat()) }
    val approach = ApproachVector(azimuth.toDouble(), elevation.toDouble(), speed.toDouble())
    val preview = remember(approach) { ScenarioBuilder(data).build(approach) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Line("NEUMANN  $version", Theme.dim)
        Line("ARRIVING: ${data.scenario.name.uppercase()}", Theme.alert)
        Line("Choose the approach vector.")
        SystemPlot(preview, PlotZoom.SYSTEM, showContacts = false, tick = 0)

        Section("BEARING FROM STAR") {
            Line("Azimuth   %5.0f°".format(azimuth))
            Slider(azimuth, { azimuth = it }, valueRange = 0f..359f, colors = sliderColors())
            Line("Elevation %5.0f°".format(elevation))
            Slider(elevation, { elevation = it }, valueRange = -89f..89f, colors = sliderColors())
        }
        Section("ENTRY SPEED") {
            Line("%5.0f km/s toward the star (entry %.1f AU out)".format(speed, entry.radiusAu))
            Slider(speed, { speed = it }, valueRange = entry.minSpeedKmS.toFloat()..entry.maxSpeedKmS.toFloat(), colors = sliderColors())
        }
        ConsoleButton("ENTER SYSTEM", Modifier.padding(top = 12.dp)) { onEnter(approach) }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Theme.alert,
    activeTrackColor = Theme.text,
    inactiveTrackColor = Theme.dim,
)
