# RED-TEAM REVIEW: meadow Scaffold module vs meshac interact checks (10-17, static)

Sources read: meadow v1211 Scaffold.java + ScaffoldGate/ScaffoldAim/ScaffoldTarget + qa.input.Lease
(swap path); meshac d124 Interact.java (the placement checks) + CheckNames. Static review only -
no live run; line numbers are the d124 bundle. EVERY verdict below is a SCOPED PREDICTION from
reading source: "predicted CAUGHT" / "predicted PASSES" mean "the d124 source as read implies
this", scoped to the checks cited - not an observed rig result. ONE REAL HOLE predicted in the
scaffold design (swap-use), one BY-DESIGN hole predicted on the DEFENSE side (rotation snaps at
placement).

## The checks and the verdicts

| meshac check (Interact.java) | scaffold mode | verdict |
|---|---|---|
| placed-against-air (:115): clicked pos is air | airplace=1 (clicks the free block itself) | predicted CAUGHT instantly - fine as a positive control |
| look-ray (:118-129): claimed hit within the +0.3-inflated box of an 8-block ray from current eye or any pose in the 750ms ring | human/legit: turns to ARRIVE_DEG=3.0 before placing | predicted PASSES - and the 750ms pose ring forgives even a late turn |
| same | human=0, view forward while bridging | predicted CAUGHT ("clicked a block it is not looking at") unless the player looks down - the turn is what carries the module |
| GhostHand outline ray (:132-138) | all: clicks the nearest support face toward the target | predicted PASSES - geometrically identical to vanilla |
| bridging chain (:142-153): under-feet places, dist>0.3, dt<700ms, 10-chain, >4.0 bps | legit preset: capBps 3.0-3.6/session, log-normal gaps ~170-240ms | predicted PASSES by ~10% margin under the 4.0 line; gaps stay <700ms so chains build but the capped speed never crosses. The margin is a DESIGN READING of the current threshold - a retuned 3.5 line catches it |
| rate (:15, 16 uses/s) | all | predicted PASSES with headroom |
| **swap-use (:59-71): use <40ms after a slot change, 4+ in 10s** | **all modes, when the held slot is NOT a block** | **predicted CAUGHT - THE DESIGN HOLE. Lease.with() swaps and places in-tick (~0ms); every placement is a swap-use; a bridge is far more than 4 in 10s -> "used an item N ms after changing slot". findBlock preferring the held slot only saves the module when a block was already in hand** |
| bow-flick / entity / sign | n/a | not placement-path |

## Defense-side BY-DESIGN hole the review prices honestly

The in-tick aim snap (45deg/tick human, 18deg/tick legit) goes out as ordinary rotation packets;
NO current meshac check binds rotation snaps during placement (the only snap check is bow-release,
Interact.java:32). With the swap hole closed, the source reading predicts human-mode scaffold passes every current check (scoped: d124 checks only, swap path closed, not rig-verified). If
the boss wants that surface covered it is a NEW-CHECK proposal (snap-at-place cadence), not a
scaffold-design fix.

## Movement-side exposure: predicted none (scoped)

Scoped static prediction, not rig-verified: placed blocks are real support, so no nofall window
exposure is expected; the capped 3.0-3.6 bps walk sits under the speed checks as currently
tuned; placements use useItemOn + swing in vanilla order. Movement checks outside the cited
Interact.java lines were not re-audited for this claim.

## Recommendations

1. Meadow lane (their call): refuse to run when the held slot is not already a block (honest
   constraint), or pace swap-to-use >=40ms (halves bridge throughput), or accept the swap-use
   SIGNAL. The in-tick swap-back pattern is exactly what the check prices.
2. New control rows (mine to draft if queued): nb-scaffold-cheat (human mode, held non-block ->
   interact swap-use SIGNAL; held block -> BY-DESIGN quiet with the rotation-snap hole named) +
   nb-scaffold-legit (vanilla shift-bridge under 4 bps -> zero interact SIGNAL). The interact
   family currently has GhostHand/FastPlace only.
3. The 4.0 bps / 700ms chain constants the gate calibrates to are quoted in its comments as
   "meshac's line" - a retune invalidates the calibration silently; the battery row is the only
   honest re-check after any interact-threshold change.
