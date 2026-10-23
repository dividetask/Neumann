package neumann.core

import neumann.core.data.EnemyPlacement
import neumann.core.game.GameSession
import neumann.core.game.TimeSkipResult
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.ApproachVector
import neumann.core.world.HeadingMode

/**
 * The balance fight: four raiders 300,000 km away, closing; the probe sits
 * still and plays with no strategy (every weapon rule on; target the nearest
 * enemy and face it; 1-hour time skips). [onCommand] hears each player
 * command; [onSkip] each skip's result.
 */
class FourRaiderFight(
    private val onCommand: (GameSession, String) -> Unit = { _, _ -> },
    private val onSkip: (GameSession, TimeSkipResult) -> Unit = { _, _ -> },
) {
    fun run(): GameSession {
        val raiders = (1..4).map { EnemyPlacement("Raider $it", "raider", orbits = "Shale", offsetKm = listOf(0.0, 200000.0 + it, 0.0)) }
        val data = Fixture.data.copy(scenario = Fixture.data.scenario.copy(enemies = raiders))
        val s = GameSession(data, ApproachVector(0.0, 0.0, 15.0))
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

        p.rules.apply { laserAutoFire = true; cannonAutoFire = true; torpedoAutoLaunch = true }
        onCommand(s, "weapon rules: laser auto-fire ON, cannon auto-fire ON, torpedo auto-launch ON (point defence already ON)")
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
            onCommand(s, "time skip 1 hour")
            onSkip(s, s.skip(3600.0))
        }
        return s
    }
}
