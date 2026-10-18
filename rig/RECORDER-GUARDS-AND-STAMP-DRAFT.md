# Recorder guards and packet-identity receive stamp draft

Basec141f0d. No live packet hooks, client/rig run or command executed.

## Recorder command and thread guard

Dump budget4accepted pages/caller per fixed monotonicminute, max64caller keys,
expired keys removed on access. Player UUID identifies caller; all console/RCON
calls deliberately share one bucket because the command source does not prove a
unique authenticated human. Overflow callers fail closed. Each accepted page is
at most17response lines. Rejection emits one failure response; upstream command
logs and rejected-command floods are not bounded by this layer. Arbitrary sliding
minute can straddle two windows. Start/clear allocation churn isn't rate-limited
by this dump-only budget and remains separate work.

Combat recorder methods guard server-thread access before touching arrays/maps.
Production supplier uses MinecraftServer.isSameThread with SERVER_STARTED binding
and SERVER_STOPPED unbinding. Default pure-test constructor binds its constructing
thread; it is not used for production recorder. Commands also assert server-thread
entry. Decoder work must never call the recorder directly. Lifecycle ordering
and both Fabric versions still need API compile/native validation.

## Draft PacketIdentityStamps

Per-connection synchronized identity map,256strong packet references max,2.5second
TTL, monotonically increasing receive ordinal and connection session. Equality
collisions don't merge packets. receive(packet,time) is idempotent for the same
object; consume(packet,handlingTime) removes exactly that object's original stamp.
No FIFO association, even when handling order differs. Overflow returns null and
increments loss, never steals another packet's timestamp. Disconnect clears and
increments session. TTL expires abandoned references on receive/consume access.

This is **storage draft only**, no decoder injection/mixin configured. Integration
must stamp once at actual connection event-loop decode and consume on main-thread
attack handling, with cancellation/error cleanup and lifecycle tests on both
packet families. No receive operation should be called a second time on handler
redispatch even though the map is idempotent. Per-instance bound does not replace
an explicit live connection-count/global memory policy. Counters are not authority
for client event generation times; earlier click-clock uncertainty remains.

Pure tests: whole suite including caller budget/thread guards and identity/equals,
redispatch, reverse-order handling, consume-once, overflow, TTL and disconnect.
Full Fabric compile is queued via parent behind Meadow under shared build lock;
not declared compiled from pure tests. No native capture or injection performed.
