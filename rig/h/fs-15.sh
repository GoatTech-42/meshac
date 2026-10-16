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
cd -- "${WORKSPACE}/meshac-work/meshac/rig/h" || exit 1
. ./run.sh || exit 1
RES="${WORKSPACE}/meshac-work/results/final-sweep/sweep15.csv"
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcF15x5651
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 d="${WORKSPACE}/meshac-work/clients/meteor/run/meteor-client"
 cp "$d/modules.nbt" /tmp/meteor81-modules-before.nbt
 rm "$d/modules.nbt"
 docker run -d --name meshac-wurst --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v "${WORKSPACE}/meshac-work/clients/gh:/gh" -v "${WORKSPACE}/meshac-work/clients/meteor:/w" -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $NAME --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
 for i in $(seq 1 45);do sleep 4;connected && { sleep 8;return 0;};done;return 1
}

start_client || exit 1
sh ./world.sh >/dev/null
go(){ h=$1; sc=$2; shift 2; connected || start_client; say ".t all off"; if [ "$h" != none ]; then for st in "$@"; do say ".settings $h $st"; done; fi; run_case $sc $h; }
setup_bowt2(){ X0=100 Y0=-60 Z0=280; KEYS=""; SECS=14; rc "forceload add 100 280" >/dev/null; rc "fill 90 -60 270 110 -50 300 air" >/dev/null; rc "fill 90 -61 270 110 -61 300 stone" >/dev/null; rc "kill @e[type=zombie]" >/dev/null; rc "summon zombie 100.5 -60 292.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null; rc "item replace entity $NAME hotbar.0 with bow" >/dev/null; rc "give $NAME arrow 64" >/dev/null; PRE='X key 1; (sleep 1; for i in 1 2 3 4 5 6; do X mousedown 3; sleep 1.3; X mouseup 3; sleep 1.2; done) &'; }
measure_bowt2(){ rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
setup_anchor2(){ X0=100 Y0=-60 Z0=490; KEYS=""; SECS=12; rc "forceload add 100 490" >/dev/null; rc "fill 90 -60 480 110 -50 500 air" >/dev/null; rc "fill 90 -61 480 110 -61 500 stone" >/dev/null; rc "item replace entity $NAME hotbar.0 with respawn_anchor 16" >/dev/null; rc "item replace entity $NAME hotbar.1 with glowstone 32" >/dev/null; rc "effect give $NAME resistance 120 4 true" >/dev/null; PRE='X key 1; say ".t fake-player"; sleep 1'; }
measure_anchor2(){ rc "execute if entity @e[type=item]" | head -1; rc "clear $NAME respawn_anchor 0" | grep -o "[0-9]\+" | head -1; }
go none bowt2
go bow-aimbot bowt2
go anchor-aura anchor2
echo DONE >> $RES
