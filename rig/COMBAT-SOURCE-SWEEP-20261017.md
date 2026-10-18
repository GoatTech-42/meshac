# Full-tree source-claim sweep - 2026-10-17 (lands with patch 0048)

Scope (parent, 10-17 10:14pm): re-verify every earlier "absent / dead / no caller" claim in
the series against EVERY src/* source set (main, attack26, attack1211, slot26, slot1211,
packets_a, packets_b, bounce26, bouncelegacy) across all four version builds. Method:
full-tree grep at this commit, build.gradle sourceSet wiring, meshac.mixins.json,
versions/*.properties, git show against base 9353fbd. SOURCE-LEVEL wiring verdicts only;
runtime behaviour stays rig-verified separately and is flagged where open.

| # | claim (where) | verdict | evidence | correction |
|---|---|---|---|---|
| 1 | "Combat.check has NO CALLER on d124; the attack hook was absent" (0043 decision doc; CALLERS.txt in the 0043 evidence tar + the 0046 archive; worker reports) | FALSE | src/attack26/java/dev/meshac/mixin/AttackMixin.java:26 and src/attack1211/.../AttackMixin.java:35 call Combat.check(player, e); both in base 9353fbd; wired via build.gradle attack_src + mixins.json "AttackMixin" | RETRACTED; doc rewritten; 0044/0046 withdrawn; pin added (test-combat-wiring.py) |
| 2 | Matrix rows: killaura/reach/LOS/aimbot/multi-aura "DEAD / computed, never emitted" | FALSE | same as #1; Verdict.signal emits the SIGNAL line (monitor: log-only real mon heat; monitor off: enforcement) | corrected matrix artifact ships with 0048 |
| 3 | COVERAGE-RECHECK-20261017 :10 + :33 "dead path on d124 / DEAD (matrix)" | FALSE | #1 | corrected in 0048 |
| 4 | REDTEAM-SCOREBOARD-FORMAT.md:15 "a dead path (combat SIGNAL on d124)" | FALSE | #1 | corrected in 0048 |
| 5 | REDTEAM-SCOREBOARD-SKELETON.md:10 "DEAD on d124, alive-pending-replay on the emit-fixed build (10b-C)" | FALSE | #1; 10b rescinded | corrected in 0048 |
| 6 | BOX-BUILD-AND-RUN-PLAN.md:65 "combat (CONDITIONAL - dead path on d124...)" | FALSE | #1 | corrected in 0048 |
| 7 | nb-combat-cheat row "CONDITIONAL: combat SIGNAL is a dead path on d124 ... BLIND CONTROL" | FALSE | #1 | corrected in the rows patch (0049) |
| 8 | docs/veil/EDGE-CASES.md "known leak, designed but not wired" + BORDER-LEAKS.md "wired on branch veil-border-wiring-20261016 (uncompiled)" | STALE, opposite direction: the wiring IS on main | Veil.java:111 PlanGuard.check(ownFresh, p.mask(), maskOf, borderUnchanged) + BorderGuard.compute at :166-168, present in base 9353fbd | docs corrected in 0048; residuals restated (newly-solid-neighbour stays fail-open; retroactive resend still design-only; runtime replan not rig-verified) |
| 9 | nofall "dead vs interleaved spoof" (matrix row 23; FP-HYPOTHESIS-MATRIX H1) | wiring VERIFIED present; the question is behavioural | Movement.java:318-331 emits "nofall ground spoof" from Movement.check, wired via handleMovePlayer | none; H1/O6/O9 + the B2 re-bind hold already track the behavioural question |
| 10 | docs/FALSE-POSITIVES.md:101 "sign-text + chat-command rules were dead (wrong thread); now run where the packet arrives" | VERIFIED accurate | mixin meshac$sign + meshac$chatCommand run on the arrival thread, thread comments intact | none (historical record) |
| 11 | 0043 sub-claims: Combat.tick wired (Meshac.java:38), Combat.swapped wired (mixin :66), low-confidence patterns evidence-only | VERIFIED present | full-tree grep; pinned in test-combat-wiring.py | none |
| 12 | docs/coverage.md combat rows | no dead/absent claim found | :37 is a coverage statement; :114 documents the sign-thread fix accurately | none |

Root cause of #1-#7: the CALLERS.txt greps ran on src/main only; the per-version source sets
were not in the search path, and the "full-tree" label on them was wrong. Guard:
rig/test-combat-wiring.py (0048) pins the combat wiring across ALL source sets with an
in-process mutation control; the reviewer also receives a corrected full-tree CALLERS.txt +
source archive (build.gradle, mixins.json, versions/*, src/main, src/attack26, src/attack1211)
with MANIFEST shas.
