# Grace-decay scoring (patch 6): false-positive hardening review

Review of 126fac1 (GroundEvidence.decay + GroundReplay policy=decay-on-clear table). The patch is a
measurement stage - no production call site, live jar full-clears - so "FP hardening" here means:
will the decay table's numbers mislead the promotion decision, and where are the FP edges.

## Findings (no code change required)

1. **D-row / sneak-edge margin.** Decay only bites when the count is >= 1 at a captured clear, and
   only changes outcomes when count >= 2 survives a boundary. A2's pre-committed bound allows a
   legit sneak-edge row to reach max count 2. Under decay + flagAt=2 that row flags - the same
   zero-margin argument as K1 made for arm-at-2. Pre-committed reading: decay is viable only at
   flagAt >= 3, and only for configs where every legit row's decay-table maxCount stays < flagAt.
   Any legit row flagging under decay kills decay at that config, full stop.

2. **Lag.** The window counts claims, not time; a lag burst that delivers 20 packets in one tick is
   the same event stream to both policies. No new FP vector from lag itself. A lag-induced
   interleave (claim, claim, supported...) scores identically in replay regardless of wall-clock.

3. **Uncaptured clears: teleport / setback / post-flag.** gndclear capture covers exempt/grace
   ground-claim resets only (Movement.java:336). Own-teleport (Movement.java:83), setback, and the
   post-flag resets (342/344) all clear the live window without a capture line; the replay scores
   those spans as window-lived-on. Under the DECAY table this retains strictly MORE evidence than a
   future decay-policy jar would keep across the same boundary, so:
   - legit reading (FP side): over-count - conservative, the safe direction;
   - cheat reading (catch side): a spoofer shedding evidence via teleports looks WORSE in replay
     than the real policy would treat them - the table can over-promise catch rate. Read cheat-row
     wins under decay with that discount; K3-style win claims need the jar, not just the table.

4. **Exempt vs grace indistinguishable.** Captured clears do not say which kind they were, so decay
   is scored at every captured clear. On legit rows that retains more evidence than a grace-only
   decay would - conservative for FP reading (documented in the GroundReplay javadoc).

5. **Arm-latch semantic is a promotion-patch decision, flagged not hidden.** The replay drops the
   arm latch when the surviving count falls below armAt (arm can re-fire). The live shadow arm
   (patch 4/5) latches until a full clear. If the promotion patch chooses hold-until-clear, arm-edge
   counts will differ from the table (fewer). The table's choice is the conservative one for
   spotting bad arms (more edges, more chances to see a legit WOULD-fire under K2).

6. **maxCount is the battery number to watch** on legit rows under decay: it is the direct analog
   of the K1 arm-level measurement, and it is already printed per config.

## Test coverage added

GroundReplayTest: consecutive grace boundaries (UUU, clear, clear, U) - two decays forgive the two
oldest claims; the arm latch drops only after the second decay and re-fires on the next claim.
Existing pins cover: oldest-only forgiveness, no-op at zero, un-flag without clear, latch drop
semantics, and the grace-farming win case.

## Verdict

Patch 6 stands as shipped. Every identified inaccuracy is in the conservative direction for the
FP decision the table feeds; the two places the table can over-promise on the CHEAT side
(uncaptured teleport/setback clears, arm-latch semantics) are documented here and belong to the
promotion patch's review, not to this measurement stage.
