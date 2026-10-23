package neumann.core.sim

import neumann.core.world.World
import kotlin.math.min

/** Electrical engines charge the batteries; scanners draw their power. */
class PowerPhase(private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive) continue
            v.energy = min(performance.energyCapacity(v), v.energy + performance.generatorMW(v) * dt)
            val draw = performance.scannerDrawMW(v) * dt
            v.scannersPowered = v.energy >= draw
            if (v.scannersPowered) v.energy -= draw
        }
    }
}
