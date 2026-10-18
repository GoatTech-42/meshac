# Box: one-pass build + battery run plan (bundle HEAD = patch 0025)

> SUPERSEDED SERIES MANIFEST (10-18, patch 0068): the series table in section 0 covers delivered
> patches through 0025 only, and its "verified clean" note applies to a 17-patch predecessor, not
> the 25 it sits next to. The delivered bundle is meshac-series-9353fbd-to-a078d4b-box.tar.gz
> (63 patch files spanning 0001-0066, tar sha256
> 3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602) with its own
> MANIFEST.txt / PATCHES-SHA256.txt; BOX-REPORTED 10-17 11:36pm: clean apply onto c4b3fe8,
> javac harness GREEN (85 test mains, exit 0), full compile x4 GREEN. Follow the DELIVERED-bundle
> manifest for the current apply order; the table below is HISTORICAL. The build/run STEPS below
> remain the intended shape.

## 0. Inputs
- Base: meshac main line c4b3fe8 (bundle base commit 9353fbd).
- Series: meshac-series-0001-0022.tar.gz = 25 format-patch files, git-numbered 0001-0025 in APPLY
  ORDER. Mapping to the delivered numbering (reviewer verdicts cite the DELIVERED numbers):

  | series file | delivered patch | content |
  |---|---|---|
  | 0001 | 1 | windowed nofall evidence + chain-reset + powder band (GO 5:16:58, GC01+GC05 conditions) |
  | 0002 | 3 | O3 soft-grace shadow + classify mode (clean GO) |
  | 0003 | 4 | P3-strong shadow handback + capture + GroundReplay (GO) |
  | 0004 | 5 | FIX 1: clearPendingActions clears shadow + witness pin |
  | 0005 | 6 | GroundEvidence.decay() + decay table (NOT reviewed; no production caller) |
  | 0006 | 7 | grader TEXT2CHECK all 23 checks (NOT reviewed) |
  | 0007 | 8 | newbounds capture spec (NOT reviewed) |
  | 0008 | 9 | grace-decay FP review + pin (NOT reviewed) |
  | 0009 | 0010 | promotion readiness + changelog (NOT reviewed - jar is RIG-ONLY) |
  | 0010 | 0011 | strike unconfirmed GO claim + GS fixtures (static GO 8:20pm, GroundReplay lineage) |
  | 0011 | 0012 | readiness cites conditional GO (static GO 8:20pm, GroundReplay lineage) |
  | 0012 | 0013 | xdotool_legs driver + blink-lane pins (static GO 8:20pm, GroundReplay lineage) |
  | 0013 | 0014 | FIX 2 (K4) + GC01/02/05 + boatfly + coverage/scoreboard docs (static GO 8:20pm, GroundReplay lineage) |
  | 0014 | 0015 | gap rows (nuker/GhostHand/HighJump/Timer/ElytraFly/AutoTotem) + skeleton (static GO 8:20pm, GroundReplay lineage) |
  | 0015 | 0016 | microhop/kb rows + honest ground-spoof red-team plan (static GO 8:20pm, GroundReplay lineage) |
  | 0016 | 0017 | meadow attack specs (MicroHop, NoFall interleave) (NOT reviewed) |
  | 0017 | 0018 | this plan + readiness update (static GO 8:20pm) |
  | 0018 | 0019 | citation correction + rig-only jar label |
  | 0019 | 0020 | grace-boundary D-row grader fixtures (hand-built synthetic unit fixtures; scoped GO) |
  | 0020 | 0021 | grace-decay promotion draft (doc-only) |
  | 0021 | 0022 | grace-decay Stage-1 shadow (log-only, TRACE-gated) + witness + battery plan |
  | 0022 | 0023 | 0020 corrections: D3 comment + synthetic-fixture relabel |
  | 0023 | 0024 | 0016 corrections: GS-C respec, GS-C2 row, scoring fixes |
  | 0024 | 0026 | 0022 corrections: independent telemetry, full boundary capture, per-site witness, battery preamble (NOTE: applies BEFORE the 0014/15 corrections - git order) |
  | 0025 | 0025 | 0014/15 corrections: GC01 300s legs, GC05 executable phases, preconditions, INCONCLUSIVE rule, EVADE harmonization, K4 jar-state (depends on 0026's witness content) |

  CITATION GUARD: the "8:17pm 0011-0013 GO" relay named MONITOR-SERIES items, a different
  numbering - the GroundReplay-lineage GO is 0011-0016 + 0018, relayed 8:20pm. The battery jar
  carries UNREVIEWED patches 6-9 + 0010: label the jar RIG-ONLY in the manifest and never cite
  those patches as reviewed.

  Delivered patch 2 was NO-GO'd and is absent by design.

## 1. Apply + tree check
  git clone <c4b3fe8 base> meshac-build && cd meshac-build
  git am series/*.patch        # 25 patches; 17-patch predecessor verified clean 10-17 on a fresh worktree
  git log --oneline | head -18 # subjects must match the table above; git am rewrites committer
                               # ids, so compare TREES/subjects, not commit hashes

## 2. Compile (JDK 21)
  cd sim && bash run-tests.sh  # ~92 pure sims. FIRST javac check for patch 0014's GroundReplay
                               # K4 section (authored in a JRE-only sandbox; python-verified only).
                               # A compile break here STOPS the pass - report back, do not patch around it.
  ./gradlew build              # the battery jar; record filename + sha256 into the manifest

## 3. Rig run (log-only, monitor mode, TRACE on)
Manifest per session: jar sha256, acts() lines, meadow build sha, per-row config receipts.
RCON side-channel (required): /tp per row; /effect (GC05 jump_boost 60 0 then 1, NO leather boots;
kb rows Resistance II); /item (glide rows: elytra into armor.chest); /damage (totem rows); zombie
pen + boat placements per the spec's G table. Legs driven by rig/xdotool_legs.py (all 7 verbs;
--selftest must pass on the rig before the first row).

ORDER (GO conditions first):
 1. GC01 nb-gc01-sneakedge (loop 5 min), GC05 nb-gc05-jb-powder (JB I then II), GC02 nb-gc02-autostep
 2. GS01-GS03 verdict rows (rebind v5 expectations; checks=('nofall',),('high',),('jump','step'))
 3. newbounds control pairs: fly, speed, combat (LIVE on d124 - monitor mode required, log-only
    real-heat SIGNAL; the 10-17 dead-path note was retracted in 0048), mine, nuker, interact, ghosthand, inv,
    totem, boatfly (meadow -Prig build for the rig BoatFly module), high, timer, glide, kb
 4. nb-microhop-legit (snow ramp + ceiling)
 5. pb rows incl. blink (bl-burst control + BF08/BF14 legit-bunching pins)

## 4. Score
  grade every row with rig/pb_grade.py (checks= scoping per the spec row)
  capture gndclaim/gndclear (TRACE) -> java -cp <suite-out> GroundReplay <logs> ->
  K1-K4 per PROMOTION-READINESS (K4 = per-player maxCount + flag/first-arm PRESENCE, not edges)
  + the decay-on-clear table for the D-rows.

## 5. Pass bars
- Zero bound signals on every legit row (candidate blocks only where the baseline is clean on that
  row; dual-jar partial-block rows are tolerance data - battery nit 1).
- GC01: ZERO nofall SIGNAL over the full 5-minute oscillation. GC05: 0 nofall flags + measured
  struggle dy reported, band untouched.
- Positive control silent = INCONCLUSIVE (blind control), never a pass.
- An FP twin caps the check at SPLIT on the scoreboard.

## 6. Report back
Per-row verdicts + manifests + GroundReplay tables + the jar sha -> worker and reviewer. Any
compile break, blind control, or legit-row flag stops the pass and goes back with trace lines.
