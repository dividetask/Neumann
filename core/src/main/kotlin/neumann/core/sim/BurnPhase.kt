package neumann.core.sim

import neumann.core.data.ShipRules
import neumann.core.math.Units
import neumann.core.math.Vec3
import neumann.core.world.Burn
import neumann.core.world.BurnStage
import neumann.core.world.EventKind
import neumann.core.world.Heading
import neumann.core.world.HeadingMode
import neumann.core.world.ModuleKind
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.min

/**
 * Runs burns: accelerate toward the desired speed (or halfway), coast, then
 * brake to a stop at the desired distance. Thrusts only when the front is
 * within the align tolerance of the way it should face; uses fuel only while
 * thrusting.
 */
class BurnPhase(
    private val resolver: HeadingResolver,
    private val performance: ShipPerformance,
    private val rules: ShipRules,
) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            v.burning = false
            if (v.active) burn(world, v, dt)
        }
    }

    private fun burn(world: World, v: Vessel, dt: Double) {
        val burn = v.burn ?: return
        if (!v.has(ModuleKind.PROPULSION)) return end(world, v, "no working propulsion")
        if (v.fuel <= 0.0) return end(world, v, "out of fuel")
        val match = v.heading?.mode == HeadingMode.MATCH_TARGET
        val dir = resolver.resolve(v) ?: return end(world, v, if (match) "velocity matched" else "no direction (no target?)")
        val accel = performance.accelerationMS2(v, burn.drive)
        if (accel <= 0.0) return

        // Progress along the burn direction, seen from where the ship was when the order was given.
        val speed = (v.velocity - burn.startVelocity).dot(dir)
        val travelled = (v.position - burn.startPosition - burn.startVelocity * (world.time - burn.startTime)).dot(dir)

        val wanted: Double = if (match) {
            val rel = (v.contacts[v.targetId]!!.observedVelocity - v.velocity).length()
            if (rel < 1e-6) return end(world, v, "velocity matched")
            rel
        } else {
            advanceStage(burn, speed, travelled, accel, v, dt)
            when (burn.stage) {
                BurnStage.ACCELERATE -> burn.targetSpeed?.let { it - speed } ?: Double.MAX_VALUE
                BurnStage.COAST -> 0.0
                BurnStage.BRAKE -> speed
            }
        }
        if (burn.stage == BurnStage.BRAKE && speed <= 1e-6) return end(world, v, "stopped after ${(travelled / Units.KM).toLong()} km")
        // Within a centimetre a second is there: gravity's nudges would otherwise keep a gentle burn going for ever.
        if (burn.stage == BurnStage.ACCELERATE && burn.targetDistance == null && wanted <= SPEED_TOLERANCE_MS) {
            return end(world, v, "reached ${"%.2f".format(speed / Units.KM)} km/s")
        }
        if (wanted <= 0.0) return
        val facing = resolver.facing(v) ?: return
        if (v.forward.angleTo(facing) > rules.alignToleranceDeg * Units.DEGREE) return
        thrust(v, burn, min(accel * dt, wanted), accel * dt, dt)
        if (v.fuel <= 0.0) end(world, v, "out of fuel")
    }

    /** Moves from accelerating to coasting to braking as speed and distance require. */
    private fun advanceStage(burn: Burn, speed: Double, travelled: Double, accel: Double, v: Vessel, dt: Double) {
        val distance = burn.targetDistance
        val brakeNow = distance != null && run {
            // Stopping distance, plus coasting while turning around, plus one step of margin.
            val turnTime = Math.PI / performance.turnRate(v).coerceAtLeast(1e-6)
            distance - travelled <= speed * speed / (2 * accel) + speed * (turnTime + dt)
        }
        burn.stage = when (burn.stage) {
            BurnStage.ACCELERATE -> when {
                distance != null && (travelled >= distance / 2 || brakeNow) -> BurnStage.BRAKE
                burn.targetSpeed != null && speed >= burn.targetSpeed - 1e-6 && distance != null -> BurnStage.COAST
                else -> BurnStage.ACCELERATE
            }
            BurnStage.COAST -> if (brakeNow) BurnStage.BRAKE else BurnStage.COAST
            BurnStage.BRAKE -> BurnStage.BRAKE
        }
    }

    private fun thrust(v: Vessel, burn: Burn, dv: Double, fullDv: Double, dt: Double) {
        var change = dv
        var fuelUsed = performance.fuelKgS(v, burn.drive) * dt / 1000.0 * (dv / fullDv)
        if (fuelUsed > v.fuel) {
            change *= v.fuel / fuelUsed
            fuelUsed = v.fuel
        }
        v.velocity += v.forward * change
        v.fuel -= fuelUsed
        v.burning = true
    }

    private fun end(world: World, v: Vessel, reason: String) {
        val braking = v.burn?.stage == BurnStage.BRAKE
        v.burn = null
        if (v.heading?.mode == HeadingMode.MATCH_TARGET || braking) v.heading = Heading(HeadingMode.FIXED, v.forward)
        if (v === world.player) world.event(EventKind.BURN_COMPLETE, v.id, "Burn ended: $reason")
    }
}

private const val SPEED_TOLERANCE_MS = 0.01
