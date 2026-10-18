# Slow-fall fly flags: "fly dy -0.118 expect -0.194" (static study, no exemption added)

STATUS: exemption added in the commit after b2e0ed0 (FpGuards.flyMediumExempt, keyed on effect or block, 400 ms grace). The table below is still the rig probe list.

Where it comes from: the fly check expects dy = (prevDy - 0.08) * 0.98 and flags dy > expect + 0.03 for several packets.
expect -0.194 means prevDy was -0.118. So the player fell at about -0.118 per tick on two packets in a row.
Seen 10 times in rig/flag-triage.log (Nogocool 23:31:57, .Eagte_aka_layla 05:44-05:46, Java and Bedrock-style names).

Fit found offline (arithmetic, not a rig run): with gravity 0.01 and drag 0.98 (the vanilla Slow Falling effect value, from memory),
a fall passes -0.113, -0.121 on ticks 13-14. That is -0.118 within a hair, and the next tick is -0.125, not -0.194.
Movement.java exempts Slow Falling in the speed/nofall paths (lines ~133, ~287) but NOT in the fly block (~195), which only
checks !ground. Nothing else is skipped there: water, climbable, web, honey, bubble columns, levitation all reach the fly buffer
unless they happen to be ground-flagged. So Slow Falling is the first thing to probe. This is a hypothesis, not a fix.

## Sources to probe (one player, one scenario each, log-only rig, expect "SIGNAL vic1 fly" count, record dy trace via [trace])

| id | source | what vanilla does (verify on rig, values from memory) | pass criterion |
|----|--------|--------------------------------------------------------|----------------|
| SF1 | Slow Falling potion, drop from y+60 | gravity 0.01, drag 0.98, dy walks -0.01 ... -0.27 over 40 ticks | zero fly signals the whole fall |
| SF2 | Slow Falling ends mid-air | dy jumps to normal gravity chain in one tick | zero fly signals across the switch |
| SF3 | Slow Falling after Levitation | levitation lifts then Slow Falling shield (common potion combo) | zero fly signals |
| HN1 | Honey block side slide | dy clamped to about -0.05 while hugging the wall | zero |
| HN2 | Honey block jump off side | reduced jump | zero |
| CW1 | Cobweb fall, enter and exit | motion x0.25 / y0.05, resets each tick | zero; web exit frame |
| SC1 | Scaffolding descend (sneak) and walk through | dy -0.15 climbing rules, falls through when not sneaking | zero |
| VN1 | Vines/ladder/twisting vine descend, sneak hold, jump off | onClimbable descent -0.15 cap, hold dy 0 | zero |
| WT1 | Water surface edge: step out of water onto a 1 block ledge, jump out | dy bursts of +0.3 to +0.4 near the surface | zero |
| WT2 | Walk off ledge into water, then out | entry dy damping | zero |
| BC1 | Bubble column (soul sand up, magma down) enter/exit | upward drag-up and downward -0.03 per tick | zero |
| LV1 | Levitation end at height | rising stops, then normal gravity from dy ~0 | zero (levitation exempt in other paths only) |
| GL1 | Elytra stop (land, collide, firework end) then fall | glide momentum gives dy outside gravity chain for a few ticks | zero after the existing 2.5 s window |
| PS1 | Powder snow climb | 0.2 per tick (handled by 65426da) | zero, covered by FpGuardsTest |
| LG1 | Lag-bunched fall (handled by 65426da) | multi-tick dy | zero, covered by FpGuardsTest |
| RF1 | Real cheat: constant -0.118 sink on server with no effect | | flag within about 10 ticks, so any slow-fall exemption must be tied to the actual effect, not to dy |

Instrumentation for the rig agent: set the trace flag so each packet prints dy, expect, ground, effects; compare each vanilla trace against the table.
Any exemption goes in only for a source whose trace actually trips the fly buffer, and is tied to the server-side state
(pl.hasEffect(SLOW_FALLING), block at pos), never to the dy value.
