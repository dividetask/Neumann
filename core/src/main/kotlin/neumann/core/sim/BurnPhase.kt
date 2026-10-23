package neumann.core.sim

import neumann.core.data.ShipRules
import neumann.core.math.Units
import neumann.core.world.EventKind
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.min

/**
 * Thrusts along the front once it is within the align tolerance of the
 * heading. Uses fuel; ends the burn when its time, fuel, or engines run out.
 */
class BurnPhase(
    private val resolver: HeadingResolver,
    private val performance: ShipPerformance,
    private val rules: ShipRules,
) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            v.burning = false
            if (v.alive) burn(world, v, dt)
        }
    }

    private fun burn(world: World, v: Vessel, dt: Double) {
        val burn = v.burn ?: return
        if (!v.has(ModuleKind.PROPULSION)) return end(world, v, "no working propulsion")
        if (v.fuel <= 0.0) return end(world, v, "out of fuel")
        val match = v.heading?.mode == HeadingMode.MATCH_TARGET
        val dir = resolver.resolve(v) ?: return end(world, v, if (match) "velocity matched" else "no direction (no target?)")
        if (v.forward.angleTo(dir) > rules.alignToleranceDeg * Units.DEGREE) return

        val fullDv = performance.thrustN(v) * burn.throttle / performance.massKg(v) * dt
        if (fullDv <= 0.0) return
        var dv = fullDv
        var finished = false
        if (match) {
            val rel = (v.contacts[v.targetId]!!.observedVelocity - v.velocity).length()
            if (rel <= dv) {
                dv = rel
                finished = true
            }
        }
        var fuelUsed = performance.fuelKgS(v) * burn.throttle * dt / 1000.0 * (dv / fullDv)
        if (fuelUsed > v.fuel) {
            dv *= v.fuel / fuelUsed
            fuelUsed = v.fuel
        }
        v.velocity += v.forward * dv
        v.fuel -= fuelUsed
        v.burning = true
        burn.remainingSeconds -= dt
        when {
            finished -> end(world, v, "velocity matched")
            v.fuel <= 0.0 -> end(world, v, "out of fuel")
            burn.remainingSeconds <= 1e-9 -> end(world, v, "complete")
        }
    }

    private fun end(world: World, v: Vessel, reason: String) {
        v.burn = null
        if (v.heading?.mode == HeadingMode.MATCH_TARGET) v.heading = Heading(HeadingMode.FIXED, v.forward)
        if (v === world.player) world.event(EventKind.BURN_COMPLETE, v.id, "Burn ended: $reason")
    }
}
