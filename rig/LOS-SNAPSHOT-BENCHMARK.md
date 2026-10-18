# Bounded LOS snapshot cache experiment

Parentc939c5b. Pure source experiment, not a rig deployment.

## Implementation and semantic trade

LosSnapshotCache holds at most8historical observations per player per tick.
New ticks clear the cache; no visibility is reused across tick/block changes.
Duplicate requests for the same entity in that tick reuse its immutable historical
observation, not a claim about the current world. Eight targets are selected by
round-robin index so later targets aren't permanently starved by list order.
The existing15tick movement/aim history remains. Current attack LOS checks still
clip current geometry directly; this budget applies to tick snapshot work only.

Over-budget/unselected observations are UNKNOWN, never BLOCKED. This avoids
turning CPU exhaustion into a false attack rejection. **It weakens LOS detection
in dense fights**: if any relevant retained snapshot is unknown, it cannot support
an all-blocked conclusion. At100entities most samples are unknown; this can keep
LOS fail-open for that history. Do not call it equivalent coverage or ship it
without the representative accepting/reworking this trade. A smaller demand-driven
active-combat set may preserve better coverage; that is not implemented here.

This is not the dangerous optimization of assuming stationary objects mean
unchanged blocks. Doors, pistons and placed blocks can change with still entities.
The historical visibility cache must never be used as a current-world oracle.

## Pure benchmark, same server, nice19, JDK25, heap128MiB

Workload:20players x100entities x2000ticks per mode. Supplier executes four
synthetic rays of24integer-mixing iterations, always blocked (worst clip count).
Warmup100ticks each mode. Three measured paired passes:

| Pass | Baseline ms/tick | Bounded ms/tick |
| --- | ---: | ---: |
| 1 | 0.4346 | 0.0663 |
| 2 | 0.4093 | 0.0478 |
| 3 | 0.3762 | 0.0727 |

Median0.4093versus0.0663ms/tick, approximately83.8%less synthetic CPU time.
Exact supplier clips8000versus640/tick,92%reduction. Each2000tick bounded pass
returned3,680,000UNKNOWN samples. Caches remain at most8entries/player.

**These are not Minecraft raycast timings, TPS measurements or a light-footprint
release clearance.** Actual collision traversal, entity enumeration, allocations,
world state and mixed game-server load were not measured. Timing varies with JIT
and shared-box load. The clip-count bound is the reliable result.

## Tests

LosSnapshotCacheTest covers same-tick cache hit, next-tick recomputation after a
changed result,8entry bound, fail-open UNKNOWN, supplier not called over budget,
and fair selection covering all100entities. Whole pure suite and8candidate6
wiring guards pass. No full Fabric build/native clients or server/world copies.

Run benchmark after compiling the pure class and sim/LosSnapshotBenchmark.java:
`java -Xmx128m -cp <classes> LosSnapshotBenchmark`
