# PROPOSAL: rotation-snap-at-placement surface (10-17, from the scaffold red-team review)

STATUS: proposal only. NO enforcement change, NO threshold change, and NOTHING is deployed to
ilhsmc: no jar change, no logging change, ilhsmc is untouched in every way. This
prices a defense-side gap the meadow scaffold review exposed; the boss decides whether it ever
becomes a check.

## The gap

The only snap-cadence check today is bow-release (Interact.java:32, "view snaps onto a target as
the bow is let go"). Placements have no rotation binding: a module may rotate any number of
degrees in one tick, place against the newly-faced block the same tick, and pass every current
check as long as the click ray hits and the swap timing is clean. Meadow's own human mode
(ScaffoldAim: 45deg/tick max step, arrive-within-3deg-then-click, at most 3 aim ticks) walks
straight through this: with the swap hole closed, the static reading PREDICTS it is quiet
against the current battery (scoped: d124 checks only) - a prediction, not a measurement.
That is the BY-DESIGN hole named in nb-scaffold-cheat.

## What a check could measure (all measurable from packets the server already sees)

1. Snap angle per placement: |yaw/pitch delta| on the tick of a use-item packet vs the previous
   rotation. A 40-90deg same-tick snap ending within a few degrees of a valid placement face is
   the candidate machine signature. Whether legit players also turn-and-click within one tick is
   UNMEASURED - the legit overlap must be measured before any number is proposed, and nothing
   here assumes humans cannot produce same-tick turn-clicks.
2. Arrive-then-click cadence: placement within 0-1 ticks of a large rotation STOPPING, repeated.
   The tell is the regularity (always arrive, then immediately click), not the angle alone.
3. Turn-rate regularity over a bridge: constant deg/tick steps (18 or 45 exactly, every tick)
   are machine-regular; human mouse input is noisy. Cheapest signal, weakest alone.

## FP surface (why this is a shadow proposal, not a check proposal)

- Legit flick-turns: a player who spins 180 and places mid-spin (MLG bucket, panic walling)
  produces a large same-tick snap with a valid click. Single events must NOT flag; only cadence
  over a window could bind. It is NOT established that humans never turn-click in one tick -
  the montage exists to measure exactly that.
- Jitter-clickers and high-CPI mice: real rotation noise overlaps small machine steps.
- Any threshold is a proposal until a battery row prices it; no number ships unpriced.

## Measurement rules any recorder must implement FIRST

- "Same tick" is only decidable from data: the capture must record per-packet arrival timestamps
  and the server tick boundary. A rotation packet and a use packet count as same-tick only when
  both arrive inside one tick window (50ms) with monotonic arrival order.
- Missing timestamps, an ambiguous tick boundary, or out-of-order arrival => that placement's
  measurement is INCONCLUSIVE: recorded as inconclusive, never counted as clean and never
  counted as a snap.
- The legit montage row (flick turns, MLG-style saves, panic walling) runs BEFORE any threshold
  is proposed; overlap that no threshold separates kills the proposal in writing.

## Proposed shape, if the boss wants it priced

- LOG-ONLY shadow (MESHAC_TRACE-gated, like the existing decay shadow): record the three
  measurements per placement, emit nothing to SIGNAL. A new control row pair (snap-cheat =
  scaffold held-block quiet variant, snap-legit = flick-turn montage) reads the shadow output.
- Pre-committed kill criteria: if the legit montage produces any measurement overlap with the
  cheat distribution that no threshold separates, the proposal dies in writing - no widening,
  no averaging away the overlap.
- Estimated work: one pass-through recorder in Interact.place + the pose ring already kept
  (750ms) + two control rows. No movement-path changes.

## What this is NOT

Not a claim that meadow's scaffold is "caught" - today it is not, with a block in hand. Not a
battery prerequisite. Not a threshold proposal: every number above is a measurement definition,
not a bound.
