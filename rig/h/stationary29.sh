#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/stationary29.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcstill$(date +%H%M%S)
start_client || exit 1
cp /home/luke/meshac-work/clients/wurst/run/wurst/enabled-hacks.json /tmp/stationary29-hacks.json
setup_still(){ setup_meleetank; JITTER=0; PITCH=10; SECS=18; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; PRE='X key 1'; }
measure_still(){ measure_meleetank; }
setup_varied(){ setup_still; CLICK=0; PRE='X key 1; (for gap in 0.8 1.15 0.7 1.4 0.9 1.2 0.65 1.05 1.3 0.8 1.1 1.4 0.75 1.25; do X click 1; sleep "$gap"; done) &'; }
measure_varied(){ measure_meleetank; }
run_case still none
run_case varied none
