# Game Design

## Summary

The player commands one sublight ship, a von Neumann probe, against AI
opponents. They choose a star to travel to, choose an approach vector into
its system, and fight whatever is there. Ship state carries over from battle
to battle.

## First version scope

The first version is a single battle:

A. The player arrives at one made-up star system.
B. The player picks an approach vector.
C. The player gives orders and uses time skips until the system is claimed
   (win) or the ship is lost.

Star map, travel, mineral estimates, saving ship state between battles, and
probe copies are **Pending** for later versions.

## Core loop (full game)

A. On the star map, the player picks a destination star, helped by its
   mineral estimate.
B. The ship travels there (see [Travel](#travel)).
C. On arrival, the player picks an approach vector into the system.
D. Inside the system, the player gives orders and advances time with
   time skips until the system is claimed, fled, or the ship is lost.
E. Back to A, keeping the ship's state.

## Win and loss

- **First version:** the player wins by claiming the system.
  **(interpretation)** A system is claimed when every enemy ship, fixed
  defence, and base in it is destroyed.
- **Later versions:** there is a limited number of unclaimed stars, all within
  a random radius. The game is won by destroying all enemy players.
- The player loses when their ship's hull is destroyed, or the ship hits a
  star or planet.

## Time skips

Controls: **1 minute**, **1 hour**, **4 hours**, **1 day**.

A time skip moves the game forward until the chosen time has passed, or until
an auto-pause event happens, whichever comes first.

The game never makes the player wait in real time: a time skip resolves at
once.

### Auto-pause events

- A new contact is detected.
- A torpedo is launched at the player's ship (the player detects a torpedo
  aimed at them).
- The player's ship enters an enemy's weapon range.
- The player's ship takes damage.
- A target is destroyed.
- Fuel or ammunition runs low (drops below `lowSupplyFraction`).
- **(interpretation)** A burn order finishes.
- The system is claimed, or the player's ship is lost.

**(interpretation)** Repeat suppression: after an event pauses a skip, the
same kind of event from the same source does not pause again for
`repeatSuppressionSeconds`. Without this, a steady laser hit would stop every
skip after a few seconds. It is still logged.

## Orders

Between time skips the player can:

A. **Set a burn:** direction, throttle (fraction of full thrust), and
   duration. **(interpretation)** Directions are presets: prograde,
   retrograde, toward target, away from target, and match target velocity
   (burns against the velocity relative to the target and stops itself when
   that reaches zero).
B. **Pick a target** from the contact list.
C. **Fire a weapon now** at the target. **(interpretation)** The cannon
   fires one slug and the torpedo launcher one torpedo, as soon as each is
   ready. The laser fires a burst of `burstSeconds`, and only if the target is
   in range.
D. **Set weapon rules**: laser point defence, laser auto-fire at target,
   cannon auto-fire at target, torpedo auto-launch at target. Each fires only
   when its weapon is in range.

## Space

- The game is in **3D**.
- **(interpretation)** An approach vector is a bearing from the star
  (azimuth and elevation) plus an entry speed. The ship starts on the entry
  sphere (`entryRadius` from the star) at that bearing, heading toward the
  star.
- **Realistic scale.** Distances are real: a system is billions of km across,
  and light takes minutes to cross it.
- **Light delay.** Sensors show where a contact was when its light left it,
  not where it is now. An object whose light has not yet reached the observer
  is not seen.

## Physics

- Real Newtonian movement. A ship keeps its velocity until it burns its
  engine; to stop, it turns around and burns against its motion.
- Burns use fuel. **(interpretation)** Fuel used is proportional to the
  change in speed (`fuelPerKmPerSecond`); the rocket equation is not modelled.
- The gravity of stars and planets affects ships, torpedoes, and slugs.
  **(interpretation)** Stars and planets follow fixed circular orbits and are
  not moved by anything.
- **(interpretation)** Fixed defences and bases hold station next to a
  planet: they move with it and are not moved by gravity or burns.

## Sensors

- Each ship has a sensor range. An object is detected when its light-delayed
  distance is within `sensorRange × signature`.
- An object's signature is higher while it is burning its engine.
  **(interpretation)** A ship lying in ambush is not burning, so it is hard to
  see until close.

## Damage

The ship has damageable parts. Each hit lands on one part, chosen at random by
the part's hit weight. Damage beyond what a part has left goes to the hull.

| Part | When destroyed |
|---|---|
| Hull | The ship is destroyed. |
| Engine | No burns. |
| Sensors | Sensor range drops to `damagedSensorFraction`. |
| Laser | Laser cannot fire. |
| Cannon | Cannon cannot fire. |
| Torpedo launcher | Torpedoes cannot launch. |
| Factory | No mining or manufacturing. |
| Fuel tank | All fuel is lost. |

## Star systems

A star system may contain any mix of:

- Fixed defences.
- Enemy ships (one ship or a fleet).
- A permanent base.
- An ambush: ships waiting with no base.

The first-version system is defined in `data/scenario.yaml`.

## Player ship

- One ship: a von Neumann probe. Drones may be added in a later version.
- Ship state (damage, fuel, ammunition, minerals) is kept between battles.
- Pending: the probe builds copies of itself. Copies are not player
  controlled.

## Opponents

- AI only in the first version.
- An opponent may be a single ship or a fleet.
- **(interpretation)** First-version AI:
  - Fixed defences and bases fire at the player whenever in range.
  - Enemy ships close on the player and match velocity at weapon range, then
    fire everything that is in range.
  - Ships in ambush lie still (not burning) until the player comes within
    their `ambushRange`.
  - Enemy lasers always do point defence.

## Travel

- Travel between stars uses fuel.
- The game skips straight to the next decision (arrival, in the first
  version).
- Pending: events during travel.

## Mining and the auto factory

- The ship carries an auto factory and mining equipment.
- Mining is automatic once the area is cleared; mining before then is not
  realistic. **(interpretation)** The area is cleared when no known enemy
  (ship, defence, base, or torpedo) is within `clearRadius` of the ship.
  Mining also needs the ship within `miningDistance` of a planet and moving
  slower than `maxRelativeSpeed` relative to it.
- The factory turns minerals into repairs, fuel, cannon slugs, and torpedoes.
  **(interpretation)** Priority order: repair parts, then refill fuel, then
  slugs, then torpedoes.
- When choosing a destination, the player sees a mineral estimate for it.

## Presentation

- Low graphics, close to text.
- Styled like the ship's own display: readouts, contact lists, and simple
  diagrams and pictures. Nothing complicated.
- First version: a status readout, contact list, event log, order controls,
  time skip buttons, and a plot of the system from above and from the side.
