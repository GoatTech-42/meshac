# Chunk border leaks: design

> UPDATE 10-17 (0048 sweep): the leak-1/leak-2 wiring described below as branch-only IS
> present in the d124 main line (Veil.java:111 PlanGuard.check with maskOf + borderUnchanged;
> BorderGuard.compute at :166-168; all in the c4b3fe8 base). The "wired on branch,
> uncompiled" framing is stale: it compiled as part of the 26.3 Box build; runtime replan
> behaviour is not rig-verified. The retroactive resend remains design-only.

The two leaks fail in opposite directions: leak 1 fails OPEN (an ore wrongly stays visible), leak 2 fails CLOSED (a block wrongly stays hidden). Neither is a false positive against a player - the safety claim is about enforcement errors only, not security. Pure parts and tests are in place (`sim/VeilBorderTest.java`). Leaks 1 and 2 are wired in the current tree (Veil.java:118 `Veil.mods` runs `PlanGuard.check`; the `veil-border-wiring-20261016` branch content is merged; compiled on all four targets (Box-reported 10-17 11:36pm, series through 0066 - HISTORICAL; the current-tip binary is not verified); the old VERIFY markers on these names were resolved to compile-verified in 0072v2): the cache now holds a `Plan` (mods, neighbour mask, border blocks and their outward bits) and `Veil.mods` runs `PlanGuard.check`. `PlanLifecycleTest` runs a two-chunk toy world through the same pure pieces. The retroactive resend is still design only.

## Leak 1: a no-update change next to a border
A planned block on the border (x or z at 0 or 15) depends on one block in the chunk next door. If that block changes without a setBlock update (WorldEdit, no-update flags), the cached plan of this chunk does not hear about it, and the freshness check only compares this chunk's own blocks.

Design (`BorderGuard`):
- Each planned block on the border stores 4 bits: which outward neighbours were see-through at plan time. Interior blocks store nothing.
- On a send, for planned border blocks only, re-read the outward neighbours (a getBlockState each, at most 2 for a corner) and compare. Any difference makes the section replan, the same path as a stale rebuild.
- Cost: one lookup per planned border block per send. Plans hold a few dozen blocks per section, border blocks are a fraction of that. No events, no timers.
- Covered: a hidden ore whose neighbour became air. Not covered: an ore that should now be hidden because the neighbour became solid (it has no plan entry). That one stays visible, which is the safe direction.
- Wiring sketch: add `byte border` to `Veil.Mod`, fill it in `modsCached` through `View.at()` for idx with `BorderGuard.isBorder`, and compare it inside `fresh()`.

## Leak 2: a chunk planned before its neighbour was loaded
An unloaded neighbour counts as see-through, so a border ore is shown. Nothing replans the chunk when the neighbour loads.

Design (`NeighbourMask`):
- Store a 4-bit mask of which side neighbours were loaded when the plan was made.
- On a send, recompute the mask (4 chunk-map lookups). Replan only if a neighbour that was missing is loaded now. An unload never replans.
- Bound: stored is overwritten at each replan, so a rebuild needs a neighbour that was missing at that time. A flapping neighbour cannot cause a rebuild storm (tested).
- No Fabric chunk event is used. Their `ServerChunkEvents.Load` takes 2 arguments in 1.21.11 and 3 in 26.x (checked in the cached jars).

Retroactive part (`BorderDiff`, design only): the replan above only helps later sends. A client that already holds the chunk keeps the visible border ore. After a replan, `BorderDiff.diff(oldPlan, newPlan, cap)` lists the border positions that changed. Sending those few as block updates (fake for the newly hidden, real for the newly shown) would fix chunks players already have. It needs a veiled block-update path, which does not exist in this branch (block updates go out real). Past the cap (suggested 256 positions) nothing is sent and the old behaviour stays. Not worth building unless a Bedrock or Java test shows the visible border ore matters in practice.

## Order of work if it is wanted
1. Leak 2 first (smaller: one int next to the plan).
2. Leak 1 (changes `Mod`).
3. Retroactive resend last, and only after a rig test shows it is needed.
