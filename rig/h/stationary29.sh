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
RES=/tmp/stationary29.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcstill$(date +%H%M%S)
start_client || exit 1
cp "${WORKSPACE}/meshac-work/clients/wurst/run/wurst/enabled-hacks.json" /tmp/stationary29-hacks.json
setup_still(){ setup_meleetank; JITTER=0; PITCH=10; SECS=18; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; PRE='X key 1'; }
measure_still(){ measure_meleetank; }
setup_varied(){ setup_still; CLICK=0; PRE='X key 1; (for gap in 0.8 1.15 0.7 1.4 0.9 1.2 0.65 1.05 1.3 0.8 1.1 1.4 0.75 1.25; do X click 1; sleep "$gap"; done) &'; }
measure_varied(){ measure_meleetank; }
run_case still none
run_case varied none
