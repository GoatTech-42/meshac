#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h/
. ./run.sh
RES=/tmp/open72.csv
: > "$RES"
NAME=mcOpen72
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
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


setup_swaphand(){ setup_meleetank; SECS=6;PITCH=10;JITTER=0;rc 'kill @e[type=zombie]' >/dev/null;rc 'summon zombie 100.5 -60 23 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:"minecraft:max_health",base:1000}]}' >/dev/null;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;rc "item replace entity $NAME hotbar.1 with stone_axe" >/dev/null;PRE='X key 1; sleep 0.8; X key 2; sleep 0.8; X click 1; sleep 0.6; X key 1'; }
measure_swaphand(){ measure_meleetank; }
setup_windhand(){ X0=100 Y0=-60 Z0=900; PITCH=90;SECS=6;rc 'fill 96 -60 896 108 -40 912 air' >/dev/null;rc 'fill 96 -61 896 108 -61 912 stone' >/dev/null;rc "item replace entity $NAME hotbar.0 with wind_charge 16" >/dev/null;PRE='X key 1; X keydown space; sleep 0.2; X mousedown 3; sleep 0.2; X mouseup 3; X keyup space; (for i in $(seq 1 15);do pos >> /tmp/wind61-pos.log;sleep 0.2;done) &'; }
measure_windhand(){ rc "data get entity $NAME Inventory"; }
setup_levsafe(){ X0=100 Y0=-60 Z0=0;SECS=10;rc "effect give $NAME resistance 60 255 true" >/dev/null;PRE='rc "effect give $NAME levitation 4 4 true" >/dev/null; (for i in $(seq 1 15);do pos >> /tmp/lev61-pos.log;sleep 0.3;done) &'; }
setup_zkb(){ setup_zombiehit;rc 'fill 96 -61 696 108 -61 710 stone' >/dev/null;rc 'fill 96 -60 696 108 -54 710 air' >/dev/null; }
setup_glidehand(){ X0=100 Y0=10 Z0=460;SECS=8;rc "item replace entity $NAME armor.chest with elytra" >/dev/null;rc "effect give $NAME resistance 60 255 true" >/dev/null;PRE='X keydown space; sleep 0.25; X keyup space; sleep 0.6; X keydown space; sleep 0.25; X keyup space; X keydown w; (for i in $(seq 1 16);do rc "data get entity $NAME FallFlying" >> /tmp/glide45.log;pos >> /tmp/glide45.log;sleep 0.25;done) &';KEYS='w'; }
measure_glidehand(){ rc "data get entity $NAME FallFlying"; }

setup_glidefix(){ X0=100 Y0=-60 Z0=460; SECS=8;KEYS='w'; rc "item replace entity $NAME armor.chest with elytra" >/dev/null;rc "effect give $NAME resistance 60 255 true" >/dev/null;PRE='rc "tp $NAME 100.5 90 460.5 0 0" >/dev/null; sleep 0.3; X keydown space; sleep 0.3; X keyup space; (for i in $(seq 1 20);do rc "data get entity $NAME FallFlying" >> /tmp/glide61.log;pos >> /tmp/glide61.log;echo >> /tmp/glide61.log;sleep 0.2;done) &'; }
measure_glidefix(){ rc "data get entity $NAME FallFlying"; }
setup_refillslow(){ X0=100 Y0=-60 Z0=420;SECS=3;rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null;rc "item replace entity $NAME hotbar.0 with totem_of_undying" >/dev/null;PRE='X keydown 1; sleep 0.25; X keyup 1; rc "damage $NAME 40 generic" >> /tmp/refill46.log; sleep 1.2; X keydown f; sleep 0.8; X keyup f; sleep 0.5; rc "data get entity $NAME Inventory" >> /tmp/refill46.log; rc "data get entity $NAME equipment" >> /tmp/refill46.log'; }
measure_refillslow(){ rc "data get entity $NAME Inventory"; }

setup_chesthand(){ X0=100 Y0=-60 Z0=160;PITCH=28;SECS=3;rc 'fill 97 -61 157 103 -61 164 stone' >/dev/null;rc 'setblock 100 -60 162 chest{Items:[{Slot:0,id:"minecraft:diamond",count:8}]}' >/dev/null;PRE='X mousedown 3;sleep 0.2;X mouseup 3;sleep 1;X mousemove 495 244;sleep 0.4;X keydown shift;X click 1;X keyup shift;sleep 1;X key Escape'; }
measure_chesthand(){ rc "data get entity $NAME Inventory";rc 'data get block 100 -60 162 Items'; }
setup_fastplacehand(){ setup_buildhand;SECS=8;CLICK=0;PRE='X key 1;X mousedown 3;(sleep 8;X mouseup 3) &'; }
measure_fastplacehand(){ measure_buildhand; }
setup_totemslow(){ X0=100 Y0=-60 Z0=420;SECS=3;rc 'fill 97 -61 417 103 -61 423 stone' >/dev/null;rc "item replace entity $NAME weapon.offhand with totem_of_undying" >/dev/null;rc "item replace entity $NAME hotbar.0 with cobblestone" >/dev/null;rc "item replace entity $NAME hotbar.1 with totem_of_undying" >/dev/null;PRE='X key 1;sleep 1;rc "damage $NAME 40 generic" >> /tmp/totem61.log;sleep 0.4;X key 2;sleep 0.1;X keydown f;sleep 0.2;X keyup f;sleep 0.3;rc "data get entity $NAME Inventory" >> /tmp/totem61.log'; }
measure_totemslow(){ rc "data get entity $NAME Inventory"; }
setup_totemfast(){ setup_totemslow;PRE='X key 1;sleep 1;rc "damage $NAME 40 generic" >> /tmp/totem61.log;sleep 0.12;X key 2;X keydown f;sleep 0.2;X keyup f;sleep 0.3;rc "data get entity $NAME Inventory" >> /tmp/totem61.log'; }
measure_totemfast(){ measure_totemslow; }

setup_glideremove(){ setup_glidefix; SECS=5;PRE+=' X keydown w; sleep 5; rc "item replace entity $NAME armor.chest with air" >/dev/null; for i in $(seq 1 30);do rc "data get entity $NAME FallFlying" >> /tmp/glide62-motion.log;pos >> /tmp/glide62-motion.log;echo >> /tmp/glide62-motion.log;sleep 0.1;done'; }
measure_glideremove(){ measure_glidefix; }
setup_glideland(){ setup_glidefix; SECS=22;PRE='rc "tp $NAME 100.5 -30 460.5 0 20" >/dev/null; sleep 0.3; X keydown space; sleep 0.3; X keyup space; (for i in $(seq 1 70);do rc "data get entity $NAME FallFlying" >> /tmp/glideland62-motion.log;pos >> /tmp/glideland62-motion.log;echo >> /tmp/glideland62-motion.log;sleep 0.3;done) &'; }
measure_glideland(){ measure_glidefix; }

setup_walklong(){ setup_walk;SECS=25; }
measure_walklong(){ echo 0; }
setup_flylong(){ setup_flyw;SECS=25; }
measure_flylong(){ echo 0; }
setup_walllong(){ setup_noclipw;SECS=25; }
measure_walllong(){ echo 0; }

setup_farclick(){ setup_meleefar;PITCH=10;CLICK=1;JITTER=0;SECS=20;rc "item replace entity $NAME hotbar.0 with diamond_sword" >/dev/null;PRE='X key 1'; }
measure_farclick(){ measure_meleefar; }
setup_bunker(){ X0=100 Y0=-60 Z0=1000;SECS=12;rc 'fill 93 -60 993 107 -50 1007 air' >/dev/null;rc 'fill 93 -61 993 107 -61 1007 stone' >/dev/null;rc "item replace entity $NAME hotbar.0 with cobblestone 64" >/dev/null;PRE='X key 1'; }
measure_bunker(){ rc "data get entity $NAME Inventory"; }
setup_tired(){ setup_walklong;KEYS='w';SECS=12; }
measure_tired(){ rc "data get entity $NAME Rotation"; }
setup_vein(){ setup_minefix;rc 'fill 96 -61 50 104 -55 57 coal_ore' >/dev/null;SECS=16;PRE='X key 1;X click 1'; }
measure_vein(){ rc "data get entity $NAME Inventory"; }
for c in farclick bunker tired minefix;do run_case "$c" none;done
for pair in 'farclick ClickAura' 'farclick TP-Aura' 'bunker InstantBunker' 'tired Tired' 'vein VeinMiner';do
 set -- $pair
 docker stop meshac-wurst >/dev/null
 rc "mesh unban $NAME" >/dev/null
 start_client || exit 1
 run_case "$1" "$2"
 docker exec -e DISPLAY=:99 meshac-wurst import -window root /tmp/end.png 2>/dev/null || true
 docker cp meshac-wurst:/tmp/end.png /home/luke/meshac-work/shots/open72-$2-end.png 2>/dev/null || true
 docker logs -t meshac-rig > /tmp/open72-server.log 2>&1
done
