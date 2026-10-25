package neumann.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neumann.core.game.DirectionDials
import neumann.core.game.GameSession
import neumann.core.game.TimeSkipResult
import neumann.core.math.Vec3
import neumann.core.sim.SensorPhase
import neumann.core.world.BurnStage
import neumann.core.world.EventKind
import neumann.core.world.HeadingMode
import neumann.core.world.WeaponKind
import neumann.core.world.World

private val skips = listOf(60.0 to "1 MIN", 3600.0 to "1 HR", 4 * 3600.0 to "4 HR", 86400.0 to "1 DAY")
private val modes = listOf(
    HeadingMode.FIXED to "DIALS",
    HeadingMode.PROGRADE to "PRO",
    HeadingMode.RETROGRADE to "RETRO",
    HeadingMode.TOWARD_TARGET to "TO TGT",
    HeadingMode.AWAY_FROM_TARGET to "AWAY",
    HeadingMode.MATCH_TARGET to "MATCH",
)
private val throttles = listOf(0.25 to "25%", 0.5 to "50%", 1.0 to "100%")

/** The bottom tabs; each opens one panel over the map. */
private enum class Tab(val label: String) { SHIP("SHIP"), CONTACTS("CONTACTS"), MANOEUVRE("MANV"), WEAPONS("WEAPONS"), LOG("LOG") }

/**
 * The main game screen, with no scrolling: the full-screen map, a top bar
 * (time, skips, pause reasons), and bottom tabs that each show or hide one
 * panel.
 */
@Composable
fun BridgeScreen(session: GameSession, version: String, onNewGame: () -> Unit) {
    val world = session.world
    var tick by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var lastSkip by remember { mutableStateOf<TimeSkipResult?>(null) }
    var skipStart by remember { mutableStateOf(0.0) }
    var notice by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf<Tab?>(null) }
    val manoeuvre = remember { ManoeuvreState() }
    val scope = rememberCoroutineScope()

    fun changed(problem: String? = null) {
        notice = problem
        tick++
    }

    fun skip(seconds: Double) {
        busy = true
        notice = null
        skipStart = world.time
        scope.launch {
            val r = withContext(Dispatchers.Default) { session.skip(seconds) }
            lastSkip = r
            busy = false
            tick++
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Theme.background)) {
        val area = with(LocalDensity.current) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        // The default view: the star, the ship, and every ship it can see, as they are now.
        fun startView() = MapCamera.framing(visibleShips(world) + world.player.position + Vec3.ZERO, area)
        var camera by remember { mutableStateOf(startView()) }
        MapView(world, camera, { camera = it }, onReset = { camera = startView() },
            tick = tick, showContacts = true)

        // The world is mutable and not observable, so the overlays are rebuilt
        // whenever tick changes (after every skip or order).
        key(tick) {
            Column(Modifier.align(Alignment.TopStart).fillMaxWidth().background(Theme.panel.copy(alpha = 0.85f)).padding(6.dp)) {
                TopBar(session, version, busy, onNewGame, ::skip)
                MessagePanel(world, busy, lastSkip, skipStart, notice)
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                tab?.let { t ->
                    Column(Modifier.fillMaxWidth().background(Theme.panel.copy(alpha = 0.92f)).padding(8.dp)) {
                        when (t) {
                            Tab.SHIP -> ShipPanel(session)
                            Tab.CONTACTS -> ContactsPanel(session, ::changed)
                            Tab.MANOEUVRE -> ManoeuvrePanel(session, manoeuvre, ::changed)
                            Tab.WEAPONS -> WeaponsPanel(session, ::changed)
                            Tab.LOG -> LogPanel(session)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().background(Theme.panel)) {
                    for (t in Tab.entries) ConsoleButton(t.label, selected = t == tab) { tab = if (tab == t) null else t }
                }
            }
        }
    }
}

@Composable
private fun TopBar(session: GameSession, version: String, busy: Boolean, onNewGame: () -> Unit, skip: (Double) -> Unit) {
    val world = session.world
    Row(verticalAlignment = Alignment.CenterVertically) {
        Line("${DisplayFormat.missionTime(world.time)} ", Theme.alert)
        Line(version, Theme.dim)
    }
    Row(Modifier.fillMaxWidth()) {
        for ((seconds, label) in skips) ConsoleButton(label, enabled = !busy && !world.over) { skip(seconds) }
    }
    when {
        world.claimed -> Line("*** SYSTEM CLAIMED ***", Theme.alert)
        world.lost -> Line("*** ${world.player.name.uppercase()} LOST ***", Theme.danger)
    }
    if (world.over) ConsoleButton("NEW GAME") { onNewGame() }
}

/** Ship summary; only damaged modules are listed, so it fits. */
@Composable
private fun ShipPanel(session: GameSession) {
    val player = session.world.player
    val perf = session.performance
    val fuelCap = perf.fuelCapacity(player)
    val energyCap = perf.energyCapacity(player)
    Line("Speed  ${DisplayFormat.speed(player.velocity.length())} (rel. star)  from star ${DisplayFormat.distance(player.position.length())}")
    Line("Mass   %.1f t  accel %.2f m/s²  turn %.1f°/s".format(
        perf.massKg(player) / 1000, perf.accelerationMS2(player), Math.toDegrees(perf.turnRate(player))))
    Line("Fuel   ${DisplayFormat.bar(if (fuelCap > 0) player.fuel / fuelCap else 0.0)} %.2f/%.0f t".format(player.fuel, fuelCap))
    Line("Power  ${DisplayFormat.bar(if (energyCap > 0) player.energy / energyCap else 0.0)} %.0f/%.0f MJ +%.1f MW".format(
        player.energy, energyCap, perf.generatorMW(player)))
    val sensorsOn = player.sensorsOn.joinToString(", ") { SensorPhase.sensorName(it.type.sensor!!.type) }
    Line("Sensor ${sensorsOn.ifEmpty { "none" }}${if (player.emitting) "  EMITTING" else ""}",
        if (player.emitting) Theme.alert else Theme.text)
    Line("Slugs ${player.slugs}/${perf.slugCapacity(player)}   Torpedoes ${player.torpedoes}/${perf.torpedoCapacity(player)}")
    Line(manoeuvreStatus(session), if (player.burn == null) Theme.dim else Theme.alert)
    val damaged = player.modules.filter { it.hitPoints < it.type.hitPoints }
    if (damaged.isEmpty()) Line("All modules intact", Theme.dim)
    for (m in damaged.take(MODULE_LINES)) {
        Line("%-18s %s %4.0f".format(m.type.name.take(18), DisplayFormat.bar(m.hitPoints / m.type.hitPoints), m.hitPoints),
            if (m.destroyed) Theme.danger else Theme.alert)
    }
    if (damaged.size > MODULE_LINES) Line("+${damaged.size - MODULE_LINES} more damaged", Theme.dim)
}

/** The nearest contacts, one line each; tap a vessel to target it. */
@Composable
private fun ContactsPanel(session: GameSession, changed: (String?) -> Unit) {
    val player = session.world.player
    val contacts = player.contacts.current.values.sortedBy { it.distance }
    if (contacts.isEmpty()) Line("No contacts", Theme.dim)
    for (c in contacts.take(CONTACT_LINES)) {
        val targeted = c.id == player.targetId
        val color = if (c.targetsObserver || targeted) Theme.alert else Theme.text
        val range = if (c.bearingOnly) "bearing only" else "${DisplayFormat.distance(c.distance)} ±${DisplayFormat.distance(c.uncertaintyM)}"
        val closing = if (c.bearingOnly) "" else "  ${DisplayFormat.speed(c.closingSpeed)}"
        Line("${if (targeted) ">" else " "} ${c.name.take(14)} $range$closing (${SensorPhase.sensorName(c.sensor)})", color,
            Modifier.fillMaxWidth().clickable(enabled = c.kind.isVessel) { changed(session.orders.setTarget(c.id)) }.padding(vertical = 3.dp))
    }
    if (contacts.size > CONTACT_LINES) Line("+${contacts.size - CONTACT_LINES} further", Theme.dim)
    Line("Tap a contact to target it.", Theme.dim)
}

/** Order inputs kept between panel openings. */
private class ManoeuvreState {
    var mode by mutableStateOf(HeadingMode.FIXED)
    var dials by mutableStateOf<DirectionDials.Angles?>(null)
    var throttle by mutableStateOf(1.0)
    var speed by mutableStateOf<Double?>(null)
    var distance by mutableStateOf<Double?>(null)
}

@Composable
private fun ManoeuvrePanel(session: GameSession, s: ManoeuvreState, changed: (String?) -> Unit) {
    val world = session.world
    val player = world.player
    ChoiceRow(modes.take(3), s.mode) { s.mode = it }
    ChoiceRow(modes.drop(3), s.mode) { s.mode = it }
    DirectionPad(s.dials, session.turnToGo(), dialSize = 96.dp) { s.dials = it; s.mode = HeadingMode.FIXED }
    ChoiceRow(throttles, s.throttle) { s.throttle = it }
    LogSlider("Speed   ", s.speed, 0.01, 300.0, { "%.2f km/s".format(it) }) { s.speed = it }
    LogSlider("Distance", s.distance, 100.0, 1e8, { DisplayFormat.distance(it * 1000) }) { s.distance = it }
    // Dial angles become a direction fixed in space when the order is given.
    fun target() = s.dials?.let { DirectionDials.direction(player.forward, player.up, it) } ?: Vec3.ZERO
    Row(Modifier.fillMaxWidth()) {
        ConsoleButton("FACE", enabled = !world.over) {
            val problem = session.orders.face(s.mode, target())
            if (problem == null) s.dials = null
            changed(problem)
        }
        ConsoleButton("BURN", enabled = !world.over) {
            val problem = session.orders.burn(s.mode, target(), s.throttle, s.speed, s.distance)
            if (problem == null) s.dials = null
            changed(problem)
        }
        ConsoleButton("CANCEL", enabled = player.burn != null) { session.orders.cancelBurn(); changed(null) }
    }
    Line(manoeuvreStatus(session), if (player.burn == null) Theme.dim else Theme.alert)
}

@Composable
private fun WeaponsPanel(session: GameSession, changed: (String?) -> Unit) {
    val world = session.world
    val player = world.player
    Row(Modifier.fillMaxWidth()) {
        ConsoleButton("LASER", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.LASER)) }
        ConsoleButton("CANNON", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.CANNON)) }
        ConsoleButton("TORPEDO", enabled = !world.over) { changed(session.orders.fireNow(WeaponKind.TORPEDO)) }
    }
    Line("Fires at the target during the next time skip.", Theme.dim)
    val rules = player.rules
    Rule("Laser point defence", rules.laserPointDefence) { rules.laserPointDefence = it; changed(null) }
    Rule("Laser auto-fire", rules.laserAutoFire) { rules.laserAutoFire = it; changed(null) }
    Rule("Cannon auto-fire", rules.cannonAutoFire) { rules.cannonAutoFire = it; changed(null) }
    Rule("Torpedo auto-launch", rules.torpedoAutoLaunch) { rules.torpedoAutoLaunch = it; changed(null) }
    val sr = player.sensorRules
    Rule("Radar on (can be detected)", sr.radarActive) { sr.radarActive = it; changed(null) }
    Rule("Lidar on target (can be detected)", sr.lidarActive) { sr.lidarActive = it; changed(null) }
    Rule("Evasive jinking (fuel, heat)", player.evasive) { changed(session.orders.setEvasive(it)) }
}

@Composable
private fun LogPanel(session: GameSession) {
    for (e in session.world.log.events.asReversed().take(LOG_LINES)) {
        val color = when (e.kind) {
            EventKind.DAMAGE_TAKEN, EventKind.SHIP_LOST -> Theme.danger
            EventKind.INFO, EventKind.DAMAGE_DEALT -> Theme.dim
            else -> Theme.alert
        }
        Line("${DisplayFormat.missionTime(e.time)} ${e.message}", color)
    }
}

@Composable
private fun Rule(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(on, onChange, colors = CheckboxDefaults.colors(checkedColor = Theme.text, uncheckedColor = Theme.dim, checkmarkColor = Theme.background))
        Line(label)
    }
}

/** Where the player's sensors place each ship they can see; bearing-only ones dead-reckoned, if ever fixed. */
private fun visibleShips(world: World): List<Vec3> {
    val book = world.player.contacts
    return book.current.values.filter { it.kind.isVessel }.mapNotNull { book.placeOf(it, world.time) }
}

/** Burn and turn state, e.g. "Burn 100% accelerate: turning, 40° to go". */
private fun manoeuvreStatus(session: GameSession): String {
    val p = session.world.player
    val dir = session.headingDirection()
    val toGo = dir?.let { Math.toDegrees(p.forward.angleTo(it)) } ?: 0.0
    val turning = if (toGo > session.data.rules.ships.alignToleranceDeg) "turning, %.0f° to go".format(toGo) else null
    val burn = p.burn
    return when {
        burn != null -> "Burn ${(burn.throttle * 100).toInt()}% ${burn.stage.name.lowercase()}: " +
            (turning ?: if (burn.stage == BurnStage.COAST) "coasting" else "thrusting")
        turning != null -> "Heading $turning"
        else -> "Burn none"
    }
}

private const val LOG_LINES = 10
private const val CONTACT_LINES = 7
private const val MODULE_LINES = 6
