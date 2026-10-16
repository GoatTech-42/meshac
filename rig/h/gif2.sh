#!/bin/bash
# Resolve the workspace without baking a user name into the script.
if [ "${WORKSPACE+x}" != x ]; then
  if [ -n "${SUDO_USER:-}" ] || [ -z "${HOME:-}" ]; then
    printf '%s\n' 'set WORKSPACE explicitly when HOME is unset or running through sudo' >&2
    exit 1
  fi
  WORKSPACE="$HOME"
fi
case "$WORKSPACE" in
  ''|*[\"\'\$\`]*|*'
'*)
    printf '%s\n' 'WORKSPACE is empty or contains unsafe shell characters' >&2
    exit 1 ;;
  /*) ;;
  *) printf '%s\n' 'WORKSPACE must be an absolute path' >&2; exit 1 ;;
esac
if [ ! -d "$WORKSPACE/meshac-work/meshac/rig/h" ] ||
   [ ! -r "$WORKSPACE/meshac-work/meshac/rig/h/run.sh" ] ||
   [ ! -r "$WORKSPACE/meshac-work/meshac/rig/h/lib.sh" ]; then
  printf '%s\n' 'WORKSPACE must contain the prepared rig/h directory and readable run.sh/lib.sh' >&2
  exit 1
fi
export WORKSPACE
# GIF 1: Wurst Flight on the rig with /mesh watch on. Records frames from the client display.
cd -- "${WORKSPACE}/meshac-work/meshac/rig/h/" || exit 1
. ./lib.sh
NAME=mcGif$(date +%s); TAG=${1:-fly}
trap "docker stop meshac-wurst >/dev/null 2>&1" EXIT
mkdir -p /tmp/gif-$TAG; rm -f /tmp/gif-$TAG/*
sh ./world.sh >/dev/null
reset_hacks; docker rm -f $CL >/dev/null 2>&1
(cd "${WORKSPACE}/meshac-work/meshac/rig/client" && ./run-client.sh "${WORKSPACE}/meshac-work/clients/wurst" "${WORKSPACE}/meshac-work/clients/gh" $NAME >/dev/null)
for i in $(seq 1 45); do sleep 4; connected && [ -n "$(hp)" ] && break; done; sleep 10
rc "time set day" >/dev/null; rc "gamerule advance_time false" >/dev/null; rc "op $NAME" >/dev/null; rc "gamemode survival $NAME" >/dev/null; rc "effect clear $NAME" >/dev/null
rc "tp $NAME 100.5 -60 0.5 0 0" >/dev/null; rc "give $NAME diamond_sword" >/dev/null; rc "effect give $NAME resistance 600 4 true" >/dev/null; rc "summon zombie 100.5 -60 4.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null; sleep 3
docker exec $CL mkdir -p /tmp/fr
( n=0; while true; do docker exec -e DISPLAY=:99 $CL import -window root -resize 854x480 /tmp/fr/f$(printf %04d $n).png 2>/dev/null; n=$((n+1)); done ) &
REC=$!
sleep 2
say "/mesh watch"; sleep 2
say ".t KillAura"; sleep 1; X key 1
for i in $(seq 1 40); do sleep 1; connected || break; done
sleep 5
kill $REC 2>/dev/null; sleep 1
docker cp $CL:/tmp/fr/. /tmp/gif-$TAG/ >/dev/null 2>&1
ls /tmp/gif-$TAG | wc -l
docker logs meshac-rig 2>&1 | grep "SIGNAL $NAME\|kick\|Kicked" | tail -6 | cut -c1-200
