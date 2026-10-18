# Native action trace request, candidate action-audit branch

Source patch is log-only/off by default: exact MESHAC_ACTION_TRACE=1 enables it.
This is a request to rig owner, NOT permission to change the rig/config/jar or
start clients now. Parent/Box compile scheduling and private meshac-red-only
restriction apply. Reuse existing server/world/clients. No public/ilhsmc traffic,
process killing, new world copies, or40native clients. Maintain2GiB available.

## Patch entry points / schema

[action-trace] adds monotonic ns, wall ms, handling tick, counted client tick,
player UUID, kind, cumulative global dropped count, bounded512char fields.
Max200records/sec globally. Count losses, don't treat missing events as success.
Supplier evaluation is behind off/default and budget checks; no trace data feeds
Verdict or heat. Format/getter RuntimeException is contained so diagnostics don't
refuse gameplay. No chat/sign text or inventory contents emitted. Tool components
and effects are local rig diagnostics, potentially long/truncated.

- container-click: network-arrival wall ms from existing FIFO, container id,
  slot, button, type, menu implementation and creative status.26.x containerInput
  renamed click type, runtime getter fallback supports older clickType/getClickType;
  UNKNOWN_ACCESSOR means missing evidence, not an allowed action. Cached26.1.2,
 26.2,26.3javap confirmed containerInput; full versions still need compile/native.
- dig-start/stop/break-attempt: position, real state/hardness, server-computed
  per-tick destroy progress, tool/components, effects, ground/water. Break-attempt
  is not proof of actual block removal. Haste/efficiency are applied by server's
  getDestroyProgress; pure formula fixtures are not a replacement.
- totem-pop: server pop wall time. totem-click: arrival wall time, prior click,
  pop time, prior/current slot, offhand classification and gaps. Pop has no
  network-arrival stamp; don't call mixed-clock gaps trusted click reaction time.
- boat-move: entity id, prior/packet Y, server velocity, ground/water and block
  at/below. These give context but do not prove impulse cause. Fixture controller
  must mark piston/slime/launch source and exact activation tick externally.

## Bounded capture matrix requested

For each case use short10..20s capture,3repetitions initially, exact version/jarSHA,
config and scenario id. Run disabled baseline then candidate at same conditions.
Send cumulative dropped count and relevant [meshac]SIGNAL/CASE lines too. Request
one Java operator client and optional Bedrock fixture only under existing owner
budget. Prefer no enforcement during instrumentation validation; rig owner must
select mode and record it. No new exemption requested.

1. Bridge: held-right-click normal, shift-edge, fast/sprint variants available to
   operator, vertical tower; underfeet geometry, attempted vs placed block counts,
   video/screenshot + world observation. Pure audit hasn't established4blocks/s
   as a legal maximum. Include speed effects if actually used, record level.
2. Containers: chest/shulker shift-click sorting, QUICK_CRAFT drag full row,
   double-click collect, number-key swap, offhand swap, creative inventory stage.
   Label menu/slot coordinates and clicked type. Record actual slot/item outcome
   separately without dumping entire private inventory. Distinguish packet count
   from moved items. Do NOT assume container0/offhand45valid in every menu.
3. Mining: stone hardness1.5 plain diamond vs efficiencyV+hasteII; dirt hardness.5
   efficiencyIIIshovel; torch/instant foliage; deepslate/tough slow block and mixed
   path. Capture actual break count and original block before removal. Confirm
  1tick instant vs sustained slow cases. Include tool swap/effect expiry mid-dig,
   normal wall sweep and deliberate legal flick-mining. Repeat veil off/on fake
   substitutions to test real/client progress mismatch.
4. Totem: controlled normal pop/refill, anticipated refill, keyboard offhand swap,
   sorting then refill; queued-before-pop input with measured stall. Correlate
   recorded pop vs arrival, do not infer reaction from main-thread proximity.
   Need actual offhand item/result and pop count, not just a cancellation log.
5. Vehicle: boat shore/water transition, bubble exit, supported climb, legal fall,
   piston/slime launch and external knockback as available; horse/mount transition
   control. World marker activation times required for cause. Boatfly positive
   control five distinct unsupported rises, private authorized fixture only.
6. Lag:150/250/400ms jitter and500/1000/2000msstalls within approved existing
   proxy fixture for each representative action. Logs alone don't establish
   client send time. Include actual successful outcome counts within10%baseline.

## Return evidence / stop

Return native-client version, hashes, configuration, timestamps, fixture spec,
raw local logs and outcome proof. State missing/truncated/dropped evidence.
Record FPS/tick health during capture; stop own fixture on throttle/challenge,
RAM<2GiB, unexpected route or user/rep stop, report rather than killing anything.
No zero-signal shortcut to PASS. Compile pure/source alone grants no rig release.

## Mining rate patch under review

Old8/s flat cap now sums normalized cost from current server destroy progress:
ceil(1/progress)/2.5clamped.4..1. One-tick blocks bounded20/s, two-tick10/s,
slow blocks retain8/s; mixed blocks consume proportional work. Positive infinity (zero hardness) also costs.4. Unknown/NaN/negative/
nonpositive progress retains old full cost, never exempts. Pure hardness fixtures:
https://minecraft.wiki/w/Breaking and https://minecraft.wiki/w/Instant_mining
Stone1.5, diamond8+efficiencyV26, hasteII1.4=>47.6/45>1; dirt.5, diamond
shovel8+efficiencyIII10=>18/15>1.21instant/sec and9slow/sec remain positives.
This tunes rate evidence, not geometry/LOS/early-stop/sweep. Fixed wall-second
window and transport batches remain a known limitation; native data required.
