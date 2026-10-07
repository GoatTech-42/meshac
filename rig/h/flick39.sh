#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/flick39.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcPlace38
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst-263/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst-263/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst-263 /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}
ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
sh ./world.sh >/dev/null

setup_swapfix(){ setup_meleetank; PITCH=10; JITTER=0; SECS=8; rc 'kill @e[type=zombie]' >/dev/null; rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null; rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null; rc "item replace entity $NAME hotbar.1 with stone_axe" >/dev/null; PRE='X key 1; sleep 0.8; X key 2; sleep 0.8; X click 1; sleep 0.6; X key 1'; }
measure_swapfix(){ measure_meleetank; }
setup_windhand(){ X0=100 Y0=-60 Z0=900; PITCH=90; SECS=8; rc 'fill 96 -60 896 108 -40 912 air' >/dev/null; rc 'fill 96 -61 896 108 -61 912 stone' >/dev/null; rc "item replace entity $NAME hotbar.0 with wind_charge 16" >/dev/null; PRE='X key 1; X key space; sleep 0.12; X click 3; (for i in $(seq 1 16); do pos | cut -d, -f2 >> /tmp/wind37-y.log; sleep 0.2; done) &'; }
measure_windhand(){ rc "data get entity $NAME Inventory"; }
setup_anchorf(){ setup_anchorl; rc 'fill 97 -61 485 104 -61 493 stone' >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; rc "item replace entity $NAME hotbar.0 with glowstone 16" >/dev/null; PRE='X key 1'; }
measure_anchorf(){ rc 'execute if block 102 -60 488 respawn_anchor[charges=3]' | grep -o 'passed\|failed'; }
setup_buildhand(){ X0=100 Y0=-60 Z0=80; PITCH=70; SECS=6; CLICK=3; rc 'fill 96 -60 76 104 -54 88 air' >/dev/null; rc 'fill 96 -61 76 104 -61 88 stone' >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE='X key 1'; }
measure_buildhand(){ rc "data get entity $NAME Inventory"; }
setup_totemhand(){ X0=100 Y0=-60 Z0=420; SECS=4; rc 'fill 97 -61 417 103 -61 423 stone' >/dev/null; rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null; rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null; PRE='X key 1; sleep 1; rc "damage $NAME 40 generic" >> /tmp/totem37.log; sleep 1; rc "data get entity $NAME Inventory" >> /tmp/totem37.log; X key f; sleep 1; rc "data get entity $NAME Inventory" >> /tmp/totem37.log'; }
measure_totemhand(){ rc "data get entity $NAME Inventory"; }
setup_chesthand(){ X0=100 Y0=-60 Z0=160; PITCH=10; SECS=3; rc 'fill 97 -61 157 103 -61 164 stone' >/dev/null; rc 'setblock 100 -60 162 chest{Items:[{Slot:0,id:"minecraft:diamond",count:8}]}' >/dev/null; PRE='X click 3; sleep 1; X mousemove 496 265; X keydown shift; X click 1; X keyup shift; sleep 1; X key Escape'; }
measure_chesthand(){ rc 'data get block 100 -60 162 Items'; rc "data get entity $NAME Inventory"; }

setup_flickhand(){ setup_anchorf; SECS=5; CLICK=0; PRE='X key 1; for i in 1 2 3; do X mousemove_relative -- 80 0; sleep 0.3; X mousemove_relative -- -80 0; sleep 0.3; X click 3; sleep 0.6; done'; }
measure_flickhand(){ measure_anchorf; }
for c in flickhand;do run_case "$c" none;done
