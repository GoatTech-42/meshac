# Test rig

A throwaway Minecraft server plus test clients, all on a Docker network with no internet and no route to any real server.

- `run-server.sh <dir>` starts a Fabric server in a container called `meshac-rig` (2 GB, 2 CPUs). Pass `BOOTSTRAP=1` the first time so it can download Minecraft, and `MESHAC_TRACE=1` to log every move.
- `bots/` has scripted mineflayer bots (`run-all.sh legit speed fly ...`). mineflayer only goes up to 26.1, so the rig runs 26.1.2. Build the mod for it with `./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2`.
- `client/` runs a real Wurst client headless (see `REAL-CLIENTS.md`).
- The server needs `white-list=false`. Test runs use a flat world so the player never spawns against a wall.

## Parallel bot rigs

`botrig.sh up <id> <version> <jar>` starts `meshac-bot-<id>`, a server on its own copy of `smoke-<version>` (1 CPU, 1 GB, trace on), and `botrig.sh down <id>` removes it. Bot rigs are cheap (a few percent of one core when idle), so several can run at once for packet-level tests. `bots/thru2.sh` is an example: a mineflayer bot digs the block behind another block while a second rig runs the control.

The real clients are the expensive part. A Wurst or Meteor client takes about 2 cores and 2 GB (software rendering), so run one at a time. On the 4-core box, one client rig plus three bot rigs peaks around half the CPU.

## Things that bite

- The real client needs the `meshac-client-egl` image. `client/Dockerfile.egl` builds it on top of `meshac-client`, pointed at a mirror that works from the box.
- Gradle hangs when it has no network. Build with `--offline`.
- Wurst 26.3 needs the 26.3 server (`smoke-26.3`); the Meteor client only exists for 26.2.
- Give every rig run a fresh player name.
- Scenario scripts are in `h/`; `h/scenarios.sh` has the setups, and each run writes a csv plus the server trace lines.
