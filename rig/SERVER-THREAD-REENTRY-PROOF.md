# Pre-GO 2: server-thread / reentry proof for the combat log-only path (0065 rev)

Status: PAPER for review, over the 0063 atomic-drain rework and the 0065a destroyBlock
guard. Reviewer verdicts so far: 0063 scoped SOURCE GO; 0064 scoped conditional PAPER GO
(per-injection guard inventory accepted as better than blanket vanilla scheduling - NOT an
all-version thread proof: the 6 static string pins assert the cited line-level facts only
and prove nothing about the indirect call graph or vanilla/Fabric API contracts). This
revision: (a) destroyBlock moves from ASSUMPTION to CODE-GUARDED via the 0065a early
isSameThread guard (detection-loss caveat, no server.execute - reviewer item 4); (b) the
forget() lifecycle argument is corrected - the in-flight-reference race is a documented
residual, not "durable either way" (reviewer item 1); (c) the h.name visibility argument
is restated precisely - monitor-edge happens-before, not immutability (reviewer item 2).
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

### B. Vanilla/Fabric call-chain ASSUMPTION routes (NOT code-guarded; named explicitly)

- ServerPlayerGameModeMixin.destroyBlock: CODE-GUARDED since 0065a (scoped SOURCE GO,
  reviewer 11:06pm) - an early isSameThread return runs BEFORE Meshac.skip / Mining.refuse
  and any mining/world-content read (the guard's own player.level().getServer() is the
  same necessary thread query as the attack guard, not a world-content read). An
  off-thread invocation now loses detection (fail-safe direction) instead of reaching
  Verdict. Deliberately NO server.execute: a delayed refusal would change the synchronous
  return/cancel semantics of destroyBlock and could act after the block is mined
  (reviewer 11:03pm item 4). Guarding narrows the assumption; it does not replace the
  all-4-version build/runtime proof.
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
netty passes. None touches Verdict HEAT/H state (static pins in
rig/test-reentry-proof-wiring.py; the pins assert exactly these line-level facts and
nothing more).

## T2 (defense in depth): what the per-player lock does and does NOT cover

Lock-covered (synchronized(h) held): every mutation of mon/lh/sigLog/trace and every read
of h.name - in signal(), signalLogOnly() (including the roll receipt, emitted INSIDE the
lock since 0063, atomic with the roll), tickFlush() (roll + receipt per bucket inside one
locked pass), flushSuppressed() (whole loop inside the lock; reentrant-monitor use from
inside synchronized(h) is safe - Java monitors are reentrant).

Deliberately OUTSIDE the lock, enumerated (0064, replaces the blanket T2 claim):

1. `h.name = pl.getGameProfile().name()` in signalLogOnly is assigned BEFORE the
   synchronized(h) block. Visibility argument (0065, precise - immutability alone is NOT
   the proof): the assigning thread performs, in program order, name write -> monitor
   enter -> bucket create -> monitor exit. Every reader of h.name (flushSuppressed,
   tickFlush) reads it while holding the same monitor. A monitor exit happens-before every
   subsequent enter of that monitor, and program order carries the pre-lock name write
   into the writer's pre-exit set - so the name write happens-before any locked read that
   can observe a bucket. Invariant: sigLog buckets are created only in signalLogOnly
   inside the lock, after the name assignment in the same call, so a receipt can never
   observe a bucket whose name write is not already visible; the value written is also the
   same for a given UUID (names cannot change while connected; disconnect removes the H),
   which covers concurrent writers but is secondary to the monitor-edge argument.
2. HEAT map lifecycle: computeIfAbsent (note/signal/signalLogOnly) relies on
   ConcurrentHashMap's thread-safe publication of H; no per-player lock is (or needs to
   be) held. forget() does HEAT.remove(id) THEN flushSuppressed on the removed instance.
   RESIDUAL (0065, reviewer item 1 - the 0064 "durable either way" claim was WRONG): a
   thread that captured the H reference BEFORE the remove (computeIfAbsent or tickFlush's
   HEAT.get) can be paused, let forget() remove + flush, then resume and increment the
   DETACHED instance's suppressed count after the flush - that count is orphaned: the
   instance is unreachable from HEAT and never flushed again. Post-remove computeIfAbsent
   creating a fresh H does not help; same-instance in-flight references are the hazard.
   Unreachable when T1 holds (all four routes serialized on the server thread); NOT
   reworked - a tombstone-flag rework is the known fix if T1 is ever abandoned. Classified
   alongside the clearPendingActions residual below.
3. clearPendingActions(): the flush loop over HEAT.entrySet() and HEAT.clear() are not
   atomic; a concurrent computeIfAbsent between them could drop one pending count.
   Unreachable when callers are serialized (T1); no global lock added, it would be a no-op
   under T1. (Residual retained from 0063.)

Budget windows run on System.nanoTime, forward-roll only (0057): a wall-clock rollback
counts within the window and never reopens the budget.

## What this does NOT prove (empirical closure, proposed)

- The remaining bucket-B assumptions (Fabric event threads, brigadier dispatch thread)
  on all four target versions, and the guarded routes' behavior on real builds: propose a
  one-off thread-assertion run on the rig (log Thread.currentThread() at each route, one
  pass per version build). The static pins are line-level assertions only - they do not
  prove the indirect call graph or any vanilla/Fabric API contract (reviewer 0064 note).
- Box compile x4 + FP 100/300 remain Box's gates.

## 0091 addendum: policy commit vs decision/effect atomicity

A reload (or the /mesh monitor-on toggle) is a policy COMMIT: wipe all ladder evidence, then publish.
0090 guarded the transition with an epoch token plus a check-then-act re-read; review found the classic
races in that shape (read-old-cfg-then-reload-then-same-token; reload between the check and the removal;
computeIfAbsent landing between the wipe and the publish). 0091 replaces the token with structure:

- Policy and epoch publish as ONE immutable Config.Snapshot (volatile), read ONCE per decision. Two
  independent volatile values could tear; one final-field bundle cannot.
- Config.POLICY_LOCK serializes every commit against every Verdict decision AND its effects
  (signal/signalLogOnly/note hold it for their whole body; the reload commit and the monitor-on toggle
  hold it for wipe+publish). An action computed under policy P cannot dispatch after policy P+1 commits,
  and the wipe cannot interleave with a decision's HEAT access. The 0090 epoch re-check is removed:
  mutual exclusion, not detection.
- Lock order: POLICY_LOCK -> per-player heat lock -> Cases monitor. No path acquires them in reverse.
- Cost, stated honestly: decisions across all players serialize on one lock. The section contains heat
  maths, log lines, watcher chat, async Discord posts (HttpClient.sendAsync - non-blocking) and, on a
  removal, one Cases.save file write plus the disconnect. Reloads and monitor toggles are operator-rare,
  but a signal burst during a removal's file write stalls other signals for that write's duration.
- Residuals, stated honestly: tickFlush/forget/clearPendingActions' flushSuppressed stay per-player
  synchronized only (policy-free bookkeeping: suppression counters and disconnect flushes, never ladder
  inputs). Unsynchronized monitorOnly reads in the check-level enforce() gates can observe a toggle late
  (Boolean reference write: atomic, no tearing); that affects only the raw-refusal gate, never the Verdict
  decision, which reads the snapshot under the lock. Signals still arrive off-server-thread; the claim is
  mutual exclusion, not server-thread confinement. Evidence loss on a commit remains fail-closed.
- Owed (Box, MC-side/harness): a threaded signal+reload stress test asserting no post-commit dispatch of
  a pre-commit REMOVE and no cross-commit heat survival. Not writable as a pure unit test (Verdict needs
  ServerPlayer).

## 0092 addendum: claim narrowed to inside-Verdict effects; mutation discipline

Reviewer round on 0091, three corrections folded:
- COMPILE: Config.load still used cur as a Config in two places (cur is the Snapshot since 0091) - fixed to
  cur.cfg. The python wiring tests regex-match structure; they do not typecheck. Box runs javac FIRST on any
  new tip before harness or mutation work.
- NARROW CLAIM: 0091 wording said "decision AND effects" are atomic against a commit. That holds only for
  effects dispatched INSIDE Verdict (heat, case store, disconnect, Discord, watcher alerts, log lines). The
  Step a signal RETURNS is consumed by the check caller - cancel/setback/refusal in Movement, Interact,
  Mining, Glide, Inventory, Vehicle, Totem, Fish and the packet-listener mixin - AFTER POLICY_LOCK is
  released. A commit can land between the return and that caller action; outer actions are explicitly NOT
  covered. Stale outer actions have NO count bound (corrected in 0093): any number of off-thread callers
  can retain returned Steps and all act after a commit. The bound is qualitative only: outer actions never
  touch heat, cases, or bans (those all stayed inside the lock).
- MUTATION DISCIPLINE: the Snapshot guarantees identity, not deep immutability. Exactly two post-publication
  mutations are sanctioned, both under POLICY_LOCK: monitorOnly (in-place Boolean write) and disabledChecks
  (copy-on-write swap - /mesh check on|off now builds a new list and swaps the reference, so unsynchronized
  readers iterating disabled() can never see a half-mutated list or a ConcurrentModificationException; the
  in-place removeIf/add it replaces carried that risk since the command was added). Every other Config write
  is pre-publication (Gson parse, validatePolicy).

## 0093 addendum: reviewer small-items round

- The 0092 "one in-flight event" bound is WITHDRAWN: it was per-decision wording, not a real bound. Any
  number of off-thread callers can retain returned Steps and all act after a commit; only the qualitative
  bound holds (outer actions never touch heat/cases/bans).
- disabledChecks is now volatile and disabled() reads the reference once into a local: the copy-on-write
  swap is immediately visible to unsynchronized readers and the null check plus iteration see the same list.
- Missing-file load() no longer validates the published instance: next is a Gson round-trip COPY of cur.cfg
  (exactly the persistable fields; validatePolicy recomputes the transient policy), and validatePolicy now
  runs INSIDE the commit lock, so its writes (the policy field on the unpublished next, the lastValidLadder
  static) are commit-side state. "Exactly two sanctioned post-publication mutations" is now exact.
