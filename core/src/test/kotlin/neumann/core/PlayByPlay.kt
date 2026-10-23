package neumann.core

import java.io.File
import kotlin.test.Test

/**
 * Not a rule test: writes a play-by-play of the balance fight (every player
 * command and event) to the file named by the PBP_OUT environment variable.
 * Does nothing when PBP_OUT is not set. See CLAUDE.md.
 */
class PlayByPlay {
    @Test
    fun `write a play-by-play of the balance fight when asked`() {
        val path = System.getenv("PBP_OUT") ?: return
        val out = StringBuilder()
        var logged = 0
        fun t(sec: Double): String { val x = sec.toLong(); return "%dd %02d:%02d:%02d".format(x / 86400, x % 86400 / 3600, x % 3600 / 60, x % 60) }
        val s = FourRaiderFight(
            onCommand = { s, text ->
                for (e in s.world.log.events.drop(logged)) out.append("${t(e.time)}     ${e.kind.name.padEnd(20)} ${e.message}\n")
                logged = s.world.log.events.size
                out.append("${t(s.world.time)}  >> PLAYER: $text\n")
            },
            onSkip = { s, r ->
                val w = s.world
                val p = w.player
                for (e in w.log.events.drop(logged)) if (!e.message.startsWith("Lost contact")) out.append("${t(e.time)}     ${e.kind.name.padEnd(20)} ${e.message}\n")
                logged = w.log.events.size
                val why = if (r.pausedBy.isEmpty()) "full hour" else "paused: " + r.pausedBy.joinToString("; ") { it.message }
                val alive = w.vessels.filter { it !== p && it.alive }
                out.append("${t(w.time)}  -- skip ran %.0f s (%s). Probe integrity %.0f%%, energy %.0f MJ, fuel %.1f t. Raiders left %d, nearest %s\n".format(
                    r.elapsedSeconds, why, p.modules.sumOf { it.hitPoints } / p.modules.sumOf { it.type.hitPoints } * 100, p.energy, p.fuel,
                    alive.size, alive.minByOrNull { (it.position - p.position).length() }?.let { "%,.0f km".format((it.position - p.position).length() / 1000) } ?: "-"))
            },
        ).run()
        val w = s.world
        val byWeapon = w.damage.by(w.player.id)
        out.append("\nRESULT: claimed=${w.claimed} lost=${w.lost} after ${t(w.time)}. Probe integrity %.0f%%. Probe damage by weapon: %s\n".format(
            w.player.modules.sumOf { it.hitPoints } / w.player.modules.sumOf { it.type.hitPoints } * 100,
            byWeapon.entries.joinToString { "${it.key} ${it.value.toLong()} MJ" }))
        File(path).writeText(out.toString())
    }
}
