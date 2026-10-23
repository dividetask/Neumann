package neumann.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neumann.core.game.GameSession
import neumann.core.game.TimeSkipResult
import neumann.core.game.DirectionCircles
import neumann.core.math.Frame
import neumann.core.math.Vec3
import neumann.core.world.HeadingMode
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.WeaponKind

private val skips = listOf(60.0 to "1 MIN", 3600.0 to "1 HR", 4 * 3600.0 to "4 HR", 86400.0 to "1 DAY")
private val modes = listOf(
    HeadingMode.FIXED to "CIRCLE",
    HeadingMode.PROGRADE to "PRO",
    HeadingMode.RETROGRADE to "RETRO",
    HeadingMode.TOWARD_TARGET to "TO TGT",
    HeadingMode.AWAY_FROM_TARGET to "AWAY",
    HeadingMode.MATCH_TARGET to "MATCH",
)
private val throttles = listOf(0.25 to "25%", 0.5 to "50%", 1.0 to "100%")
private val durations = listOf(60.0 to "1m", 600.0 to "10m", 3600.0 to "1h", 4 * 3600.0 to "4h", 86400.0 to "1d")

/** The main game screen: status, plot, contacts, orders, time skips, event log. */
@Composable
fun BridgeScreen(session: GameSession, version: String, onNewGame: () -> Unit) {
    val world = session.world
    val player = world.player
    var tick by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var lastSkip by remember { mutableStateOf<TimeSkipResult?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableStateOf(PlotZoom.SYSTEM) }
    var mode by remember { mutableStateOf(HeadingMode.FIXED) }
    var dot by remember { mutableStateOf<DirectionCircles.Dot?>(null) }
    var throttle by remember { mutableStateOf(1.0) }
    var duration by remember { mutableStateOf(600.0) }
    val scope = rememberCoroutineScope()

    fun changed(problem: String? = null) {
        notice = problem
        tick++
    }

    fun skip(seconds: Double) {
        busy = true
        notice = null
        scope.launch {
            val r = withContext(Dispatchers.Default) { session.skip(seconds) }
            lastSkip = r
            busy = false
            tick++
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(10.dp)) {
        // The world is mutable and not observable, so the whole display is rebuilt
        // whenever tick changes (after every skip or order).
        key(tick) {
            Line("NEUMANN  ${DisplayFormat.missionTime(world.time)}", Theme.alert)
            Line(version, Theme.dim)

            when {
                world.claimed -> Line("*** SYSTEM CLAIMED ***", Theme.alert)
                world.lost -> Line("*** ${player.name.uppercase()} LOST ***", Theme.danger)
            }
            if (world.over) ConsoleButton("NEW GAME") { onNewGame() }

            Section("TIME SKIP") {
                Row(Modifier.fillMaxWidth()) {
                    for ((seconds, label) in skips) ConsoleButton(label, enabled = !busy && !world.over) { skip(seconds) }
                }
                when {
                    busy -> Line("Running...", Theme.dim)
                    lastSkip != null -> {
                        val r = lastSkip!!
                        if (r.pausedBy.isEmpty()) Line("Skipped ${DisplayFormat.duration(r.elapsedSeconds)}", Theme.dim)
                        else {
                            Line("PAUSED after ${DisplayFormat.duration(r.elapsedSeconds)}:", Theme.alert)
                            for (e in r.pausedBy) Line("  ${e.message}", Theme.alert)
                        }
                    }
                }
                notice?.let { Line(it, Theme.danger) }
            }

            Section("PLOT") {
                ChoiceRow(PlotZoom.entries.map { it to it.label }, zoom) { zoom = it }
                SystemPlot(world, zoom, showContacts = true, tick = tick)
            }

            Section("SHIP") {
                val perf = session.performance
                val fuelCap = perf.fuelCapacity(player)
                val energyCap = perf.energyCapacity(player)
                Line("Speed      ${DisplayFormat.speed(player.velocity.length())} (rel. star)")
                Line("From star  ${DisplayFormat.distance(player.position.length())}")
                Line("Mass       %.1f t   accel %.2f m/s²   turn %.1f°/s".format(
                    perf.massKg(player) / 1000, perf.accelerationMS2(player), Math.toDegrees(perf.turnRate(player))))
                Line("Fuel       ${DisplayFormat.bar(if (fuelCap > 0) player.fuel / fuelCap else 0.0)} %.2f/%.0f t".format(player.fuel, fuelCap))
                Line("Power      ${DisplayFormat.bar(if (energyCap > 0) player.energy / energyCap else 0.0)} %.0f/%.0f MJ +%.1f MW".format(
                    player.energy, energyCap, perf.generatorMW(player)), if (player.scannersPowered) Theme.text else Theme.danger)
                Line("Slugs      ${player.slugs}/${perf.slugCapacity(player)}")
                Line("Torpedoes  ${player.torpedoes}/${perf.torpedoCapacity(player)}")
                Line(manoeuvreStatus(session), if (player.burn == null) Theme.dim else Theme.alert)
                for (m in player.modules) {
                    val color = when {
                        m.destroyed -> Theme.danger
                        m.hitPoints < m.type.hitPoints -> Theme.alert
                        else -> Theme.text
                    }
                    Line("%-18s %s %4.0f".format(m.type.name.take(18), DisplayFormat.bar(m.hitPoints / m.type.hitPoints), m.hitPoints), color)
                }
            }

            Section("CONTACTS") {
                val contacts = player.contacts.current.values.sortedBy { it.distance }
                if (contacts.isEmpty()) Line("none", Theme.dim)
                for (c in contacts) {
                    val targeted = c.id == player.targetId
                    val color = when {
                        c.targetsObserver -> Theme.alert
                        targeted -> Theme.alert
                        else -> Theme.text
                    }
                    Column(Modifier.fillMaxWidth().clickable(enabled = c.kind != ContactKind.TORPEDO) {
                        changed(session.orders.setTarget(c.id))
                    }.padding(vertical = 3.dp)) {
                        Line("${if (targeted) ">" else " "} ${c.name} (${c.kind.name.lowercase()})", color)
                        Line("   ${DisplayFormat.distance(c.distance)}  closing ${DisplayFormat.speed(c.closingSpeed)}  delay ${DisplayFormat.duration(c.lightDelaySeconds)}", Theme.dim)
                    }
                }
                Line("Tap a contact to target it.", Theme.dim)
            }

            Section("WEAPONS") {
                Row(Modifier.fillMaxWidth()) {
                    ConsoleButton("LASER", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.LASER)) }
                    ConsoleButton("CANNON", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.CANNON)) }
                    ConsoleButton("TORPEDO", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.TORPEDO)) }
                }
                Line("Fire now at the target. Fires during the next time skip.", Theme.dim)
                val rules = player.rules
                Rule("Laser point defence", rules.laserPointDefence) { rules.laserPointDefence = it; changed() }
                Rule("Laser auto-fire at target", rules.laserAutoFire) { rules.laserAutoFire = it; changed() }
                Rule("Cannon auto-fire at target", rules.cannonAutoFire) { rules.cannonAutoFire = it; changed() }
                Rule("Torpedo auto-launch at target", rules.torpedoAutoLaunch) { rules.torpedoAutoLaunch = it; changed() }
            }

            Section("MANOEUVRE") {
                ChoiceRow(modes, mode) { mode = it }
                val headingDot = session.headingDirection()?.let {
                    DirectionCircles.toDot(Frame.toShip(player.forward, player.up, it))
                }
                DirectionPad(dot, headingDot) { dot = it; mode = HeadingMode.FIXED }
                ChoiceRow(throttles, throttle) { throttle = it }
                ChoiceRow(durations, duration) { duration = it }
                fun vector() = dot?.let { Frame.toWorld(player.forward, player.up, DirectionCircles.toShip(it)) } ?: Vec3.ZERO
                Row(Modifier.fillMaxWidth()) {
                    ConsoleButton("FACE", enabled = !world.over) {
                        val problem = session.orders.face(mode, vector())
                        if (problem == null) dot = null
                        changed(problem)
                    }
                    ConsoleButton("BURN", enabled = !world.over) {
                        val problem = session.orders.burn(mode, vector(), throttle, duration)
                        if (problem == null) dot = null
                        changed(problem)
                    }
                    ConsoleButton("CANCEL", enabled = player.burn != null) { session.orders.cancelBurn(); changed() }
                }
                Line("Directions are relative to the ship. The ship turns first, then burns.", Theme.dim)
                Line("Ring: current heading. MATCH stops itself when velocity matches.", Theme.dim)
            }

            Section("LOG") {
                for (e in world.log.events.asReversed().take(LOG_LINES)) {
                    val color = when (e.kind) {
                        EventKind.DAMAGE_TAKEN, EventKind.SHIP_LOST -> Theme.danger
                        EventKind.INFO, EventKind.DAMAGE_DEALT -> Theme.dim
                        else -> Theme.alert
                    }
                    Line("${DisplayFormat.missionTime(e.time)} ${e.message}", color)
                }
            }
        }
    }
}

@Composable
private fun Rule(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(on, onChange, colors = CheckboxDefaults.colors(checkedColor = Theme.text, uncheckedColor = Theme.dim, checkmarkColor = Theme.background))
        Line(label)
    }
}

/** Burn and turn state, e.g. "Burn 100% 9.5 min left: turning, 40° to go". */
private fun manoeuvreStatus(session: GameSession): String {
    val p = session.world.player
    val dir = session.headingDirection()
    val toGo = dir?.let { Math.toDegrees(p.forward.angleTo(it)) } ?: 0.0
    val turning = if (toGo > session.data.rules.ships.alignToleranceDeg) "turning, %.0f° to go".format(toGo) else null
    val burn = p.burn
    return when {
        burn != null -> "Burn       ${(burn.throttle * 100).toInt()}% ${DisplayFormat.duration(burn.remainingSeconds)} left: " +
            (turning ?: "thrusting")
        turning != null -> "Heading    $turning"
        else -> "Burn       none"
    }
}

private const val LOG_LINES = 40
