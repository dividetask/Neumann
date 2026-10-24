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
- A slug is heading for the player's ship (only radar can see slugs; see
  [sensors.md](sensors.md#slugs)).
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

Events that arrive at nearly the same time are one piece of news, so one
pause. **(interpretation)** After an event pauses, further events of the
same kind within `groupSeconds` do not pause again, whatever their source
(for example, the rest of a torpedo salvo).

The end of a burn does not pause; it is logged.

## Orders

Between time skips the player can:

A. **Face** a direction: the ship turns to it and holds it.
B. **Burn** in a direction: throttle (fraction of full thrust), and a
   **desired speed**, a **desired distance**, or both (two sliders). The ship
   turns to the direction first (see [ships.md](ships.md#facing-and-turning)).
   - **Speed only:** burn until moving at the desired speed, then coast.
   - **Distance only:** speed up until halfway there, then brake the rest of
     the way and stop.
   - **Both:** speed up until at the desired speed or halfway, whichever comes
     first; coast until it is time to slow down; then brake and stop.

   Braking turns the ship around (it thrusts only forward), so braking starts
   early enough to cover the turn. **(interpretation)** Speed and distance are
   measured along the burn direction, from the ship's position and velocity
   when the order is given (the ship's point of view). Match target velocity
   ignores both and stops itself.

All directions are relative to the ship. They are chosen with:

- **Direction circles** — two dials, each with a dot that can be dragged
  around it. Two are enough: a direction needs only yaw and pitch. Roll would
  matter only for weapons that fire to the ship's sides, and every weapon arc
  is measured from the front. **(interpretation)**
  - **YAW** (seen from above): the top is the ship's front; clockwise is
    right.
  - **PITCH** (seen from the side): the top is the ship's front; clockwise is
    up. Limited to ±90°.

  The turn is yaw, then pitch, from the ship's facing when the order is
  given; the resulting direction is then fixed in space. While the ship
  turns, each dot shows the turn still to go, so the dots move to the top as
  the ship turns.
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
F. **Set sensor rules**: radar on/off, lidar on/off (see
   [sensors.md](sensors.md)).
G. **Evasive manoeuvres** on/off: jink sideways with the thrusters to spoil
   cannon fire (see [ships.md](ships.md#facing-and-turning)).

Skill lies in these choices: when to emit and when to stay dark, when to
jink (it costs fuel and shows you), how to face so weapon arcs bear, and how
to spend torpedoes against point defence.

## Space

- The game is in **3D**.
- Everything the player chooses is from the ship's point of view; there are
  no star-relative controls.
- **(interpretation)** The ship arrives on the entry sphere (`radiusAu` from
  the star) at a bearing set by where it came from (`entry.bearingDeg` in
  `data/scenario.yaml` for the first version). The approach vector is chosen
  from the ship, looking at the star: the YAW and PITCH dials aim it off the
  star, and a slider sets the entry speed.
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
- Fixed defences are not necessarily attached to anything: they lack the
  engines to move much and only keep their orbit. **(interpretation)**
  Fixed defences and bases start in a circular orbit of a planet or the star
  (`orbits` in `data/scenario.yaml`) and follow it under gravity, like any
  ship that is not burning.

## Sensors

- Ships carry several kinds of sensor with different range and accuracy;
  see [sensors.md](sensors.md).
- An object's signature (what telescopes see) is higher while it is burning
  its engine.
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
- **First-version enemy: the raider** — a simple, easy ship with 1 laser,
  2–4 torpedoes, and a cannon (`raider` in `data/ships.yaml`).
- **Balance rule:** the probe must be able to beat four raiders at once
  without significant strategy (all weapon rules on, facing the nearest
  enemy), at medium difficulty. **(interpretation)** Medium means it wins with
  40–80% integrity left (hit points across all its modules; hits land on
  every module, not just the hull). `BalanceTest` checks this against the
  data.
- **Balance rule:** lasers, cannons, and torpedoes must all matter.
  **(interpretation)** In that same fight, each of the three deals at least
  10% of the damage the probe deals.
- The first-version system has four raiders, a base, and a defence platform.
- **(interpretation)** First-version AI:
  - Fixed defences and bases fire at the player whenever in range.
  - Enemy ships close on the player and match velocity at weapon range, then
    face the player and fire everything that is in range and arc.
  - Ships in ambush lie still (not burning) until the player comes within
    their `ambushRange`.
  - Enemy lasers always do point defence.
  - Enemy ships with translating thrusters jink while a hostile ship is
    within `jinkRangeKm` (`data/rules.yaml`). The raider has none.

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
