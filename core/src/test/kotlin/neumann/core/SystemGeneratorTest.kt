package neumann.core

import neumann.core.data.GameDataLoader
import neumann.core.game.ScenarioBuilder
import neumann.core.game.SystemGenerator
import neumann.core.game.TrajectoryPredictor
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.sim.SimulationStep
import neumann.core.world.ApproachVector
import neumann.core.world.Faction
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemGeneratorTest {
    private val data = Fixture.data
    private val gen = SystemGenerator(data)
    private val ranges = data.generation

    @Test
    fun `the same seed gives the same system`() {
        assertEquals(gen.generate(42), gen.generate(42))
        assertTrue(gen.generate(42) != gen.generate(43))
    }

    @Test
    fun `generated systems stay within the ranges and are valid`() {
        val loader = GameDataLoader { File(System.getProperty("neumann.dataDir"), it).readText() }
        for (seed in 1L..200L) {
            val s = gen.generate(seed)
            loader.checkScenario(data.designs, s)
            val planets = s.planets.filter { it.parent == null }
            assertTrue(planets.size in ranges.planets.countMin..ranges.planets.countMax, "seed $seed: ${planets.size} planets")
            assertTrue(s.star.massSolar in ranges.star.massSolarMin..ranges.star.massSolarMax)
            for (p in planets) {
                val kind = if (p.orbitAu < ranges.planets.rockyLimitAu) ranges.planets.rocky else ranges.planets.giant
                val moons = s.planets.count { it.parent == p.name }
                assertTrue(moons <= kind.moonsMax, "seed $seed: ${p.name} has $moons moons")
            }
            assertTrue(planets.all { it.orbitAu <= ranges.planets.maxOrbitAu }, "seed $seed: as compact as real systems")
            assertEquals(planets.maxOf { it.orbitAu } + ranges.entry.detectionRangeMultiple * neumann.core.game.DetectionReach(data).au(),
                s.entry.radiusAu, 1e-9, "seed $seed: arrives far out, beyond every enemy's detection")
            assertEquals(1, s.enemies.count { it.design == "base" })
            assertEquals(1, s.enemies.count { it.design == "defencePlatform" })
            assertEquals(ranges.enemies.raiderCount, s.enemies.count { it.design == "raider" })
        }
    }

    @Test
    fun `the furthest an enemy can detect the probe is a telescope against it burning`() {
        assertEquals(4.0, neumann.core.game.DetectionReach(data).au(), 1e-9)
    }

    @Test
    fun `enemies in a generated system stay in orbit instead of crashing`() {
        for (seed in 1L..20L) {
            val w = ScenarioBuilder(data).build(gen.generate(seed), ApproachVector(null, 15.0))
            val enemies = w.vessels.filter { it.faction == Faction.ENEMY }
            enemies.forEach { it.ambush = true; it.ambushRange = 0.0 } // keep the AI still
            val step = SimulationStep(data)
            repeat(10 * 24) { step.run(w, 3600.0 / 6); step.run(w, 3600.0 / 6); step.run(w, 3600.0 / 6) }
            assertTrue(enemies.all { it.alive }, "seed $seed: " + enemies.filter { !it.alive }.joinToString { it.name })
        }
    }

    @Test
    fun `the predicted path coasts straight in empty space and ends at a body it hits`() {
        val w = ScenarioBuilder(data).build(gen.generate(7), ApproachVector(null, 15.0))
        val predictor = TrajectoryPredictor(w.star, w.planets)
        // Aimed straight at the star from close in: it hits.
        val start = Vec3(0.1 * Units.AU, 0.0, 0.0)
        val hit = predictor.predict(start, Vec3(-50_000.0, 0.0, 0.0), 30 * 86400.0, 600.0)
        assertTrue(hit.hits)
        // Gravity bends the path toward the star.
        val miss = predictor.predict(Vec3(1.0 * Units.AU, 0.0, 5.0 * Units.AU), Vec3(0.0, 20_000.0, 0.0), 86400.0, 600.0)
        assertTrue(miss.points.last().z < 5.0 * Units.AU)
    }
}
