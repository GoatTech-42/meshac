# A15 combat trace gate

Status: instrumentation only. No new signals or attack refusal.

The round-1 A15 run dealt damage with cooldown KillAura and no existing signals.
That is a miss, not evidence that regular timing is an unsafe click. Still aim
on a still target and a regular legal rhythm must stay legal.

## Capture

Merge the trace commit into the rig representative's candidate, then enable
`MESHAC_TRACE` for the isolated rig. It adds one `[a15]` line per checked attack
only while tracing is enabled. Do not force tracing on in a published jar.
No worker should replace the rig jar or change the rig configuration.

Keep raw logs and the source commit, jar SHA, actual module settings, player
name, target setup, TPS/lag conditions, input description and screenshot proof
of module ON with each case. Recreate the client profile between cases. The
old A15 preparation script pins an old jar SHA, so its integrity check must be
updated by the rig representative to the actual reviewed candidate, not bypassed.

Fields retain numeric precision: monotonic handling gap in milliseconds,
server tick and tick gap, cooldown, aim error, yaw/pitch and their hit-to-hit
deltas, attacker displacement, same-target centre displacement, velocity,
reach/rewound reach, ping, slot, ground and existing verdict.

These are **server handling times**, not network arrival or human click times.
A zero gap or same-tick group can be packet batching. Centre displacement is
between hits and is -1 after a target change; it is not a movement-packet trace.

## Required cases

- Exact A15 reproduction, 30 seconds ON, no manual clicks, module ON pixels.
- Matched vanilla 1Hz manual sword clicks plus a 30-second verdict window.
- Vanilla repeated clicks near the sword cooldown, still mouse on a stationary
  target. This is a mandatory negative even when timing and aim are perfect.
- Skilled tracking while the attacker or target moves, jumps and strafes.
- Fast legal target switches and stationary farming.
- Low TPS and latency/bunched delivery, plus weapon/slot changes and fight gaps.

Analyze each captured server log with:

```
python3 rig/analyze-combat-trace.py attack.log control.log
python3 rig/test-combat-trace.py
```

The analyzer is descriptive. It never emits an automation verdict.

## Enforcement gate

No thresholds are selected without the real attack trace and matched controls.
An enforcing classifier needs a separate observable violation or a demonstrated
joint feature that distinguishes all the controls, including perfect still aim
and cooldown rhythm. Zero observed control flags is the acceptance budget, not
a claim of universal zero false positives. Timing regularity, full cooldown,
still aim and damage rate alone do not clear this gate.

If the attack and legal controls overlap, report the behavior as not separable
with these signals. Do not turn a statistical preference into a punishment.

MESHAC_TRACE_FULL=1 (rig only): TRACE-kind combat log lines bypass the 120/min per-player budget so offline cadence analysis sees complete captures; EVIDENCE/SUMMARY stay capped. Never set on live servers; MESHAC_TRACE remains the production-safe gate.
