# Box run specs v5 (10-18) - reviewer v4 round folded. Supersedes rig/BOX-RUN-SPECS-v4-20261018.md.

STATUS OF EVERYTHING BELOW: PROPOSAL. No confinement runner exists yet and nothing here is
implemented or verified. No run of any spec starts without the reviewer's GO on this spec,
the runner verification in section 0, and (for spec S) the owner approvals named in it.

SCOPE LINE (unchanged, correct per reviewer): no spec ever touches an EXISTING service:
ilhsmc, emberstead, dl-share, backups, docker volumes, PID 633625, or any process, port, or
file of an existing game server. The only server a spec starts or signals is its OWN freshly
launched disposable server inside its owned run dir and network namespace.

## 0. CONTAINMENT RUNNER - prerequisite phase, PROPOSAL until verified
Before ANY spec runs, Box implements a small containment runner and verifies it on a
NON-GAME throwaway workload (e.g. a shell that sleeps, forks, setsids, and tries to open a
socket). Phase 0 itself is not blanket execution permission: the runner source and the
probe plan are reviewed and authorized BEFORE the probe runs, and the game specs stay on
HOLD until the phase-0 receipts are reviewed. Nothing in this section is asserted to work; the verification receipts are the
deliverable:
1. Probe first, design second: record the actual systemd version and which properties the
   installed version supports on Scope units vs transient Service units (systemd-run --scope
   may NOT carry the service-style namespace/filesystem properties). The runner uses only
   properties proven supported ON THAT BOX, with the probe output attached. If neither unit
   type carries the needed properties, the runner design comes back for review before any
   improvisation.
2. The runner's own source is a reviewed artifact: read and approved like a patch.
3. Containment proof on the non-game workload, all receipts attached:
   - cleanup: `systemctl --user stop <unit>` (NOT `systemd --user ...`), then the unit's
     cgroup.procs is EMPTY - including descendants that called setsid/double-forked;
   - network: inside the unit, IPv4 AND IPv6 route tables are loopback-only, `ip link`
     shows only lo, and /proc/<pid>/ns/net of EVERY spawned child (not just the leader)
     differs from the host's netns inode;
   - privileges: NoNewPrivileges is not capability dropping - attach /proc/self/status
     CapEff == 0 (or equivalent) from inside the unit, under an unprivileged uid;
   - filesystem: the workload sees only its declared read-only system trees and its
     writable run dir; an attempt to read a host user file fails, receipt attached.
4. Deadline arithmetic (honest bound): each spec run gets ONE monotonic deadline
   T_end = start + 1800 s; phases share it; the last 120 s are reserved for cleanup. The
   runner is wrapped in `timeout --signal=TERM --kill-after=15s 1980`, so the worst case is
   1995 s, stated as such. No OS mechanism hard-bounds an uninterruptible (D-state) process:
   if cleanup cannot confirm an empty cgroup, the run is labeled INCOMPLETE (not
   failed-clean) and the leftover state is reported by name.
5. Filesystem visibility policy (replaces the four-path lease list): the runner produces the
   ACTUAL runtime closure the build + harness need (JDK, Gradle distribution, sh/coreutils,
   python, shared libraries, any certs) and mounts exactly that closure read-only; host user
   data is invisible; only the run dir is writable; HOME and GRADLE_USER_HOME point inside
   the run dir. The closure list is a verification receipt, not a guess in this spec.
6. Offline build cache: the read-only master cache is hash-listed (input receipt), then
   COPIED into an owned writable private cache inside the run dir (Gradle writes lock files;
   a read-only cache is unusable), within the run's budget. The build then runs offline
   against the copy. NOTE: build-all.sh as shipped forwards NO arguments (verified 10-18:
   it builds `./gradlew clean build -q --no-daemon` with only -P args from versions/*.properties),
   so offline mode requires EITHER a reviewer-gated build-all.sh change OR invoking
   ./gradlew --offline directly per target - the runner picks one and the choice is
   reviewed with the runner source.

## FROZEN SERIES (qualification as narrowed in v4)
Part 2 tar sha256 bbe8f701b5903d8ef0d22a2d2ac4bef5f8de2e1a3bcfc0b5c79129b5ae02b0cd is
reviewer-confirmed (10-18). Part 1 (worker-cited tar sha256
3f0dea27f1e67380114c893716886d9ec2a017dc2704cf54f1d69b6e24aa9602), every per-patch apply
result, and the result tree hash 7e794648d90dd2b9f48ee4a0218ec00d83247ca9 are WORKER-STATED
until spec M proves them. Pre-apply sha256 verification of every Part-2 file; mismatch = STOP.

## FROZEN TARGETS + TOOLCHAIN (unchanged)
1.21.11 (JDK 21 runtime), 26.1.2, 26.2, 26.3 (JDK 25 runtime). Driver JDK 25.
Harness exactly `sh sim/run-tests.sh` (now includes LadderTierATest, patch 0079).

## Spec M - offline build + apply manifest (runs only after section 0 is verified)
1. Fresh git archive import of meshac main c4b3fe8 (read-only input).
2. `git am` Part 1 in manifest order, then Part 2 in filename order; record each apply
   result (clean / offset / fuzz).
3. VERIFICATION: applied patch bytes == frozen files (pre-apply sha256);
   `git rev-parse HEAD^{tree}` == 7e794648... (worker-stated until proven here).
   MANIFEST.txt + PATCHES-SHA256.txt; actual HEAD recorded for reference.
4. Offline build per target via the runner's chosen mechanism (section 0.6) against the
   private writable cache copy. A cache miss = STOP, report the missing coordinates.
5. `sh sim/run-tests.sh`; test-main count + exit code. The expected mains are exactly the
   list wired in the FROZEN TREE's own run-tests.sh - record the list and count from the
   tree, never from a worker-stated number. The frozen closure (part 1 + part 2) ends at
   0072v2 / tree 7e794648: LadderTierATest and MeshCommandsDocSmokeTest wiring (0079/0083,
   reviewer gate pending) are NOT in it and are not claimed here. If those land before the
   run, they enter as a NEW frozen part-3 tar with its own reviewer-confirmed sha256 and
   tree hash - the closure and the expectation update together or not at all.
Pass: every patch clean; tree hash matches; four targets build GREEN offline; harness GREEN.
Fail: anything else - attach output, STOP, no other spec runs.

## Spec S - listing screenshots 1-4 (TWO independent approvals still needed before ANY run)
APPROVALS: (a) the client/world/server EXECUTION itself needs its own approval; (b) the kick
in shot 1 - in EITHER form - is a removal action against the inherited log-only rig
restriction and needs Luke's scope decision, routed via the parent. This spec assumes
NEITHER. The reviewer holds the kick question until the spec is otherwise clean.
Shot 1 candidates once (a)+(b) exist - ANY resulting screenshot is LABELED with how it was
produced: (i) manual op `/mesh kick SUBJECT` = "manual staff action on an isolated rig, NOT
detection evidence"; (ii) detector-driven removal = "detector-driven on an isolated rig,
disposable account". A shot-1 image is never represented as detection proof on its own.
Shots 2-4 (after approval (a), monitor ON, log-only): `/mesh cases`; `/mesh status SUBJECT`
with fresh logged flags; `/mesh watch` feed with a reconnect-and-fresh-burst boundary (feed
timestamps must postdate the reconnect). Roles: SUBJECT/OPERATOR assigned by Box from its
VERIFIED rig inventory into the run plan; no assumed names. Receipts: shot1..4 PNGs with
production-method labels, the log segment for each shown event, the rejoin/fresh-event
evidence. Reviewer pixel-inspects and approves before any gallery use; this spec uploads
nothing.

## Spec C - legit-controls rerun on the frozen jar (substance unchanged from v3/v4)
Monitor ON throughout. Detector-live proof first (flight burst MUST signal, else stop).
Controls 1-7 as v3 (sprint-jump; wind-charge; TNT/crystal kb; elytra x3; tunnel mining;
lag-proxy walk via the in-namespace proxy; GC01/GC02/GC05), each with its named coverage
trace lines. Pass: positive control signalled AND zero legit SIGNALs AND coverage lines
present; anything else fails as v3 describes.

## Spec V - per-version runs (substance unchanged from v3/v4)
Each frozen target, its jar from spec M, its runtime JDK: boot (init line), disposable bot,
60 s legit movement (zero SIGNALs, no crash), positive-live control (flight burst MUST
signal) on EVERY target. A failing version comes off the published list until fixed and
re-run. Receipts: per-version logs + pass/fail table.
