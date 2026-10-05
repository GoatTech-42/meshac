# Test rig

A throwaway Minecraft server plus test clients, all on a Docker network with no internet and no route to any real server.

- `run-server.sh <dir>` starts a Fabric server in a container called `meshac-rig` (2 GB, 2 CPUs). Pass `BOOTSTRAP=1` the first time so it can download Minecraft, and `MESHAC_TRACE=1` to log every move.
- `bots/` has scripted mineflayer bots (`run-all.sh legit speed fly ...`). mineflayer only goes up to 26.1, so the rig runs 26.1.2. Build the mod for it with `./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2`.
- `client/` runs a real Wurst client headless (see `REAL-CLIENTS.md`).
- The server needs `white-list=false`. Test runs use a flat world so the player never spawns against a wall.
