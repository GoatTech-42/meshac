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
cd -- "${WORKSPACE}/meshac-work/meshac/rig/h/" || exit 1
. ./run.sh || exit 1
RES=/tmp/open77.csv
: > "$RES"
NAME=mcOpen77
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p "${WORKSPACE}/meshac-work/clients/wurst-263/run/wurst"
 echo [] > "${WORKSPACE}/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json"
 (cd "${WORKSPACE}/meshac-work/meshac/rig/client" && ./run-client.sh "${WORKSPACE}/meshac-work/clients/wurst-263" "${WORKSPACE}/meshac-work/clients/gh" $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
sh ./world.sh >/dev/null; rc "forceload add -16 -16 16 16" >/dev/null; rc "fill -12 -61 -12 12 -61 12 stone" >/dev/null
start_client || exit 1
setup_buildhand(){ X0=100 Y0=-60 Z0=80; PITCH=70; SECS=6; CLICK=3; rc "fill 96 -60 76 104 -54 88 air" >/dev/null; rc "fill 96 -61 76 104 -61 88 stone" >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE="X key 1"; }
measure_buildhand(){ rc "data get entity $NAME Inventory"; }
setup_fastplacehand(){ setup_buildhand;SECS=8;CLICK=0;PRE="X key 1;X mousedown 3;(sleep 8;X mouseup 3) &"; }
measure_fastplacehand(){ measure_buildhand; }
run_case fastplacehand none
docker logs -t meshac-rig > /tmp/open77-server.log 2>&1
echo FIN >> /tmp/open77.csv
