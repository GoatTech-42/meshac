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
RES=/tmp/scaffold41.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcPlace38
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p "${WORKSPACE}/meshac-work/clients/wurst-263/run/wurst"
 echo [] > "${WORKSPACE}/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json"
 (cd "${WORKSPACE}/meshac-work/meshac/rig/client" && ./run-client.sh "${WORKSPACE}/meshac-work/clients/wurst-263" "${WORKSPACE}/meshac-work/clients/gh" $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
sh ./world.sh >/dev/null


setup_bridge(){ X0=100 Y0=-41 Z0=800; KEYS='w'; SECS=12; PITCH=20; rc 'forceload add 90 790 110 870' >/dev/null; rc 'fill 96 -46 798 104 -38 866 air' >/dev/null; rc 'fill 98 -42 799 102 -42 801 stone' >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE='X key 1'; }
measure_bridge(){ rc "data get entity $NAME Inventory"; }
run_case bridge none
rc "effect give $NAME instant_health 1 10 true" >/dev/null
run_case bridge ScaffoldWalk
cp "${WORKSPACE}/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json" /tmp/scaffold41-modules-after.json
