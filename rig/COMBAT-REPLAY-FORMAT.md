# Synthetic legit combat replay corpus v1

This corpus is inspired by the recorded10-11 false-positive families. It is NOT
those players' captured traffic, a Minecraft physics simulation or a native
server replay. Unit PASS cannot close the owed10-11 rig regression gate.

## Transport and provenance

UTF8JSON root: schema=`meshac.combat-replay.v1`, provenance=`synthetic`, cases=[...].
Do not label generated samples as player recordings. Future real-capture support
must introduce its own provenance fields/source references, redaction and loader;
this loader intentionally rejects real-recording claims.

Each case has a unique stringid, nonempty strictly increasing tick samples,
one attack and an expected geometry result. Each sample stores:

- tick: integer observation order, not wall time.
- eye: finite[x,y,z].
- box: finite[minX,minY,minZ,maxX,maxY,maxZ], noninverted.
- visibility: visible/blocked/unknown, supplied observation rather than a real
  block-world clipping result.

Attack stores normalized finite look[dx,dy,dz], finite nonnegative sentMs and
handledMs, with handling not before sending. These clocks are separate on
purpose: a server-handled burst does not rewrite a legal client's click rhythm.
No real network clock alignment is implied by these synthetic numbers.

Expected values: allow_geometry, deny_reach, deny_los, deny_aim. The pure oracle
tests3.2reach across coherent current/historical pairs,6block ray/box segment
intersection with0.6inflation, and recorded visibility. Unknown visibility cannot
prove a blocked hit. It does not reproduce repeated-miss counters, verdict heat,
packet gates, damage application, entity ticking or version-specific behavior.

## Coverage

12cases: corner strafe, closed door after legal hit, bad-ping delay, bounce,
vertical moving target, moving attacker, current reach entering since old
history, large-entity edge, unknownLOS and3geometric positive controls.

13test methods include all12case outcomes, malformed schema/provenance/geometry,
nonfinite values, duplicate cases, impossible timestamps, unsorted samples,
separate sent/handled clocks, real bounce endpoint aliasing and immutability.
The batching/bounce tests also guard production's evidence-only handling paths.
The geometry implementation is an independent Python oracle, not live Minecraft
code. Native replay and full Java engine geometry remain required.

Run from repo root:

```
python3 rig/combat-replay.py rig/corpus/combat-legit-synthetic-v1.json
python3 rig/test-combat-replay.py
```

This reuses no test servers, worlds or client dirs; it reads only source/corpus.
Integration into a future all-tests command should keep the synthetic/native
labels intact so a pure test cannot be mistaken for a clean actual10-11 replay.
