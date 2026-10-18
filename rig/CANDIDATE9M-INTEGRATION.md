# Candidate9m source integration

Base: candidate9l bb50881547d09cc656554a39551ba80bc965f484.
Inputs: movement b2796d8beab7d453cc43dae341cefede4fda5fc5 and af01120; bc01276 and 66219b9 were already in the base.

## Semantic union
- Retain one early awaiting-confirm guard, before look feed, Blink, fall bookkeeping and status processing. A position packet consumes one arrival stamp; rotation/status consumes none. Added timer balance and air-jump resets from b2796d8 in that early guard. Removed the late duplicate.
- Preserve the steady powder-snow positional delta model (-0.1176/tick). Rig Motion -0.0784 describes the same fall. Include the supported-floor restingInPowder rule and pure fixture replay.
- Preserve BounceTracker, BlinkClock, StepAccumulator, PathCollision, lifecycle identity and all inherited context budgets.
- Correct ctxLog ground accessor from isOnGround() to onGround(). All other ctx API arguments compiled unchanged.

## Validation
- All 70 pure Java test classes passed with JDK25.0.4.1+1.
- All 18 rig/test*.py entry scripts passed.
- A small javac API probe containing every external ctxLog MoveContext.line argument compiled against each actual mapped game jar: 1.21.11, 26.1.2, 26.2, 26.3. It includes profile name, onGround, water/lava/climb/passenger/fall-flying, hurtTime, damage source and getMsgId. javap confirms onGround in all four Entity mappings. This is a focused API check, not a complete Fabric/version build.
- Source/pure only. No deployment, rig jar/config change, server/world/client copy or process kill. Tests run sequentially at nice19; probe javac heap capped256MiB; available RAM observed3405MiB before validation.

## Remaining gates
- Full four-version builds and native private meshac-red regression/positive-control results belong to the box owner. This SHA is not release permission.
- BounceTracker arming-order fix is not in this candidate; flight worker patch is awaited for9n.
- Pending packets are inert inside check; Netty arrival BlinkClock still receives packet events.
- Prior diagnostic limits and MESHAC_TRACE_FULL rig override caveats remain unchanged.
