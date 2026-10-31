# Source-bound protocol checkpoint, 2026-10-30

This checkpoint tests scripted protocol traffic on Minecraft 26.1.2. It is not a native hacked-client or physics qualification, and it did not change a live server.

## Artifact

- Source: `ddbfe9f7e4713c8bb027660ef67319b473d6ed68` on `port/airchain-main-20261026`.
- Runtime jar SHA256: `6e1dcef83660cb372b48164e6939139a2c773586acab665030bec909ce625dea`.
- Movement source SHA256: `9b228604bad3802ef940664701eb2ef0caa653e740b8caf39ae8be60c3fcea56`.
- Build: Java 25, Minecraft 26.1.2, Fabric API 0.155.3+26.1.2, loader 0.19.5. Loom pinned to `1.18-20261010.111021-4`; Gradle 9.7.1. A clean offline rebuild produced identical runtime and sources jars.
- Full version profile: `versions/26.1.properties`, including `mc_range`, `java_min`, `attack_src` and `packet_src`. The two dependency overrides alone do not provide the full build profile.

## Actual result

One disposable isolated server, two sequential generated players, one run each:

| Stream | Client history | Server evidence | Result |
|---|---|---|---|
| Supported walk | 40 standing settle positions, then 20 positions moving 0.2 blocks each | Accepted final position `(4.5, -60, 0.5)`; no meshac signals or client corrections | Passed this scripted control |
| Hover | Fresh player, 40 settle positions, then 16 positions held 0.42 blocks above the surface | `fly dy 0.000 expect -0.078 heat=3 -> log`; accepted final position `(0.5, -59.58, 0.5)`; no client corrections | Passed this scripted positive |

Both players disconnected normally. Server and both client processes exited zero, and the server saved and stopped. The disposable processes and world were removed after evidence capture. Full logs, timestamped writes, observer snapshots, effective config, process samples and hashes were retained privately; a source path or this note is not a substitute for those receipts.

Before each stream, actual server-thread observations established the generated name/UUID, survival mode, no flying ability, no pending teleport, grace zero, no relevant movement exemption, normal-friction support and clear path. A separate test-only read-only observer supplied those snapshots without changing the production jar. Client completion alone did not count: final positions were read from the server after the stream.

The server used `monitorOnly=true`, empty `egregiousChecks`, and empty exemption/disabled lists. These checks logged only in this run. This is not a claim that monitor mode suppresses every correction path in that artifact: its two noclip paths returned corrections even when Verdict returned LOG.

## Later source-only fix

Commit `946f744e6a3dbd5d3ebef89173fbcf2eba681af5` makes those two Movement noclip paths return no correction when Verdict returns LOG. Scripted checks against real Movement source cover a player inside solid blocks, a two-block move through a wall, and an eleven-block move. Each still signals in LOG mode without returning a correction. SETBACK, HOLD and REMOVE still return the saved safe position, and a pending server teleport still skips the long-move rule.

The checks use collision and Verdict doubles, not a native client or a server runtime. Baseline failures and causal mutations show the tests catch the changed behavior. The affected movement simulation checks also passed. This source fix was not built into the checkpoint jar, deployed, or tested through a native client. It does not qualify other correction paths or change the results above.

## Boundaries

- Mineflayer 4.39.0 injected positions, with automatic movement packets suppressed during the scripted phase. This does not reproduce a native hacked client's behavior or vanilla client physics.
- The actual test used a bounded supported platform in a new world, four verified loaded chunks and `minecraft:respawn_radius=0`. It does not cover terrain, combat, other movement modes, lag, other versions or a full legitimate-play suite.
- The final run's peak sampled combined resident memory was 707,526,656 bytes, about 675 MiB. Sampling is not a hard aggregate memory cap or a general capacity estimate.
- Java's virtual address-space allowance was 1536 MiB, with a 256 MiB heap. A 1280 MiB virtual allowance had caused native allocation failures; the larger allowance did not raise the physical-memory stop thresholds. This is a test configuration, not a recommended production sizing rule.
- No native-client test, production deployment, enforcement change or release follows from this checkpoint.

The older real-client tables in `coverage.md` describe historical tested jars, not a fresh qualification of this artifact.

## Later persistence boundary: deployment held

Cases atomic-save source checkpoint `cea93343f63134f3754f531641d067ceaa20eb97` has not been deployed and is not deployment-ready. Its finite synthetic tests establish normal replacement and preservation of prior bytes under a capped staged-write failure, plus temporary-file cleanup. They do not establish compatibility with prior file metadata or links. The Config atomic-save candidate is unpublished. Existing non-atomic write behavior has a known partial-write/truncation risk; that risk is not declared solved for a deployable runtime.

Disposable Java 25 Linux-provider research showed that replacing a file with a temporary inode can change permissions and inode identity, sever the path from hardlink aliases, and replace a symlink rather than write its target. A real POSIX ACL on a synthetic file was invisible to Java's ACL view and user-xattr list; mode-only replacement lost that ACL even while the nine permission bits and fixture owner/group matched. Matching fixture ownership does not establish ownership of an existing installation's file. Group, full mode, ACLs/xattrs, ownership, links, creation time and inherited metadata remain a shared compatibility boundary for both writers. No mode-only fix, new-file default, symlink policy or provider adapter is selected by this research.

The branch as a whole is not qualified for runtime rollout. No fsync, power-loss, directory-durability or metadata-fidelity claim follows from atomic byte-publication tests. Failed-save memory semantics have not changed. Further persistence work needs one reviewed compatibility contract for both writers, not separate attribute patches.

## Later source-only fishing state qualification

`sim/test-fish.py` pins Fish source SHA256 `84b1840e477d046d423831bf934e7f65b75424fa43fb3d080b20d38ecedf19dc`. It compiles that source with only a reversible clock substitution and focused nibble, player, server and Verdict doubles. Its 56 assertions cover the four-fast-bite threshold, consumed-bite behavior, slow-reel reset at exactly 100 ms, latency clamp at 0 and 300 ms, nonnegative displayed reaction, continued-nibble timing, ended/absent hooks, creative use, UUID isolation and explicit `forget` cleanup. Nine causal mutants are rejected by authored Java assertions.

This is not a native fishing, nibble accessor/Mixin application, packet-hook ordering, real latency or server lifecycle-event qualification. Production fishing logic, thresholds and defaults are unchanged. The test does not establish spectator/exemption policy or infer a policy change from it.
