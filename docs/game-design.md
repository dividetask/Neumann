# Game Design

## Summary

The player commands one sublight ship, a von Neumann probe, against AI
opponents. They choose a star to travel to, choose an approach vector into
its system, and fight whatever is there. Ship state carries over from battle
to battle.

## Core loop

A. On the star map, the player picks a destination star, helped by its
   mineral estimate.
B. The ship travels there (see [Travel](#travel)).
C. On arrival, the player picks an approach vector into the system.
D. Inside the system, the player gives orders and advances time with
   time skips until the system is won, fled, or the ship is lost.
E. Back to A, keeping the ship's state.

## Time skips

Controls: **1 minute**, **1 hour**, **4 hours**, **1 day**.

A time skip moves the game forward until the chosen time has passed, or until
an auto-pause event happens, whichever comes first.

The game never makes the player wait in real time: a time skip resolves at
once.

### Auto-pause events

- A new contact is detected.
- A torpedo is launched at the player's ship.
- The player's ship enters an enemy's weapon range.
- The player's ship takes damage.
- A target is destroyed.
- Fuel or ammunition runs low.

## Space

- The game is in **3D**.
- **(interpretation)** An approach vector is a direction in 3D plus an entry
  speed, chosen before entering the system.
- **Realistic scale.** Distances are real: a system is billions of km across,
  and light takes minutes to hours to cross it.
- **Light delay.** Sensors show where a contact was when its light left it,
  not where it is now.

## Physics

- Real Newtonian movement. A ship keeps its velocity until it burns its
  engine; to stop, it turns around and burns against its motion.
- Burns use fuel.
- The gravity of stars and planets affects ships. **(interpretation)** Stars
  and planets follow fixed orbits; only ships, torpedoes, and slugs are moved
  by gravity.

## Star systems

A star system may contain any mix of:

- Fixed defences.
- Enemy ships (one ship or a fleet).
- A permanent base.
- An ambush: ships waiting with no base.

## Player ship

- One ship: a von Neumann probe. Drones may be added in a later version.
- Ship state (damage, fuel, ammunition, minerals) is kept between battles.

## Opponents

- AI only in the first version.
- An opponent may be a single ship or a fleet.

## Travel

- Travel between stars uses fuel.
- The game skips straight to the next decision (arrival, in the first
  version).
- Pending: events during travel.

## Repair and resupply

- The ship carries an auto factory and mining equipment.
- It repairs itself using minerals mined in the system it is in.
- When choosing a destination, the player sees a mineral estimate for it.

## Presentation

- Low graphics, close to text.
- Styled like the ship's own display: readouts, contact lists, and simple
  diagrams and pictures. Nothing complicated.

## Open questions

- **O-5 Orders.** What orders can the player give before a time skip
  (course, target, weapon rules)?
