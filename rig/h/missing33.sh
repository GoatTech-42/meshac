#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/missing33.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mchuman$(date +%H%M%S)
start_client || exit 1
setup_swapfix(){ setup_meleetank; PITCH=10; JITTER=0; SECS=12; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; rc "item replace entity $NAME hotbar.1 with stone_axe" >/dev/null; PRE='X key 1; sleep 0.8; X key 2; sleep 0.8; X click 1; sleep 0.6; X key 1'; }
measure_swapfix(){ measure_meleetank; }
setup_totemhand(){ X0=100 Y0=-60 Z0=420; PITCH=0; SECS=8; rc 'fill 97 -61 417 103 -61 423 stone' >/dev/null; rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null; rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null; PRE='X key 1; rc "damage $NAME 40 generic" >/dev/null; sleep 0.4; X key f; sleep 0.7'; }
measure_totemhand(){ rc "data get entity $NAME equipment.offhand" | grep -o 'totem_of_undying' | wc -l; }
setup_windhand(){ X0=100 Y0=-60 Z0=900; PITCH=90; SECS=8; rc 'fill 96 -60 896 108 -40 912 air' >/dev/null; rc 'fill 96 -61 896 108 -61 912 stone' >/dev/null; rc "item replace entity $NAME hotbar.0 with wind_charge 16" >/dev/null; rc "effect give $NAME resistance 60 4 true" >/dev/null; PRE='X key 1; X key space; sleep 0.12; X click 3; (for i in $(seq 1 16); do pos | cut -d, -f2 >> /tmp/wind33-y.log; sleep 0.2; done) &'; }
measure_windhand(){ rc "data get entity $NAME Inventory" | grep -o 'wind_charge' | wc -l; }
run_case swapfix none
run_case totemhand none
run_case windhand none
