# meshac

An anticheat that runs inside a Fabric server. Players install nothing.

meshac watches how each player moves, works out what vanilla physics would allow, and steps in when the two don't match. First it puts the player back. If it keeps happening it freezes them for a moment, and if it still keeps happening it kicks them. It reacts to repeated evidence, not to one odd packet, so a laggy player or a lucky bounce doesn't get punished.

## Where it stands

Early beta work, not yet released. It checks movement, combat, building, mining, interaction and elytra flight. Against real hacked clients it catches, among others: Flight, Speed (above legal sprint-jump speed), Step, Spider, Jesus, Timer, Blink, Slippy, ElytraFly, KillAura, CrystalAura, Criticals, Nuker, PacketMine, GhostHand and AutoClicker. Legit play (sprint-jumping, wind charges, TNT and crystal knockback, elytra, lag spikes) has produced zero flags in rig runs so far. Not everything is proven: many modules are still untested and the detection runs so far are on 26.2 only. The honest per-module tables are [docs/COVERAGE-METEOR.md](docs/COVERAGE-METEOR.md) (Meteor) and [docs/coverage.md](docs/coverage.md) (Wurst), and open problems are in [docs/FALSE-POSITIVES.md](docs/FALSE-POSITIVES.md).

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

One source tree builds for 1.21.11, 26.1.x, 26.2 and 26.3. Run build-all.sh with JDK 25 and the jars land in dist/. The 1.21.11 jar uses the older obfuscated toolchain and runs on Java 21; the rest need Java 25. rig/smoke.sh boots each jar on a real server of its version and checks that meshac loads. So far that is a boot test only; the movement checks have been exercised on 26.1.2.
