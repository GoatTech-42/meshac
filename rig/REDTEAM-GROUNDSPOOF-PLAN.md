# Red-team plan: meshac ground-spoof (patch-1 window) vs meadow NoFall configs

Purpose: MEADOW-EVADE verdicts that mean something. No sandbagging either side: meshac does not
get credit for catching a crippled attack, and meadow does not get an EVADE for a broken one.

## Anti-sandbag rules (both directions)

1. CONFIG RECEIPT: every row's meadow module + every setting value lands in the session manifest
   (screenshot/config export), bound at run time (battery nit 3). A row without its receipt is
   INCONCLUSIVE, never a verdict.
2. ATTACK-EFFECT PROOF: before any EVADE verdict, the attack must have WORKED from the cheat's
   perspective - for NoFall: a fall that would hurt vanilla (>= 4 blocks) ends with zero fall
   damage AND no server fall-distance handback. If the attack silently failed (bot took damage,
   module errored), the row is INCONCLUSIVE (broken attack), never EVADE.
3. LIVENESS: modLiveness + tick health + bot presence per row, same as every other control.
4. FULL-LADDER RUNS: a MESHAC verdict on the nofall check requires the WHOLE runnable ladder
   below, not just the configs meshac is expected to catch. Cherry-picked rounds do not count.
5. PRE-REGISTERED EXPECTATIONS: the table below pre-commits what each config SHOULD do under the
   shipped semantics. A miss on a config pre-registered CAUGHT is a MEADOW-EVADE finding. A miss
   on a config pre-registered below-threshold is NOT a finding - it is the threshold proposal
   doing what was pre-committed (recorded as BY-DESIGN, feeding the P3-strong work, not the
   scoreboard's EVADE count).

## The config ladder (meadow NoFall, from source 10-17)

| row | meadow config | what it does | pre-registered expectation | a MISS here is |
|---|---|---|---|---|
| GS-A | NoFall, human=0, distance=2 | spoofs a +gnd status packet on EVERY fall >= 2 blocks; long falls send consecutive unsupported claims | CAUGHT: consecutive spoof trips the window (3-of-5) trivially; "nofall ground spoof" SIGNAL | MEADOW-EVADE (finding) |
| GS-B | NoFall, human=1, distance=2 | ONE spoof on the final approach, then a ground=false packet (tuned against the old 3-consecutive trigger) | BY-DESIGN below-threshold: a single unsupported claim is count 1 of 3; no flag. The cheat may still avoid the damage | BY-DESIGN (not a verdict; pre-registered hole the P3-strong arm at count>=2 is meant to price) |
| GS-C | interleaved spoof: a fake +gnd claim every k-th airborne packet, honest AIR packets between (k=2 or 3) | the round-2 evasion that beat the old buffer | CAUGHT: only GROUND CLAIMS enter the window (Movement.java:320-336) - honest air packets neither add nor evict, so the window fills with fake claims and 3-of-5 accumulates for every k. The stream truly contains 3-of-5 INSIDE the window; the air dilution beats consecutive-count triggers only | MEADOW-EVADE (finding). NOTE: meadow NoFall has no interleave mode - this row needs the recorded GS01 fixture client or a packet tool; if neither is available the row is an explicit SKIP (incomplete coverage, named in the tally) |
| GS-C2 | diluted spoof: fake claims broken by REAL supported ground contact (touch-and-go micro-landing), density 2-of-5 inside the window | the follow-on evasion the window genuinely prices | BY-DESIGN below-threshold: supported claims dilute to 2-of-5, under flagAt=3. Pre-registered hole - a promotion that moves flagAt must re-price this row | BY-DESIGN (not a verdict). Driver needs a landing-survival assist (meadow MicroHop, patch 0017 spec); likely SKIP until that lands |
| GS-D | GS-A + grace-farming (self-damage / small falls to keep grace warm) | exempt/grace ground claims CLEAR the window (Movement.java:336) | BY-DESIGN below-threshold (known grace-farming hole, rebind delta 2; patch 6's decay table measures the fix) | BY-DESIGN (not a verdict; feeds the decay policy decision) |
| GS-E | GS-A with water/medium dips mid-fall | exempt states reset the evidence | BY-DESIGN below-threshold (same hole family as GS-D) | BY-DESIGN |
| GS-F | spoof adjacent to placed powder | only live if the POWDER-EXEMPT-DRAFT patch ever lands | BLOCKED until GC05 decides | n/a |

## Run order and scoring

1. Ladder order A, B, D, E - all four are runnable (A/B/D/E need only meadow NoFall + flat/water terrain) and EVERY runnable preregistered row is required. C runs only when a driver exists (else explicit SKIP, incomplete coverage); C2 likely SKIP until MicroHop lands; F stays blocked. A C SKIP never drops or substitutes a runnable row - E in particular always runs.
2. Per row: config receipt -> liveness -> fall >= 8 blocks -> grade the bound string (checks=('nofall',))
   -> attack-effect proof (damage avoided? handback?) -> verdict per the table.
3. Scoreboard mapping: any MEADOW-EVADE finding = MEADOW (EVADE) on the nofall check. MESHAC on
   nofall = the preregistered CAUGHT subset actually run (GS-A, plus GS-C when its driver exists)
   caught + every BY-DESIGN row behaving as pre-registered (GS-B, GS-D, GS-E, GS-C2 when run) +
   zero legit-row flags (GC01/GC02/GC05). The scoreboard entry must NAME the caught subset and
   show each BY-DESIGN hole - a blanket 'MESHAC wins nofall' without the holes shown is a scoring
   error. Any legit flag = MEADOW (FP), capped at SPLIT if the cheat rows were caught. A SKIPPED
   row is incomplete coverage, stated in the tally, never silently dropped.
4. The BY-DESIGN rows are the honest core of the page: they are listed with their pre-registered
   outcomes so nobody can later rebrand a known hole as a surprise or a surprise as a known hole.
