package neumann.app

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import neumann.core.sim.ShipPerformance
import neumann.core.world.Vessel

/**
 * The drive dial: left is most economical, right is full thrust. Shows the
 * acceleration it gives and the fuel each km/s of speed change costs.
 */
@Composable
fun DriveDial(drive: Double, ship: Vessel, performance: ShipPerformance, onChange: (Double) -> Unit) {
    val accel = performance.accelerationMS2(ship, drive)
    val exhaust = performance.exhaustMS(ship, drive)
    // Fuel for 1 km/s at today's mass: mass × (1 − e^(−1 km/s ÷ exhaust)).
    val perKmS = if (exhaust > 0.0) performance.massKg(ship) * (1 - Math.exp(-1000.0 / exhaust)) / 1000.0 else 0.0
    Line("Drive  %.3f m/s²  %.2f t fuel per km/s".format(accel, perKmS))
    Slider(drive.toFloat(), { onChange(it.toDouble()) },
        colors = SliderDefaults.colors(thumbColor = Theme.alert, activeTrackColor = Theme.text, inactiveTrackColor = Theme.dim))
}
