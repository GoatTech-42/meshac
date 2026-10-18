# Red-team scoreboard: meadow (attack) vs meshac (defense) - FORMAT DRAFT v1

One page. One row per (check x meadow module/config) pairing actually run on the rig. Who beat
whom, per check, with evidence. Nothing here is a result; the page fills at battery time.

## Verdicts per row

- MESHAC (defense wins the check): every cheat row fired its bound SIGNAL string AND every
  legit twin stayed quiet.
- MEADOW: the attack won. Two flavors, always labeled:
  - EVADE - a cheat row ran clean: silent where its check was bound AND bot liveness proven AND attack-effect proven (the effect the row binds: damage avoided, block placed, boat risen, totem refilled, knockback denied). Silent + liveness WITHOUT attack-effect proof is NOT an EVADE.
  - FP - a legit twin flagged.
- SPLIT: cheat caught but an FP fired too (or the reverse). Record both halves.
- INCONCLUSIVE: blind control (bot/module unproven), a silent cheat row WITHOUT attack-effect
  proof (setup unproven - e.g. GhostHand facing away placing nothing), or SKIP (missing rig
  piece, e.g. Geyser). Never counts as a win for either side. (The combat-SIGNAL-on-d124
  "dead path" example was retracted 10-17: combat is live, monitor-gated - 0048.)

A check with any FP twin can never be scored MESHAC - the FP-free gate outranks detection.

## The page

| check | meadow attack (module + exact config) | meshac result (exact string fired / silent) | legit twin | verdict | evidence (log span + trace lines) |
|---|---|---|---|---|---|
| nofall | GroundSpoof (interleaved) | | GC01/GC02/GC05 quiet? | | |
| fly | Fly (abilities mode) | | nb-fly-legit | | |
| speed | Speed | | nb-speed-legit | | |
| combat | Aimbot @ 4.5 blocks | | nb-combat-legit | | |
| mining | NoBreakDelay+PacketMine | | nb-mine-legit | | |
| interact | FastPlace | | nb-interact-legit | | |
| inventory | ChestStealer | | nb-inv-legit | | |
| blink | Blink | | BF08/BF14 bunching | | |
| boatfly | BoatFly (rig, -Prig) | | nb-boatfly-legit | | |
| jump | powder jump > 0.70 | | GC05 (JB I/II struggle) | | |
| air | AirJump | | pb-e | | |
| step | Step | | pb-n | | |

(one row per bound family; GAP families from COVERAGE-RECHECK-20261017.md join as their rows land)

## Footer (required - keeps the page honest)

- Jar sha256 + patch level, meadow build sha, rig id, run date.
- The two patch-1 GO conditions named with their verdicts (GC01 sneak-edge, GC05 JB-powder)
  before ANY enforcement-adjacent reading of the nofall/jump scores.
- Tally: checks MESHAC x / MEADOW y (EVADE a / FP b) / SPLIT z / INCONCLUSIVE w.
  No partial credit beyond SPLIT; an FP twin caps the check at SPLIT.
