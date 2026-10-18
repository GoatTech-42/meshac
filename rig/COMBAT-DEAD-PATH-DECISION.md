# Combat path on d124: RETRACTION + corrected state (was: "DECIDED: combat re-arm") - 10-17

RETRACTION (worker error, the worker's own): the 0043 version of this doc claimed
Combat.check() has NO CALLER on d124 ("the attack hook was absent"). That claim came from a
grep scoped to src/main only. The build adds per-version source sets (build.gradle:
sourceSets.main.java.srcDir "src/${project.attack_src}/java"; versions/*.properties pick
attack26 for 26.1/26.2/26.3, attack1211 for 1.21.11), and BOTH attack source sets carry
dev.meshac.mixin.AttackMixin - present since the base commit (c4b3fe8) - which calls
Combat.check(player, e) on every attack. Combat was never dead. Every conclusion built on
the no-caller claim is retracted or corrected here: the control matrix's combat DEAD rows,
the 10b option-C decision, the 0044/0046 emit-path drafts (WITHDRAWN - dropped from the
series 10-17, superseded shas named in the delivery), and the combat rows' "BLIND CONTROL"
wording (fixed in the rows patch).

## The actual d124 state (source-verified at this commit; full-tree evidence:
## rig/COMBAT-SOURCE-SWEEP-20261017.md + the CALLERS.txt archive shipped with this patch)

- Every attack packet reaches AttackMixin: on 26.x via handleAttack(ServerboundAttackPacket)
  (Box javap 10-17: 26.1.2, 26.2 and 26.3 all have the split-packet API), on 1.21.11 via
  handleInteract + dispatch(onAttack) + p.getTarget(level). The target is resolved from the
  level by entityId and null-checked; a same-thread gate and the Meshac.skip gate run first.
- Combat.check then computes the four hard strings (reach, hit through a block, not looking
  at target, multi-aim - the strings 0043 cited at :123/:135/:157/:195) plus the
  evidence-only low-confidence patterns ([combat-evidence] INFO, punishEligible=false), and
  touches Veil.attacked, the recorder, features and the measurement rings.
- On a hard-string hit, AttackMixin calls Verdict.signal(player, "combat", hit, 2) and
  cancels the hit unless the returned step is LOG.
- Verdict.signal under Config.monitor(): accrues REAL mon heat (weight 2), emits the
  grader-visible '[meshac] SIGNAL <player> combat <detail> heat=<mon heat> -> log', alerts
  /mesh watch staff, returns LOG (never cancels). With monitor off it runs the full ladder
  (heat/HOLD/REMOVE/case/kick/ban) and AttackMixin cancels the hit.
- So combat is LIVE on all four builds and log-only exactly when monitor mode is on. The
  grader-visible SIGNAL line the battery needs already exists, with real mon heat - no
  synthetic heat=0, no new emit path.

## Decisions (Instinct, 10-17 10:14pm, under Luke's standing autonomy, report-after - NOT
## Luke quotes; same provenance class as the decisions they replace)

- 10b (option C, "fix the emit path in d124") is RESCINDED: it rested on the false no-caller
  premise. There is no emit path to fix. The original 10b was also an Instinct decision
  under the same autonomy; Luke never quoted either way.
- W1 RE-SCOPED to "nothing to build": combat battery rows target the real Verdict SIGNAL
  line and run under monitor mode. Monitor mode is a row precondition.
- W2 STANDS unchanged as the ilhsmc behaviour rule: d124 thresholds unchanged (the 3.2 reach
  line, the reach>0.5 LOS rule); combat monitor-only; any threshold change needs an FP
  replay at 100/300 ms ping plus a reviewer GO. NOTE: W2 holds by CONFIG today, not by
  construction - with monitor off, d124 combat cancels and punishes. A DRAFT-ONLY proposal
  to bind W2 by construction lives in COMBAT-LOGONLY-BY-CONSTRUCTION-PROPOSAL.md (not
  applied, no GO asked).
- W3/W4 continue against the corrected target (live path, monitor mode).
- W5 (docs cleanup) lands in this patch: matrix combat rows DEAD -> LIVE/monitor-gated;
  COVERAGE-RECHECK, REDTEAM-SCOREBOARD-FORMAT/SKELETON, BOX-BUILD-AND-RUN-PLAN, the
  nb-combat control row and the stale veil border docs corrected; the corrected control
  matrix artifact ships with this patch.
- W6 unchanged: the reviewer gates every merge.

## Reviewer pre-GO notes, carried onto the EXISTING path (from the 0046 review; still live)

- Verdict's monitor path has NO per-player SIGNAL log budget: a high-cps aura spams one WARN
  line + one staff alert per attack. CombatLogBudget bounds evidence lines, not SIGNAL.
  Bounded logging is a precondition for any future change to this path.
- Attack-target validation (null check) and the same-thread gate already exist in
  AttackMixin. A server-thread reentry proof for Combat.check's world reads is still owed
  before any GO that changes this path.
- 26.1.2/26.2 carry the same split-packet API as 26.3 (Box javap 10-17); attack26 serves all
  three 26.x builds, attack1211 serves 1.21.11.
- W4's "every flag carries a CombatRecorder replay reference" is NOT satisfied by the live
  path: check() writes RECORDER but no replay reference rides the SIGNAL line. Open.

Non-goals (unchanged): no enforcement changes, ilhsmc untouched (nothing deploys there), no
threshold changes, no public claims beyond what monitor-mode rig evidence supports.
