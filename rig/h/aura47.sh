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
RES=/tmp/aura47.csv
: > "$RES"
NAME=mcAura47
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p "${WORKSPACE}/meshac-work/clients/wurst/run/wurst"
 echo [] > "${WORKSPACE}/meshac-work/clients/wurst/run/wurst/enabled-hacks.json"
 (cd "${WORKSPACE}/meshac-work/meshac/rig/client" && ./run-client.sh "${WORKSPACE}/meshac-work/clients/wurst" "${WORKSPACE}/meshac-work/clients/gh" $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}

ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
sh ./world.sh >/dev/null

setup_multi(){ X0=100 Y0=-60 Z0=20; SECS=25;CLICK=0;PITCH=10;rc 'fill 95 -61 15 105 -61 28 stone' >/dev/null;for xyz in '100.5 23' '98.5 22.5' '102.5 22.5';do rc "summon zombie ${xyz% *} -60 ${xyz#* } {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null;done;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_multi(){ rc 'execute as @e[type=zombie] run data get entity @s Health'; }
setup_manualmulti(){ setup_multi;CLICK=1;JITTER=1;PRE='X key 1; (for i in $(seq 1 12);do X mousemove_relative -- 60 0;sleep 0.5;X mousemove_relative -- -120 0;sleep 0.5;X mousemove_relative -- 60 0;sleep 1;done) &'; }
measure_manualmulti(){ measure_multi; }
run_case manualmulti none
run_case multi Killaura
run_case multi MultiAura
run_case multi KillauraLegit
