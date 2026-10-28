package neumann.app

import androidx.compose.runtime.Composable
import neumann.core.game.TimeSkipResult
import neumann.core.world.EventKind
import neumann.core.world.World

/**
 * The most recent messages, always on screen. The first line says how the
 * last time skip ended; after a pause every reason follows. The rest are
 * the newest log entries, bright if they came in the last skip.
 */
@Composable
fun MessagePanel(world: World, busy: Boolean, lastSkip: TimeSkipResult?, skipStart: Double, notice: String?) {
    var lines = 0
    notice?.let { Line(it, Theme.danger); lines++ }
    when {
        busy -> Line("Running...", Theme.dim)
        lastSkip == null -> Line("No time skip yet.", Theme.dim)
        lastSkip.pausedBy.isEmpty() -> Line("Skipped ${DisplayFormat.duration(lastSkip.elapsedSeconds)}: nothing to pause for", Theme.text)
        else -> {
            Line("PAUSED after ${DisplayFormat.duration(lastSkip.elapsedSeconds)}:", Theme.alert)
            for (e in lastSkip.pausedBy.take(PAUSE_LINES)) { Line("  ${e.message}", Theme.alert); lines++ }
            val more = lastSkip.pausedBy.size - PAUSE_LINES
            if (more > 0) { Line("  +$more more (LOG)", Theme.alert); lines++ }
        }
    }
    lines++
    if (busy) return
    val shown = lastSkip?.pausedBy.orEmpty().toSet()
    for (e in world.log.events.asReversed().filter { it !in shown }.take(maxOf(0, MESSAGE_LINES - lines))) {
        val fresh = lastSkip != null && e.time >= skipStart
        val color = when {
            !fresh -> Theme.dim
            e.kind == EventKind.DAMAGE_TAKEN || e.kind == EventKind.SHIP_LOST -> Theme.danger
            else -> Theme.text
        }
        Line("${DisplayFormat.missionTime(e.time)} ${e.message}", color)
    }
}

private const val MESSAGE_LINES = 5

/** Enough for a full torpedo salvo; any beyond are in LOG. */
private const val PAUSE_LINES = 6
