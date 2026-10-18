# PROPOSAL: rotation-snap-at-placement surface (10-17, from the scaffold red-team review)

STATUS: proposal only. NO enforcement change, NO threshold change. ilhsmc stays log-only. This
prices a defense-side gap the meadow scaffold review exposed; the boss decides whether it ever
becomes a check.

## The gap

The only snap-cadence check today is bow-release (Interact.java:32, "view snaps onto a target as
the bow is let go"). Placements have no rotation binding: a module may rotate any number of
degrees in one tick, place against the newly-faced block the same tick, and pass every current
check as long as the click ray hits and the swap timing is clean. Meadow's own human mode
(ScaffoldAim: 45deg/tick max step, arrive-within-3deg-then-click, at most 3 aim ticks) walks
straight through this: with the swap hole closed, it is quiet against the whole current battery.
That is the BY-DESIGN hole named in nb-scaffold-cheat.

## What a check could measure (all measurable from packets the server already sees)

1. Snap angle per placement: |yaw/pitch delta| on the tick of a use-item packet vs the previous
   rotation. A 40-90deg same-tick snap ending within a few degrees of a valid placement face is
   the machine signature; humans turn-then-click over several ticks.
2. Arrive-then-click cadence: placement within 0-1 ticks of a large rotation STOPPING, repeated.
   The tell is the regularity (always arrive, then immediately click), not the angle alone.
3. Turn-rate regularity over a bridge: constant deg/tick steps (18 or 45 exactly, every tick)
   are machine-regular; human mouse input is noisy. Cheapest signal, weakest alone.

## FP surface (why this is a shadow proposal, not a check proposal)

- Legit flick-turns: a player who spins 180 and places mid-spin (MLG bucket, panic walling)
  produces a large same-tick snap with a valid click. Single events must NOT flag; only cadence
  over a window could bind.
- Jitter-clickers and high-CPI mice: real rotation noise overlaps small machine steps.
- Any threshold is a proposal until a battery row prices it; no number ships unpriced.

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
