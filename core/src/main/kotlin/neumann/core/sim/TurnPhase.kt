package neumann.core.sim

import neumann.core.data.ShipRules
import neumann.core.math.Frame
import neumann.core.math.Units
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.abs
import kotlin.math.min

/**
 * Turns vessels toward their heading at their turn rate: the front first,
 * then any roll. Needs working propulsion and fuel; uses a little fuel.
 */
class TurnPhase(
    private val resolver: HeadingResolver,
    private val performance: ShipPerformance,
    private val rules: ShipRules,
) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            if (!v.alive || v.station != null || !v.has(ModuleKind.PROPULSION) || v.fuel <= 0.0) continue
            val dir = resolver.resolve(v) ?: continue
            var budget = performance.turnRate(v) * dt
            val turned = turnFront(v, dir, budget)
            budget -= turned
            val rolled = if (v.heading?.mode == HeadingMode.FIXED) roll(v, budget) else 0.0
            val degrees = (turned + rolled) / Units.DEGREE
            v.fuel = (v.fuel - rules.turnFuelKgPerTonneDeg * performance.massKg(v) / 1000.0 * degrees / 1000.0).coerceAtLeast(0.0)
        }
    }

    /** Turns the front toward [dir] by up to [budget] radians; returns the angle turned. */
    private fun turnFront(v: Vessel, dir: neumann.core.math.Vec3, budget: Double): Double {
        val angle = v.forward.angleTo(dir)
        if (angle < 1e-9) return 0.0
        var axis = v.forward.cross(dir).normalized()
        if (axis.lengthSquared() == 0.0) axis = v.up
        val turn = min(angle, budget)
        val f = Frame.rotate(v.forward, axis, turn).normalized()
        val u = Frame.rotate(v.up, axis, turn)
        v.forward = f
        v.up = (u - f * u.dot(f)).normalized()
        return turn
    }

    /** Rolls toward the heading's up by up to [budget] radians; returns the angle rolled. */
    private fun roll(v: Vessel, budget: Double): Double {
        val target = v.heading?.up ?: return 0.0
        if (budget <= 0.0) return 0.0
        val tu = target - v.forward * target.dot(v.forward)
        if (tu.lengthSquared() == 0.0) return 0.0
        val angle = v.up.angleTo(tu)
        if (angle < 1e-9) return 0.0
        val sign = if (v.up.cross(tu).dot(v.forward) >= 0) 1.0 else -1.0
        val r = min(abs(angle), budget)
        v.up = Frame.rotate(v.up, v.forward, sign * r).normalized()
        return r
    }
}
