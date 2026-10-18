#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/meteorSW7.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcSW7
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
setup_wj3(){ X0=100 Y0=-60 Z0=900; KEYS="space"; SECS=12; CLICK=3; PITCH=90; rc "forceload add 100 900" >/dev/null; rc "fill 96 -60 896 108 -30 912 air" >/dev/null; rc "fill 96 -61 896 108 -61 912 stone" >/dev/null; rc "give $NAME wind_charge 32" >/dev/null; rc "effect give $NAME resistance 60 4 true" >/dev/null; rm -f /tmp/wj.txt; PRE='for i in 1 2 3 4 5 6 7 8 9 10; do pos | cut -d, -f2 >> /tmp/wj.txt; sleep 1; done &'; }
measure_wj3(){ sort -n /tmp/wj.txt 2>/dev/null | tail -1; }
go none wj3
echo DONE >> $RES
