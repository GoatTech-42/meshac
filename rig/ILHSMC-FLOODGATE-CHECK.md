# Checking the Floodgate hook on ilhsmc

Read-only. Nothing here restarts the server, changes config or sends commands that change state. Deploying the jar is a separate step and is not covered here. Run this after the build with the hook (branch floodgate-linked-skip-9g-20261016, last commit "bedrock skip: log when the Floodgate API is found...") is running.

Why it exists: ilhsmc runs Floodgate-Fabric 2.2.6 and Geyser 2.11.3 with account linking on. A linked Bedrock player joins with their Java account's UUID, which does not have the zero high half Meshac used to look for, so they were checked like a Java client. The hook asks Floodgate (`org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid)`) and treats a yes as Bedrock.

## What Meshac says in the log
Meshac writes three lines, all starting `bedrock:` in the `meshac` logger. They appear lazily, the first time a player with a normal-looking UUID does something Meshac checks (so after the first Java player moves, not at startup):
- `bedrock: floodgate api found, asking it about non-Bedrock-looking UUIDs` means the class and its two methods were found and Floodgate had started.
- `bedrock: floodgate api not found, Bedrock skip uses the zero-high UUID rule only` means the class or method was missing. The hook is off and nothing is worse than before.
- `bedrock: Floodgate player with a normal UUID (linked account) treated as Bedrock: <uuid>` once per linked player per session, within about 5 seconds of their first checked action.
Unlinked Bedrock players have a zero-high UUID and never produce a line. That is expected.

## What was checked about the API names
Checked against the jar ilhsmc actually runs, Floodgate 2.2.6-b67, with javap on `org.geysermc.floodgate.api.FloodgateApi`: `public static FloodgateApi getInstance()`, `public abstract boolean isFloodgatePlayer(UUID)` and `isFloodgateId(UUID)` are all there. `FloodgatePlayer` has `getJavaUniqueId`, `getCorrectUniqueId`, `isLinked` and `getLinkedPlayer`. The hook only uses getInstance and isFloodgatePlayer. Step 3 below ("api found") is still the live confirmation after deploy; if it says "not found" the hook stays off and the UUID rule applies.

## Steps
1. Before anything else, note the Floodgate and Geyser versions from the startup log (`grep -i "floodgate\|geyser" logs/latest.log | head`). Expected: Floodgate-Fabric 2.2.6, Geyser 2.11.3. Do not open Floodgate's `key.pem`.
2. Read the Floodgate config (`config/floodgate/config.yml`): confirm `username-prefix`, `player-link` enabled, `require-link`. Read only. Write down the values.
3. With one ordinary Java player online and moving, `grep "bedrock:" logs/latest.log`. You should see the "api found" line. If you see "not found", stop: send us the Floodgate jar name and the first lines of its load, and leave it. Nothing broke, the hook is just inactive.
4. `/geyser list` (read only) shows the connected Bedrock players, linked or not. Write down each name.
5. For each linked Bedrock player on that list, find their Java UUID (`/mesh status <name>` or the player's UUID in the join line) and check the log has the "treated as Bedrock: <uuid>" line for that UUID. A linked player with no such line after 10 seconds of play is the failure case: write down the name, the UUID, and whether `player-link` shows them linked.
6. Control: a Java player that is NOT on `/geyser list` must have no "treated as Bedrock" line. If one appears, stop and tell us: that would be a false skip.
7. For one linked Bedrock player over about 10 minutes of normal play (mining, a fight if it happens, moving items): `/mesh status <name>` and `grep <name> logs/latest.log | grep SIGNAL`. There should be no `combat`, `interact`, `inventory` or `mining` signals. Movement signals can still appear: Bedrock players keep the movement checks with the looser Bedrock budget, and that is by design.
8. Compare with the 24 hour flag list from before the change: the players whose flags were all inventory bursts and speed should stop producing those. If a player still does, their name goes in the report.

## Pass
- Step 3 shows "api found".
- Every player on `/geyser list` with a normal UUID has a "treated as Bedrock" line, and no Java player has one.
- No combat, interact, inventory or mining signals for those players.

## Not proven by this
- That Floodgate's answer is right in every case. It is Floodgate's own registry of players that came in through its handshake, which a Java client cannot fake.
- Movement flags on Bedrock players (microhop, noclip, fly). Those checks run for Bedrock on purpose.
- Geyser with online auth and no Floodgate (such a player is a normal Java session as far as the server can tell). Not present on ilhsmc if Floodgate is what the config says.

## Roll back
Put the previous jar back the usual way. The old rule, zero-high UUID only, comes back with it. Nothing is stored on disk by the hook.
