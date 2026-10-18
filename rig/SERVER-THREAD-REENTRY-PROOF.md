# Pre-GO 2: server-thread / reentry proof for the combat log-only path (0063)

Status: PAPER for review, plus the 0063 atomic-drain rework in the same patch. Empirical
closure (a thread-assertion run on the rig) is proposed, not done. Box compile x4 and the FP
100/300 ms replay remain Box's gates.

## Scope: every route that mutates Verdict state (HEAT, H: lh, mon, sigLog, trace, name)

signal(), signalLogOnly(), tickFlush(), forget(), clearPendingActions(), note(); remove()
only runs inside signal(); status() is read-only.

## T1 (load-bearing): every mutating route executes on the server thread

- Combat path: BOTH AttackMixins guard `if (!player.level().getServer().isSameThread())
  return;` BEFORE Combat.check or Verdict are touched (attack26/java/dev/meshac/mixin/
  AttackMixin.java, the line after the skip guard; attack1211 same shape). CODE-GUARDED:
  Combat.check's world reads and signalLogOnly can only ever run on the server thread,
  whatever vanilla packet scheduling does. This also closes the original pre-GO 2 concern
  (Combat.check world reads from the AttackMixin path).
- Tick pump: Verdict.tickFlush is called only from the END_SERVER_TICK handler
  (Meshac.java, the ServerTickEvents registration). Fabric tick events fire on the server
  thread (API contract; the same handler already mutates Movement/Combat state unguarded
  on d124 today).
- Monitor toggle: /mesh monitor on|off calls Verdict.clearPendingActions()
  (MeshCommands.java, the monitor literal). Brigadier commands registered via
  CommandRegistrationCallback execute on the server thread (vanilla command dispatch runs
  from the server tick loop).
- Disconnect: ServerPlayConnectionEvents.DISCONNECT (Meshac.java) fires on the server
  thread (Fabric contract); Verdict.forget runs there.
- Other signal() callers (Movement tick, packet callbacks): vanilla schedules packet-
  listener handlers onto the server thread (PacketUtils/ensureRunningOnSameThread); the
  paths that need re-dispatch already do server.execute
  (ServerGamePacketListenerImplMixin.java, the sign + chat lambdas). EVIDENCE LEVEL:
  vanilla/Fabric behavior assumption - the SAME assumption d124's existing per-player
  state (plain HashMaps in Movement etc., no locks) already relies on. 0063 relies on
  nothing new.

## T2 (defense in depth): per-player state is synchronized even if T1 fails

- HEAT is a ConcurrentHashMap; per-player H creation via computeIfAbsent.
- Every mutation of mon/lh/sigLog/trace holds synchronized(h): signal, signalLogOnly
  (including the roll receipt - see below), tickFlush, flushSuppressed.
- 0063 rework (atomic drain): the window-roll receipt is now emitted INSIDE
  synchronized(h) in signalLogOnly, atomic with the roll - the 0059/0061 lock-release gap
  (cross-window mixing) is closed. tickFlush rolls and emits per bucket inside one
  synchronized(h) pass. flushSuppressed holds synchronized(h) for its whole loop;
  reentrant-monitor use from inside synchronized(h) is safe (Java monitors are reentrant).
- Budget windows run on System.nanoTime, forward-roll only (0057): a wall-clock rollback
  counts within the window and never reopens the budget.

## Residual hazards if T1 ever breaks (documented, NOT claimed closed)

- clearPendingActions: the flush loop over HEAT.entrySet() and HEAT.clear() are not atomic;
  a concurrent computeIfAbsent between them could drop one pending count. Unreachable when
  callers are serialized (T1); no global lock added, it would be a no-op under T1.
- h.name is set on every signal/signalLogOnly before any budget bucket can exist, so a
  receipt never prints a null name.

## What this does NOT prove (empirical closure, proposed)

- Fabric event + vanilla packet-thread guarantees on all four target versions: propose a
  one-off thread-assertion run on the rig (log Thread.currentThread() at each route, one
  pass per version build). Box compile x4 + FP 100/300 remain Box's gates.
