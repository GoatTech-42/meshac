# sim and rig test tags (static review plus 3 timed runs, Oct 16, nice 19, box lightly loaded)

Historical docs-branch result:38registered sim tests passed3of3runs. Candidate9 adds tests; see rig/CANDIDATE9-INTEGRATION.md for its actual run. Compile takes about 5 s. Nothing was changed in the tests or scripts, this file only tags them.

## tags
- LONG: SizeBytesInvariantTest. 17-21 s per run, about 3000 rounds of 4096-entry sections. It is nearly the whole suite runtime. Deterministic (seeded Random 1234, 99), so slow but not flaky. Candidate for a `quick` skip flag, or cut rounds to about 500 for pre-commit runs.
- WALL-CLOCK (can fail on a loaded box, no logic bug): 
  - VeilPerfTest: "50000 pair decisions < 400 ms per cycle", includes JIT warm-up. Measured well under the ceiling here (whole test 0.2-0.4 s).
  - EntityGridTest: "grid rebuild + 25 queries < 5 ms per tick", includes JIT warm-up. Tightest ceiling in the suite. First to fail if the box is under build load.
  - VeilPerf3Test: "fake roll < 100 ns per call", the message says "even on a loaded box" but a heavy gradle build next to it can still trip it.
  Do not run these inside the heavy-build lock window. If one fails, rerun alone before treating it as a regression.
- BENCHMARK, not registered, never run by run-tests.sh: LosSnapshotBenchmark, Recorder40Benchmark, VeilBench, LadderSim. Fine to leave out.
- SEEDED-RANDOM (deterministic, fine): VeilPerf2Test, EntityGridTest, NeighbourGridTest, VeilBorderTest, SizeBytesInvariantTest.
- FAST and pure (everything else): all under 0.7 s, no clocks, no files, no network.
- DESCRIPTIVE-ONLY: CombatFeaturesTest prints "descriptive only" with 11 control assertions; it cannot fail on combat quality, only on math.

## rig python tests (rig/test-*.py, not run by sim/run-tests.sh)
- NETWORK/TIMING: test-tcp-stall-proxy.py. Opens local sockets on ports found by bind-and-close (a small race for the port), starts a subprocess proxy and sleeps 0.1 s between steps. Most likely flaky test in the repo. Needs 127.0.0.1 only, no server.
- SMALL and static (no sleep, no network): test-candidate5-combat, test-candidate6-combat, test-combat-feature-wiring, test-combat-log-wiring, test-combat-recorder-wiring, test-combat-replay, test-combat-trace, test-fp-combat-hardening, test-los-verdict, test-stall-reconciliation-wiring. They check source and files, so they fail only if the source moves.
- test-dense-profile-plan.py: one time reference, pure unittest.
- Not reviewed: rig/botrig.sh, run-server.sh, smoke.sh (they start servers or bots, so they were not run) and the rig/tests directory.

## how to run quick
Not scripted. Suggested: run-tests.sh with SizeBytesInvariantTest skipped for local edits, full run before a commit that touches veil code.
