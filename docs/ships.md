# Ships

Ships are modular. The player does not design ships, but every design is
stored as a list of modules so designs can be changed for balance. Enemy
ships use their own, weaker designs built from the same modules.

- Module types: `data/modules.yaml` (`modules`).
- Munitions (cannon slugs, torpedoes): `data/modules.yaml` (`munitions`).
- Ship designs: `data/ships.yaml` — a design names its modules and how many
  of each. Fixed defences and bases are designs too.

Numbers follow [tech-level.md](tech-level.md). Hit points are energy:
1 hit point = 1 MJ.

## Modules

Every module type has:

| Field | Meaning |
|---|---|
| `cost` | Resources to manufacture it. |
| `massT` | Weight, in tonnes. |
| `buildHours` | Manufacturing time. |
| `hitPoints` | Damage it can take before it stops working. |
| `materials` | What it is made of: tonnes of each material (`IRON`, `ALUMINIUM`, `TITANIUM`, `CARBON`, `SILICON`, `COPPER`, `RARE_EARTHS`, `TUNGSTEN`, `LITHIUM`), adding up to `massT`. |

`cost`, `buildHours`, and `materials` are stored now and used once mining
and the auto factory exist (Pending). **(interpretation)** The material
split of each module is a rough engineering guess. Munitions' materials are
Pending.

Module kinds and what they do:

| Kind | Does |
|---|---|
| Hull | Structure and armour. The ship is destroyed when every hull module is destroyed. |
| Propulsion | Thrust (`thrustKN`), fuel use (`fuelKgS` at full throttle), and turning (`turnAuthority`). Different engine types trade thrust, turning, weight, and fuel use. |
| Electrical engine | Generates power (`powerMW`) into the batteries. |
| Battery | Stores power (`capacityMJ`). |
| Fuel tank | Holds fuel (`capacityT`). |
| Sensor | Telescope, radar, lidar, or emission receiver; see [sensors.md](sensors.md). Draws `powerMW` while on. |
| Laser turret | A laser (see [weapons.md](weapons.md)); draws power (`powerMW`) while firing. |
| Cannon | Fires slugs from the ammo containers. |
| Ammo container | Holds cannon slugs (`capacity`). |
| Missile bay | Holds and launches torpedoes (`capacity`). |
| Computer | Runs the ship; draws `powerMW` all the time. Its settings decide how well the ship plans (see [Computers](#computers)). |

Pending module kinds: auto factory, mining equipment.

Munitions also have `cost`, `massKg`, and `buildHours`, for the factory
later.

## Performance

Worked out from the modules that are still working:

- **Mass** = all modules + fuel + slugs + torpedoes. It falls as fuel and
  ammunition are used.
- **Acceleration** = total thrust × throttle ÷ mass.
- **Turn rate** (°/s) = total `turnAuthority` ÷ mass in tonnes.
- **Sensors**: see [sensors.md](sensors.md).
- **Signature** (`signature`, `burningSignature`) and **size** (`sizeM`)
  belong to the design.

## Facing and turning

- Every ship has a facing: a front, and an up. Right is front × up.
- Ships turn toward their heading at their turn rate. A ship with no
  working propulsion, or no fuel, cannot turn.
- Turning uses a small amount of fuel: `turnFuelKgPerTonneDeg`
  (`data/rules.yaml`) × mass in tonnes × degrees turned. **(interpretation)**
  Heavier ships use more fuel to turn the same angle.
- **Evasive manoeuvres (jinking):** with evasion on, propulsion modules that
  can translate (`translates`, the manoeuvring thrusters) push the ship
  sideways in a new random direction every `jinkIntervalSeconds`
  (`data/rules.yaml`), without turning it. A slug flies for minutes at long
  range, so even a small sideways push carries the ship hundreds of metres
  off the aim point. Jinking uses the thrusters' fuel and counts as burning,
  so it raises the ship's signature. **(interpretation)**
- The main engines push only toward the front, so a burn turns the ship to
  its direction first and thrusts once the front is within
  `alignToleranceDeg` (`data/rules.yaml`) of it. A burn uses fuel only while
  it is thrusting, speeding the ship up or slowing it down; not while turning
  or coasting.
- All player controls are relative to the ship (see
  [game-design.md](game-design.md#orders)).
- Fixed defences and bases have no propulsion, so they do not turn.
  **(interpretation)** Their front starts pointing away from the body they
  orbit and stays fixed in space.

## Computers

Every ship, fixed defence, and base carries a computer, and its intelligence
is what the computer can do.

| Computer | Carried by | Mass | Power | Hit points |
|---|---|---|---|---|
| **Sentient core** | the probe | 10 t | 5 MW | 4000 |
| **Daemon core** | raiders, fixed defences, bases | 0.5 t | 0.05 MW | 100 |

- The **sentient core** is the player: a sentient AI. It automates nothing
  beyond the weapon rules the player sets, and its automatic torpedo salvos
  use the full point-defence estimate (see
  [weapons.md](weapons.md#torpedo-salvos)). It is heavy because it is
  hardened and redundant.
- The **daemon core** is a primitive daemon process. It can work out speeds
  and closing, and makes very basic assumptions:
  - About enemies: every target carries two standard laser turrets
    (`assumedLaserTurrets` of `assumedLaser`) for point defence, whatever it
    really has.
  - About allies: it pools torpedoes with allies that have the same target
    in range.
  - It launches torpedoes only when it expects the salvo to destroy the
    target (`salvoGoal: DESTROY`), and otherwise closes and keeps firing its
    other weapons.
  - It forgets a contact not seen for `forgetAfterSeconds` (a day), as if
    it had never seen it.
- **(interpretation)** Fixed defences and bases carry daemon cores too; a
  ship cannot act without a computer.
- **A destroyed computer** leaves the ship dead: it drifts, and its engines,
  sensors, and weapons (point defence included) stop. A dead ship is as good
  as destroyed: it no longer counts toward claiming the system or toward
  remaining. If the probe's sentient core is destroyed, the game is lost.
- **(interpretation)** The computer draws its power before anything else; its
  draw is tiny next to the electrical engine.
- Pending: a tactical core between the two.

## Power

- Electrical engines charge the batteries; anything beyond battery capacity
  is lost.
- Sensors draw their power every step they are on. Lasers draw theirs only
  while firing; a laser with too little stored power does not fire. The
  batteries carry the ship through an engagement and recharge in the gaps
  between them, so the generator need not match the lasers' full draw.
- A destroyed battery loses its share of the stored power.

## Damage

- Each hit lands on one module, chosen at random weighted by `massT`. If that
  module is already destroyed, or the hit is bigger than it has left, the
  rest goes to a working hull module.
- A destroyed module stops working. What it held is lost: a fuel tank or ammo
  container loses its share of the fuel or slugs (by capacity); a missile bay
  loses its torpedoes; a battery its share of stored power.
- The ship is destroyed when every hull module is destroyed, and dead (as
  good as destroyed) when its computer is destroyed (see
  [Computers](#computers)).
