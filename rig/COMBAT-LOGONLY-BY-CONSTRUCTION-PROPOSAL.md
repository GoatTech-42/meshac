# DRAFT proposal: combat log-only BY CONSTRUCTION (option 2, 10-17) - NOT APPLIED, no GO asked

Status: APPLIED by patch 0055 (10-17, parent decision under Luke standing meshac autonomy,
report-after): shape B shipped - Verdict.signalLogOnly (mon bucket + budgeted SIGNAL line +
alert, always LOG) and both AttackMixins call it, never cancel, not cancellable. Pre-GO 1
(per-player SIGNAL budget: 30 lines per 60 s per (player, check), proposal value) shipped in
the same patch. Pre-GO 2 (server-thread reentry proof) and 3 (Box compile + rig FP replay)
remain outstanding - Box compile is the standing gate. Original commissioning note: Commissioned
by the Instinct re-scope (10-17 10:14pm, report-after, not a Luke quote): "draft (2) as a
separate, small, reviewable proposal only: AttackMixin never cancels/punishes combat
regardless of config, so W2 holds by construction."

## Intent

W2 (interim binding rule: d124 combat thresholds unchanged, combat monitor-only - no heat,
no cancel, no punish) holds today by CONFIG: Config.monitor() makes Verdict.signal log-only,
and AttackMixin then declines to cancel because the step is LOG. Flip monitor off and d124
combat cancels hits and feeds the removal ladder. This proposal makes combat log-only by
CONSTRUCTION so a config flip can never turn combat enforcement on.

## Candidate shapes (reviewer picks; both are small)

A. Signal-only cancel removal: in both AttackMixin files drop `cancellable = true` and the
   ci.cancel() line; keep calling Verdict.signal. WEAK shape: with monitor off, combat heat
   would still feed HOLD/REMOVE (kick/ban) even though hits are never denied. Does NOT meet
   W2 ("no punish"). Recorded for completeness, not recommended.

B. Monitor-bucketed combat signals (recommended): add Verdict.signalLogOnly(pl, check,
   detail, weight) that ONLY touches the mon bucket (heat record + the grader-visible
   '[meshac] SIGNAL ... -> log' line + staff alert) and always returns LOG, regardless of
   config; both AttackMixin files call it and drop `cancellable = true` + ci.cancel().
   Combat then behaves exactly like today's monitor mode in every config: real mon heat for
   the rig, zero enforcement ever.

## Blast radius (shape B)

- src/attack26 + src/attack1211 AttackMixin (two small edits) + one new Verdict method.
- Detection semantics, thresholds (3.2 / reach>0.5), the measurement rings and the
  low-confidence evidence path: untouched.
- Monitor-mode behaviour: identical to today. Non-monitor behaviour: combat stops enforcing
  (that is the point); every other check family is unaffected.
- ilhsmc: no change deploys (standing rule).

## Pre-GO requirements (carried from the 0046 review, apply to whichever shape)

1. Bounded logging: Verdict's monitor path has NO per-player SIGNAL budget today - a
   high-cps aura spams one WARN + one alert per attack. Add a per-player SIGNAL log/alert
   budget (CombatLogBudget pattern) BEFORE any combat-path change ships.
2. Server-thread reentry proof for Combat.check's world reads from the AttackMixin path.
3. Box compile of all four version builds; rig FP replay at 100/300 ms ping per W2 for any
   behavioural change; runtime mixin application verified on the rig (source wiring is
   already pinned by test-combat-wiring.py).
