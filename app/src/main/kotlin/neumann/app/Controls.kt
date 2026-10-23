package neumann.app

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A bordered text button in the ship-display style. */
@Composable
fun ConsoleButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit) {
    val color = when {
        !enabled -> Theme.dim
        selected -> Theme.alert
        else -> Theme.text
    }
    Text(
        label,
        style = Theme.body.copy(color = color),
        modifier = modifier
            .padding(2.dp)
            .border(1.dp, color)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** A row of options, one of which is selected. */
@Composable
fun <T> ChoiceRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        for ((value, label) in options) {
            ConsoleButton(label, selected = value == selected) { onSelect(value) }
        }
    }
}

/** A section with a heading line. */
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text("── $title ──", style = Theme.heading.copy(color = Theme.dim))
        content()
    }
}

@Composable
fun Line(text: String, color: Color = Theme.text, modifier: Modifier = Modifier) {
    Text(text, style = Theme.body.copy(color = color), modifier = modifier)
}

/**
 * A slider on a log scale from [min] to [max], with its far left meaning
 * "off" (null). Shows [label] and the value via [format].
 */
@Composable
fun LogSlider(label: String, value: Double?, min: Double, max: Double, format: (Double) -> String, onChange: (Double?) -> Unit) {
    val position = value?.let { (Math.log(it / min) / Math.log(max / min)).toFloat().coerceIn(0f, 1f) * 0.95f + 0.05f } ?: 0f
    Line("$label ${value?.let(format) ?: "off"}", if (value == null) Theme.dim else Theme.text)
    Slider(position, { p ->
        onChange(if (p < 0.05f) null else min * Math.pow(max / min, ((p - 0.05f) / 0.95f).toDouble()))
    }, colors = SliderDefaults.colors(thumbColor = Theme.alert, activeTrackColor = Theme.text, inactiveTrackColor = Theme.dim))
}
