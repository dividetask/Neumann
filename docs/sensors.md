# Sensors

Sensors are modules (kind `SENSOR`, see [ships.md](ships.md)), each of one
sensor type. Their numbers live in `data/modules.yaml`. Starting values are
untuned.

## Sensor types

| Type | Active? | Detects | Accuracy |
|---|---|---|---|
| **Telescope** (infrared/optical) | Passive | Anything within `rangeAu × signature`: an engine plume is visible across the system, a cold hull only up close | Direction very precise; distance poor |
| **Radar** (microwave) | Active | Anything within `rangeAu × (sizeM ÷ 100 m)^¼`, hot or cold | Good direction, good distance and speed |
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
- Each contact carries its **uncertainty** (the position error size). The plot
  shows it as an error circle and the contact list as "±".
- A contact seen only by an emission receiver is **bearing only**: its
  direction is known but not its distance. **(interpretation)** It can be
  targeted (torpedoes can be launched toward it; their seekers do the rest),
  but cannons will not fire at it.

## Who uses the estimates

- **Cannons** aim at the estimate, so long-range shots miss unless the target
  is well tracked (lidar).
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
