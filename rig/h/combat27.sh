#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
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
