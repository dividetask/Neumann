package neumann.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import neumann.core.game.GameSession
import neumann.core.sim.SensorPhase

/**
 * One enemy's panel: where it is, lasers and cannons on or off at it (one
 * target each), how well each weapon would do at this range, a torpedo salvo
 * slider with LAUNCH, and whether it is the manoeuvre target.
 */
@Composable
fun EnemyPanel(session: GameSession, id: String, salvo: Int, onSalvo: (Int) -> Unit, onBack: () -> Unit, changed: (String?) -> Unit) {
    val player = session.world.player
    val c = player.contacts[id]
    ConsoleButton("< BACK") { onBack() }
    if (c == null) {
        Line("Contact lost", Theme.dim)
        return
    }
    val range = if (c.bearingOnly) "bearing only" else "${DisplayFormat.distance(c.distance)} ±${DisplayFormat.distance(c.uncertaintyM)}"
    Line("${c.name} (${c.kind.name.lowercase()})  $range", Theme.alert)
    if (!c.bearingOnly) Line("Closing ${DisplayFormat.speed(c.closingSpeed)} (${SensorPhase.sensorName(c.sensor)})", Theme.dim)

    val lasersOn = player.rules.laserAutoFire && player.laserTargetId == id
    val cannonsOn = player.rules.cannonAutoFire && player.cannonTargetId == id
    Row(Modifier.fillMaxWidth()) {
        ConsoleButton(if (lasersOn) "LASERS ON" else "LASERS OFF", selected = lasersOn) { changed(session.orders.setLaserTarget(if (lasersOn) null else id)) }
        ConsoleButton(if (cannonsOn) "CANNONS ON" else "CANNONS OFF", selected = cannonsOn) { changed(session.orders.setCannonTarget(if (cannonsOn) null else id)) }
    }
    val lidarOn = player.lidarTargetId == id
    Row(Modifier.fillMaxWidth()) {
        ConsoleButton(if (lidarOn) "LIDAR ON" else "LIDAR OFF", selected = lidarOn) { changed(session.orders.setLidarTarget(if (lidarOn) null else id)) }
        ConsoleButton("MANOEUVRE TARGET", selected = player.targetId == id) { changed(session.orders.setTarget(id)) }
    }

    val e = session.engagement(id) ?: return
    Line(if (e.laserMJS > 0.0) "Lasers %.1f MJ/s at this range (reach %s)".format(e.laserMJS, DisplayFormat.distance(e.laserRangeM))
        else "Lasers out of range (reach ${DisplayFormat.distance(e.laserRangeM)})")
    val flight = e.slugSeconds
    val aim = e.slugAimErrorM
    Line(if (flight == null || aim == null) "Cannons need a known distance"
        else "Cannons: slug ${DisplayFormat.duration(flight)}, aim ±${DisplayFormat.distance(aim)} vs ${"%.0f".format(e.targetSizeM)} m ship (fire within ${DisplayFormat.distance(e.cannonRangeM)})")

    // Torpedoes: how many to launch now, as one salvo.
    val aboard = player.torpedoes
    val count = salvo.coerceIn(1, maxOf(1, aboard))
    Line("Torpedoes: salvo $count of $aboard" + (e.salvo?.let { ", needs ${it.toHit} to hit, ${it.toDestroy} to destroy" } ?: ""))
    if (aboard > 1) Slider(count.toFloat(), { onSalvo(Math.round(it)) }, valueRange = 1f..aboard.toFloat(), steps = maxOf(0, aboard - 2),
        colors = SliderDefaults.colors(thumbColor = Theme.alert, activeTrackColor = Theme.text, inactiveTrackColor = Theme.dim))
    ConsoleButton("LAUNCH $count", enabled = aboard > 0 && !session.world.over) { changed(session.orders.launchTorpedoes(count, id)) }
}
