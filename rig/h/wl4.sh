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
RES=/tmp/wl9.csv
: > "$RES"
NAME=mcWl11
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
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_minefix(){ setup_mine;rc "fill 96 -61 58 104 -61 62 stone" >/dev/null;PRE="X key 1; X mousedown 1; (sleep 10; X mouseup 1) &"; }
measure_minefix(){ measure_mine; }
for pair in "nuke Nuker" "minefix none" ; do
 set -- $pair
 docker stop meshac-wurst >/dev/null 2>&1
 rc "mesh unban $NAME" >/dev/null
 start_client || exit 1
 run_case "$1" "$2"
 docker logs -t meshac-rig > /tmp/wl9-server.log 2>&1
done
echo FIN >> /tmp/wl9.csv
