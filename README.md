# meshac

An anticheat that runs inside a Fabric server. Players install nothing.

meshac watches how each player moves, works out what vanilla physics would allow, and steps in when the two don't match. First it puts the player back. If it keeps happening it freezes them for a moment, and if it still keeps happening it kicks them. It reacts to repeated evidence, not to one odd packet, so a laggy player or a lucky bounce doesn't get punished.

## Where it stands

Early beta work, not released yet. It checks movement, combat, building, mining, interaction and elytra flight, and it is built for 1.21.11, 26.1.2, 26.2 and 26.3.

What has been tried so far is real hacked clients (Wurst on 26.3, Meteor on 26.2) against a throwaway server, always with a hack-off control run next to it. Caught so far: Flight, Speed above sprint-jump pace, Step, Spider, Jesus, FastClimb, NoSlow, Timer, Blink (0.75 s and up), Slippy, AirJump, HighJump, ElytraFly, Reach, KillAura, Criticals, CrystalAura, Anchor Aura, AutoTotem, Nuker, PacketMine, GhostHand and AutoClicker. Legit play has not produced a flag in any control run: sprint-jumping, wind charges, knockback from TNT and crystals, elytra, hand mining through tunnels, lag spikes.

It does not catch everything. Known gaps: speed at or below sprint-jump pace (indistinguishable from a fast legit player), short Blink, Surround, BowAimbot, boosts above 45 blocks per second, and explosion knockback is exempt on purpose. Some modules are untested. The per-module tables are in [docs/COVERAGE-METEOR.md](docs/COVERAGE-METEOR.md) (Meteor) and [docs/coverage.md](docs/coverage.md) (Wurst). Open problems are in [docs/FALSE-POSITIVES.md](docs/FALSE-POSITIVES.md).

## Build

You need Java 25.

    ./gradlew build

The jar ends up in `build/libs/`. Drop it in a Fabric server's `mods` folder next to Fabric API. To build for another version, pass `-Pminecraft_version=... -Pfabric_api_version=...` (see `gradle.properties`).

## What the names mean

- A **signal** is one check catching something.
- **Heat** is a player's recent signals. It cools off after ten quiet seconds.
- A **trace** is the evidence behind a signal.
- A **ward** is an exemption.

The plan is a `/mesh` command and a small config file with three presets (calm, steady, strict). Neither exists yet. Details are in [docs/IDENTITY.md](docs/IDENTITY.md).

## Testing

`rig/` has a throwaway server and test clients on a Docker network with no internet. It runs real Wurst and Meteor clients, and a lag proxy for testing laggy connections. See [rig/README.md](rig/README.md).

## License

GNU Affero General Public License v3.0 only (see LICENSE). You can use, change and share meshac freely. If you distribute it or run a modified copy for others over a network, you have to share your changes under the same license and keep the copyright notices.

## Versions

One source tree builds for 1.21.11, 26.1.2 (the 26.1 build, dist/meshac-26.1.jar, targets 26.1.2), 26.2 and 26.3. Run build-all.sh with JDK 25 and the jars land in dist/. The 1.21.11 jar uses the older obfuscated toolchain and runs on Java 21; the rest need Java 25. rig/smoke.sh boots each jar on a real server of its version and checks that meshac loads. So far that is a boot test only; the movement checks have been exercised on 26.1.2.
