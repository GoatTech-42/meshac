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
# usage: lagrun.sh <stall_ms> <every_s> <tag>   N8 flicker through the lag proxy against the 26.3 rig
H="${WORKSPACE}/meshac-work/meshac/rig/h"
docker rm -f meshac-rig meshac-wurst meshac-lagproxy >/dev/null 2>&1
MESHAC_TRACE=1 sh "${WORKSPACE}/meshac-work/meshac/rig/run-server.sh" "${WORKSPACE}/meshac-work/rig/smoke-26.3" >/dev/null; sleep 30
docker run -d --name meshac-lagproxy --network meshac-net -e STALL_MS=$1 -e EVERY_S=$2 -v $H/lagproxy.py:/p.py python:3-alpine python /p.py >/dev/null
sed "s/NAME=mcWl22/NAME=mcWl2$3/; s#/tmp/wl22#/tmp/wl22-$3#g" $H/wl22.sh > $H/wl22-$3.sh
bash $H/wl22-$3.sh
docker rm -f meshac-rig meshac-wurst meshac-lagproxy >/dev/null 2>&1
