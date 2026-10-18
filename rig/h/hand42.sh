#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/hand42.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcHand42
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


setup_refill(){ X0=100 Y0=-60 Z0=420; SECS=3; rc 'fill 97 -61 417 103 -61 423 stone' >/dev/null; rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null; rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null; PRE='X keydown 1; sleep 0.2; X keyup 1; sleep 1; rc "damage $NAME 40 generic" >> /tmp/refill42.log; sleep 1; X keydown f; sleep 0.25; X keyup f; sleep 1; rc "data get entity $NAME Inventory" >> /tmp/refill42.log'; }
measure_refill(){ rc "data get entity $NAME Inventory"; }
run_case refill none
rc "tp $NAME 100.5 -60 160.5 0 28" >/dev/null
rc 'fill 97 -61 157 103 -61 164 stone' >/dev/null
rc 'setblock 100 -60 162 chest{Items:[{Slot:0,id:"minecraft:diamond",count:8}]}' >/dev/null
sleep 1
X mousedown 3;sleep 0.25;X mouseup 3;sleep 3
docker exec -e DISPLAY=:99 meshac-wurst import -window root /tmp/chest42.png
docker cp meshac-wurst:/tmp/chest42.png /tmp/chest42.png
sleep 90
