package neumann.core.sim

import neumann.core.math.Frame
import neumann.core.world.ModuleKind
import neumann.core.world.World
import kotlin.math.min

/** Turns vessels toward their heading at their turn rate. Needs working propulsion. */
class TurnPhase(private val resolver: HeadingResolver, private val performance: ShipPerformance) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || v.station != null || !v.has(ModuleKind.PROPULSION)) continue
            val dir = resolver.resolve(v) ?: continue
            val angle = v.forward.angleTo(dir)
            if (angle < 1e-9) continue
            var axis = v.forward.cross(dir).normalized()
            if (axis.lengthSquared() == 0.0) axis = v.up
            val turn = min(angle, performance.turnRate(v) * dt)
            val f = Frame.rotate(v.forward, axis, turn).normalized()
            val u = Frame.rotate(v.up, axis, turn)
            v.forward = f
            v.up = (u - f * u.dot(f)).normalized()
        }
    }
}
