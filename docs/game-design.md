# Game Design

## Summary

The player commands one sublight ship against AI opponents. They choose a
star to travel to, choose an approach vector into its system, and fight
whatever is there. Ship state carries over from battle to battle.

## Core loop

A. On the star map, the player picks a destination star.
B. The ship travels there (see [Travel](#travel)).
C. On arrival, the player picks an approach vector into the system.
D. Inside the system, the player gives orders and advances time with
   time skips until the system is won, fled, or the ship is lost.
E. Back to A, keeping the ship's state.

## Time skips

Controls: **1 minute**, **1 hour**, **4 hours**, **1 day**.

A time skip moves the game forward until the chosen time has passed, or until
an auto-pause event happens, whichever comes first.

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

## Star systems

A star system may contain any mix of:

- Fixed defences.
- Enemy ships (one ship or a fleet).
- A permanent base.
- An ambush: ships waiting with no base.

## Player ship

- One ship. Drones may be added in a later version.
- Ship state (damage, fuel, ammunition) is kept between battles.

## Opponents

- AI only in the first version.
- An opponent may be a single ship or a fleet.

## Travel

Open — see question O-3.

## Presentation

- Low graphics, close to text.
- Styled like the ship's own display: readouts, contact lists, and simple
  diagrams and pictures. Nothing complicated.

## Open questions

- **O-1 Physics.** Real Newtonian movement (momentum; to stop, turn around and
  burn; fuel limits) or something simpler? Does the gravity of stars and
  planets matter?
- **O-2 Scale.** Real distances (a system is billions of km across; light
  takes minutes to cross it, so sensors see where things *were*) or shrunk
  to keep things simple?
- **O-3 Travel between stars.** Does the trip between stars take game time,
  use fuel, or have events, or is it instant?
- **O-4 Repair and resupply.** Where and how does the ship repair and get
  fuel and ammunition back (captured bases, friendly ports, between
  battles)?
- **O-5 Orders.** What orders can the player give before a time skip
  (course, target, weapon rules)?
