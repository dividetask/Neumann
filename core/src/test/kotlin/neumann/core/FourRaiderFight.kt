package neumann.core

import neumann.core.data.EnemyPlacement
import neumann.core.game.GameSession
import neumann.core.game.TimeSkipResult
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.ApproachVector
import neumann.core.world.HeadingMode

/** How the probe spends its torpedoes in the balance fight. */
enum class TorpedoStrategy {
    /** The automatic salvo rule: enough to destroy the target, or all if that at least hits. */
    AUTO,
    /** Once every raider is in range: at least enough to hit each one (nearest first), the rest spread evenly. */
    EVEN,
    /** One raider at a time, nearest first: enough to destroy it (or all, if that at least hits); the next when that salvo is spent. */
    CONCENTRATE,
}

/**
 * The balance fight: four raiders 300,000 km away, closing; the probe sits
 * still and plays with no strategy (lasers and cannons on the nearest
 * enemy; face it; 1-hour time skips), spending torpedoes by [strategy].
 * [onCommand] hears each player command; [onSkip] each skip's result.
 */
class FourRaiderFight(
    private val onCommand: (GameSession, String) -> Unit = { _, _ -> },
    private val onSkip: (GameSession, TimeSkipResult) -> Unit = { _, _ -> },
    private val strategy: TorpedoStrategy = TorpedoStrategy.AUTO,
) {
    fun run(): GameSession {
        val raiders = (1..4).map { EnemyPlacement("Raider $it", "raider", orbits = "Shale", offsetKm = listOf(0.0, 200000.0 + it, 0.0)) }
        val data = Fixture.data.copy(scenario = Fixture.data.scenario.copy(enemies = raiders))
        val s = GameSession(data, ApproachVector(null, 15.0))
        val w = s.world
        val p = w.player
        Fixture.place(w, p, Fixture.openSpace, Vec3.ZERO)
        w.vessels.filter { it !== p }.forEachIndexed { i, r ->
            val at = Fixture.openSpace + Vec3(300_000.0, (i - 1.5) * 20_000.0, 0.0) * Units.KM
            Fixture.place(w, r, at, Vec3(-5_000.0, 0.0, 0.0), forward = Vec3(-1.0, 0.0, 0.0))
        }
        p.contacts.current.clear() // forget the scan from before the raiders were placed
        p.contacts.everSeen.clear()
        SimulationStep(data).sensors.run(w)

        p.rules.apply { laserAutoFire = true; cannonAutoFire = true; torpedoAutoLaunch = strategy == TorpedoStrategy.AUTO }
        onCommand(s, "weapon rules: lasers and cannons on the target" +
            (if (strategy == TorpedoStrategy.AUTO) ", torpedo auto-launch ON" else ", torpedoes by hand ($strategy)") + " (point defence already ON)")
        val torpedoes = TorpedoOrders(s, onCommand)
        while (!w.over && w.time < 3 * 86400.0) {
            p.contacts.current.values.filter { it.kind.isVessel }.minByOrNull { it.distance }?.let {
                if (p.targetId != it.id) {
                    s.orders.setTarget(it.id)
                    onCommand(s, "target ${it.name} (%,.0f km, %s)".format(it.distance / 1000, it.sensor.name.lowercase()))
                }
                if (p.heading?.mode != HeadingMode.TOWARD_TARGET) {
                    s.orders.face(HeadingMode.TOWARD_TARGET)
                    onCommand(s, "face toward target")
                }
            }
            when (strategy) {
                TorpedoStrategy.AUTO -> {}
                TorpedoStrategy.EVEN -> torpedoes.even()
                TorpedoStrategy.CONCENTRATE -> torpedoes.concentrate()
            }
            onCommand(s, "time skip 1 hour")
            onSkip(s, s.skip(3600.0))
        }
        return s
    }
}

/** Torpedo launch orders for the hand-fired strategies, using the probe's own salvo estimates. */
private class TorpedoOrders(private val s: GameSession, private val onCommand: (GameSession, String) -> Unit) {
    private val p = s.world.player
    private val launchRange = p.working(neumann.core.world.ModuleKind.MISSILE_BAY).maxOf { it.type.missileBay!!.launchRangeKm } * Units.KM
    private val launcher = SimulationStep(s.data)

    private fun known() = p.contacts.current.values.filter { it.kind.isVessel && !it.bearingOnly }.sortedBy { it.distance }

    /** Launch [n] at [id] now (a tiny step lets the launch happen before the next order). */
    private fun launch(n: Int, id: String) {
        if (n <= 0 || s.orders.launchTorpedoes(n, id) != null) return
        onCommand(s, "launch $n torpedoes at ${p.contacts[id]!!.name}")
        launcher.run(s.world, 0.001)
    }

    fun even() {
        val all = known()
        if (p.torpedoes == 0 || all.isEmpty() || all.any { it.distance > launchRange }) return
        var left = p.torpedoes
        val share = LinkedHashMap<String, Int>()
        for (c in all) {
            val toHit = s.engagement(c.id)?.salvo?.toHit ?: continue
            if (left >= toHit) { share[c.id] = toHit; left -= toHit }
        }
        if (share.isEmpty()) return
        var i = 0
        val ids = share.keys.toList()
        while (left > 0) { share[ids[i % ids.size]] = share.getValue(ids[i % ids.size]) + 1; left--; i++ }
        for ((id, n) in share) launch(n, id)
    }

    fun concentrate() {
        if (p.torpedoes == 0 || s.world.torpedoes.any { it.alive && it.ownerId == p.id }) return
        val target = known().firstOrNull { it.distance <= launchRange } ?: return
        val e = s.engagement(target.id)?.salvo ?: return
        if (p.torpedoes < e.toHit) return
        launch(minOf(p.torpedoes, e.toDestroy), target.id)
    }
}

