# Opt-in combat capture in RAM

Base5e3e796. Source-only; no live command invoked and no native build.

Staff `/mesh` admin permission gates all three subcommands:

- `/mesh capture <online-player> start`: replace/start that player's capture.
- `/mesh capture <online-player> dump <page>`: page1through32,16hits/page,
  oldest retained first. Only the command caller receives the output.
- `/mesh capture <online-player> clear`: erase that capture and free its slot.

No background diagnostic log or file output. No command broadcasts to other
staff. RCON command responses may be retained by its caller; this code doesn't
send evidence to another service. These commands must be tested in the isolated
rig before claiming their Brigadier/version wiring works.

## Bounds and retention

512typed hit records/player, at most4retained player captures. Collection stops
after120monotonic seconds. Expired captures stay available in RAM until explicit
clear, restart/replace or player disconnect; retained expired captures count
against the four-player limit. No hidden eviction of someone else's capture.
Global bound2048records, no unbounded strings/pattern keys. No disk serialization,
threads, timers, external messages or Verdict paths.

Ring overwrites old hits and reports total/overwritten counts. A512hit capture
at20inputs/s retains~25.6seconds, not a guaranteed full120second transcript.
Dump pages are copies, not a frozen session: if a player keeps hitting between
commands, page boundaries can advance. Sequence numbers reveal that change.
Capture after expiry for a stable full pagination. Disconnect clears data for
privacy, so dump before disconnect. Restart loses everything.

## Fields

Monotonic timestamp and sequence, server tick, target entity ID, lateral and
normalized vertical closest-ray offsets, target lateral velocity, charge,
attack-speed attribute, server handling gap, latency, current reach and raw aim
error. Recorder receives these independently of log limiter/Trace.ON, so raw
log exhaustion doesn't stop the capture. This uses the features1/7/10sampling
path: radial eye-target geometry near zero is omitted rather than divided by
zero; skipped/config-exempt attacks are not captured. It is not a complete
network record, frame recording or actual human click timestamp stream.

The recorder is descriptive only and cannot heat/cancel/punish. No feature
independence, automation identity or clean-player verdict is inferred.

## Tests

CombatRecorderTest covers opt-in only, overflow512/600loss count, chronological
paging, max32pages, immutable snapshots,120second stop, global4capture bound
and disconnect/clear. Python wiring guards require admin-root placement,
caller-only responses, no recorder logging/Verdict and append before log gates.
Whole pure suite must pass. No full Fabric compile/native screenshots or actual
command execution is included in this source-only assignment.
