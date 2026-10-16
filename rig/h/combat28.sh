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
RES=/tmp/combat28.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
setup_tankfix(){ setup_meleetank; JITTER=0; PITCH=10; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; PRE='X key 1'; }
measure_tankfix(){ measure_meleetank; }
for hack in KillauraLegit MultiAura; do
 NAME=mctank$(date +%H%M%S)
 start_client || exit 1
 say '.t all off'
 run_case tankfix none
 say '.t all off'
 run_case tankfix "$hack"
 sleep 3
 echo "$NAME $hack FINAL_CONNECTED $(connected && echo yes || echo no)" >> /tmp/combat28-state.log
 docker stop meshac-wurst >/dev/null
 done
