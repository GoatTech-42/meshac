# Chunkveil profiling plan for the rig

Goal: find out where anti-xray and anti-ESP spend time with real players, before anyone changes code for speed. The pure benches only measure our own logic. Minecraft's own block and ray calls are the other half and only a real server shows them.

Rig server only. Production veils stay off. Keep the usual limits: nice 19, no heavy build while the lock is taken, the rig has priority.

## Setup
- Same world on every run: the flat-stone arena with the buried ore field from CLIENT-TEST-SPEC.md, plus one run on a copy of a real world with caves and mob farms.
- Bots, not people, for load: 20, then 40 joins. Each bot flies a fixed path so chunks load at a steady rate.
- Run each case three times for 10 minutes after a 5 minute warm-up. Java warms up slowly, so ignore the first minutes.

## Cases
| # | veil | what it measures |
|---|---|---|
| A | all off | baseline tick time and chunk send time |
| B | xray only, fake ore 0 | plan cost without fake ore |
| C | xray only, fake ore on (default ratio) | plan cost with the fake roll and its exposure checks |
| D | esp only, 20 bots | ESP tick cost |
| E | esp only, 40 bots, 500 entities | ESP at the budget limit |
| F | both on, 40 bots | the combined cost players would see |
| G | esp with the tracked index forced off (fallback) | what the grid saves |

## What to read
- `/mesh veil` second line: average and worst ESP tick time, hides per tick, culls skipped for budget, index state. Read it at the end of each run and write it down.
- Server tick time p50 and p99 for A against F. The target is under 1 ms p99 difference for xray and under 1.5 ms for ESP (same bars as the client spec).
- Chunk send time per chunk, with and without xray, from the sender timings if the rig has them, otherwise from a profiler.
- A sampling profiler on the server for 3 minutes of case F. Look for time in: `Veil.modsCached` (the plan), `Veil.View.at`, `BlockState.canOcclude` and `getFluidState` (called by transparent), `Veil.veiled` (the section copy), `Veil.visible` and the voxel walk, `ChunkMap.entityMap` iteration, and `Veil.tick` itself. Hits in our code under 2% of the tick mean there is nothing to fix.

## Questions to answer
1. First plan per section against the veiled copy per packet: which is bigger? The plan is cached, the copy is made for every send.
2. How often is a section planned twice (stale rebuilds in `/mesh veil`)?
3. How many loaded entities are untracked, and does the index change the ESP tick time (G against D)?
4. Does the section-local lookup patch (docs/veil/patches/0001-view-neighbour-grid.patch) change case C? In a dense synthetic world (3% hidden blocks, 15% fake roll) it saves about 29% of neighbour lookups; the saving is untested in sparser worlds. It costs a 5.8 KB memo reset per section. Apply it only if `View.at` is above 5% of the tick in the profile. Real ore is sparser than the synthetic test, so the saving may be small.
5. With 40 bots, does the 200 ray per tick budget leave a long queue of unchecked pairs (culls skipped for budget climbing without hidden pairs)?

## Decide
- Nothing above 2% in our code: stop, leave it as is.
- The plan dominates: try the patch, rerun C and F.
- The copy dominates: consider planning once per section per version and caching the veiled bytes, a bigger change, discuss first.
- ESP dominates with the budget skipped count high: raise nothing yet. First check whether many entities sit within the 8 block exemption, which costs nothing to check.
- Report the numbers and the profile top 10 back to us. Do not tune parameters on the rig without telling us.

## Pure bench numbers (sim/VeilBench, box, 4 cores, no Minecraft calls, so a floor)
- Anti-xray decision for one 16x384x16 column with 2% ore and 5% air pockets: about 8 ms cold, 1.0 to 1.3 ms warm. It runs once per section when the section is first sent and the result is cached and shared by all viewers. A 21x21 chunk area is about 0.6 s of work spread over the joins, warm, and under 4 s if every column were cold.
- Anti-ESP logic for 100 viewers x 500 entities, every pair once per 4 ticks: 2.7 ms per tick warm (9.5 ms in the first cold round). The ray tests (200 per tick) come on top and are not in this number.
- Sample point buffer reuse: 0.08 ms per tick against 0.60 ms allocating.
The real cost includes block lookups and the ray walk, so profile on the rig before trusting these.

## Ray test cost (sim/VeilRayBench, box, 200 new culls per tick, 1 to 39 rays each, 13 sample points x 3 eye origins, early exit)
Voxel walk plus the RayBlocking decision on a synthetic world (hills, trees, tunnels, bases with walls, slabs, fences, glass). The shape clip is a cube test, so real stairs and slabs cost more.
- Array lookups, a floor: open field 1.1 ms per tick, forest and bases 1.7, inside rock 0.7, surface viewer with targets underground (all 39 rays, long walk) 2.0.
- Slow lookups through boxed hash keys, a pessimistic ceiling: 10 ms, 18 ms (p95 33), 2 ms and 20 ms.
Real getBlockState sits between the two. Worst case is a surface viewer with many entities hidden below it: 39 rays of about 30 blocks each per pair. If the rig tick ms for the veil goes past 10, lower RAYS_PER_TICK first.
