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
RES=/tmp/meteor34.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=meshLegit34
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 d="${WORKSPACE}/meshac-work/clients/meteor/run/meteor-client"
 cp "$d/modules.nbt" /tmp/meteor34-modules-before.nbt
 rm "$d/modules.nbt"
 docker run -d --name meshac-wurst --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v "${WORKSPACE}/meshac-work/clients/gh:/gh" -v "${WORKSPACE}/meshac-work/clients/meteor:/w" -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $NAME --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
 for i in $(seq 1 45);do sleep 4;connected && { sleep 8;return 0;};done;return 1
}
ensure(){ connected && [ "$(hp)" != 0.0 ]; }
start_client || exit 1
say '.t all off'
sh ./world.sh >/dev/null
setup_still(){ setup_meleetank; JITTER=0; PITCH=10; SECS=30;rc 'kill @e[type=zombie]' >/dev/null;rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_still(){ measure_meleetank; }
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_foodfix(){ setup_food;rc "item replace entity $NAME hotbar.0 with bread 16" >/dev/null;SECS=14;PRE='X key 1; X mousedown 3; (sleep 14; X mouseup 3) &'; }
measure_foodfix(){ measure_food; }
setup_stepfix(){ setup_step;KEYS='w space'; }
for scenario in still walk fall water wall ladder stepfix web soulsand snow boat meleeclick meleetank minefix foodfix;do run_case "$scenario" none;done
