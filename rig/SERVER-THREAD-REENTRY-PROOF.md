# Pre-GO 2: server-thread / reentry proof for the combat log-only path (0064 rev)

Status: PAPER for review, over the 0063 atomic-drain rework. 0063 reviewer verdict:
scoped SOURCE GO; paper GO only as a conditional thread-assumption inventory. This
revision replaces the generic "vanilla schedules handlers" claim with per-injection-point
scheduling evidence (item 1), and replaces the "every mutation holds the lock" claim with
an explicit enumeration of what is and is not lock-covered (item 3). Reviewer correction
folded in: Ladder.Heat.now does not mutate, so status() read-only stands (item 2 dropped).
Empirical closure (thread-assertion rig run) is proposed, not done. Box compile x4 and the
FP 100/300 ms replay remain Box's gates.

## Scope: every route that mutates Verdict state (HEAT map, H fields: lh, mon, sigLog, trace, name)

signal(), signalLogOnly(), tickFlush(), forget(), clearPendingActions(), note(); remove()
only runs inside signal(); status() is read-only (Heat.now returns the decayed value
without mutating - reviewer-confirmed).

## T1 (load-bearing): every Verdict-mutating route executes on the server thread

### A. CODE-GUARDED routes (the guard or re-dispatch IS the evidence)

The evidence for these is not vanilla scheduling: at the point Verdict is invoked, either
`MinecraftServer.isSameThread()` has been checked true (the API's own definition of the
server thread) or the call sits inside `server.execute(...)` (runs on the server thread by
definition). Line refs are ServerGamePacketListenerImplMixin.java unless noted.

- Combat (both AttackMixins): `if (!...isSameThread()) return;` BEFORE Combat.check or any
  Verdict call. Combat.check's world reads and signalLogOnly can only run on the server
  thread, whatever vanilla packet scheduling does.
- handleMovePlayer HEAD (:32): on the netty pass ONLY `Movement.arrive(player, p)` runs
  and the handler returns (:35). arrive()'s whole body (Movement.java:42-51) touches only
  Movement's own S state (arrivals queue, BlinkClock, burstMark, blinkHit) - NO Verdict
  call (static pin). Vanilla then re-queues the packet and the re-run enters the guarded
  branch -> Movement.check -> Verdict.signal on the server thread.
- handleContainerClick HEAD (:133): same two-pass shape - netty pass runs only
  `Inventory.arrive(player)` (:136; Inventory.java:16, one queue add, no Verdict) and
  returns; the server-thread re-run reaches Inventory.click / Totem.refuse (:140/:144) ->
  Verdict.signal.
- Guarded single-pass HEAD injections (isSameThread true-branch only): handleMoveVehicle
  :48 (Vehicle.check), handleSetCarriedItem :54 (Combat.swapped, Interact.swap,
  Inventory.hotbar), handleUseItemOn :60 (Interact.place), handleUseItem :91 (Fish.use,
  Interact.use), handlePlayerCommand :117 (Glide.startFly), handlePlayerAction :123-130
  (Trace.t, Mining.digStart/digStop/digAbort inside the :124 guard, Totem.refuse SWAP at
  :129, Interact.release at :130).
- Explicit re-dispatch (no guard; the Verdict call is wrapped in server.execute):
  handleSignUpdate :84 and handleChatCommand :105. The netty-thread part of the sign hook
  (:78-81) calls only Packets.lines + Interact.signGap + Trace.t (Interact/Trace state, NO
  Verdict) before the dispatch; the refusal ci.cancel() at :82 is a packet-level decision,
  not Verdict state.
- handleClientTickEnd :111: netty pass -> Movement.tickEnd (Movement.java:54, own state
  only); server pass -> Ticks.inc (not Verdict). No Verdict route at all.
- handleCustomPayload (ServerCommonPacketListenerImplMixin.java:17-19): isSameThread OR
  server.execute for Fingerprints.brand; no Verdict route.
- Totem.popped (LivingEntityMixin.java:16 -> Totem.java:22): writes Totem's own long[] +
  ActionTrace + Trace only - NO Verdict call. The only Verdict.signal in Totem is in
  refuse() (Totem.java:39), reached solely via the guarded container-click/SWAP routes
  above. Totem is therefore fully code-guarded on the Verdict side.

Liveness note (fail-safe direction): if vanilla ever stopped re-queuing a packet, the
guarded branch would simply never run for it - checks for that packet would silently not
fire (a detection LOSS, not a concurrency hazard). The T1 concurrency claim does not
depend on the re-queue behavior; only detection liveness does.

### B. Vanilla call-chain ASSUMPTION routes (NOT code-guarded; named explicitly)

- ServerPlayerGameModeMixin.destroyBlock HEAD (:17-19) -> Mining.refuse -> Verdict.signal:
  no isSameThread guard. Assumption: vanilla invokes ServerPlayerGameMode methods only
  from the server thread (packet path after its own ensureRunningOnSameThread re-queue,
  plus game logic); no netty-thread caller of destroyBlock exists in vanilla. This is the
  SAME class of assumption d124's existing unguarded per-player state (plain HashMaps in
  Movement etc.) already relies on; 0063/0064 stacks nothing new on it. If the reviewer
  wants this route code-guarded too, it is a two-line change (isSameThread check +
  server.execute else-branch) - say the word.
- Fabric event contracts: END_SERVER_TICK (Meshac.java:38, Verdict.tickFlush) and
  ServerPlayConnectionEvents.DISCONNECT (Meshac.java:42, Verdict.forget) fire on the
  server thread per the Fabric API contract; the same tick handler already mutates
  Movement/Combat state unguarded on d124 today.
- Brigadier command dispatch: /mesh monitor (MeshCommands.java:41 ->
  Verdict.clearPendingActions), /mesh status (:30, read-only), /mesh reload (:61 ->
  Config.load -> Config.java:129 -> Verdict.clearPendingActions). Vanilla executes
  brigadier commands on the server thread (chat-command packets are re-queued by vanilla
  before dispatch; console commands run on the server thread).
- Config.load() at init (Meshac.java:27) runs during server startup, before networking is
  up; no concurrent signalers exist yet.

### What runs on the netty thread (complete list, all non-Verdict)

Movement.arrive (:35), Inventory.arrive (:136), Movement.tickEnd (:111), Interact.signGap
+ Packets.lines + Trace.t in the sign hook (:78-81), Trace.t/ack lines in guarded hooks'
netty passes. None touches Verdict HEAT/H state (static pin in the wiring suite).

## T2 (defense in depth): what the per-player lock does and does NOT cover

Lock-covered (synchronized(h) held): every mutation of mon/lh/sigLog/trace and every read
of h.name - in signal(), signalLogOnly() (including the roll receipt, emitted INSIDE the
lock since 0063, atomic with the roll), tickFlush() (roll + receipt per bucket inside one
locked pass), flushSuppressed() (whole loop inside the lock; reentrant-monitor use from
inside synchronized(h) is safe - Java monitors are reentrant).

Deliberately OUTSIDE the lock, enumerated (0064, replaces the blanket T2 claim):

1. `h.name = pl.getGameProfile().name()` in signalLogOnly is assigned BEFORE the
   synchronized(h) block. Safety argument (benign-by-immutability, not lock protection):
   the H instance is per-UUID; for a given UUID the written value is the same on every
   write (a player's name cannot change while connected; disconnect removes the H, so a
   rejoin gets a fresh H); a String reference write is atomic; and every READ of h.name
   (flushSuppressed, tickFlush) happens inside synchronized(h), which the same thread
   passed through before any receipt can reference the name. Invariant: sigLog buckets are
   created only in signalLogOnly AFTER the name assignment, so a receipt never prints a
   null name.
2. HEAT map lifecycle: computeIfAbsent (note/signal/signalLogOnly) relies on
   ConcurrentHashMap's thread-safe publication of H; no per-player lock is (or needs to
   be) held. forget() does HEAT.remove(id) THEN flushSuppressed on the removed instance:
   a signal racing the remove would computeIfAbsent a FRESH H and start a clean bucket,
   while the removed instance's pending counts are still flushed - the receipt is durable
   either way; the two instances never share state.
3. clearPendingActions(): the flush loop over HEAT.entrySet() and HEAT.clear() are not
   atomic; a concurrent computeIfAbsent between them could drop one pending count.
   Unreachable when callers are serialized (T1); no global lock added, it would be a no-op
   under T1. (Residual retained from 0063.)

Budget windows run on System.nanoTime, forward-roll only (0057): a wall-clock rollback
counts within the window and never reopens the budget.

## What this does NOT prove (empirical closure, proposed)

- The bucket-B assumptions (game-mode dispatch thread, Fabric event threads, brigadier
  dispatch thread) on all four target versions: propose a one-off thread-assertion run on
  the rig (log Thread.currentThread() at each route, one pass per version build).
- Box compile x4 + FP 100/300 remain Box's gates.
