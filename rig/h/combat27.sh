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
RES=/tmp/combat27.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
for hack in KillauraLegit MultiAura; do
 NAME=mciso$(date +%H%M%S)
 start_client || exit 1
 say '.t all off'
 run_case meleetank none
 say '.t all off'
 run_case meleetank "$hack"
 sleep 3
 echo "$NAME $hack FINAL_CONNECTED $(connected && echo yes || echo no)" >> /tmp/combat27-state.log
 docker stop meshac-wurst >/dev/null
 done
