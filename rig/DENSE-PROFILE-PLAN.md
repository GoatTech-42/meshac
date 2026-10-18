# Dense40player harness proposal (not execution)

No connections, process control, game fixture setup, jar change or GC flag change
is implemented by these scripts. Run `python3 rig/dense-profile-plan.py` to emit
the proposed matrix; feed a measured JSONL file via --summarize for descriptive
statistics. It never generates a server-performance PASS. Five pure tests cover
plan generation, empty-data rejection, aggregation and overlap rejection.

## Rig handoff and resource gate

Private meshac-red only, reuse existing arena/world/server. No ilhsmc/public
routes. Rep must approve synthetic protocol-bot route and measured memory budget
before40connections. Do not spawn40rendered Minecraft clients. Proposal retains
at most2native clients for visual proof; protocol bots use a shared bounded
non-rendering implementation. Measure its actual heap/RSS at1and4bots first.
Keep at least2GiB MemAvailable throughout. Disk3GiB is an additional proposed
abort guard, not a replacement for parent's current storage/resource rules.
No collector starts server/processes, kills processes, or requests GC. If a stop
condition fires, stop the harness's own load via the supported disconnect path;
report to rep rather than killing unrelated processes. One owner/one run at a
time under parent's rig/build coordination.

## Paired, reproducible matrix

Use1,4,10,20,40connected active players. At each level use60s warmup,120s
measurement and3repetitions, alternating candidate/baseline order to avoid warmup
bias. These are proposed durations, not commitments. Include identical seed,
positions, attack/movement cadence, mob count and visibility fixture for each
pair. Pair disabled baseline, checks-enabled TRACE_OFF/no capture, and enabled
with4active512-hit captures. No TRACE_FULL. Stop and report failures without
silently reducing fixture density. Keep idle40, dense attacking40, sparse moving40
and dense blocked/visible corner-orbit subcases separate. Use a fixed100nearby
entities/player target with actual measured sample counts; never label merely
connected idle bots as dense active play. Separate attack controls from legal
corner movement/build/inventory/water fixtures; do not use bots as native FP
clearance.

## Measurement adapter to implement after review

On server thread, record monotonic tick entry/exit and aggregate each check's
exclusive elapsed nanoseconds and call count. Suggested buckets: combat geometry,
LOS/cache/clip, history lookup, movement, veil candidate gathering, grid lookup,
veil visibility, recorder, command dump. Do not sum inclusive parents and children.
Measure timer overhead with an empty identical loop and compare instrumented and
uninstrumented runs; never subtract a guessed cost. Bounded per-tick ring, numeric
counters only on hot path. Snapshot/export after measurement, not per-hit printing.
No filesystem IO per attack. Report timing distribution and counts together, so a
faster run that skipped checks cannot pass.

JVM adapter samples MemoryMXBean/GC MXBeans and process CPU time outside hot path,
plus process RSS/MemAvailable. Existing GC logs/JFR may supplement only if enabled
by rep under resource budget; no forced collection/heap dump. GC MXBean collection
milliseconds are aggregate time, not maximum pause. Actual pause samples require
GC notification/log/JFR adapter; unknown max pause must remain unknown. Tick time
includes GC interruption but must not be added to pause time as extra workload.
Record sample interval, lost samples, JVM/build flags and instrumentation SHA.

JSONL reducer schema is provisional: stage,tick,tickNs,checkNs (exclusive bucket
map),gcPauseNs,validPlayers,attacks. Duration units nanoseconds. Missing adapter
values must not be fabricated as zero. Reducer is descriptive and expects valid
input collection; future collector must validate monotonic unique ticks, aligned
window boundaries, config/stage metadata, missing bucket semantics and execution
proof. Its current means treat absent check buckets as zero, so adapter must emit
all buckets every tick. No per-tick CPU allocation can be inferred from tick
wall-clock time alone.

## Evidence / report

Per stage: exact jarSHA/config/fixture and synthetic/native origin, active players,
attack/damage/movement proof counts, ticks and overruns, p50/p95/max tick/check
wall times, call counts, CPU seconds vs wall seconds, RSS/heap range, allocation
if measured, GC count/time/pause quantiles if measured, queue delay, cache hits/
misses/unknown/clip counts, dropped records. Paired ratios with per-repetition
spread, not pooled averages hiding slow runs. Baseline legal damage/execution
within10% is a required native proof gate, not this synthetic reducer's verdict.

Proposed safety aborts: MemAvailable<2GiB, tick p95>50ms, actual observed GC pause
>200ms, route mismatch, throttling/challenge, invalid proof. Thresholds are safety
proposals, not tested acceptance SLOs. Existing parent/rep gates supersede them.
Release needs native FP1105/1107, attack positive controls and supported-version
compile; a CPU improvement alone cannot authorize deployment.
