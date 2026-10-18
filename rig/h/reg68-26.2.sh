#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/reg68-26.2.csv
: > "$RES"
NAME=mcR68262
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 d=/home/luke/meshac-work/clients/meteor/run/meteor-client
 [ ! -f "$d/modules.nbt" ] || cp "$d/modules.nbt" /tmp/meteor44-modules-before.nbt
 rm -f "$d/modules.nbt"
 docker run -d --name meshac-wurst --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v /home/luke/meshac-work/clients/gh:/gh -v /home/luke/meshac-work/clients/meteor:/w -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $NAME --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
 for i in $(seq 1 45);do sleep 4;h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ] && { sleep 8;return 0;};done;return 1
}

ensure(){ h=$(hp); connected && [ -n "$h" ] && [ "$h" != 0.0 ]; }
start_client || exit 1
say ".t all off"
sh ./world.sh >/dev/null
setup_still(){ setup_meleetank; JITTER=0; PITCH=10; SECS=30;rc 'kill @e[type=zombie]' >/dev/null;rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_still(){ measure_meleetank; }
setup_minefix(){ setup_mine;rc 'fill 96 -61 58 104 -61 62 stone' >/dev/null;PRE='X key 1; X mousedown 1; (sleep 10; X mouseup 1) &'; }
measure_minefix(){ measure_mine; }
setup_foodfix(){ setup_food;rc "item replace entity $NAME hotbar.0 with bread 16" >/dev/null;SECS=14;PRE='X key 1; X mousedown 3; (sleep 14; X mouseup 3) &'; }
measure_foodfix(){ measure_food; }
setup_stepfix(){ setup_step;KEYS='w space'; }
setup_anchorf(){ setup_anchorl; rc 'fill 97 -61 485 104 -61 493 stone' >/dev/null; rc "effect give $NAME resistance 60 255 true" >/dev/null; rc "item replace entity $NAME hotbar.0 with glowstone 16" >/dev/null; PRE='X key 1'; }
measure_anchorf(){ rc 'execute if block 102 -60 488 respawn_anchor[charges=3]' | grep -o 'passed\|failed'; }
setup_buildhand(){ X0=100 Y0=-60 Z0=80; PITCH=70; SECS=6; CLICK=3; rc 'fill 96 -60 76 104 -54 88 air' >/dev/null; rc 'fill 96 -61 76 104 -61 88 stone' >/dev/null; rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null; PRE='X key 1'; }
measure_buildhand(){ rc "data get entity $NAME Inventory"; }
turnto(){
 for j in $(seq 1 8);do
  r=$(rc "data get entity $NAME Rotation");y=$(echo "$r" | sed -n 's/.*\[\([-0-9.]*\)f,.*/\1/p');[ -n "$y" ] || return 1
  dx=$(awk -v y="$y" 'BEGIN{d=-149-y;while(d>180)d-=360;while(d< -180)d+=360;print int(d/0.15)}')
  [ "$dx" = 0 ] && break
  X mousemove_relative -- "$dx" 0;sleep 0.4
 done
 rc "data get entity $NAME Rotation" >> /tmp/flick40-rotation.log
}
setup_flickreal(){ setup_anchorf; SECS=3; CLICK=0; PRE='X key 1; for i in 1 2 3; do X mousemove_relative -- 300 0; sleep 0.7; turnto; X click 3; sleep 0.7; done'; }
measure_flickreal(){ measure_anchorf; }

turn(){
 local aim=$1
 for j in $(seq 1 10);do
  r=$(rc "data get entity $NAME Rotation");y=$(echo "$r" | sed -n 's/.*\[\([-0-9.]*\)f,.*/\1/p');[ -n "$y" ] || return 1
  dx=$(awk -v y="$y" -v a="$aim" 'BEGIN{d=a-y;while(d>180)d-=360;while(d< -180)d+=360;print int(d/0.15)}')
  [ "$dx" = 0 ] && break
  X mousemove_relative -- "$dx" 0;sleep 0.2
 done
}
setup_three(){ X0=100 Y0=-60 Z0=20;SECS=20;PITCH=10;rc 'fill 95 -61 15 105 -61 28 stone' >/dev/null;rc 'kill @e[type=zombie]' >/dev/null;for v in '98.5 22.5 left' '100.5 23.0 centre' '102.5 22.5 right';do set -- $v;rc "summon zombie $1 -60 $2 {Tags:[\"$3\"],NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null;done;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_three(){ for tag in left centre right;do printf '%s=' "$tag";rc "data get entity @e[type=zombie,tag=$tag,limit=1] Health";done; }
setup_handthree(){ setup_three;SECS=4;PRE='X key 1; for i in 1 2 3;do for a in 45 0 -45;do turn "$a";sleep 0.8;rc "data get entity $NAME Rotation" >> /tmp/motion54.log;rc "data get entity $NAME Pos" >> /tmp/motion54.log;for tag in left centre right;do rc "data get entity @e[tag=$tag,limit=1]" >> /tmp/motion54-entities.log;done;X mousedown 1;sleep 0.15;X mouseup 1;sleep 0.65;done;done'; }
measure_handthree(){ measure_three; }

for c in still walk fall water wall ladder stepfix web soulsand snow boat meleeclick meleetank minefix foodfix anchorf buildhand flickreal handthree; do run_case "$c" none;done
