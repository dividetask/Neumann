package neumann.app

import neumann.core.math.Units
import kotlin.math.abs

/** Distances, speeds, and times as display text. */
object DisplayFormat {
    fun distance(m: Double): String = when {
        m < 1e4 -> "%.0f m".format(m)
        m < 1e9 -> "%,.0f km".format(m / Units.KM)
        m < 0.1 * Units.AU -> "%.2f Mkm".format(m / 1e9)
        else -> "%.3f AU".format(m / Units.AU)
    }

    fun speed(ms: Double): String = if (abs(ms) < 1000) "%.0f m/s".format(ms) else "%.2f km/s".format(ms / 1000)

    fun missionTime(s: Double): String {
        val t = s.toLong()
        return "T+ %dd %02d:%02d:%02d".format(t / 86400, t % 86400 / 3600, t % 3600 / 60, t % 60)
    }

    fun duration(s: Double): String = when {
        s < 60 -> "%.1f s".format(s)
        s < 3600 -> "%.1f min".format(s / 60)
        s < 86400 -> "%.1f h".format(s / 3600)
        else -> "%.1f d".format(s / 86400)
    }

    /** A text bar like "███████░░░". */
    fun bar(fraction: Double, width: Int = 10): String {
        val filled = (fraction.coerceIn(0.0, 1.0) * width).toInt()
        return "█".repeat(filled) + "░".repeat(width - filled)
    }
}
