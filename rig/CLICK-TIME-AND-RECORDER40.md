# Click-time mapping: what can actually be trusted

Source review of current Fabric packet mixins and recorderc141f0d. No protocol
injection/native experiment, and no exact click-time mapping is claimed.

## Existing hooks and gap

AttackMixin1.21.11dispatches ServerboundInteractPacket attack action;26.x uses
ServerboundAttackPacket. Both check on server thread and currently lack a matching
one-time decoder receive stamp. Movement already captures an off-thread arrival
before main-thread redispatch. Reusing a global movement FIFO for attacks would
misassociate timestamps across packet types, cancellations and thread queues.

Use-item/block action sequences acknowledge predicted block actions in existing
mixin code. They are not attack timestamps and must not be attached to melee
by proximity. ServerboundClientTickEnd is client-originated. A server tick tag at
handling measures handler scheduling, not the client's generation tick.
Keepalive/nonce echoes prove the peer returned a server-supplied value, not when
a click was produced. TCP gives ordered bytes, not truthful game-event timing.
A modified client can delay, batch, reorder its own action generation, and send
plausible tick/sequence data. Even a signed/authenticated connection doesn't
make its local clock or click claim true.

## Implementable causal instrumentation

1. At the connection/decoder event loop, assign monotonically increasing receive
   ordinal and monotonic receive time exactly once to each actual decoded attack.
   Use packet-object identity plus connection session, bounded weak/explicitly
   cleared transport map, not target ID or an independent queue. A main-thread
   redispatch must consume the original stamp rather than stamp again. Validate
   actual packet lifecycle/thread behavior separately for both version families.
2. On handling, attach server tick, handling monotonic time and accepted-state
   revision. Receive-to-handle gap is trusted SERVER queue delay only. Network
   time before decode remains unknown. Capture target-box/eye world revision
   coherently, including dimension/teleport/entity-generation lifecycle IDs.
3. Optional outbound nonce/ACK records supply causal server-message receive
   bounds for that connection, but never a trusted lower click-generation bound.
   A client custom payload with claimed tick needs a mod and remains an untrusted
   claim. Neither is sufficient to expand the original750msrewind.
4. Queue delay, loss and unknown mappings feed uncertainty handling, not guilt.
   Do not allow old reach, replay damage, ban on unknown click time or substitute
   ping/2as a trusted timestamp. Any actual pending-attack policy must preserve
   vanilla damage/cooldown order and requires separate protocol design/testing.

PacketCausalClock is a pure demonstration of this boundary: server queue/order
are exposed, every nonce/tick/sequence combination still has trustedLowerBound
false and trustedClientTick false. It isn't integrated in live packet hooks.
No server-only method identified here proves exact click generation time.
The2secondstall gate remains unresolved until native evidence/handling is designed.

## Recorderc141f0d at40players

The global max is4captures, NOT40. Forty start requests accept4and reject36;
no silent resource expansion. At40players each20hits/s for60seconds,48000add
attempts yield4800record allocations,2048retained records; uncaptured calls are
map lookup+return.20Hz captured512hit rings overwrite after25.6seconds and report
loss. Original capture start120secondstop and disconnect cleanup unchanged.
Record layout carries8long/double-ish values plus integer fields; exact heap
bytes depend on JVM object layout and are NOT measured here. Guaranteed bounds
are2048retained record objects+4arrays of512references, not a byte estimate.

Pure nice19/JDK25/128MiB process,3fresh JVM trials:

| attempted calls | accepted captures | elapsed ms | ns/attempt |
| ---: | ---: | ---: | ---: |
|48000|4|14.9457|311.37|
|48000|4|26.1605|545.01|
|48000|4|28.5493|594.78|

Median26.1605ms across a simulatedminute, not real TPS or a steady-state JIT
microbenchmark. Includes map lookup/record allocation only, not Combat geometry,
feature math, server scheduling or actual command transport. A live heap/CPU
benchmark is still needed. No automatic log output occurs in the recorder.

Dump is at most16records+1header/command, caller-only. There is **no command
rate limit**: an admin/RCON caller can repeatedly request dumps and flood their
own response channel or grow upstream command logs. Permission gating isn't a
performance throttle. Repeated start replaces a ring, creating allocation churn;
expired captures hold slots until clear. These are bounded retention, not bounded
command churn. A follow-up should add a per-caller command budget and stable
freeze/snapshot paging before claiming admin-facing noise is bounded.

Recorder uses unsynchronized HashMap/arrays, safe only under its current intended
server-thread add/command/disconnect contract. Never invoke from decoder threads;
transport stamps must queue to the server thread or use separate thread-safe
bounded storage. No changed recorder behavior/policy in this review commit.

## Tests

PacketCausalClockTest: exact receive-order, queue delay, no false trust from
nonce/tick/sequence, receive upper bound and cross-session invalidation.
Recorder40Benchmark compiles against recorder source recovered fromc141f0d,
not an invented substitute. No full Fabric or native40player run.
