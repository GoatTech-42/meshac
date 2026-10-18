#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/home/luke/meshac-work/results/final-sweep/sweep13.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcF13x5214
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
setup_kbo(){ X0=100 Y0=-60 Z0=1100; KEYS=""; SECS=12; rc "forceload add 100 1100" >/dev/null; rc "fill 80 -60 1080 120 -50 1130 air" >/dev/null; rc "fill 80 -62 1080 120 -61 1130 obsidian" >/dev/null; rc "effect give $NAME resistance 120 4 true" >/dev/null; PRE='for i in 1 2 3 4 5; do rc "summon tnt 100.5 -60 1099.5 {fuse:0}" >/dev/null; sleep 2; done'; }
measure_kbo(){ pos | cut -d, -f3; }
go none kbo
go velocity kbo "knockback-horizontal 0" "knockback-vertical 0" "explosions-horizontal 0" "explosions-vertical 0"
echo DONE >> $RES
