# Candidate9n source integration

Base: candidate9m 20031e8b2c53263491aca527f392918ee05985c3.
Input patch: e7ad7f0d2665bc13b79851393a987d4ba5f2abe8, imported with semantic union.

## Scope
- BounceTracker arms in the supported-contact fall record before jump/high-jump/microhop consumers, tracks restitution-capped rises and decay-chain re-landings, and guards microhop.
- Keep the one early awaiting-confirm guard, all b2796d8 timer/air-jump resets, powder-floor/sink fixes, onGround accessor and diagnostic budgets from9m. Runner conflict resolved by deduplicated union; no classes removed.
- Compatibility fix: getBounceRestitution is present only in26.2/26.3 mapped jars. BounceRestitution adapter uses native getter there. For1.21.11/26.1.2, javap of actual mapped SlimeBlock/BedBlock bounceUp confirms living-player coefficients1.0 and0.6600000262260437; legacy adapter uses these exact values for those block classes, else0. Sneak suppression remains in Movement.
- Compile-time source selection via explicit bounce_src in each versions/*.properties; default bounce26 in gradle.properties. Both build scripts include selected source. No reflection or runtime guessed method lookup.

## Validation
- All71 pure Java test classes PASS (JDK25.0.4.1+1), including InteractionPoseRingTest, BounceTrackerTest and BounceFixtureTest.
- All18 rig/test*.py scripts PASS, repeated after compatibility edits.
- Exact new Movement restitution expression plus selected adapter javac-compiles against each actual mapped game jar1.21.11/26.1.2/26.2/26.3. Probe uses isShiftKeyDown, level/getBlockState/blockPosition/below/getBlock, legacy block types or native getter as appropriate.
- Source assertions confirm a single early pending guard before Blink and bounce arm before first bouncing consumer. git diff --check clean.
- Source and focused API checks only, not complete Fabric builds. No deploy, rig config/jar changes, server/world/client copy or process kill. Sequential nice19 tests and256MiB javac probe heap; pre-run available RAM3860MiB.

## Remaining gates/caveats
- Full four-version builds and private meshac-red native legit/positive controls remain with the box owner. Pure fixture replay is not native clearance or release permission.
- BounceFixtureTest uses sampled candidate8 positions and skips presumed setback snaps via grace; slime25 recording has no surviving bounce rise. Its zero uncovered count cannot prove all native bounce episodes pass.
- Tracker starts its rise baseline from the first bouncing query, not the landing y parameter. Direct single-packet overshoot sensitivity and repeated supported-contact behavior need native controls.
- Existing pending Netty BlinkClock and diagnostic override caveats remain.
