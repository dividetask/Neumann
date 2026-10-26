# Sensors

Sensors are modules (kind `SENSOR`, see [ships.md](ships.md)), each of one
sensor type. Their numbers live in `data/modules.yaml`. Starting values are
untuned.

## Sensor types

| Type | Active? | Detects | Accuracy |
|---|---|---|---|
| **Telescope** (infrared/optical) | Passive | Anything within `rangeAu × signature`: an engine plume is visible across the system, a cold hull only up close | Direction very precise; distance poor |
| **Radar** (microwave) | Active | Anything within `rangeAu × √(sizeM ÷ 100 m)`, hot or cold (radar range goes as the fourth root of radar cross-section, which goes as size²) | Good direction, good distance and speed |
| **Lidar** (laser ranging) | Active | Only the vessel's current target, if already known and within `rangeAu` | Very precise distance and speed |
| **Emission receiver** | Passive | Any vessel using an active sensor, within `rangeAu` | Direction only |

Every sensor has:

| Field | Meaning |
|---|---|
| `rangeAu` | Detection range (see the table above). |
| `bearingErrorDeg` | Direction error (1 sigma). |
| `rangeErrorFraction` | Distance error, as a fraction of the distance (1 sigma). |
| `velocityErrorKmS` | Speed error (1 sigma). |
| `powerMW` | Power drawn while it works. |

## Contacts and accuracy

- Light delay still applies: sensors see objects as they were.
- An object is a contact if any working, powered sensor detects it. The
  contact uses the most accurate detection that step.
- A contact's position and velocity are **estimates**: the true (light-
  delayed) values plus random errors of the sensor's size, drawn again every
  step. **(interpretation)** Position error = `bearingErrorDeg` × distance
  across the line of sight and `rangeErrorFraction` × distance along it.
- **Passive tracking improves with time.** **(interpretation)** A sensor with
  `trackTimeConstantSeconds` (the telescope) narrows its distance and speed
  errors the longer it follows the same contact: error = base ÷
  (1 + seconds tracked ÷ `trackTimeConstantSeconds`)^1.5, down to
  `minRangeErrorFraction` / `minVelocityErrorKmS`. This is passive bearing
  tracking: a sequence of bearings over minutes gives a range; for a target
  on a steady course, a tracking filter's velocity error falls roughly as
  time^-1.5. A ship can aim its cannons without emitting, given about an
  hour of quiet tracking.
- Each contact carries its **uncertainty** (the position error size). The plot
  shows it as an error circle and the contact list as "±".
- A contact seen only by an emission receiver is **bearing only**: its
  direction is known but not its distance. **(interpretation)** It can be
  targeted (torpedoes can be launched toward it; their seekers do the rest),
  but cannons will not fire at it.
- **Bogeys: seen and estimated positions.** A **bogey** is an enemy ship
  or torpedo: an object that can move on its own, so once seen its position
  can only be estimated. Every bogey that has been seen with a position is
  drawn twice on the map, in different colours:
  - **Seen** (hollow, grey): where the instruments last placed it, as of
    when that light left it.
  - **Estimated now** (filled, red): that fix carried forward to the
    present by **dead reckoning**, under the gravity of the star, planets,
    and moons (which move on their orbits meanwhile), over the light delay
    and any time since the fix. Around it, a mostly translucent sphere
    shows where it could be: radius = √(position error² + (velocity error ×
    time since the fix)²).
  - A faint line joins the two.

  This holds while the bogey is tracked, bearing only, or lost altogether,
  for up to `estimateMaxAgeSeconds` after the fix (`data/rules.yaml`,
  `map`). After that, the estimate and its sphere would only clutter the
  map, so only a **ghost** is left: a faint marker where it was last seen.
  After `ghostMaxAgeSeconds` the ghost disappears too.

  **(interpretation)** The estimate assumes the bogey coasts: a ship that
  burns or turns after its light left it, or a torpedo under thrust, will
  drift outside the sphere. A bogey that drops to bearing only keeps its
  last fix. One never seen with a position shows only its bearing line. A
  bogey known to be destroyed (reported destroyed, or seen right up until
  it no longer exists) leaves no ghost.
- **Fixed defences and bases** cannot move on their own, so once detected
  their position is known: they are drawn once, where they are now, whether
  or not the sensors still see them, until destroyed. **(interpretation)**
  This covers bases as well as fixed defences.
- Slugs are drawn once, where seen.
- A bearing-only contact is also drawn as a line from the ship in its
  direction, since its distance is unknown.

## Slugs

- Slugs give off no heat, so telescopes never see them.
- Radar can see them, but they are tiny (`sizeM` 0.1 m), so only at short
  range: about 3% of radar's range against a 100 m ship.
- A detected slug heading for the ship (its straight-line path passes within
  `slugAlertKm` of the ship, allowing three times the estimate's errors)
  raises **slug inbound**, an auto-pause event.
  Seeing slugs coming therefore means emitting.

## Who uses the estimates

- **Cannons** aim at the estimate, so long-range shots miss unless the target
  is well tracked: by lidar (which emits), or by a long passive telescope
  track (which does not, but is less precise).
- **Torpedoes** are launched toward the estimate, then steer by their own
  seekers.
- **Lasers** **(interpretation)** have their own fine tracking within weapon
  range, so they hit what they fire at.
- **AI and the player** see only estimates.

## Active sensors give you away

- Radar and lidar are active: while one is on and powered, the vessel is
  **emitting**, and enemy emission receivers within range can find it.
- Radar and lidar can be switched on and off (sensor rules, next to the
  weapon rules). Passive sensors are always on.
- **(interpretation)** AI: ships and defences keep radar and lidar on once
  they have seen the player; ships lying in ambush keep them off until they
  spring.

## Power

Each working sensor draws its `powerMW` every step it is on. If the batteries
cannot cover a sensor, it is off for that step.
