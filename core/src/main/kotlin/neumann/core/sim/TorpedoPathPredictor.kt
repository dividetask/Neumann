package neumann.core.sim

import neumann.core.data.TorpedoConfig
import neumann.core.math.Vec3
import neumann.core.world.Contact
import neumann.core.world.Vessel

/**
 * Where to aim a slug at a torpedo coming at [shooter]. A torpedo steers by a
 * fixed law, so if the shooter keeps its course its path can be predicted:
 * it accelerates toward the shooter at its [TorpedoConfig.accelerationMS2].
 * Steps that path forward until a slug at [slugSpeed] from the shooter could
 * meet it, and aims there; null if it would arrive first.
 */
class TorpedoPathPredictor(private val torpedo: TorpedoConfig) {
    fun aim(shooter: Vessel, contact: Contact, slugSpeed: Double): Vec3? {
        var r = contact.predictedPosition - shooter.position
        var u = contact.observedVelocity - shooter.velocity
        var t = 0.0
        while (t < HORIZON_S) {
            if (r.length() <= slugSpeed * t) return r.normalized()
            if (r.length() < torpedo.blastRadiusKm * 1000.0) return null
            u -= r.normalized() * (torpedo.accelerationMS2 * STEP_S)
            r += u * STEP_S
            t += STEP_S
        }
        return null
    }

    companion object {
        const val STEP_S = 0.25
        const val HORIZON_S = 600.0
    }
}
