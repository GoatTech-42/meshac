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
NAME=mcfix$(date +%H%M%S)
RES=/tmp/controls25.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
start_client || exit 1
sh ./world.sh >/dev/null
setup_minefix(){ setup_mine; rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_pickaxe" >/dev/null; PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_foodfix(){ setup_food; rc "item replace entity $NAME hotbar.0 with bread 16" >/dev/null; SECS=14; PRE='X key 1; X mousedown 3; (sleep 14; X mouseup 3) &'; }
measure_foodfix(){ measure_food; }
setup_stepfix(){ setup_step; KEYS='w space'; }
setup_anchorf(){ setup_anchorl; rc 'fill 97 -61 485 104 -61 493 stone' >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; rc "item replace entity $NAME hotbar.0 with glowstone 16" >/dev/null; PRE='X key 1'; }
measure_anchorf(){ rc 'execute if block 102 -60 488 respawn_anchor[charges=3]' | grep -o 'passed\|failed'; }
for scenario in minefix foodfix stepfix anchorf; do run_case "$scenario" none; done
docker stop meshac-wurst >/dev/null
