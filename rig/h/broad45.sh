#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/broad45.csv
: > "$RES"
NAME=mcBroad45
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 mkdir -p /home/luke/meshac-work/clients/wurst/run/wurst
 echo [] > /home/luke/meshac-work/clients/wurst/run/wurst/enabled-hacks.json
 (cd /home/luke/meshac-work/meshac/rig/client && ./run-client.sh /home/luke/meshac-work/clients/wurst /home/luke/meshac-work/clients/gh $NAME >/dev/null)
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}

ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
sh ./world.sh >/dev/null

setup_swaphand(){ setup_meleetank; SECS=6;PITCH=10;JITTER=0;rc 'kill @e[type=zombie]' >/dev/null;rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;rc "item replace entity $NAME hotbar.1 with stone_axe" >/dev/null;PRE='X key 1; sleep 0.8; X key 2; sleep 0.8; X click 1; sleep 0.6; X key 1'; }
measure_swaphand(){ measure_meleetank; }
setup_windhand(){ X0=100 Y0=-60 Z0=900; PITCH=90;SECS=6;rc 'fill 96 -60 896 108 -40 912 air' >/dev/null;rc 'fill 96 -61 896 108 -61 912 stone' >/dev/null;rc "item replace entity $NAME hotbar.0 with wind_charge 16" >/dev/null;PRE='X key 1; X keydown space; sleep 0.2; X mousedown 3; sleep 0.2; X mouseup 3; X keyup space; (for i in $(seq 1 15);do pos >> /tmp/wind45-pos.log;sleep 0.2;done) &'; }
measure_windhand(){ rc "data get entity $NAME Inventory"; }
setup_levsafe(){ X0=100 Y0=-60 Z0=0;SECS=10;rc "effect give $NAME resistance 60 255 true" >/dev/null;PRE='rc "effect give $NAME levitation 4 4 true" >/dev/null; (for i in $(seq 1 15);do pos >> /tmp/lev45-pos.log;sleep 0.3;done) &'; }
setup_zkb(){ setup_zombiehit;rc 'fill 96 -61 696 108 -61 710 stone' >/dev/null;rc 'fill 96 -60 696 108 -54 710 air' >/dev/null; }
setup_glidehand(){ X0=100 Y0=10 Z0=460;SECS=8;rc "item replace entity $NAME armor.chest with elytra" >/dev/null;rc "effect give $NAME resistance 60 255 true" >/dev/null;PRE='X keydown space; sleep 0.25; X keyup space; sleep 0.6; X keydown space; sleep 0.25; X keyup space; X keydown w; (for i in $(seq 1 16);do rc "data get entity $NAME FallFlying" >> /tmp/glide45.log;pos >> /tmp/glide45.log;sleep 0.25;done) &';KEYS='w'; }
measure_glidehand(){ rc "data get entity $NAME FallFlying"; }
for c in swaphand windhand levsafe zkb glidehand;do run_case "$c" none;done
