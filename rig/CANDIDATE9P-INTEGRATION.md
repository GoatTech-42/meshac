# Candidate9p source integration

Base: candidate9o aa6c029cb6b3987c26fce3c714fbadf11181e8b2.
Inputs: 62061e2 +833d237, then exact serialized-plan and atomic replacement fixes.

## Scope and fixes
- Add chunk-packet hidden block-entity strip and ViewerPolicy boolean Bedrock answer overload. Include shuffled-map tests, beStripped/beSizeMismatch counters and BE-STRIP-CHECK.md.
- No mask recomputation at constructor return. Veil.veiledResult returns the exact private section copy plus only block-entity indices actually replaced in that copy. Stale skipped entries are not recorded; failed/null copies return no mask. PacketPlan sizes and writes the same selected section. Encoded masks are eligible only after that section write completes.
- Hold the per-packet plan through constructor RETURN, consume the written masks there and clear in finally. No strip from an unrelated chunk plan. Existing size/write behavior otherwise retained.
- Build filtered list privately; shadow is Final+Mutable so assigning the finished list is the single commit point. Never remove from original. Failures before swap leave original reference and contents unchanged. Counters updated after commit.

## Validation
- All72 pure Java classes PASS (JDK25.0.4.1+1,192MiBheap), including null-copy/throwing-copy fallback, copy-without-write, exact written-mask gating, failure-midway filtering, successful replacement and map-order controls.
- All18 rig/test*.py entry scripts PASS after functional edits. Final additional fallback assertions passed in the full72-class Java sweep.
- Exact ChunkDataMixin compiled against all4mapped game jars with actual Mixin annotations plus focused Veil glue containing the real modified veiled/veiledResult bodies. Other Veil dependencies stubbed: this is a focused API/type check, not complete Fabric compilation or a runtime transform.
- javap -p -c -s confirms all4 constructor descriptors (LevelChunk)V and private final blockEntitiesData descriptor List; all4 constructors build mutable ArrayList and add exactly one entry per getBlockEntities().entrySet() item, unfiltered.
- 1.21.11 mappings.tiny confirms packet named class -> intermediary class_6603, LevelChunk ->class_2818, field blockEntitiesData ->field_34865, descriptor List. Constructor argument remaps accordingly. 26.x uses deobfuscated names. Actual annotation-processor refmap generation and runtime injection remain Box gates.
- git diff --check clean. No deployment, rig jar/config change, process kills, server/world/client copies or heavy build. Sequential nice19 with192MiBheaps;2868MiB RAM available at validation close.

## Remaining gates/caveats
- Full four-version build and runtime mixin application, packet payload/list consistency, hidden/visible chest/spawner controls, reveal, ordinary block entities and native performance checks remain. No release permission.
- Index association still assumes the block-entity map order does not change between vanilla constructor walk and RETURN. Size mismatch skips stripping, but same-size map churn is not detected; server-thread-only masking reduces but does not prove absence of another mixin changing it.
- Constructor exceptions may leave a thread-local plan until the next size-pass resets it, as before. A normal RETURN always clears it.
- All inherited9n/9o movement/Floodgate/native-proof caveats remain.
