# Candidate9o source integration

Base: candidate9n 3f313c6671563afd53a45674b44dcc7c50d3ede7.
Inputs: Floodgate documentation ee396ed, hook ed5d968 and operator guide2c92c43; movement ctx tag86ed244.

## Semantic union
- Floodgate hook fetches the current instance per uncached question, preserves a known positive through temporary unavailability, refreshes positive answers at60s and negative answers at5s, clears session cache on join as well as disconnect, and tests reload/prefix/spoof/expiry behavior with a fake API.
- UUID identity, not player name or configured prefix, drives Bedrock classification. Operator guide explains exempt-name handling independently of automatic classification.
- Add awaiting-tp-ack to general ctx labels when pending. Resolve conflict without restoring isOnGround or dropping shared global/per-caller diagnostic budgets and dropped counters. Preserve single early pending guard and all9m/9n fixes.

## Validation
- All71 pure Java classes PASS with JDK25.0.4.1+1 and192MiBheap, including expanded BedrockIdsTest.
- All18 rig/test*.py entry scripts PASS.
- Only new mapped game API expression is join cleanup handler.player.getUUID(), already used by disconnect. Exact join expression plus current BedrockIds compiled against all four mapped game jars1.21.11/26.1.2/26.2/26.3. Narrowed library classpath allowed128MiB javac; prior broad-classpath192MiB attempt exhausted compiler heap before validation, not a source failure.
- No new Minecraft API in ctx string-only tag. Native bounce adapters and onGround fix remain from9n/9m.
- git diff --check clean. No full version build, deployment, rig mutation, copies or process kills. Observed3479MiB available after API checks.

## Caveats / remaining gates
- Source/pure plus focused mapped API check only. Full Fabric builds and private meshac-red native controls remain with Box; no release permission.
- Fake Floodgate tests do not prove live plugin registration/linked-account behavior. Retained jar-inspection documentation is inherited provenance, not a fresh live-server check.
- A cached yes can persist beyond60s while the API cannot answer; a fresh Java session during Floodgate pending-remove can be classified yes until a later successful refresh. Join forget avoids permanently carrying the old session cache, not the plugin pending-remove window.
- awaiting-tp-ack label is conditional at ctxLog invocation. Since early pending packets return before normal checks, importing the tag does not itself create diagnostic lines for every ignored packet.
- Prior9n bounce fixture/overshoot/native-control and pending Netty BlinkClock caveats remain.
