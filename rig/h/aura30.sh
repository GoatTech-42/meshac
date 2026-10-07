#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/aura30.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
setup_tankfix(){ setup_meleetank; JITTER=0; PITCH=10; SECS=18; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; PRE='X key 1'; }
measure_tankfix(){ measure_meleetank; }
for hack in Killaura KillauraLegit MultiAura Criticals; do
 NAME=mcfresh$(date +%H%M%S)
 start_client || exit 1
 cp /home/luke/meshac-work/clients/wurst/run/wurst/enabled-hacks.json "/tmp/aura30-$hack-before.json"
 run_case tankfix none
 run_case tankfix "$hack"
 sleep 3
 echo "$NAME $hack FINAL_CONNECTED $(connected && echo yes || echo no)" >> /tmp/aura30-state.log
 docker stop meshac-wurst >/dev/null
 done
