# Box run specs v3 (10-18) - reviewer v2 round folded. Supersedes rig/BOX-RUN-SPECS-v2-20261018.md.

NEVER ilhsmc, emberstead, dl-share, backups, docker volumes, PID 633625, or any game-server
process. No live deploy. No run starts without the reviewer's GO on this spec. Identities
(subject/operator accounts) are ROLES: Box assigns the exact names/uuids from its VERIFIED
rig inventory into the run plan before any run; this spec assumes no account names.

## FROZEN SERIES (unchanged from v2, verified by the reviewer)
- Part 1: delivered bundle 0001-0066, tar sha256
  3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602.
- Part 2: meshac-frozen-0067-0072v2.tar.gz, tar sha256
  bbe8f701b5903d8ef0d22a2d2ac4bef5f8de2e1a3bcfc0b5c79129b5ae02b0cd (reviewer-verified
  against delivery), commits e9029f3..b5fa854, per-patch sha256 in its FROZEN-MANIFEST.txt.
  Pre-apply sha256 verification of every Part-2 file; a mismatch = STOP.

## FROZEN TARGETS + TOOLCHAIN (unchanged)
1.21.11 (JDK 21 runtime), 26.1.2, 26.2, 26.3 (JDK 25 runtime). Driver JDK 25.
Build command exactly `sh build-all.sh`; harness exactly `sh sim/run-tests.sh`.

## COMMON RUN LIMITS v3 (every spec; mechanisms BOUND, not just declared)
- Supervision (owned tree, no PID races): every spec process (server, proxy, bots) is
  launched by the runner via setsid (own process group, runner = direct parent = owned
  root); the runner opens a pidfd at launch and waits/signals ONLY through it
  (pidfd_send_signal) - immune to PID reuse. Cleanup: verify the group leader via its pidfd,
  SIGTERM to the whole process group (kill -PGID), 10 s grace, SIGKILL to the group, then a
  /proc sweep for any survivor in the session/group: an escaped descendant = run FAIL +
  report, never silently abandoned.
- Wall clock + cancellation, implemented: every phase runs under
  `timeout --signal=TERM --kill-after=10s 1800` (30 min hard cap per spec run; phases carry
  their own tighter budgets listed inline). The deadline mechanism is the `timeout`
  supervisor above, not a note.
- Resource caps, whole tree incl. Gradle: each run executes inside
  `systemd-run --user --scope -p MemoryMax=4G -p TasksMax=256` (cgroup bounds total RSS and
  task count for the ENTIRE process tree; -Xmx flags alone are not a cap). If systemd-run
  --user is unavailable on Box: STOP and report; no improvised fallback.
- Network, runtime phases (S, C, V): everything runs inside a dedicated network namespace
  (`unshare --net`, only `lo` brought up): IPv4/IPv6 egress and DNS are impossible by
  construction, for the whole process group. Server binds 127.0.0.1:25565 inside the
  namespace; lag proxy (spec C row 6) 127.0.0.1:25566; pre-start bind check fails the run if
  either is taken. The private namespace makes collision with ilhsmc or any host service
  impossible by construction. If the namespace cannot be created: STOP and report.
- Network, build phase (spec M step 4 only): Gradle needs dependency egress. This phase runs
  OUTSIDE the confined namespace with a DECLARED destination scope (Gradle/Fabric/Mojang
  artifact hosts only); the runner captures the build process group's connection list as a
  receipt. Any connection outside the declared hosts = run FAIL. Runtime phases never share
  this exception.
- Filesystem: one owned run dir per spec (/opt/box-runs/<spec>-<epoch>/); nothing outside it;
  no external accounts/services/notifications; Discord webhook disabled in run configs.
- Cleanup + deadline receipts: stop per the supervision rule, sweep, copy receipts out (full
  server log, manifest, timings, config diff, connection-list receipt for M), delete the
  world, report a cleanup receipt. On ANY failure the same cleanup runs; a failed cleanup is
  itself reported.

## Spec M - current full apply manifest at the frozen tip (prerequisite for S, C, V)
1. Fresh git archive import of meshac main line c4b3fe8 (same source as the 10-17 bundle).
2. `git am` Part 1 in manifest order, then Part 2 in filename order; record each patch's
   apply result (clean / offset / fuzz).
3. VERIFICATION (corrected): git am REGENERATES commits (committer metadata), so HEAD sha
   equality is NOT assumed. The evidence is: (a) applied patch bytes == the frozen files
   (sha256, pre-apply, per FROZEN-MANIFEST.txt); (b) the resulting tree hash equals the
   frozen tree: `git rev-parse HEAD^{tree}` MUST print
   7e794648d90dd2b9f48ee4a0218ec00d83247ca9 (the tree of product tip b5fa854). Emit
   MANIFEST.txt + PATCHES-SHA256.txt with the actual HEAD recorded for reference.
4. `sh build-all.sh` (build-phase network scope above); record per-target result.
5. `sh sim/run-tests.sh`; record test-main count + exit code (85 mains at 0066 on 10-17;
   0072v2 adds MeshCommandsDocSmokeTest; report any delta by name).
Pass: every patch clean; tree hash matches; four targets build GREEN; harness GREEN.
Fail: anything else - attach output, STOP, other specs do not run.

## Spec S - listing screenshots 1-4 (pixel-inspected; reviewer approval before any use)
Roles: SUBJECT = rig-owned disposable account; OPERATOR = rig-owned op account (exact
names/uuids assigned by Box from its verified inventory into the run plan). Nothing external.
Shots 2-4 run under monitor mode ON (log-only; no policy change).
Shot 1 (kick screen) - TWO ROUTES:
  (a) DEFAULT, no policy change: OPERATOR runs `/mesh kick SUBJECT <reason>` (a staff action
  against the disposable account on the isolated rig; detector enforcement stays untouched).
  Produces the same kick-screen artifact.
  (b) Detector-driven removal (monitor off on the isolated rig): BLOCKED - this is a policy
  CHANGE vs the inherited log-only rig restriction and needs Luke's explicit authorization,
  routed via the parent. This spec does NOT assume it; without that authorization only
  route (a) runs.
2. Shot 2: OPERATOR `/mesh cases`. Shot 3: `/mesh status SUBJECT` with fresh logged flags.
3. Shot 4 (`/mesh watch` feed): SUBJECT disconnects and rejoins; a fresh flight burst AFTER
  the rejoin; feed lines in the shot must carry timestamps after the rejoin (proof of a fresh
  event, not a stale capture). If route (b) were ever authorized and produced a tempban:
  `/mesh unban SUBJECT` + `/mesh pardon <case>`, verify `/mesh status` shows heat 0 and no
  open offences, before the rejoin.
Receipts: shot1..4 PNGs (route used for shot 1 recorded), the server log segment for each
shown event, rejoin/fresh-event evidence for shot 4. Reviewer pixel-inspects and approves
before any gallery use; this spec uploads nothing.

## Spec C - legit-controls rerun on the frozen jar (unchanged substance from v2)
Monitor ON throughout. Detector-live proof: a scripted flight burst MUST signal ("fly")
before the legit controls, else the run stops and no zero-signal result counts.
Controls 1-7 as v2 (sprint-jump; wind-charge; TNT/crystal kb; elytra x3; tunnel mining;
lag-proxy walk via 127.0.0.1:25566 in the namespace; GC01/GC02/GC05), each with its named
coverage trace lines required in the log segment. Pass: positive control signalled AND zero
legit SIGNALs AND all coverage lines present; anything else fails as v2 describes.

## Spec V - per-version runs (unchanged substance from v2)
Each frozen target, its jar from spec M, its runtime JDK: boot (init line), disposable bot,
60 s legit movement (zero SIGNALs, no crash), then the positive-live control (flight burst
MUST signal) on EVERY target. A failing version comes off the published list until fixed and
re-run. Receipts: per-version logs + pass/fail table.
