# Tech Level

Every number in `data/` should be a realistic estimate for one tech level:
about **200 years ahead of today** (23rd-century hard science fiction). This
document records the assumptions behind the numbers. **(interpretation)** The
figures are estimates from published studies and physics limits, chosen
toward the optimistic end where the studies disagree.

## Units

- **Hit points are energy:** 1 hit point = 1 MJ deposited in the module.
  Laser beams, slug impacts, and warheads all deal damage in MJ, so their
  strengths compare directly.
- **Hulls** take about 300 MJ per tonne. **(interpretation)**

## Propulsion — fusion

- **How it works:** a pulsed or magnetic-nozzle fusion drive burns deuterium
  and helium-3 (D-He3, which gives few neutrons), with hydrogen added as
  reaction mass. The hot plasma leaves through a magnetic nozzle.
- **Fuel:** "fuel" in the data is that D-He3 and hydrogen mix, in tonnes.
  Helium-3 is mined from gas-giant atmospheres (a later resource).
- **Limits:** jet power per tonne of engine is the hard limit. Real design
  studies range from about 5 MW/t (Discovery II, 2005) to far more for pure
  pulse drives (Daedalus, 1978). Assumed: **~100 MW/t** for a fusion drive,
  **~65 MW/t** for the smaller plasma drive.
- **Exhaust speed** trades thrust against fuel use at the same power:
  thrust = 2 × jet power ÷ exhaust speed; fuel use = thrust ÷ exhaust speed.
  A magnetic-nozzle drive can change it in flight by adding more or less
  hydrogen reaction mass (real variable-exhaust plasma engines, such as
  NASA's VASIMR design, work this way). Assumed range for the fusion and
  plasma drives: from the table's figure up to **2,000 km/s**. At 2,000
  km/s the probe's 100 t of fuel gives about 700 km/s of delta-v instead of
  about 110, at a seventh of the thrust.

| Engine | Mass | Jet power | Exhaust speed | Thrust | Fuel use |
|---|---|---|---|---|---|
| Fusion drive | 40 t | 4 GW | 300 km/s | 26.7 kN | 0.089 kg/s |
| Plasma drive | 15 t | 1 GW | 200 km/s | 10 kN | 0.05 kg/s |
| Manoeuvring thrusters | 3 t | 40 MW | 40 km/s | 2 kN | 0.05 kg/s |

- **Result:** ships accelerate at about **0.01 g** (0.07–0.1 m/s²) and carry
  **~70–110 km/s** of delta-v. Crossing 1 AU takes weeks to months, which is
  what the 1-day time skip is for. Entry speeds are a few tens of km/s.

## Power

- **Fusion reactor** ("electrical engine"): ~5 MW of electricity per tonne.
  Fuel use is negligible (D-He3 holds ~3.5 × 10¹⁴ J/kg).
- **Batteries/capacitors:** ~2 MJ/kg (about twice today's best cells).

## Lasers

- **How far a beam stays tight:** spot width ≈ 2.44 × wavelength × distance ÷
  mirror width. A ~3 m mirror at visible light (~0.5 µm) keeps a spot under
  ~1 m out to ~2,500 km, ~2 m out to ~5,000 km.
- **Model:** full damage out to `focusRangeKm`; beyond it, the spot grows and
  intensity falls with the square of distance; no damage past `rangeKm`.
  Damage per second = `beamMW` (all of the beam's energy lands on the
  target). **(interpretation)**
- **Efficiency:** ~50% wall-plug, so a turret draws `powerMW` = 2 × `beamMW`.

| Laser | Beam | Draw | Focus range | Max range |
|---|---|---|---|---|
| Laser turret (probe) | 15 MW | 30 MW | 5,000 km | 50,000 km |
| Light laser turret (raider) | 14 MW | 28 MW | 5,000 km | 50,000 km |
| Heavy laser turret | 60 MW | 120 MW | 8,000 km | 80,000 km |

Beam powers were tuned within the realistic 10–100 MW band to meet the
balance rule in [game-design.md](game-design.md#opponents).

## Cannons — coilguns

- **Today:** railguns reach ~2–3 km/s. A 23rd-century space coilgun:
  10–20 km/s.
- **Slugs:** 2 kg. Damage = kinetic energy at impact, relative to the target
  (½·m·v²): 400 MJ at 20 km/s, more when closing fast.
- **Range:** with lidar tracking (metre-level position, ~0.1 m/s velocity),
  a slug can hit a ship that is not manoeuvring out to ~20,000 km (about
  1,000 s of flight). A target that keeps changing course dodges at long
  range.
- **Energy:** each shot draws `shotEnergyMJ` (~2× the slug's muzzle energy,
  at ~50% efficiency) from the batteries.

## Torpedoes

- **Propulsion:** a nuclear-thermal motor: 50 m/s² (5 g), 15 km/s of
  delta-v.
- **Warhead:** a fragmentation warhead, damaging everything within
  `blastRadiusKm`. **(interpretation)** Not nuclear; nuclear warheads are
  Pending (they would kill any ship at 1 km).
- **Toughness:** an ablative armour shell (~100–200 kg of carbon-phenolic
  type ablator at ~30 MJ/kg, with the torpedo spinning to spread the heat)
  absorbs a few thousand MJ of laser energy before the guidance fails. A
  15 MW laser needs a minute or two per torpedo, so a salvo can overwhelm
  point defence.

## Sensors

- **Passive tracking:** following a target's bearing for minutes, while the
  ship moves, gives its range and velocity (as submarines do). Assumed: a
  telescope's errors shrink with tracking time as t^-1.5 (a tracking filter
  on a steady target), to 0.05% of the distance and 1 m/s.
- **Slugs on radar:** a ~10 cm slug has a radar cross-section a million
  times smaller than a 100 m ship, so radar sees it at about 3% of the
  range (range goes as cross-section^¼).

| Sensor | Basis |
|---|---|
| Telescope | A GW-class drive plume is visible across a system; a 300 K hull only to a few million km. |
| Radar | MW-class radar against a ~100 m ship: about a million km (radar return falls as 1/distance⁴). |
| Lidar | Laser ranging to a known target: about a million km, metre accuracy. |
| Emission receiver | Picks up MW-class radar and lidar across most of a system. |

## Years ahead of today

| Technology | Years ahead |
|---|---|
| Space coilguns at 10–20 km/s | ~100 |
| 20 MW lasers with 3 m optics | 50–100 |
| Fusion drives at ~100 MW/t | 150–200 |
| Self-replicating von Neumann probes | 200+ |
