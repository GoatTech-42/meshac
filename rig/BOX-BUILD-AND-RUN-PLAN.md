# Box: one-pass build + battery run plan (bundle HEAD = patch 0018)

## 0. Inputs
- Base: meshac main line c4b3fe8 (bundle base commit 9353fbd).
- Series: meshac-series-0001-0018.tar.gz = 17 format-patch files, git-numbered 0001-0017 in APPLY
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
  | 0009 | 0010 | promotion readiness + changelog (NOT reviewed) |
  | 0010 | 0011 | strike unconfirmed GO claim + GS fixtures (static GO 8:17pm) |
  | 0011 | 0012 | readiness cites conditional GO (static GO 8:17pm) |
  | 0012 | 0013 | xdotool_legs driver + blink-lane pins (static GO 8:17pm) |
  | 0013 | 0014 | FIX 2 (K4) + GC01/02/05 + boatfly + coverage/scoreboard docs |
  | 0014 | 0015 | gap rows (nuker/GhostHand/HighJump/Timer/ElytraFly/AutoTotem) + skeleton |
  | 0015 | 0016 | microhop/kb rows + honest ground-spoof red-team plan |
  | 0016 | 0017 | meadow attack specs (MicroHop, NoFall interleave) |
  | 0017 | 0018 | this plan + readiness update |

  Delivered patch 2 was NO-GO'd and is absent by design.

## 1. Apply + tree check
  git clone <c4b3fe8 base> meshac-build && cd meshac-build
  git am series/*.patch        # 17 patches, clean apply (verified 10-17 on a fresh worktree)
  git log --oneline | head -17 # subjects must match the table above; git am rewrites committer
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
 3. newbounds control pairs: fly, speed, combat (CONDITIONAL - dead path on d124; grade only on the
    emit-fixed build per checklist 10b-C, else INCONCLUSIVE), mine, nuker, interact, ghosthand, inv,
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
