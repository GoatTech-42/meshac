# DRAFT patch proposal: pinning the microhop buffer + antiknockback window (U7)

Status: DRAFT for reviewer, 10-18 (boss order 10-18 item c: draft patch proposal from the
remaining unpinned behaviors). Scope: of the seven unpinned behaviors in
UNPINNED-BEHAVIORS-PROPOSAL.md, U1-U4 are battery-pinned VALUES (no patch shape), U5
(grace-decay) and U6 (powder-exempt) already have draft docs; U7 is the remaining behavior
without a proposal. Nothing here ships or re-arms enforcement; the pin path is battery +
fixtures, log-only throughout. All threshold numbers below are PROPOSALS.

## Problem

Two live checks rest on values that have never been battery-pinned:

1. **Microhop** (Movement.java:298-307): a sub-jump rise (0.005 < dy < 0.30) with nothing under
   the new spot adds `bufHop += 2` (guards: bouncePad/bounce tracker, water, climbable,
   passenger, flying, lowCeiling, powder); `bufHop >= 3` fires "microhop dy %.3f" (step weight
   2, :343); hopClock forgives one hop per 30 ticks (~1.5 s). Net behavior: two unsupported
   hops inside ~1.5 s flag; one is forgiven. The 2/3/30 values and the dy band are unpinned.
2. **Antiknockback** (:392-405): a melee hurt starts a 450 ms eval; at expiry, in the open,
   unexempt, unblocking, unsneaking, alive, `moved < 0.3` (horizontal or vertical) increments
   noKb, else resets it; `noKb >= 3` signals "no push after three hits" (step 2). The 450 ms,
   0.3 and 3-hit values are unpinned.

## Candidate (measurement stage, this patch: replay tables only, no production change)

Both checks already emit the trace lines an offline scorer needs - `[trace] microhop dy=..
unsupported=.. lowCeiling=.. powder=.. buf=..` (:303) and `[trace] kb eval ...` / `[trace] kb
moved=.. pkt=..` (:396, :399). Following the GroundReplay precedent (patch 4/6):

- sim/MicrohopReplay: parse the microhop trace lines from a capture; re-score the buffer under a
  (flagAt, hopClock) grid; print per-config flags per row. No jar change; the grid is tuning
  signal over REAL traces, never a battery substitute.
- sim/KbReplay: parse the kb eval/moved lines; re-score under a (moved-threshold, eval-window)
  grid the same way.
- Both replay harnesses carry the same honesty contract as GroundReplay: D-row fixtures pin
  scorer semantics (hand-built synthetic streams, labeled NOT battery evidence), and the
  shipped-value column is read against the battery rows below.

## Pinning test list

Battery (legit ceilings - the FP side is what gates):
1. nb-microhop-legit (snow ramp, 2-block ceiling, sprint-jump): zero microhop SIGNAL; the
   capture's max bufHop is REPORTED (ceiling margin), never widened silently.
2. GC01/GC02/GC05 legs: zero microhop signals under sneak-edge, stair and powder-struggle
   movement (the bounce/powder guards' real coverage).
3. nb-kb-legit (zombie pen, ~3 vanilla melee hits): zero antiknockback SIGNAL; measured vanilla
   knockback per hit from the capture (baseline receipt required before the row reads).
4. Bed/slime/wind-charge exits (existing battery rows): zero microhop/antiknockback signals.

Attack side:
5. nb-kb-cheat (Velocity horizontal 0.0, ~3 hits): binds antiknockback; effect measured against
   the row-3 baseline (row exists, DRAFT until receipts).
6. nb-microhop-cheat: BLOCKED on the meadow MicroHop module (MEADOW-ATTACK-SPECS.md section 1) -
   meadow has no microhop module today and scripted driver hops are vanilla jumps, not a
   packet-level microhop (patch 0016). GS-C2 (landing-interleave dilution) prices the remaining
   hole once a driver exists.

Then: MicrohopReplay/KbReplay grids over the same traces confirm the shipped values sit on a
plateau (not an edge); reviewer pins the values; any value change is a SEPARATE reviewed patch.

## Pre-committed kill criteria

- ANY microhop or antiknockback flag on ANY legit battery row kills the shipped values.
- A plateau of a single grid point kills the shipped values (no margin).
- A cheat row missed at the shipped values forces a re-tune proposal, never a silent widening.
- The meadow MicroHop module landing without the nb-microhop-cheat row leaves the attack side
  UNVERIFIED - the values stay PROPOSALS, and that is stated wherever they are cited.

## Order and dependencies

Battery run (rows 1-4, shared with the U1-U4 evidence) -> replay grids -> reviewer pins values
-> meadow MicroHop module (rides the meadow queue) -> nb-microhop-cheat + GS-C2. No dependency
on the grace-decay Stage-2 gate; independent lane.
