package neumann.core.world

enum class EventKind {
    NEW_CONTACT,
    TORPEDO_INBOUND,
    ENTERED_WEAPON_RANGE,
    DAMAGE_TAKEN,
    DAMAGE_DEALT,
    DESTROYED,
    LOW_FUEL,
    LOW_AMMO,
    BURN_COMPLETE,
    SYSTEM_CLAIMED,
    SHIP_LOST,
    INFO,
}

/**
 * Something that happened. [sourceId] is what caused it (used for repeat
 * suppression); [subjectId] is what it happened to. [amount] is damage, for
 * damage events.
 */
class GameEvent(
    val time: Double,
    val kind: EventKind,
    val sourceId: String?,
    val text: String,
    val subjectId: String? = null,
    var amount: Double = 0.0,
) {
    val message: String get() = when {
        amount <= 0.0 -> text
        amount < 1.0 -> "$text: <1 damage"
        else -> "$text: ${amount.toInt()} damage"
    }
}

class EventLog {
    val events = ArrayList<GameEvent>()

    fun add(event: GameEvent) {
        events.add(event)
    }

    /**
     * Adds damage to a recent matching event (same kind, source, and subject,
     * started within MERGE_SECONDS) instead of logging a new one, so a steady
     * beam makes one entry a minute, not one per step.
     */
    fun addMerging(event: GameEvent) {
        for (i in events.indices.reversed().take(MERGE_LOOKBACK)) {
            val e = events[i]
            if (e.kind == event.kind && e.sourceId == event.sourceId && e.subjectId == event.subjectId &&
                event.time - e.time <= MERGE_SECONDS
            ) {
                e.amount += event.amount
                return
            }
        }
        add(event)
    }

    companion object {
        const val MERGE_SECONDS = 60.0
        const val MERGE_LOOKBACK = 10
    }
}
