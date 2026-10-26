# Weapons

Weapons are modules (see [ships.md](ships.md)): laser turrets, cannons, and
missile bays. Their numbers live in `data/modules.yaml` and follow
[tech-level.md](tech-level.md). Damage is energy: 1 hit point = 1 MJ.

Each weapon module fires on its own and has a firing arc: `arcDeg`, the
largest angle from the ship's front it can fire at. **(interpretation)**
Starting arcs: laser turrets wide, cannons a narrow cone forward, missile bays
any direction (180°).

## First version

The bare minimum to test the game.

- **Laser** — a beam weapon. Hits at the speed of light, so it cannot be
  dodged. Damage per second is `beamMW` out to `focusRangeKm`, then falls with
  the square of distance (the spot spreads), and is zero beyond `rangeKm`.
  Fires continuously while it has a target in
  range and arc, drawing `powerMW`; between engagements it has no target and
  draws nothing. Also serves as point defence: with point
  defence on, it fires at the nearest torpedo aimed at its ship before any
  other target. **(interpretation)** Beam travel time is ignored (at laser
  range it is a fraction of a second).
- **Cannon** — aims with the target's sensor estimate (see
  [sensors.md](sensors.md)), so it needs good tracking at range. Fires solid
  slugs (railgun-style) at `muzzleSpeed` relative to
  the ship, one per `cooldownSeconds`, taken from the ship's ammo containers
  (shared by all cannons). Each shot draws `shotEnergyMJ` from the batteries.
  A slug's damage is its kinetic energy relative to what it hits. Heavy
  damage, but a slug is slow, so a
  target that changes course after the shot can be missed. The gun aims at
  where the target will be if it keeps its current velocity. A slug hits if it
  passes within a ship's `sizeM`. The cannon's advantage is **stealth**:
  slugs give off no heat, firing does not raise the shooter's signature, and
  only radar can see a slug, at short range (see [sensors.md](sensors.md#slugs)).
  Lasers do not shoot at slugs. **(interpretation)**
- **Guided exploding torpedo** — launched from a missile bay at
  `launchSpeed`, then accelerates
  at `acceleration` until only `terminalReserveFraction` of its `deltaV` is
  left, which it keeps for steering on the final approach. Steers itself toward its
  target and explodes when it passes within `blastRadius`, damaging every
  ship in that radius. Has `hitPoints` and can be shot down.
  **(interpretation)** It steers by its own seeker, so its guidance has no
  light delay. If its target is destroyed first, it self-destructs.
  Torpedoes give off heat (their motors), so they are easy to see. They
  cannot be dodged: they out-accelerate any ship many times over and keep a
  reserve for the final approach. The defences are point defence and
  staying out of reach of their delta-v.
- **Missile bays** hold each torpedo in its own launch cell, so a bay can
  launch all it holds at once: a salvo leaves together and arrives together.
  **(interpretation)** Cells fire a second or two apart, which is ignored. A
  launch-now order launches one torpedo.

### Torpedo salvos

A torpedo that point defence shoots down is wasted, so automatic launches
(the player's torpedo auto-launch rule and the AI) fire a salvo big enough
to get through, or nothing. How well a ship plans depends on its computer
(see [ships.md](ships.md#computers)). Launch-now orders are not planned: players can
play as well or as poorly as they like.

- **Point-defence estimate.** Each of the target's laser turrets does
  `beamMW` × `focusRangeKm` × (2 − `focusRangeKm` ÷ `rangeKm`) ÷ *v* damage to
  incoming torpedoes over a salvo's approach, where *v* is the torpedoes'
  closing speed at the end: the closing speed now plus the delta-v each
  torpedo burns before its terminal reserve. (This is the laser falloff
  integrated over the approach.) The salvo loses that total ÷ the torpedo's
  `hitPoints` torpedoes, rounded down.
- **Semi-accurate.** The closing speed comes from the shooter's sensor
  estimate, taken at its least favourable: closing speed − 2 × its velocity
  error, which makes point defence look stronger. A poor track therefore
  asks for a bigger salvo, so a shooter may wait until its track improves
  (passive tracking gets better with time; see [sensors.md](sensors.md)).
  Which turrets are counted depends on the computer: the sentient core
  counts the target's design (its laser turrets as built); a daemon core
  assumes `assumedLaserTurrets` of `assumedLaser` on every target.
  **(interpretation)** Damage to its turrets, their arcs, its facing, and its
  stored power are not counted.
- **Target hit points.** The sentient core takes them from the target's
  design; a daemon core assumes `assumedTargetHitPoints` for every target.
- **Salvo size.** With *k* torpedoes expected shot down and *h* hits needed to
  destroy the target (its design's total hit points ÷ torpedo `damage`,
  rounded up):
  - *k* + *h* or more available: fire *k* + *h*.
  - Fewer, but more than *k*: fire them all; at least one should hit.
    Survival matters more than saving torpedoes. Only with `salvoGoal:
    DESTROY_OR_HIT` (sentient core); a daemon core (`DESTROY`) fires none.
  - *k* or fewer: fire none.
- **Allies fire together.** Ships of a side with the same target and
  auto-launch on pool their loaded torpedoes. With `coordination: IN_RANGE`
  (sentient core), only ships with the target within launch range and arc
  count, and they launch at the same moment. With `coordination:
  ALL_ALLIES` (daemon core), every such ally counts wherever it is, on the
  assumption that each will fire too; each ship launches its share once it
  has the target in range and arc.
  **(interpretation)** Torpedoes already in flight are a separate, earlier
  salvo and do not count.

- **(interpretation)** Slugs and torpedoes that hit nothing are removed after
  `slugLifetimeSeconds` / `torpedoLifetimeSeconds` (`data/rules.yaml`).

## Pending

Kept for later versions.

- **Nuclear warheads** — for torpedoes; any ship within about a kilometre
  would be destroyed.
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
