package neumann.core.world

/**
 * How the probe arrived: from a star [originLy] light years away, crossed at
 * [cruiseMS] in [tripYears], now moving at [speedMS] (less than cruise if it
 * had already started braking) with [fuelT] tonnes left.
 */
class Arrival(val originLy: Double, val cruiseMS: Double, val tripYears: Double, val speedMS: Double, val fuelT: Double)
