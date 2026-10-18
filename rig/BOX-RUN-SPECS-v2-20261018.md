# Box run specs v2 (10-18) - SUPERSEDED by rig/BOX-RUN-SPECS-v3-20261018.md (reviewer v2 round: tree-hash + applied-bytes verification instead of HEAD equality; bound timeout/systemd-run cgroup caps; pidfd + process-group owned-tree supervision; network namespace confinement for runtime phases; spec S enforcement-ON reclassified as a policy change needing Luke's authorization with a default no-policy-change route; roles not assumed account names). Kept for history; do not run from this file.

NEVER ilhsmc, NEVER the live server, no live deploy. No run starts without the reviewer's GO
on this spec. "Frozen series" below is exact: no floating tip, no either/or commands.

## FROZEN SERIES (the only input any spec may build from)
- Part 1: delivered bundle 0001-0066, tar sha256
  3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602 (Box holds it since 10-17;
  its MANIFEST.txt / PATCHES-SHA256.txt is the apply order).
- Part 2: six patch files in meshac-frozen-0067-0072v2.tar.gz (tar sha256
  bbe8f701b5903d8ef0d22a2d2ac4bef5f8de2e1a3bcfc0b5c79129b5ae02b0cd), applied in filename
  order, commits e9029f3, a968656, 95233cd, 4e498e9, 31c4606, b5fa854, per-patch sha256 in
  the tar's FROZEN-MANIFEST.txt. Product tip = b5fa854 (0072v2).
- Verification: before any apply, sha256 every Part-2 patch file against FROZEN-MANIFEST.txt;
  a mismatch = STOP, report, change nothing. Any patch after b5fa854 is outside the frozen
  series and requires a spec amendment.

## FROZEN TARGETS + TOOLCHAIN (no floating versions)
- Targets, exactly the four versions/*.properties at tip: 1.21.11 (java_min 21, attack_src
  attack1211), 26.1.2 (mc_range 26.1.x, java_min 25, attack_src attack26), 26.2, 26.3
  (java_min 25, attack_src attack26). "26.1" anywhere means exactly 26.1.2.
- Toolchain: driver JDK = JDK 25 (build-all.sh requires it); Gradle wrapper pinned by the
  repo (gradlew, --no-daemon); 1.21.11's jar is built by the same script through the Gradle
  toolchain declared in its properties. Build command, exactly: `sh build-all.sh`
  (no alternatives). Harness command, exactly: `sh sim/run-tests.sh` from the repo root.
- Runtime JDK per target: 1.21.11 on JDK 21; 26.1.2 / 26.2 / 26.3 on JDK 25.

## COMMON RUN LIMITS (every spec, hard)
- Wall clock: one spec run <= 30 minutes. Deadline receipts: runner logs start/end epoch
  seconds and the host clock source for every spec.
- Memory: server JVM -Xms2G -Xmx2G; gradle runs with --no-daemon (script default) and
  org.gradle.jvmargs=-Xmx1G.
- Process tree: at launch the runner records the server JVM's PID + exact start time
  (ps -o pid,lstart -p). Any kill verifies that PID+start-time pair first, then SIGTERM, 10 s,
  then SIGKILL to the SAME verified PID only. Never pkill/pattern-kill by name.
- Ports and network: server binds server-ip=127.0.0.1, port 25565 only; the lag proxy (spec C
  row 6) binds 127.0.0.1:25566. No other listeners. Loopback only: the runner asserts the
  server PID owns no non-loopback sockets for the whole run; outbound egress is out of scope
  by construction (nothing in any spec needs it).
- Filesystem: one owned run dir per spec (/opt/box-runs/<spec>-<epoch>/): world, config,
  logs, FIFOs, receipts. Nothing outside it. Owner effects scope: no external accounts or
  services, no Discord webhook (disabled in the run config), no notifications, no DNS, no
  writes outside the run dir; side effects = the run dir plus the copied-out receipts bundle.
- Cleanup + deadline receipts: stop the server (verified-PID rule above), verify the process
  group empty, copy receipts (full server log, manifest, timings, config diff) out of the run
  dir, then delete the world. Every spec reports a cleanup receipt (stop time, reaped PID,
  receipts path). On ANY failure the same cleanup runs; a failed cleanup is itself reported.

## Spec M - current full apply manifest at the frozen tip (prerequisite for S, C, V)
1. Fresh git archive import of meshac main line c4b3fe8 (same source as the 10-17 bundle).
2. `git am` Part 1 in its manifest order, then Part 2 in filename order; record each patch's
   apply result (clean / offset / fuzz).
3. Emit MANIFEST.txt + PATCHES-SHA256.txt (same shape as the delivered-bundle manifest) plus
   the resulting tree's HEAD commit (must equal b5fa854) and a tree hash.
4. `sh build-all.sh`; record per-target result for the four frozen targets.
5. `sh sim/run-tests.sh`; record the test-main count and exit code.
Pass: every patch clean; HEAD == 587ba3b; all four targets build GREEN; harness GREEN with
the count reported (85 mains at 0066 on 10-17; report the delta by name if it differs - 0072v2
adds MeshCommandsDocSmokeTest). Fail: any apply conflict, build error, or harness failure - attach
the exact output, STOP, other specs do not run. Receipt: manifest + build/harness logs.

## Spec S - listing screenshots 1-4 (pixel-inspected; reviewer approval before any use)
Account + scope: the rig's fixed disposable test account only (the rig-owned bot account,
never Luke's, never a real player). No external notifications; no bans or kicks beyond that
disposable account; Discord webhook disabled in the run config. Everything on the isolated
rig world from the common limits.
Policy note: shot 1 needs enforcement ON (monitor off) on this isolated rig - a temporary,
spec-scoped exception to the log-only default, for the disposable account only.
1. Shot 1 (kick screen): monitor off, subject runs the scripted flight burst until removal;
   capture the subject client's kick screen.
2. CONFIG RESTORE (runs even on failure): monitorOnly back to true, veil keys back to the run
   config's defaults, `/mesh reload`, then VERIFY: `/mesh monitor` reports on and `/mesh veil`
   reports the config state. A failed restore = spec FAIL, reported, rig left in the last
   verified state, never half-restored.
3. Shot 2: op client `/mesh cases`. Shot 3: `/mesh status <subject>` with fresh flags.
4. Shot 4 (`/mesh watch` feed): subject rejoins (a kick is not a ban; if the ladder escalated
   to a tempban: `/mesh unban <account>` + `/mesh pardon <case>`, then verify `/mesh status`
   shows heat 0 and no open offences). Fresh flight burst; the feed lines in the shot must
   carry timestamps AFTER the rejoin time (proof of a fresh event, not a stale capture).
Receipts: shot1..4 PNGs (legible, in-game), the server log segment covering each removal and
flag shown, the rejoin/tempban-clear evidence for shot 4, and the config-restore verification
lines. The reviewer pixel-inspects the PNGs and approves before any gallery use; this spec
uploads nothing.

## Spec C - legit-controls rerun on the frozen jar (readiness blocker 1)
Monitor mode ON for the whole spec (signals logged, nothing enforced).
Detector-live proof, per run: BEFORE the legit controls, one scripted flight burst MUST
signal ("fly") on this jar; if it does not, the jar's detection is not live, the spec stops,
and no zero-signal result counts for anything.
Controls (each with its required coverage evidence - the named trace lines must appear in the
control's log segment, proving the detector RAN during the control, not merely stayed quiet):
1. Sprint-jump course. Coverage: [trace] movement lines across the course.
2. Wind-charge jumps. Coverage: windAt-stamped movement lines.
3. TNT + crystal knockback. Coverage: kb evaluation lines during the blast windows.
4. Elytra x3 (glide from y250, rocket glide, rocket-free from y90). Coverage: glide/energy
   trace lines through each descent.
5. Hand-mining tunnel course. Coverage: dig start/stop lines.
6. Lag-proxy walk (rig/h/lagproxy.py, 800 ms freeze every 4 s, 24 s; proxy bound per the
   common limits). Coverage: timer/blink evaluation lines across each freeze window.
7. GC01 / GC02 / GC05 per rig/newbounds_controls.py (their receipts are DRAFT-until-receipt;
   this run converts them). Coverage: the rows' own receipt shapes.
Pass: positive control signalled AND zero SIGNAL lines in every legit control AND every
coverage line present. Fail: any legit signal (attach full [trace] context; the listing's
legit-play wording is then blocked), a silent positive control, or missing coverage lines
(reported as "detector not proven live for that control", NOT as a pass).

## Spec V - per-version runs (gate for the published version list)
For each frozen target (1.21.11 on JDK 21; 26.1.2, 26.2, 26.3 on JDK 25), its jar from spec M:
1. Boot a fresh flat world; the meshac init line must appear in the log.
2. Disposable bot account connects; 60 s walk + sprint-jump; zero SIGNALs, no crash.
3. Positive-live control on EVERY target: scripted flight burst MUST signal ("fly") - quiet
   here means dead, not healthy, and fails the version.
Pass/fail per version as in v1: a failing version comes off the published list until fixed
and re-run. Receipts: per-version server log + the pass/fail table + per-version positive
control line.
