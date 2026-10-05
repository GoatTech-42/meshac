# meshac

An anticheat that runs inside a Fabric server. Players install nothing.

meshac watches how each player moves, works out what vanilla physics would allow, and steps in when the two don't match. First it puts the player back. If it keeps happening it freezes them for a moment, and if it still keeps happening it kicks them. It reacts to repeated evidence, not to one odd packet, so a laggy player or a lucky bounce doesn't get punished.

## Where it stands

Early, but it works. Against a real hacked client (Wurst) it catches Flight, SpeedHack, HighJump, Glide and Timer, usually within a second, and it does that before vanilla's own checks do. It is not ready for a live server yet: it only checks movement, and some block types (ice, slime, bubble columns) still need testing. The full list of what is covered and what is not is in [docs/coverage.md](docs/coverage.md).

Builds for Minecraft 26.1.2 and 26.3. 1.21.11 is next.

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

`rig/` has a throwaway server and test clients on a Docker network with no internet. It can run scripted bots and a real Wurst client. See [rig/README.md](rig/README.md).

## License

MIT.

## Versions

One source tree builds for 1.21.11, 26.1.x, 26.2 and 26.3. Run build-all.sh with JDK 25 and the jars land in dist/. The 1.21.11 jar uses the older obfuscated toolchain and runs on Java 21; the rest need Java 25. rig/smoke.sh boots each jar on a real server of its version and checks that meshac loads. So far that is a boot test only; the movement checks have been exercised on 26.1.2.
