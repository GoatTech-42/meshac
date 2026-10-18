#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/meteorKB.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcKB1
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
setup_ghwall(){ X0=100 Y0=-60 Z0=500; YAW=0; PITCH=20; SECS=8; rc "forceload add 100 500" >/dev/null; rc "fill 96 -60 496 106 -50 510 air" >/dev/null; rc "fill 96 -61 496 106 -61 510 stone" >/dev/null; rc "setblock 100 -60 503 chest" >/dev/null; rc "fill 100 -60 502 100 -58 502 stone" >/dev/null; PRE='X mousedown 3; (sleep 8; X mouseup 3) &'; }
setup_ghopen(){ setup_ghwall; rc "setblock 100 -60 502 air" >/dev/null; rc "setblock 100 -59 502 air" >/dev/null; rc "setblock 100 -58 502 air" >/dev/null; }
measure_ghwall(){ pos | cut -d, -f2; }
kbbase(){ X0=100 Y0=-60 Z0=$1; KEYS=""; SECS=10; rc "forceload add 100 $1" >/dev/null; rc "fill 96 -60 $(($1-4)) 108 -50 $(($1+12)) air" >/dev/null; rc "fill 96 -61 $(($1-4)) 108 -61 $(($1+12)) stone" >/dev/null; rc "fill 103 -60 $(($1-4)) 103 -55 $(($1+12)) stone" >/dev/null; rc "effect give $NAME resistance 60 4 true" >/dev/null; }
setup_kbtnt(){ kbbase 900; PRE='for i in 1 2 3 4; do rc "summon tnt 100.5 -60 900.5 {fuse:0}" >/dev/null; sleep 2; done'; }
setup_kbcry(){ kbbase 940; PRE='for i in 1 2 3 4; do rc "summon end_crystal 100.5 -60 940.5 {ShowBottom:0b}" >/dev/null; sleep 0.5; rc "damage @e[type=end_crystal,limit=1] 5" >/dev/null; sleep 2; done'; }
measure_kbtnt(){ pos | cut -d, -f1; }
measure_kbcry(){ pos | cut -d, -f1; }
go none kbtnt
go none kbcry
echo DONE >> $RES
