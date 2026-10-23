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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neumann.core.data.GameData
import neumann.core.game.DirectionDials
import neumann.core.game.ScenarioBuilder
import neumann.core.world.ApproachVector

/**
 * Choose the approach vector from the ship's point of view: the dials aim it
 * off the star it is looking at, and the slider sets the entry speed.
 */
@Composable
fun ApproachScreen(data: GameData, version: String, onEnter: (ApproachVector) -> Unit) {
    val entry = data.scenario.entry
    var aim by remember { mutableStateOf(DirectionDials.Angles(0.0, 0.0)) }
    var speed by remember { mutableFloatStateOf(entry.defaultSpeedKmS.toFloat()) }
    val approach = ApproachVector(aim.yaw, aim.pitch, speed.toDouble())
    val preview = remember(approach) { ScenarioBuilder(data).build(approach) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Line("NEUMANN  $version", Theme.dim)
        Line("ARRIVING: ${data.scenario.name.uppercase()}", Theme.alert)
        Line("Choose the approach vector. The top of each dial is the star ahead.")
        SystemPlot(preview, PlotZoom.SYSTEM, showContacts = false, tick = 0)

        Section("AIM OFF THE STAR") {
            DirectionPad(aim, null, toGoLabel = "") { aim = it }
        }
        Section("ENTRY SPEED") {
            Line("%5.0f km/s".format(speed))
            Slider(speed, { speed = it }, valueRange = entry.minSpeedKmS.toFloat()..entry.maxSpeedKmS.toFloat(),
                colors = SliderDefaults.colors(thumbColor = Theme.alert, activeTrackColor = Theme.text, inactiveTrackColor = Theme.dim))
        }
        ConsoleButton("ENTER SYSTEM", Modifier.padding(top = 12.dp)) { onEnter(approach) }
    }
}
