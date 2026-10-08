#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/tmp/meteorSW15.csv
trap "docker stop meshac-wurst meshac-target >/dev/null 2>&1" EXIT
: > "$RES"
NAME=mcSW24
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
start_target(){
 docker rm -f meshac-target >/dev/null 2>&1
 docker run -d --name meshac-target --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v /home/luke/meshac-work/clients/gh2:/gh -v /home/luke/meshac-work/clients/meteor2:/w -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 800x600x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username vic1 --quickPlayMultiplayer meshac-rig:25565\" > /w/client-target.log 2>&1" >/dev/null
 for i in $(seq 1 45); do sleep 4; rc "list" | grep -q vic1 && return 0; done; return 1
}
setup_anchor3(){ X0=100 Y0=-60 Z0=490; KEYS=""; SECS=14; rc "forceload add 100 490" >/dev/null; rc "fill 80 -60 470 120 -50 510 air" >/dev/null; rc "fill 80 -62 470 120 -61 510 stone" >/dev/null; rc "item replace entity $NAME hotbar.0 with respawn_anchor 16" >/dev/null; rc "item replace entity $NAME hotbar.1 with glowstone 32" >/dev/null; rc "effect give $NAME resistance 120 4 true" >/dev/null; rc "gamemode survival vic1" >/dev/null; rc "effect give vic1 resistance 120 4 true" >/dev/null; rc "effect give vic1 regeneration 120 4 true" >/dev/null; rc "tp vic1 103.5 -60 492.5" >/dev/null; PRE='X key 1'; }
measure_anchor3(){ rc "data get entity vic1 Health" | grep -o "[0-9.]*f\?$" | head -1; }
{ rc list | grep -q vic1 || start_target; } || { echo "target failed" >> $RES; exit 1; }
go none anchor3
go anchor-aura anchor3
echo DONE >> $RES
