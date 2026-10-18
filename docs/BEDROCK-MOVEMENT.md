# Bedrock (Geyser) movement vs the movement checks: static review, no exemptions added

Sources: rig/fp-log-v31/keep.log (read-only, 1461 lines, dates fuzzy), Meshac.java skip()/bedrockUnproven(), ServerGamePacketListenerImplMixin, Movement.java at 04519ed.
Bedrock here = UUID high half 0 (Meshac.skip). Dot-prefixed names in the corpus (Floodgate style) are used below only as the log's label for Bedrock players; a name alone proves nothing.
Facts about the Bedrock client or Geyser below are from memory and are marked HYPOTHESIS until a Bedrock client on the rig shows them. The rig has no Bedrock client driver yet (fp-1106 sequence is "not written").

## What is established (from code and corpus)
- skip() removes Bedrock players from combat, interact, inventory, mining and totem checks. Movement checks still run, except while gliding, riding, climbing, levitating, slow falling or riptiding (bedrockUnproven returns before any check).
- Bedrock gets a looser speed cap (x1.30 instead of x1.12) and sustain budget (x1.30 instead of x1.00).
- Corpus split by label (dot-names / all):
  - nofall "ground spoof" / "ground flag in mid-air": 111 / 116. Almost all from one player (.AlphaWolf16983, 96 lines), also .RedPandaPlay930 8, .RulerofClubs 7. This is the dominant Bedrock movement FP.
  - air jump 17/46, microhop dy 0.200 15/57, jump dy 0.630 7/20, fly dy 7/21, speed 0/22, high jump 1/77.
  - So speed and high jump are not Bedrock problems; ground claims, hop and jump bands are shared with Java and need per-player block context to attribute.

## Differences to probe, mapped to checks
| # | Difference (HYPOTHESIS unless noted) | Check at risk | What to record on a rig Bedrock client |
|---|-------------------------------------|---------------|-----------------------------------------|
| B1 | Geyser relays Bedrock's own on-ground flag; Bedrock treats standing differently on partial blocks (slabs, stairs, carpet, snow layers, lily pads, fences, walls, chests, cauldrons, soul sand, trapdoors) and at ledge edges | nofall ground spoof (air within 0.1 below, 3 packets), statusOnly | per block type: stand, walk across, jump on/off edge; trace unsupported, ground, bufGround. Sneak-edge sub-question settled from source 2026-10-16: see GROUND-SPOOF-TOLERANCE.md - the strip is vertical, the edge question is horizontal, and both clamps (vanilla source, Boar for Bedrock) preserve overlap |
| B2 | Bedrock positions arrive eye-based and are rounded by Geyser (float rounding of y) | microhop band 0.005-0.30, jump 0.62, step 0.62 | trace dy on flat ground standing still and walking; any dy noise near the thresholds |
| B3 | Bedrock auto-jump (touch/controller) and different step-up handling | step, jump dy, microhop | climb 1-block, slabs, stairs with auto-jump on and off |
| B4 | Sprint/sneak/swim/crawl states differ (swim-sprint, crawl into 1-block gaps, sneak height, sneak-edge) | speed cap, sustain, noclip, ground spoof | swim sprint, crawl under 1-block gap, sneak edge walk |
| B5 | Bedrock input is client-predicted with server rewind; position packets can bunch or double up per tick | timer (bank 1.6 s), speed per packet, fly expect chain (65426da stale guard covers dtS>0.085 falls) | per-packet dtS during a 60 s sprint; record bal, spBal |
| B6 | Powder snow: Bedrock may not reproduce the Java 0.2/tick rise parity | microhop/fly/high jump (powderRise exempts only the Java band) | climb out of powder snow, record dy per tick |
| B7 | Wind charge, mace, riptide, elytra, boats, vehicles differ | bedrockUnproven already skips most; wind charge and mace do not | wind charge jump, mace smash |
| B8 | Slow falling and levitation are skipped for Bedrock already; the new fly medium exemption (04519ed) also skips water/climbable/web/honey/scaffolding/bubble for everyone | fly | none new |

## Findings
1. The biggest measurable Bedrock FP is ground spoof (111 flags, one player), and it is log-only today. The probable fix area is B1 (support tolerance on partial blocks), but there is no evidence which block types are involved. I did not add an exemption. Needed from the rig: the block under the player at those flags (trace unsupported/ground per packet), then decide a tolerance tied to the block shape. If any trace line shows feet=air over a full block with gap about 0, that is the sneak-edge shape, not a partial-block shape - see GROUND-SPOOF-TOLERANCE.md for why no vertical tolerance touches it.
2. The 1.30 sustain budget for Bedrock leaves a miss window: a constant 8.15 bps cheat never flags on a Bedrock player (budget 9.6 bps), a 10 bps one does. Acceptable under "FPs worse than misses", but recorded in BedrockMovementTest.
3. Nothing found for speed: 0 of 22 speed flags are from dot-names.
4. Out of scope and unchanged: combat/interact/inventory/mining skip (log "inventory steady-beat 0ms" = shortcut class), bedrockUnproven coverage.

## Proposal for the x1.30 sustain miss (source-grounded, compare mode first)
Facts: step() already scales the budget by the measured time between packets (dt clamped to 0.05-1.0 s), so a Bedrock packet that carries several ticks is forgiven by the dt term, not by the x1.30. BedrockBudgetTest shows legit 7.1 bps in 1-4 tick packets, a 1 s stall release and alternating 1/3 tick packets all stay at balance 0 with the plain 1.00x budget. The only live Bedrock speed flag in the corpus (".MICTHEPROFIT speed 0.52>0.34") is two normal 0.26 ticks in one packet, which is bunching, not speed. No log line shows a Bedrock player legitimately above 7.4 bps.
So the evidence does not support 1.30 on the sustain budget, but it also contains no real Bedrock client trace, so nothing proves 1.00 is safe on a Bedrock sprint-jump. Proposal: do not change the verdict yet. `MESHAC_BEDROCK_BUDGET=compare` runs the 1.00x budget beside the real one for Bedrock players and logs `[bedrocktol] would-have-flagged`. If a real Bedrock client on the rig (and a few days of ilhsmc logs) show zero lines for legit play, drop the sustain factor to 1.00 (keep the per-packet x1.30). If lines appear, tune toward the smallest factor that clears them (for example 1.04 to 1.10) with those traces.
