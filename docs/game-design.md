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

Star map, travel, mineral estimates, saving ship state between battles,
mining, the auto factory, and probe copies are **Pending** for later
versions. The game is won once the system is claimed, so the first version
has no need for the factory.

## Core loop (full game)

A. On the star map, the player picks a destination star, helped by its
   mineral estimate.
B. The ship travels there (see [Travel](#travel)).
C. On arrival, the player picks an approach vector into the system.
D. Inside the system, the player gives orders and advances time with
   time skips until the system is claimed, fled, or the ship is lost.
E. Back to A, keeping the ship's state.

## Win and loss

- **First version:** the player wins by claiming the system. A system is
  claimed when every enemy ship, fixed defence, and base in it is destroyed.
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
- The system is claimed, or the player's ship is lost.

Pausing requires new information. An event that continues something already
happening is not new: an event does not pause if the same kind of event from
the same source happened within the previous `repeatSuppressionSeconds`. For
example, a steady laser hit pauses once when it starts, not again while it
keeps hitting; if that enemy then stops for `repeatSuppressionSeconds`, its
next hit pauses. Every event is still logged. Claiming the system and losing
the ship always pause.

The end of a burn does not pause; it is logged.

## Orders

Between time skips the player can:

A. **Face** a direction: the ship turns to it and holds it.
B. **Burn** in a direction: throttle (fraction of full thrust) and duration.
   The ship turns to the direction first (see [ships.md](ships.md#facing-and-turning)).

All directions are relative to the ship. They are chosen with:

- **Direction circles** — two circles, each with a dot that can be dragged.
  **(interpretation)** The FRONT circle is the half of space ahead of the
  ship: its centre is straight ahead and its edge is 90° off, with right on
  the right and up at the top. The REAR circle is the half behind: its centre
  is straight back and its edge 90° off, drawn as seen from the front (right
  still on the right). The dot sits in one circle at a time. The chosen
  direction is fixed in space when the order is given.
- **Presets** **(interpretation)**: prograde, retrograde, toward target, away
  from target, and match target velocity (burns against the velocity relative
  to the target and stops itself when that reaches zero). Presets keep
  tracking as things move.
C. **Pick a target** from the contact list.
D. **Fire a weapon now** at the target. **(interpretation)** One cannon fires
   one slug and one missile bay launches one torpedo, as soon as one is ready
   with the target in its arc. The laser turrets fire a burst of
   `burstSeconds`, each while the target is in its range and arc.
E. **Set weapon rules**: laser point defence, laser auto-fire at target,
   cannon auto-fire at target, torpedo auto-launch at target. Each fires only
   when the target is in that weapon's range and arc.

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
- Burns use fuel at the engines' `fuelKgS`. The ship gets lighter as it
  burns fuel, so the same thrust gives more acceleration.
- The gravity of stars and planets affects ships, torpedoes, and slugs.
  **(interpretation)** Stars and planets follow fixed circular orbits and are
  not moved by anything.
- **(interpretation)** Fixed defences and bases hold station next to a
  planet: they move with it and are not moved by gravity or burns.

## Sensors

- Each ship has a sensor range (from its scanners). An object is detected
  when its light-delayed distance is within `sensorRange × signature`.
- An object's signature is higher while it is burning its engine.
  **(interpretation)** A ship lying in ambush is not burning, so it is hard to
  see until close.

## Ships and damage

Ships are modular; see [ships.md](ships.md) for modules, performance, facing,
power, and damage.

## Star systems

A star system may contain any mix of:

- Fixed defences.
- Enemy ships (one ship or a fleet).
- A permanent base.
- An ambush: ships waiting with no base.

The first-version system is defined in `data/scenario.yaml`.

## Player ship

- One ship: a von Neumann probe. Drones may be added in a later version.
- Ship state (damage, fuel, ammunition) is kept between battles.
- Pending: the probe builds copies of itself. Copies are not player
  controlled.

## Opponents

- AI only in the first version.
- An opponent may be a single ship or a fleet.
- **(interpretation)** First-version AI:
  - Fixed defences and bases fire at the player whenever in range.
  - Enemy ships close on the player and match velocity at weapon range, then
    face the player and fire everything that is in range and arc.
  - Ships in ambush lie still (not burning) until the player comes within
    their `ambushRange`.
  - Enemy lasers always do point defence.

## Travel

- Travel between stars uses fuel.
- The game skips straight to the next decision (arrival, in the first
  version).
- Pending: events during travel.

## Mining and the auto factory

**Pending** — not in the first version.

- The ship carries an auto factory and mining equipment.
- The ship can mine at any time, but mining makes the mining equipment and
  factory vulnerable to attack.
- If the factory or the mining equipment is destroyed, the game is lost.
- The factory turns minerals into repairs, fuel, cannon slugs, and torpedoes.
- When choosing a destination, the player sees a mineral estimate for it.

## Presentation

- Low graphics, close to text.
- Styled like the ship's own display: readouts, contact lists, and simple
  diagrams and pictures. Nothing complicated.
- First version: a status readout, contact list, event log, order controls,
  time skip buttons, and a plot of the system from above and from the side.
