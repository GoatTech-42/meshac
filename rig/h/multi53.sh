#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/multi53.csv
: > "$RES"
NAME=mcMulti53
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

turn(){
 local aim=$1
 for j in $(seq 1 10);do
  r=$(rc "data get entity $NAME Rotation");y=$(echo "$r" | sed -n 's/.*\[\([-0-9.]*\)f,.*/\1/p');[ -n "$y" ] || return 1
  dx=$(awk -v y="$y" -v a="$aim" 'BEGIN{d=a-y;while(d>180)d-=360;while(d< -180)d+=360;print int(d/0.15)}')
  [ "$dx" = 0 ] && break
  X mousemove_relative -- "$dx" 0;sleep 0.2
 done
}
setup_three(){ X0=100 Y0=-60 Z0=20;SECS=20;PITCH=10;rc 'fill 95 -61 15 105 -61 28 stone' >/dev/null;rc 'kill @e[type=zombie]' >/dev/null;for v in '98.5 22.5 left' '100.5 23.0 centre' '102.5 22.5 right';do set -- $v;rc "summon zombie $1 -60 $2 {Tags:[\"$3\"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000},{id:\"minecraft:knockback_resistance\",base:1}]}" >/dev/null;done;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_three(){ for tag in left centre right;do printf '%s=' "$tag";rc "data get entity @e[type=zombie,tag=$tag,limit=1] Health";done; }
setup_handthree(){ setup_three;SECS=4;PRE='X key 1; for i in 1 2 3;do for a in 45 0 -45;do turn "$a";sleep 0.8;rc "data get entity $NAME Rotation" >> /tmp/manual52-aim.log;rc "data get entity $NAME Pos" >> /tmp/manual52-aim.log;X mousedown 1;sleep 0.15;X mouseup 1;sleep 0.65;done;done'; }
measure_handthree(){ measure_three; }
run_case handthree none
run_case three MultiAura
