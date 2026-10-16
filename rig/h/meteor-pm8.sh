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
RES=/tmp/meteorPM8.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcPM8
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
setup_pmine(){ X0=100 Y0=-60 Z0=500; YAW=0; PITCH=20; SECS=16; rc "forceload add 100 500" >/dev/null; rc "fill 96 -60 496 106 -50 510 air" >/dev/null; rc "fill 96 -61 496 106 -61 510 stone" >/dev/null; rc "setblock 100 -60 502 obsidian" >/dev/null; rc "setblock 100 -60 503 obsidian" >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_pickaxe" >/dev/null; : > /tmp/pm-times.txt; PRE='X key 1; X mousedown 1; (for i in $(seq 1 30); do sleep 0.5; rc "execute if block 100 -60 502 air" | grep -q passed && { echo "broke at $i half-seconds after start" >> /tmp/pm-times.txt; break; }; done; X mouseup 1) &'; }
measure_pmine(){ rc "execute if block 100 -60 502 air" | grep -o "passed\|failed"; }
go none pmine
cp /tmp/pm-times.txt /tmp/pm-times-legit.txt
go packet-mine pmine
echo DONE >> $RES
