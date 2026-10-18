#!/bin/sh
# Throwaway test server. 2 CPU / 2 GB cap, internal network only (no internet, no route to live servers).
# First run needs internet once to fetch Minecraft: set BOOTSTRAP=1 to use the default network.
# usage: rig/run-server.sh <server-dir>   (dir holds fabric-server.jar, mods/, eula.txt)
NET=meshac-net; [ "$BOOTSTRAP" = 1 ] && NET=bridge
docker network inspect meshac-net >/dev/null 2>&1 || docker network create --internal meshac-net
docker rm -f meshac-rig >/dev/null 2>&1
docker run -d ${MESHAC_TRACE:+-e MESHAC_TRACE=1} --name meshac-rig --network $NET --memory 2g --cpus 2 -v "$1":/srv -w /srv eclipse-temurin:25-jre java -Xmx1400M -jar fabric-server.jar nogui
