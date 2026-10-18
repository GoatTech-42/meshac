#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/meteorSW4.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcSW4
start_client(){
 docker rm -f meshac-wurst >/dev/null 2>&1
 d=/home/luke/meshac-work/clients/meteor/run/meteor-client
 cp "$d/modules.nbt" /tmp/meteor81-modules-before.nbt
 rm "$d/modules.nbt"
 docker run -d --name meshac-wurst --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v /home/luke/meshac-work/clients/gh:/gh -v /home/luke/meshac-work/clients/meteor:/w -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $NAME --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
 for i in $(seq 1 45);do sleep 4;connected && { sleep 8;return 0;};done;return 1
}

start_client || exit 1
sh ./world.sh >/dev/null
go(){ h=$1; sc=$2; shift 2; connected || start_client; say ".t all off"; if [ "$h" != none ]; then for st in "$@"; do say ".settings $h $st"; done; fi; run_case $sc $h; }
setup_ka0(){ setup_meleetank; CLICK=0; SECS=10; }
measure_ka0(){ measure_meleetank; }
setup_cra(){ X0=100 Y0=-60 Z0=1000; KEYS=""; SECS=12; rc "forceload add 100 1000" >/dev/null; rc "fill 94 -61 994 108 -50 1012 air" >/dev/null; rc "fill 94 -62 994 108 -62 1012 obsidian" >/dev/null; rc "fill 94 -61 994 108 -61 1012 obsidian" >/dev/null; rc "kill @e[type=zombie]" >/dev/null; rc "summon zombie 104.5 -60 1004.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1000f,attributes:[{id:\"minecraft:max_health\",base:1000}]}" >/dev/null; rc "give $NAME end_crystal 64" >/dev/null; rc "effect give $NAME resistance 120 4 true" >/dev/null; }
measure_cra(){ rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
setup_bowt(){ setup_ranged; CLICK=0; SECS=10; }
measure_bowt(){ rc "data get entity @e[type=zombie,limit=1] Health" | grep -o "[0-9.]*f\?$" | head -1; }
go crystal-aura cra "entities zombie" "place-range 5" "break-range 5"
go bow-aimbot bowt
go none cra
echo DONE >> $RES
