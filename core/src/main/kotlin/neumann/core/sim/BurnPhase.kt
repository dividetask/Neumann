package neumann.core.sim

import neumann.core.world.BurnDirection
import neumann.core.world.EventKind
import neumann.core.world.PartKind
import neumann.core.world.Vessel
import neumann.core.world.World
import kotlin.math.min

/** Applies burns: thrust, fuel use, and the end of a burn. */
class BurnPhase(private val resolver: BurnDirectionResolver) {
    fun run(world: World, dt: Double) {
        for (v in world.vessels) {
            v.burning = false
            if (v.alive) burn(world, v, dt)
        }
    }

    private fun burn(world: World, v: Vessel, dt: Double) {
        val burn = v.burn ?: return
        val drive = v.shipClass.drive
        if (drive == null || !v.working(PartKind.ENGINE)) return end(world, v, "engine out")
        if (v.fuel <= 0.0) return end(world, v, "out of fuel")
        val dir = resolver.resolve(v, burn) ?: return end(world, v,
            if (burn.direction == BurnDirection.MATCH_TARGET) "velocity matched" else "no direction (no target?)")

        var dv = drive.accelerationMS2 * burn.throttle * dt
        var finished = false
        if (burn.direction == BurnDirection.MATCH_TARGET) {
            val target = v.contacts[v.targetId]!!
            val rel = (target.observedVelocity - v.velocity).length()
            if (rel <= dv) {
                dv = rel
                finished = true
            }
        }
        val fuelPerMS = drive.fuelPerKmS / 1000.0
        dv = min(dv, v.fuel / fuelPerMS)
        v.velocity += dir * dv
        v.fuel -= dv * fuelPerMS
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
        if (v === world.player) world.event(EventKind.BURN_COMPLETE, v.id, "Burn ended: $reason")
    }
}
