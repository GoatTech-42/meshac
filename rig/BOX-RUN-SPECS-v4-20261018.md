# Box run specs v4 (10-18) - reviewer v3 round folded. Supersedes rig/BOX-RUN-SPECS-v3-20261018.md.

SCOPE LINE (corrected): no spec ever touches an EXISTING service: ilhsmc, emberstead,
dl-share, backups, docker volumes, PID 633625, or any process, port, or file of an existing
game server. The only server a spec starts or signals is its OWN freshly launched disposable
server inside its owned run dir and network namespace. No run starts without the reviewer's
GO on this spec, and spec S additionally needs the owner approvals named in it.

## FROZEN SERIES (qualification narrowed)
- Part 2 (meshac-frozen-0067-0072v2.tar.gz, sha256
  bbe8f701b5903d8ef0d22a2d2ac4bef5f8de2e1a3bcfc0b5c79129b5ae02b0cd): the TAR's sha256 is
  reviewer-confirmed (10-18). Part 1 (the delivered bundle, worker-cited tar sha256
  3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602), every per-patch apply
  result, and the result tree hash 7e794648d90dd2b9f48ee4a0218ec00d83247ca9 are
  WORKER-STATED until spec M runs and proves them - that proof is spec M's whole job.
- Pre-apply sha256 verification of every Part-2 file; a mismatch = STOP.

## FROZEN TARGETS + TOOLCHAIN (unchanged)
1.21.11 (JDK 21 runtime), 26.1.2, 26.2, 26.3 (JDK 25 runtime). Driver JDK 25.
Build command exactly `sh build-all.sh --offline`; harness exactly `sh sim/run-tests.sh`.

## COMMON RUN LIMITS v4 (mechanisms bound; end-state evidence required)
- ONE monotonic deadline per spec run (not per phase): T_end = start + 1800 s (CLOCK_MONOTONIC,
  recorded). Every phase gets the REMAINING budget minus a 120 s reserved cleanup window; a
  phase's `timeout --kill-after` counts inside its own budget. The runner itself is wrapped
  in an outer supervisor (`timeout --signal=TERM --kill-after=15s 1980`) so the worst-case
  total, cleanup and receipt copy included, is 1980 s - the honest bound, stated once.
- Supervision = a dedicated cgroup, not PID arithmetic: each run executes inside
  `systemd-run --user --scope --collect -p MemoryMax=4G -p TasksMax=256 -p NoNewPrivileges=yes
  -p PrivateNetwork=yes -p ProtectSystem=strict -p ProtectHome=tmpfs
  -p BindReadOnlyPaths=<inputs> -p ReadWritePaths=<run dir>`.
  Rationale per reviewer item: cgroup membership is inherited and setsid-proof - an escaped
  descendant stays in the cgroup even across setsid/double-fork, which is exactly what a
  process-group sweep cannot see. Cleanup = `systemd --user stop` of the scope unit plus
  cgroup.kill where available, then verify `cgroup.procs` is EMPTY; anything left = run FAIL.
  pidfd is kept for LEADER liveness/wait only; it is not the kill mechanism.
  If Box lacks systemd-run --user or cgroup v2: STOP and report; no improvised fallback.
- Network end-state (the mechanism above is a PROPOSAL; the REQUIRED evidence is): (a) the
  runtime processes' /proc/<pid>/ns/net inode differs from the host's; (b) inside the
  namespace `ip link` shows ONLY lo and `ip route` shows ONLY the loopback route; (c) every
  runtime child is launched inside the confinement from the start (no inherited host-network
  fds; stdout/stderr/stdin only); (d) NoNewPrivileges and an unprivileged uid (no escape
  privileges). All four receipts attached per run.
- Ports: 25565 (server) and 25566 (lag proxy) inside the PRIVATE namespace's loopback -
  collision with any host service impossible by construction - plus a pre-start bind check
  that fails the run if taken anyway.
- Filesystem leases (exact): READ-ONLY inputs = the c4b3fe8 archive, the Part-1 bundle tar,
  the Part-2 frozen tar, and the prepopulated offline build cache. WRITABLE = the run dir
  only. HOME and GRADLE_USER_HOME point inside the run dir; no host user data, sockets, or
  profile paths are visible to the run (ProtectHome=tmpfs, ProtectSystem=strict).
- OFFLINE BUILD (spec M): the build runs with `--offline` against a PREPOPULATED dependency
  cache (Gradle wrapper dist + artifact cache), prepared by a SEPARATE fetch step that is
  outside these run specs and under Box's own provisioning policy; the cache's content hash
  listing is an INPUT receipt. No network at build time at all: a cache miss fails the build
  = STOP and report the cache gap. There is no observed-egress phase and no host allowlist -
  the v3 connection-capture idea is REPLACED by hermetic offline builds.
- Deadline + cleanup receipts: start/end epoch (monotonic + wall), the scope unit name, the
  empty-cgroup.procs verification, the receipts path. On ANY failure the same cleanup runs; a
  failed cleanup is itself reported.

## Spec M - offline build + apply manifest (prerequisite for S, C, V)
1. Fresh git archive import of meshac main line c4b3fe8 (read-only input).
2. `git am` Part 1 in manifest order, then Part 2 in filename order; record each patch's
   apply result (clean / offset / fuzz).
3. VERIFICATION: git am regenerates commits, so no HEAD-sha assumption. Evidence = (a)
   applied patch bytes == frozen files (pre-apply sha256); (b) `git rev-parse HEAD^{tree}`
   == 7e794648d90dd2b9f48ee4a0218ec00d83247ca9 (worker-stated until this step proves it).
   Emit MANIFEST.txt + PATCHES-SHA256.txt; record actual HEAD for reference.
4. `sh build-all.sh --offline` against the prepopulated cache; per-target results. A cache
   miss = STOP, report the missing artifact coordinates.
5. `sh sim/run-tests.sh`; test-main count + exit code (85 mains at 0066 on 10-17; 0072v2 adds
   MeshCommandsDocSmokeTest; report any delta by name).
Pass: every patch clean; tree hash matches; four targets build GREEN offline; harness GREEN.
Fail: anything else - attach output, STOP, other specs do not run.

## Spec S - listing screenshots 1-4 (TWO independent approvals still needed before ANY run)
APPROVALS: (a) the client/world/server EXECUTION itself (launching the disposable rig server
and a client) needs its own approval; (b) the kick in shot 1 - in EITHER form - is a removal
action against the inherited log-only rig restriction and needs Luke's scope decision, routed
via the parent. This spec assumes NEITHER. The reviewer holds the kick question until this
spec is otherwise clean.
Shot 1 candidates once (a)+(b) exist - and ANY resulting screenshot is LABELED with how it
was produced: (i) manual op `/mesh kick SUBJECT` = "manual staff action on an isolated rig,
NOT detection evidence"; (ii) detector-driven removal = "detector-driven on an isolated rig,
disposable account". A shot-1 image is never represented as detection proof on its own.
Shots 2-4 (after approval (a), monitor ON throughout, log-only): `/mesh cases`; `/mesh status
SUBJECT` with fresh logged flags; `/mesh watch` feed with a reconnect-and-fresh-burst
boundary (feed timestamps must postdate the reconnect; the fresh-event proof from v3 stands).
Roles: SUBJECT/OPERATOR assigned by Box from its VERIFIED rig inventory into the run plan; no
assumed names. Receipts: shot1..4 PNGs with production-method labels, the log segment for
each shown event, the rejoin/fresh-event evidence. Reviewer pixel-inspects and approves
before any gallery use; this spec uploads nothing.

## Spec C - legit-controls rerun on the frozen jar (substance unchanged from v3)
Monitor ON throughout. Detector-live proof first (flight burst MUST signal, else stop).
Controls 1-7 as v3 (sprint-jump; wind-charge; TNT/crystal kb; elytra x3; tunnel mining;
lag-proxy walk via the in-namespace proxy; GC01/GC02/GC05), each with its named coverage
trace lines. Pass: positive control signalled AND zero legit SIGNALs AND coverage lines
present; anything else fails as v3 describes.

## Spec V - per-version runs (substance unchanged from v3)
Each frozen target, its jar from spec M, its runtime JDK: boot (init line), disposable bot,
60 s legit movement (zero SIGNALs, no crash), positive-live control (flight burst MUST
signal) on EVERY target. A failing version comes off the published list until fixed and
re-run. Receipts: per-version logs + pass/fail table.
