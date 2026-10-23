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
import neumann.core.world.BurnDirection
import neumann.core.world.ContactKind
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.WeaponKind

private val skips = listOf(60.0 to "1 MIN", 3600.0 to "1 HR", 4 * 3600.0 to "4 HR", 86400.0 to "1 DAY")
private val directions = listOf(
    BurnDirection.PROGRADE to "PRO",
    BurnDirection.RETROGRADE to "RETRO",
    BurnDirection.TOWARD_TARGET to "TO TGT",
    BurnDirection.AWAY_FROM_TARGET to "AWAY",
    BurnDirection.MATCH_TARGET to "MATCH",
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
    var direction by remember { mutableStateOf(BurnDirection.RETROGRADE) }
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
                val drive = player.shipClass.drive
                Line("Speed      ${DisplayFormat.speed(player.velocity.length())} (rel. star)")
                Line("From star  ${DisplayFormat.distance(player.position.length())}")
                if (drive != null) {
                    Line("Fuel       ${DisplayFormat.bar(player.fuel / drive.fuelCapacity)} %.0f/%.0f".format(player.fuel, drive.fuelCapacity))
                }
                player.shipClass.cannon?.let { Line("Slugs      ${player.slugs}/${it.ammo}") }
                player.shipClass.torpedo?.let { Line("Torpedoes  ${player.torpedoes}/${it.ammo}") }
                val burn = player.burn
                Line(if (burn == null) "Burn       none" else
                    "Burn       ${burn.direction.name.lowercase()} ${(burn.throttle * 100).toInt()}% ${DisplayFormat.duration(burn.remainingSeconds)} left",
                    if (burn == null) Theme.dim else Theme.alert)
                for (part in player.parts.values) {
                    val color = when {
                        part.destroyed -> Theme.danger
                        part.hitPoints < part.maxHitPoints -> Theme.alert
                        else -> Theme.text
                    }
                    Line("%-10s %s %4.0f".format(partName(part.kind), DisplayFormat.bar(part.hitPoints / part.maxHitPoints), part.hitPoints), color)
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

            Section("BURN") {
                ChoiceRow(directions, direction) { direction = it }
                ChoiceRow(throttles, throttle) { throttle = it }
                ChoiceRow(durations, duration) { duration = it }
                Row(Modifier.fillMaxWidth()) {
                    ConsoleButton("BURN", enabled = !world.over) { changed(session.orders.setBurn(direction, throttle, duration)) }
                    ConsoleButton("CANCEL", enabled = player.burn != null) { session.orders.cancelBurn(); changed() }
                }
                Line("MATCH stops by itself when velocity matches the target.", Theme.dim)
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

private fun partName(kind: PartKind) = when (kind) {
    PartKind.TORPEDO_LAUNCHER -> "Launcher"
    PartKind.FUEL_TANK -> "Fuel tank"
    else -> kind.name.lowercase().replaceFirstChar { it.uppercase() }
}

private const val LOG_LINES = 40
