#2second stall reconciliation, bounded time history experiment

Base80bcad5. CombatReconciliation is a pure research reducer, NOT wired into
Combat/Mixins and NOT a claim to fix live stalled attacks. No rewind widened.

## Why history alone cannot establish safety

An arriving attack packet has no authenticated client click timestamp. Receive
order and ping do not prove when it was generated. A2second backlog can therefore
be observationally identical to newly generated packets with old position data.
Simply accepting any2secondold legal eye/box pair creates stale-reach advantages.
Simply rejecting all such packets may flag honest stalls. More history helps
explain order, but it cannot fill missing evidence by itself.

## Proposed path

Record server observations with monotonic receive AND handling timestamps,
dimension, entity identity and accepted server state. Keep acknowledged position
sequence intervals where the protocol actually supports those observations.
Do not substitute client timestamps, RTT/2or a guessed stall start. Target-box
history needs the same monotonic sampling and lifecycle IDs; this reducer only
prototypes position/attack retention, not a complete target collision engine.

Per player: last2.5seconds by elapsed monotonic time, max256position records and
64attack records. Eviction is by time plus hard capacity, not number of game ticks.
LowTPS stretches don't retain arbitrarily old snapshots; packet floods can't
grow memory. Overflow/lost counters and missing context are explicit. Server
clock discontinuity clears the reducer. Caller must clear on disconnect, teleport
and entity/dimension lifecycle transitions before any live integration.

An attack's server-grounded acknowledged interval entirely within the existing
750mswindow yields CHECK_ORIGINAL_WINDOW. It is NOT ALLOW. The usual range/aim/
LOS validators still judge coherent original-window state. A wider/unknown
interval yields AMBIGUOUS_TRANSPORT, never an older reach grant. Missing or
capacity-evicted context yields INSUFFICIENT_HISTORY. Neither uncertainty becomes
a cheat conviction. The representative must decide handling separately: retain
original vanilla damage outcome and withhold statistical punishment, or a
protocol-grounded pending/retry policy if one exists. Do not replay old damage,
teleport a player, grant exemptions or silently drop attacks from this reducer.

Live attack cancellation and existing geometric verdicts are unchanged here.
The endpoint does not expose a complete trusted ack mapping today. Native proxy
traces must establish what can actually be reconciled. This is a design boundary,
not a '2secondlag fixed' report.

## Native draft runner

`rig/fp-combat-native-draft.py` defaults to JSON plan output, no mutation.
Explicit --execute needs rep manifest, prepared client PID/module-OFF pixels,
reviewed jar SHA, existing arena+targets, disabled-baseline damage, resource guards
and (FP-1107) owned existing proxy socket with reviewed meshac-red binding.
It launches no clients/proxy/server/world and changes no jar/config/firewall.

FP-1105runs60s6/8/10CPS plus lateral key changes. It is NOT a full camera/orbit
or moving-target pilot: the rep must provide the existing motion fixture and
validate radius2/2.6/3, pillars/fences/doorway/ledge,40msjitter and150mslag.
The script's fixed cadence has no jitter implementation and is labeled draft.

FP-1107runs120s clicks with existing proxy HOLD500/1000/2000ms,30secondverdict
window. HOLD is real transport stalling but not150/250/400msjitter+loss. Those
other variants and movement/build/inventory/water controls remain separate rep
work. Validate receipts showing queued/flush bytes, not merely OKcontrol reply.
Logs/screenshots/health/raw events are saved, with proofStatusUNVERIFIED until
rep checks actual damage within10% disabled baseline and pixels/motion/receipts.
It never reports zero-signal PASS automatically. No run occurred in this task.

## Tests

Pure reducer tests exercise2.5secondretention,256/64caps, overflow/expiry,
dimension isolation, unknown2secondbacklog, original750msinterval and no ALLOW
result. Static runner tests must verify dry-run, loopback fixture ownership,
meshac-redonly scope and no setup/kill/netfilter actions. Full Fabric/native
integration remains unimplemented. Memory is per-instance; a live map needs
explicit global player/capture bounds before integration.
