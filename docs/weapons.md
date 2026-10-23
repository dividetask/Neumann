# Weapons

Weapons are modules (see [ships.md](ships.md)): laser turrets, cannons, and
missile bays. Their numbers live in `data/modules.yaml`. Starting values are
untuned.

Each weapon module fires on its own and has a firing arc: `arcDeg`, the
largest angle from the ship's front it can fire at. **(interpretation)**
Starting arcs: laser turrets wide, cannons a narrow cone forward, missile bays
any direction (180°).

## First version

The bare minimum to test the game.

- **Laser** — a beam weapon. Hits at the speed of light, so it cannot be
  dodged. Damage per second falls off linearly from `damagePerSecond` at
  point blank to zero at `range`. Fires continuously while it has a target in
  range and arc, drawing `powerMW`; between engagements it has no target and
  draws nothing. Also serves as point defence: with point
  defence on, it fires at the nearest torpedo aimed at its ship before any
  other target. **(interpretation)** Beam travel time is ignored (at laser
  range it is a fraction of a second).
- **Cannon** — aims with the target's sensor estimate (see
  [sensors.md](sensors.md)), so it needs good tracking at range. Fires solid
  slugs (railgun-style) at `muzzleSpeed` relative to
  the ship, one per `cooldownSeconds`, taken from the ship's ammo containers
  (shared by all cannons). Heavy damage, but a slug is slow, so a
  target that changes course after the shot can be missed. The gun aims at
  where the target will be if it keeps its current velocity. A slug hits if it
  passes within a ship's `sizeM`. **(interpretation)** Slugs are too small
  to show up on sensors.
- **Guided exploding torpedo** — launched from a missile bay (each bay holds
  its own) at `launchSpeed`, then accelerates
  at `acceleration` until only `terminalReserveFraction` of its `deltaV` is
  left, which it keeps for steering on the final approach. Steers itself toward its
  target and explodes when it passes within `blastRadius`, damaging every
  ship in that radius. Has `hitPoints` and can be shot down.
  **(interpretation)** It steers by its own seeker, so its guidance has no
  light delay. If its target is destroyed first, it self-destructs.
- **(interpretation)** Slugs and torpedoes that hit nothing are removed after
  `slugLifetimeSeconds` / `torpedoLifetimeSeconds` (`data/rules.yaml`).

## Pending

Kept for later versions.

- **Guided ramming torpedo** — steers itself into the target and damages it
  by impact.
- **Point defence** — dedicated fast short-range guns or small lasers that
  shoot down torpedoes.
- **Decoys, chaff, and jamming drones** — pull guided torpedoes off target and
  spoil enemy aim.
- **Mines** — sit still and hard to see, covering likely approach routes.
- **Bomb-pumped laser torpedo** — explodes at a distance and fires X-ray
  beams, striking from outside point-defence range.
- **Particle beam** — shorter range than the laser; more damage to armour and
  crew.
- **Flechette or sand clouds** — cheap debris fired across an enemy's path;
  good against torpedo swarms and fast ships.
- **Drones or small fighters** — carrier-style play.
