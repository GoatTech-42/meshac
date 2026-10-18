# BATTERY-SHADOW-DIVERGENCE-PLAN - Stage-1 evidence for the grace-decay proposal (patch 0022)

Goal: produce the evidence Stage 2 is gated on - the decay shadow diverging (flagging where the live
window stays silent) on NoFall cheat rows, and ZERO shadow flags on every legit row. Log-only
throughout: the shadow never flags anything live; we are only reading trace lines.

Light jobs only, ilhsmc-priority: everything runs on the rig against a throwaway jar; ilhsmc is not
stopped, restarted, or loaded. One bot at a time, short sessions. GroundReplay cross-checks run
offline on Box, not on the rig.

Setup per row: start the rig server with MESHAC_TRACE=1 in its environment (the shadow is
TRACE-gated; nothing else needed - no config changes), fresh restart between rows, capture the
session log.

## Rows

1. **GS-D - NoFall human=0 + self-damage grace-farming.** Attack-effect proof FIRST, or the row
   is INCONCLUSIVE: (a) bot falls >= 8 blocks with NoFall OFF - observe the health drop (baseline);
   (b) the same fall with NoFall human=0 ON - damage avoided (the spoof is live and bound);
   (c) only then the grace-farming phase: arrow self-damage loop, walk/idle, flat area, config
   receipt `nofall mode=human:0`. MESHAC_TRACE=1 must be set BEFORE the session so the claim
   stream exists; capture gndclaim, gndclear (with decayCount=) and decay-shadow lines.
   EXPECT: repeated `[trace] nofall decay-shadow <bot> count=N liveCount=M liveFlagged=false`
   lines; the live window stays silent (that silence is the hole this proposal closes).
   GroundReplay on the same log: decay table flags, clear table does not (or flags strictly later).
2. **GS-E - NoFall human=0 + water dip.** Same effect preamble (a/b), then bot walks through a
   water pool, dip <= 4 air claims. EXPECT: decay-shadow lines persist through the dip; an honest
   landing still ends them.
3. **Legit medium dip.** Human walks through the same pool, no cheat. EXPECT: ZERO decay-shadow
   lines. Any line here is a kill criterion.
4. **Legit knockback landing.** Zombie pen hit or RCON knockback onto ground. EXPECT: ZERO
   decay-shadow lines (window is short; the decayed leftovers must not reach 3).
5. **Legit medium exits.** Elytra landing, slime bounce, wind-charge exit. EXPECT: ZERO
   decay-shadow lines on each.

## Cross-check (offline, Box)

Run GroundReplay (clear vs decay tables) over every row's log. Every boundary that holds state in
EITHER window now emits a gndclear line (with the shadow's decayCount), so the replay sees every
decay boundary the jar saw - flag/no-flag agreement per row is assertable, and we assert it. The
remaining documented gap: uncaptured clears (teleport/setback/post-flag) keep the harness shadow
counting where the jar shadow has already cleared - that can move COUNTS, not flag presence, on
rows without teleports it does not apply at all. Count equality is not required; flag agreement is.

## Stage-2 gate (unchanged from GRACE-DECAY-PROMOTION-DRAFT.md section 6)

- divergence on rows 1-2, and
- ZERO shadow flags on rows 3-5, and
- GroundReplay flag agreement,
then, and only then, the env-gated Stage-2 swap is proposed with a full battery rerun on the
swapped jar. Rows 3-5 are the pre-committed kill criteria.
