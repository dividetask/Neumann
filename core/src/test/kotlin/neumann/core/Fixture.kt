package neumann.core

import neumann.core.data.EnemyPlacement
import neumann.core.data.GameData
import neumann.core.data.GameDataLoader
import neumann.core.game.AutoPauseMonitor
import neumann.core.game.ScenarioBuilder
import neumann.core.game.TimeSkip
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.sim.StepSizer
import neumann.core.world.ApproachVector
import neumann.core.world.HistorySample
import neumann.core.world.Tracked
import neumann.core.world.Vessel
import neumann.core.world.World
import java.io.File

/** Test worlds built from the real data files, with one passive enemy ("dummy"). */
object Fixture {
    val data: GameData by lazy {
        GameDataLoader { File(System.getProperty("neumann.dataDir"), it).readText() }.load()
    }

    /** One enemy raider lying in ambush that never springs: it never moves or targets. */
    fun world(): World {
        val dummy = EnemyPlacement("Dummy", "raider", orbits = "Shale", offsetKm = listOf(0.0, 200000.0, 0.0), ambush = true, ambushRangeKm = 0.0)
        val d = data.copy(scenario = data.scenario.copy(enemies = listOf(dummy)))
        return ScenarioBuilder(d).build(ApproachVector(null, 15.0))
    }

    fun dummy(world: World): Vessel = world.vessels.first { it.name == "Dummy" }

    /** Moves an object and resets its history, as if it had always been there. */
    fun place(world: World, t: Tracked, position: Vec3, velocity: Vec3, forward: Vec3? = null) {
        if (t is Vessel && forward != null) {
            t.forward = forward.normalized()
            t.up = neumann.core.math.Frame.upFor(t.forward)
        }
        when (t) {
            is Vessel -> { t.position = position; t.velocity = velocity; t.previousPosition = position }
            is neumann.core.world.Torpedo -> { t.position = position; t.velocity = velocity; t.previousPosition = position }
        }
        t.history.samples.clear()
        t.history.record(HistorySample(world.time, position, velocity, false))
    }

    /** A spot 1.3 AU from the star, away from every planet. */
    val openSpace = Vec3(-1.3 * Units.AU, 0.0, 0.0)

    fun timeSkip(world: World) =
        TimeSkip(SimulationStep(world.data), StepSizer(world.data.rules.sim), AutoPauseMonitor(world.data.rules.autoPause),
            world.data.rules.autoPause.groupSeconds)

    /** Skips until [seconds] have passed, ignoring pauses (stops if the battle ends). */
    fun runFor(world: World, seconds: Double) {
        val skip = timeSkip(world)
        val end = world.time + seconds
        while (world.time < end - 1e-9 && !world.over) skip.run(world, end - world.time)
    }

    /** Total damage a vessel's modules have taken. */
    fun damageTaken(v: Vessel) = v.modules.sumOf { it.type.hitPoints - it.hitPoints }

    val performance by lazy { SimulationStep(data).performance }
}
