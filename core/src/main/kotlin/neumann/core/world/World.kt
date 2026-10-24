package neumann.core.world

import neumann.core.data.BeltConfig
import neumann.core.data.GameData
import neumann.core.math.Vec3
import kotlin.random.Random

/** What the player has already been warned about. */
class PlayerWatch {
    val inRangeOf = HashSet<String>()
    var lowFuel = false
    var lowSlugs = false
    var lowTorpedoes = false
}

/** Damage dealt so far, by attacker id and weapon ("laser", "cannon", "torpedo"), in MJ. */
class DamageTally {
    val dealt = HashMap<Pair<String, String>, Double>()

    fun add(attackerId: String, weapon: String, amount: Double) {
        dealt.merge(attackerId to weapon, amount, Double::plus)
    }

    fun by(attackerId: String): Map<String, Double> =
        dealt.filterKeys { it.first == attackerId }.mapKeys { it.key.second }
}

/**
 * The player's entry choice, set on the map with the approach arrow: a
 * direction in space and an entry speed. A null direction heads straight
 * for the star.
 */
data class ApproachVector(val direction: Vec3?, val speedKmS: Double)

/** The whole state of one battle. */
class World(
    val data: GameData,
    val star: Star,
    val planets: List<Planet>,
    val player: Vessel,
    val belt: BeltConfig? = null,
    val seed: Long = 0,
) {
    var time = 0.0
    val vessels = ArrayList<Vessel>()
    val torpedoes = ArrayList<Torpedo>()
    val slugs = ArrayList<Slug>()
    val log = EventLog()
    val playerWatch = PlayerWatch()
    val damage = DamageTally()
    val random = Random(data.rules.sim.seed)
    var claimed = false
    var lost = false
    var lastHistorySample = Double.NEGATIVE_INFINITY
    private var nextId = 0

    val over: Boolean get() = claimed || lost

    fun newId(prefix: String) = "$prefix-${nextId++}"

    fun vessel(id: String?): Vessel? = vessels.firstOrNull { it.id == id && it.alive }

    fun tracked(): List<Tracked> = vessels.filter { it.alive } + torpedoes.filter { it.alive } + slugs.filter { it.alive }

    fun event(kind: EventKind, sourceId: String?, message: String) {
        log.add(GameEvent(time, kind, sourceId, message))
    }
}
