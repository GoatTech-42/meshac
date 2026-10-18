# Candidate4 combat audit

Audited revision: a85a9ef, branch round2-speed-budget-20261016.
Scope: combat lane, source-only. No jar/config changes, builds or client runs.

## Result

Do not clear this candidate's combat enforcement as false-positive-safe. The
new A15 work remains trace-only, but inherited enforcing rules have legal-input
counterexamples. This audit does not estimate live FP rates or prove a native
replay. The supplied tests are source-coupled predicate reproducers: they pass
when the risky predicate remains, and deliberately demand review on source
changes. They are not a Minecraft integration suite or release gate PASS.

Run: `python3 rig/test-candidate4-combat-audit.py`

## Findings

1. **Fast inputs, high risk.** Four successive gaps below70ms return `attack
   rate`. The API is sampled at server handling time. Four legal20CPS click
   inputs, or five ordinary200ms inputs released from a stalled connection,
   satisfy this rule. A handling-time threshold cannot separate those cases
   from an aura. Existing packet-bunching movement allowances do not apply here.
   Tests: `regular_legal_fast_click_inputs`, `lag_bunched_clicks`.

2. **Weak inputs, high risk.** Six attacks with cooldown charge below0.5 return
   `hitting before the cooldown`. Charge is a damage mechanic, not proof of an
   illegal input. Spam-clicking, a weapon change, or delayed/bunched attacks can
   produce low server-side charge. Test: `legal_weak_swings`.

3. **Skilled switching, high risk.** Two different-target turns over40degrees
   within300ms return `snapping between targets`. The comment claims people
   cannot do this; the predicate contains no input impossibility check. Two
   ordinary90degree flicks at250ms satisfy it. Test: `fast_legal_flicks`.

4. **Batching mistaken for multi-target, high risk.** Two distinct-target
   transitions inside50ms within10seconds return `two targets in one tick`.
   Three legal target clicks spaced500ms, delivered together, satisfy it.
   Server tick coalescing is not proof that clicks were generated together.
   Test: `distinct_legal_targets_batched_in_one_tick`.

5. **Real short descent mistaken for fake critical, medium/high risk.** Eight
   descending hits with positive fallDistance below0.3 inside10seconds return
   `critical hits from a ... block fall`. Real short falls can satisfy the same
   predicate. It checks fallDistance, not inconsistency against server-observed
   motion. Slabs/edges, knockback and tick timing need native controls. Test:
   `repeated_short_real_falls`.

6. **A15 unresolved, detection gap.** Regular full-cooldown attacks plus still
   same-target aim can be legal. No A15 attack trace/control separation has been
   supplied to this worker. The classifier must not be enabled from cadence,
   aim steadiness, damage rate or damage dealt alone. Test guards the current
   absence of an enforcing metronome rule: `legit_cooldown_aura_overlap`.

Every non-null Combat.check result cancels the attack and sends a combat verdict
in both AttackMixin version paths. This is gameplay impact, not a harmless log.
The above are inherited rules, not regressions introduced by the trace commit.

## Candidate4 movement results and limits

CYCLES.md supplies these observations, not independent audit replays:

- E1 valid vanilla ice control moved78.2blocks in8.8seconds, zero signals. It
  supports that one ice case only; it does not validate combat, all effects,
  ice-to-stone exit, knockback or elytra exits.
- E2 burst+budget was caught four times. It says nothing about whether a legal
  player with bunched attacks can pass combat rate/multi-target checks.
- E3 250ms Blink gained no meaningful advantage and had zero signals. Keep it
  classified as lag-sized/no advantage, not a cheat miss that needs punishment.
  Combat should tolerate equivalent honest delivery patterns too.
- E5 paced step failed to cross the wall, with zero signals. No traversal
  happened; do not add a detector to punish an unsuccessful keypress.
- Earlier candidate2 movement control battery is explicitly invalid/vacuous
  in the ledger. Neither that battery nor source unit reproducers clear it.
- D1 dot-name bypass is addressed by candidate4's platform work but requires
  rep retest. This audit does not independently verify the UUID trust model.

## Needed controls before an enforcement change

Use the existing isolated rig and reviewed candidate; create no server/world
copies. Inspect actual module-OFF, movement and hit evidence, not only logs.

- Fast manual clicks and low-charge sword swings, including weapon changes.
- Legit stationary625ms clicks and1Hz clicks; still aim must remain legal.
- Skilled tracking/flicks, mob farming and legal switching, including while
  riding or exiting a vehicle, gliding/landing and using effects.
- Real upstream packet batching200-1000ms and3seconds, plus lowTPS and normal
  latency. Capture both attacker/target movement and handling/arrival timing.
- Short real descents/slab edges, knockback, wind charges and slow-falling.
- Both AttackMixin packet families (1.21.11 versus26.x), then every supported
  version's legal control battery. No untested version is declared clean here.

Recommendation: do not turn these heuristic-only signals into new enforcement
claims. The representative should decide whether to quarantine them while
implementing server-state consistency checks, then run the actual negative and
positive controls. No production policy was changed by this audit.
