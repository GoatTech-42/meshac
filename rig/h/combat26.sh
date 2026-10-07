#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
NAME=mcaudit$(date +%H%M%S)
RES=/tmp/combat26.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
start_client || exit 1
sh ./world.sh >/dev/null
run_case meleetank none
run_case meleetank Killaura
run_case meleetank KillauraLegit
run_case meleetank MultiAura
run_case meleetank Criticals
