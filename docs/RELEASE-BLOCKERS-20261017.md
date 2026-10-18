# meshac release blockers - static status, 2026-10-17 (compiled at series tip a078d4b / patch 0066)

No product code in this document. Every gate to shipping the release jar + Modrinth listing,
what each needs, who owns it. Verdict citations trace to reviewer verdicts relayed via parent
messages (the authority channel); the repo docs only record them. "Box compile x4 done" was per
parent, 10-17 11:40pm; v2 23:43/23:46 PDT per reviewer wording corrections; v3 23:48 PDT per
main's transcript provenance check (11:47pm); v4 23:50 PDT, intro fix per reviewer. VERDICT
PROVENANCE, v4 rule: for the HISTORICAL GROUND-SPOOF PROVENANCE SUBSET ONLY, two relays are
CONFIRMED from main's transcript - the 7:39:54pm relay of patch 3 clean GO (reviewer 5:30pm)
and patch 4 (P3-strong, ceb2540) GO WITH two fixes before the battery run, and a 7:40:41pm
SEND of EXPECT-REBIND-PATCH1 + 0011/0012 conditional patches to the reviewer (a send, not a
verdict). Every other ground-spoof relayed verdict cited below is marked UNVERIFIED. This rule
does NOT cover the 0055-series combat verdicts (0061/0063/0065b SOURCE GOs, 0064/0065rev
conditional PAPER GOs) - those are separately source-reviewed by the reviewer in this session
and are not part of the unverified subset.

The compile/harness facts below are BOX-REPORTED (Box 11:36pm report, relayed by parent; not
re-run by the author): the 63-patch bundle (63 actual patch files spanning the 0001-0066 numeric
range, tar sha256 3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602) applies
CLEAN onto c4b3fe8; the javac harness (sim/run-tests.sh from repo root) ran GREEN, 85 test
mains, exit 0; the full compile x4 (1.21.11, 26.1, 26.2, 26.3; JDK 25) ran GREEN. Box
hand-filtered blank-line -P args; that bug is fixed by proposed patch 0067 (section F). The
harness's javac target classes and run logs are Box-held and not yet reconciled against the
bundle on the author's side.

## A. Gates before the release jar + listing can ship

| # | gate | what it needs | owner | status |
|---|------|---------------|-------|--------|
| A1 | Combat log-only by construction | 0055+0057+0059+0061 code; reentry paper 0063-0065 | reviewer + Box | CODE DONE, scoped SOURCE GOs (0061, 0063, 0065b guard = commit c346db6, patch-file sha 350db90a2f44...; 0065a was a mistaken 0063+0064 concatenation, REJECTED, never cite); paper = 0065rev (sha 1ab9a369...77d0) + 0066 wording-only (sha 1cebcdf3...bded), scoped conditional PAPER GO (thread-assumption inventory, not an all-version proof). The "log-only by construction" claim is SCOPED STATIC CONSTRUCTION EVIDENCE, not runtime/all-version thread proof; build/harness green does not discharge A5. Box compile x4 DONE, full four versions, bundle applied clean onto c4b3fe8 (BOX-REPORTED 11:36pm). OPEN: the listing gate text in MODRINTH-DRAFT-v7 names only 0055+0057+0059+0061 - a v8 draft or reviewer confirmation should fold in 0063/0065 closure before the claim is quoted as verified |
| A2 | javac harness run (test-monitor-invariant + wiring suites) | run the witness set (combat budget cap + window roll, rollback no-reopen, idle tick flush, monitor-toggle flush, disconnect flush, receipt one-shape) against the compiled jar classes | Box | BOX-REPORTED PASS 10-17 11:36pm: sim/run-tests.sh from repo root ran GREEN, 85 test mains, exit 0 (relayed by parent; not re-run by the author; javac target classes and logs Box-held, not yet reconciled). Reported harness pass PENDING RECEIPT REVIEW - it does not by itself close the durable-receipt claims |
| A3 | FP replay at 100/300 ms ping + reviewer GO | legit + cheat replay through tcp-stall-proxy at 100 ms and 300 ms | Box rig + reviewer | NOT RUN. Blocks ONLY a future combat-enforcement re-enable - and only because the shipped combat source cannot punish on those paths BY SCOPED STATIC CONSTRUCTION EVIDENCE (A1); if that construction claim ever fails, A3 blocks the release too. Scoping A3 to future-enforcement does NOT waive A5 or A7. The listing's "design targets" heat/lag wording stays until this exists |
| A4 | Screenshots 1-4 | kick screen, /mesh cases, /mesh status, /mesh watch feed - real in-game on meshac-lan | Box / client driver | NOT CAPTURED. Needs the built jar on the test server and a staged flag |
| A5 | Empirical thread proof | one-off thread-assertion run: log Thread.currentThread() at each Verdict route, one pass per version build (0065 paper proposal) | Box | NOT RUN. The 0064/0065 paper is explicitly a conditional inventory until this exists |
| A6 | Zero-FP battery (ground-spoof stack) | battery run on the patch-3+4 jar with TRACE on; K1-K4 scored as pre-committed; GroundReplay over the traces | Box + reviewer | BATTERY EXECUTION GO NEVER GRANTED. Prerequisite verdicts (v3 provenance): patch 3 clean GO (reviewer 5:30pm) and patch 4 (ceb2540) GO-WITH-TWO-FIXES before the battery run - relayed by main 7:39:54pm, CONFIRMED from main's transcript 11:47pm; fix 1 landed as patch 5 (386448d), fix 2 as 0014 (repo record). The later "GO now UNCONDITIONAL" wording for patches 4/5 and patch 1's reviewer GO (repo records a 5:16:58pm relay) are UNVERIFIED - not re-confirmed from main's transcript. Blocks promotion of the windowed nofall evidence (patch 1, changes what flags), classify-mode default-on, and any P3-strong enforcement patch. Does NOT block the release as-is: all of it ships log-only/shadow |
| A7 | Reviewer verdicts, promotion stack | verdicts on readiness-stack patches 6, 7, 8, 9 + series 0010, 0017 | reviewer | OUTSTANDING. Shipped-vs-excluded reconciliation (series tip e9029f3): ALL SIX ARE INSIDE the released 0001-0066 series. 126fac1 (readiness 6, GroundEvidence.decay) ships PRODUCTION CODE with no production call site - inert by static reading only. 392a5f4 (7), 0f88e21 (8), 63656cf (9), afe75e7 (0010), 67945b6 (0017) are rig tooling / specs / docs / test pins with no jar-detection change. The battery jar carrying unreviewed 6-9/0010 is labeled RIG-ONLY. The GroundReplay-lineage static GO (0011-0016 + rebind v5 + 0018, repo records an 8:20pm relay) is UNVERIFIED - not re-confirmed from main's transcript; main confirms only the 7:40:41pm SEND of those patches to the reviewer, not any verdict on them. The reviewer decides whether unreviewed inert production code + unreviewed rig tooling inside the release series gates publication; until that is answered, do not conclude only A4 + A9 remain |
| A8 | nb-cbt combat rig rows (0049-0054) | executor GO; rig run; owed: sky-air premise rig confirmation, clone-20 typed-receipt confirmation, executor gating, per-chunk ownership removal, forceload query-shape rig verification | Box + reviewer + executor | DRAFTED, NEVER RUN, no executor GO |
| A9 | Publication | Modrinth project + jar upload + gallery | Luke | NO GO. Nothing published; draft v7 is current |

## B. What the ilhsmc-held FP replay was blocking

ilhsmc held the box until 23:00 (Box reported clear 11:40pm). The FP replay (A3) itself blocks
only combat re-enable. The RIG TIME it consumed was the shared blocker for A3, A4, A5, A6 and
A8. Host availability is REPORTEDLY cleared; every one of those runs still requires its own
authority and resource/window check before it executes - nothing in this doc grants permission
to touch ilhsmc or any other workload. A2 has already executed GREEN (BOX-REPORTED 11:36pm).
Nothing in A1's code path was ever ilhsmc-blocked (static + reviewer work).

## C. Chunkveil: written vs unwritten

WRITTEN and on the main line (c4b3fe8..a078d4b):
- Anti-xray (fake ores), anti-ESP (entity culling with the per-tick tracked-set grid,
  Veil.java:311-349), Bedrock awareness (BedrockIds, veilBedrock config), viewer epochs.
- /mesh veil (status + stats line), /mesh veil xray on|off, /mesh veil esp on|off (with the
  self-check refusal), /mesh veil off - runtime until reload; config untouched.
- Border leak-1/leak-2 wiring (PlanGuard.check + maskOf + borderUnchanged, BorderGuard.compute):
  ON MAIN, compiled in the 26.3 Box build. Fail-open by design in the uncovered direction.
- Config key typo warning for veil keys; docs/ADMIN.md; docs/veil/* operator + rig docs.

WRITTEN but UNVERIFIED:
- Border replan runtime behavior: compiled, never rig-verified.
- ESP grid real-world saving: code comment states it NEEDS A REAL SERVER PROFILE (untracked
  fraction + grid build cost at real entity counts). The 65 ms -> 12 ms figure is a loaded-box
  rough number from the hardening round, not a profile.
- Geyser/Bedrock client behavior rows (docs/veil/RIG-QUICKSTART.md, CLIENT-TEST-SPEC.md):
  written as checklists, no run receipts on file.
- Tick cost and the 200-ray/tick budget under 40+ players: rig-only unknown.

UNWRITTEN:
- Veil.java:27 TODO: wire veil presets into Config (defaults per Luke: fake ores on, mobs
  culled). Currently per-switch runtime toggles + config keys, no preset plumbing.
- Retroactive resend of updated chunks after a replan: design only (docs/veil/BORDER-LEAKS.md).

## D. In-game admin tools: written vs unwritten

WRITTEN (all on main, op-gated via Perm.admin()):
- /mesh cases, /mesh case <id>, /mesh pardon <id>, /mesh unban <player>, /mesh ban (with
  reason/ladder), /mesh kick <player> <reason> (creates a case), /mesh status <player>,
  /mesh capture start|clear|dump <page>, /mesh watch (flag feed, players only),
  /mesh monitor on|off (flushes suppression receipts before clearing since 0059),
  /mesh check (runtime per-sub-check switch, not persisted, reload restores),
  /mesh reload (config + veil + cases, AdminLog receipt), /mesh veil (above).
- AdminLog ([meshac-admin] audit lines), Discord webhook for cases + first hold
  (fire-and-forget, silent when unconfigured).

UNWRITTEN: none found in the command tree. The only in-code TODO anywhere in src/main is the
veil preset wiring (C above). docs/ADMIN.md covers the staff surface.

## E. Immediate critical path (suggested order, Box's call)

1. A1 reviewer gate confirmation + v8 gate text (v8 draft delivered with this doc; folds
   0063/0065rev into the gate per reviewer wording) -> 2. A7 shipped-vs-excluded decision +
   outstanding verdicts -> 3. A4 screenshots -> 4. A3 FP replay 100/300 (unlocks enforcement
   option, not the release) -> 5. A5 thread-assertion run (one pass per version; stays OPEN
   regardless of A2's harness pass) -> 6. A6 battery (needs its own GO). A2 is a BOX-REPORTED
   pass pending receipt review. A8/A9 stay parked until their owners move.

## F. Small fix proposed (tooling, no product code)

build-all.sh "-P" bug: versions/*.properties files contain blank lines, and the parse loop
emitted a bare "-P=" arg for each one; Box hand-filtered these to get the green x4 compile.
Proposed patch 0067 (commit e9029f3, delivered as 0067-build-all-blank-line-P-fix.patch):
build-all.sh now skips empty lines and column-1 # comment lines. Reviewer: SCOPED SOURCE-DELTA
GO for empty-line and column-1 # skipping ONLY - it is not a general .properties parser:
whitespace-only lines and indented comments still become args (verified ABSENT from all four
current properties files, 10-17), and the unquoted $args split is unchanged. Verified by parse
dry-run only (sh loop: 1.21.11 -> 8 well-formed -Pk=v args, 26.1/26.2/26.3 -> 7 each, zero bare
-P args). Box's green x4 used Box's MANUAL filter, so it does NOT prove the updated build-all.sh
runs unchanged - a Box re-run with the patched script is still owed.
