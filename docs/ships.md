# Ships

Ships are modular. The player does not design ships, but every design is
stored as a list of modules so designs can be changed for balance. Enemy
ships use their own, weaker designs built from the same modules.

- Module types: `data/modules.yaml` (`modules`).
- Munitions (cannon slugs, torpedoes): `data/modules.yaml` (`munitions`).
- Ship designs: `data/ships.yaml` — a design names its modules and how many
  of each. Fixed defences and bases are designs too.

Starting values are untuned.

## Modules

Every module type has:

| Field | Meaning |
|---|---|
| `cost` | Resources to manufacture it. |
| `massT` | Weight, in tonnes. |
| `buildHours` | Manufacturing time. |
| `hitPoints` | Damage it can take before it stops working. |

`cost` and `buildHours` are stored now and used once the auto factory exists
(Pending).

Module kinds and what they do:

| Kind | Does |
|---|---|
| Hull | Structure and armour. The ship is destroyed when every hull module is destroyed. |
| Propulsion | Thrust (`thrustKN`), fuel use (`fuelKgS` at full throttle), and turning (`turnAuthority`). Different engine types trade thrust, turning, weight, and fuel use. |
| Electrical engine | Generates power (`powerMW`) into the batteries. |
| Battery | Stores power (`capacityMJ`). |
| Fuel tank | Holds fuel (`capacityT`). |
| Scanner | Sensor range (`rangeAu`); draws power (`powerMW`) all the time. |
| Laser turret | A laser (see [weapons.md](weapons.md)); draws power (`powerMW`) while firing. |
| Cannon | Fires slugs from the ammo containers. |
| Ammo container | Holds cannon slugs (`capacity`). |
| Missile bay | Holds and launches torpedoes (`capacity`). |

Pending module kinds: auto factory, mining equipment.

Munitions also have `cost`, `massKg`, and `buildHours`, for the factory
later.

## Performance

Worked out from the modules that are still working:

- **Mass** = all modules + fuel + slugs + torpedoes. It falls as fuel and
  ammunition are used.
- **Acceleration** = total thrust × throttle ÷ mass.
- **Turn rate** (°/s) = total `turnAuthority` ÷ mass in tonnes.
- **Sensor range** = the longest `rangeAu` of the working scanners, if they
  have power. Without power, range drops to `unpoweredSensorFraction`
  (`data/rules.yaml`). With no working scanner, the ship sees nothing.
- **Signature** (`signature`, `burningSignature`) and **size** (`sizeM`)
  belong to the design.

## Facing and turning

- Every ship has a facing: a front, and an up. Right is front × up.
- Ships turn toward their heading at their turn rate. A ship with no
  working propulsion, or no fuel, cannot turn.
- Turning uses a small amount of fuel: `turnFuelKgPerTonneDeg`
  (`data/rules.yaml`) × mass in tonnes × degrees turned. **(interpretation)**
  Heavier ships use more fuel to turn the same angle.
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

## Power

- Electrical engines charge the batteries; anything beyond battery capacity
  is lost.
- Scanners draw their power every step. Lasers draw theirs while firing; a
  laser with too little stored power does not fire.
- A destroyed battery loses its share of the stored power.

## Damage

- Each hit lands on one module, chosen at random weighted by `massT`. If that
  module is already destroyed, or the hit is bigger than it has left, the
  rest goes to a working hull module.
- A destroyed module stops working. What it held is lost: a fuel tank or ammo
  container loses its share of the fuel or slugs (by capacity); a missile bay
  loses its torpedoes; a battery its share of stored power.
- The ship is destroyed when every hull module is destroyed.
