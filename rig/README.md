# Test rig
Throwaway server + bots, isolated on an internal Docker network (no internet).
- rig/run-server.sh <dir>: starts Fabric server in container meshac-rig (2 GB, 2 CPU). BOOTSTRAP=1 for the first run only (downloads Minecraft).
- rig/bots: mineflayer bots. mineflayer supports up to MC 26.1, so the rig runs 26.1.2.
  Build the mod for it: ./gradlew build -Pminecraft_version=26.1.2 -Pfabric_api_version=0.155.3+26.1.2
- Server needs white-list=false in server.properties.
