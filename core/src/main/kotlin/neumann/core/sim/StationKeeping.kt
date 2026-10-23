package neumann.core.sim

import neumann.core.world.World

/** Keeps fixed defences and bases at their station next to a planet. */
class StationKeeping {
    fun run(world: World) {
        for (v in world.vessels) {
            val station = v.station ?: continue
            if (!v.alive) continue
            v.position = station.planet.position + station.offset
            v.velocity = station.planet.velocity
        }
    }
}
