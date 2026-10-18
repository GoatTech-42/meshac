#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/combat28.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
setup_tankfix(){ setup_meleetank; JITTER=0; PITCH=10; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; PRE='X key 1'; }
measure_tankfix(){ measure_meleetank; }
for hack in KillauraLegit MultiAura; do
 NAME=mctank$(date +%H%M%S)
 start_client || exit 1
 say '.t all off'
 run_case tankfix none
 say '.t all off'
 run_case tankfix "$hack"
 sleep 3
 echo "$NAME $hack FINAL_CONNECTED $(connected && echo yes || echo no)" >> /tmp/combat28-state.log
 docker stop meshac-wurst >/dev/null
 done
