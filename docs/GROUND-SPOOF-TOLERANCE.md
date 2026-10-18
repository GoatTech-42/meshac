# Ground-spoof (nofall) tolerance analysis: partial blocks and ledges (static, no behavior change)

Rule today (Movement.java, ground-spoof block): the packet says onGround, not exempt, not in grace. The player box moved to the claimed position is checked for collision in the slab [minY - 0.1, minY]. No collision = "air". Three air packets in a row = "nofall ground spoof", with no tolerance beyond the 0.1 strip. statusOnly (flag-only packets) uses the same 0.1 strip at the server position and also needs 3 in a row.
The check uses level.noCollision(entity, box), so entity-context blocks (powder snow with leather boots, scaffolding, etc.) follow the server's own shapes.

Why one claim can look like air: the server and the client disagree on either the claimed y, or the shape under the feet. Three in a row means the disagreement persists while the player stands or walks, so it is a persistent shape or height disagreement, not a landing frame.

Collision tops below are from memory of vanilla Java shapes and are listed for the rig to confirm. The new trace line (MESHAC_TRACE, commit after 0529644) prints the real top per block, so replace any entry that disagrees.

| Block / situation | Java collision top (to confirm) | Spoof outcome if the server shape is right and the client stands at the surface | Where Bedrock/Geyser could disagree (HYPOTHESIS, no evidence yet) |
|---|---|---|---|
| Full block | 1.0 | no flag (gap 0) | none expected |
| Slab (bottom) | 0.5 | no flag | translated slab state wrong (top vs bottom) leaves a 0.5 gap: flags |
| Slab (top) / double | 1.0 | no flag | same |
| Stairs | 0.5 or 1.0 per half | no flag | step height on stairs differs: gap up to 0.5 on the low half |
| Carpet | 0.0625 | no flag (gap within 0.1 only if the claimed y is at the top) | client may stand 0.0625 higher or lower: stays inside 0.1, ok |
| Snow layer 1 | 0 (no collision) | the player stands on the block below, no flag | Bedrock may give layer 1 a collision: client stands 0.125 above server floor: gap 0.125 > 0.1, flags |
| Snow layers 2-8 | (n-1)/8 | no flag | rounding or n vs n-1 gives 0.125 gap: flags |
| Trapdoor (closed, bottom half) | 0.1875 | no flag | open/closed state sync lag: client on an open trapdoor that the server thinks closed or vice versa leaves a gap of up to 0.8 |
| Lily pad | 0.09375 | no flag | Bedrock lily pad shape differs: gap within 0.1, borderline |
| Fence/wall/gate (closed) | 1.5 | no flag | Bedrock lets players stand on fence tops with a different height: possible |
| Chest/ender chest | 0.875 | no flag | gap 0.125 if the client treats it as 1.0 or 0.875: borderline flag |
| Soul sand / honey / farmland / path | 0.875 / 0.9375 / 0.9375 / 0.9375 | no flag | gap up to 0.125 on 0.875: flags if Bedrock uses a full block |
| Bed | 0.5625 | no flag | Bedrock bed height differs: gap small |
| Cauldron, hopper, brewing stand | 1.0 / 0.625 / 0.875 | no flag | inner cavities and legs: edge standing on a leg is gap 0.0, on the cavity floor deeper |
| Powder snow with leather boots | walkable, entity-context shape | no flag by noCollision(entity) | Bedrock walk-on rule may differ or be client-side only |
| Ledge edge (partial overlap) | box overlaps block by < 0.01 | the strip still hits the block, no flag | sneak-edge resolved from source, see below: the flag condition is horizontal, both clamps preserve overlap; rig item is the real Bedrock client |
| Moving platforms (boat, minecart, shulker, piston) | n/a | no collision with static strip | passenger exempt; pistons and shulkers have no exemption |
| Stuck in 1-block gap / crawl (pose box 0.6 high) | n/a | not a ground-spoof input | Bedrock crawl and swim pose boxes differ |

## Ranking (by what the corpus can support, not proven)
1. Slab/stair/snow/trapdoor state mismatch from block translation. Persistent, location-bound: one player flagged many times in one place (AlphaWolf16983, 96 lines) is the signature. Bedrock-only if Geyser translation is the cause.
2. Standing on shapes the server treats as empty or shorter (snow layer 1, carpet-class shapes with a 0.1 strip too tight on 0.125 gaps). A 0.125 gap is just outside the 0.1 tolerance, which makes snow layers and chests the first suspects.
3. Ledge-edge partial overlap with rounded positions. Not persistent by nature, needs three in a row, so unlikely alone.

## What not to do
No tolerance was added. A looser strip (for example 0.13) would hide 0.125 cases but costs real NoFall detection margin, and the cheat uses a much bigger gap. The data to pick a tolerance per block is the trace. Decision rule for the rig/owner: if the trace shows airBelow flags only above blocks whose top is a multiple of 0.125 offset from the claimed y (snow, chest, soul sand), set a per-shape tolerance for those blocks; if the traces show feet in air with no block under them, the cause is elsewhere (client y desync) and no tolerance fixes it.

## Capture plan
Run with MESHAC_TRACE set (rig, or ilhsmc only if the owner agrees to trace there; it is env-gated, off by default). Grep "groundctx" lines with flag=true. Each line has feet/below/below2 block ids with collision tops, airBelow, ground, prevGround, buffer, dy, dtS, exempt.

## Sneak-edge (Bedrock) vs the 0.1 strip: settled from source, 2026-10-16

The open question was whether a Bedrock player sneaking at a block edge can lose all overlap under the box and flag ground-spoof. Findings, all from source plus pure sim (sim/SneakEdge.java, sim/SneakEdgeTest.java):

1. The flag condition is horizontal, not vertical. A standing player's feet rest exactly on the block top (gap 0), so the [minY-0.1, minY] strip always reaches the support. Losing support needs zero XZ overlap between the box and the block top. Widening the strip (0.13 or anything else) cannot change a sneak-edge outcome in either direction, so the GroundTolerance candidate is irrelevant to this case.
2. Vanilla 26.2 sneak clamp (Player#maybeBackOffFromEdge, decompiled MCP source, Renekovski/26.2-mcp): three loops stepping 0.05, stopping at the last offset whose probe (box inset 1e-7, searched maxUpStep down) still finds support. The accepted position therefore always keeps overlap in (0, 0.05]. Fuzzed 100k starts/requests in the sim: the clamp never accepted an unsupported position, and every clamped position reads supported under both the 0.1 and 0.13 strip. Sneak-edge is not a Java false-positive source.
3. Bedrock: Boar (opencollab-incubator/Boar Collider.java), the public Bedrock-movement emulator built for Geyser anticheat work, runs the same three loops with the same 0.05 step and a 0.025-inset probe box. That is the available evidence that Bedrock's clamp has the same shape with a wider margin (overlap > 0.025). The real Bedrock client is closed source, so this stays the rig item.
4. Geyser relay (BedrockMovePlayer.java @ 0e94972): positions arrive as float32 eye positions, minus the player offset, then corrected against Java collision (CollisionManager.adjustBedrockPosition) before being sent downstream. float32 rounding moves a box edge by at most ulp/2, which reaches the 0.025 Bedrock margin only near |x,z| = 2^19 (524288 blocks out); near spawn it is ~1e-5 and cannot destroy any clamp margin.
5. Trace signature if a sneak-edge flag ever appears: feet=air, below=a full block (top 1.0), gap about 0.000, persistent at one spot. That differs from the translation-mismatch signature (below=slab/snow/chest, partial top, gap about 0.125 or 0.5), and the groundctx line already records everything needed to tell them apart.

No exemption added. Rig asks: a real Bedrock client sneak-walking off edges and corners (keyboard and touch/classic input, auto-jump on and off) with MESHAC_TRACE on; also watch whether adjustBedrockPosition ever moves a sneaking Bedrock player off support.
