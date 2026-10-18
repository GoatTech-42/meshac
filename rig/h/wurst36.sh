#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/wurst36.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcLegit36
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst-12111/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst-12111/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst-12111 /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 sleep 40;
 for i in $(seq 1 45);do sleep 4;connected && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
say '.t all off'
sh ./world.sh >/dev/null
setup_still(){ setup_meleetank; JITTER=0; PITCH=10; SECS=30;rc 'kill @e[type=zombie]' >/dev/null;rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_still(){ measure_meleetank; }
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_foodfix(){ setup_food;rc "item replace entity $NAME hotbar.0 with bread 16" >/dev/null;SECS=14;PRE='X key 1; X mousedown 3; (sleep 14; X mouseup 3) &'; }
measure_foodfix(){ measure_food; }
setup_stepfix(){ setup_step;KEYS='w space'; }
for scenario in still walk fall water wall ladder stepfix web soulsand snow boat meleeclick meleetank minefix foodfix;do run_case "$scenario" none;done
