#!/bin/bash
cd /home/luke/meshac-work/meshac/rig/h
. ./run.sh
RES=/home/luke/meshac-work/results/final-sweep/sweep10.csv
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > "$RES"
NAME=mcF10x4313
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
setup_boost2(){ X0=100 Y0=250 Z0=460; KEYS="space w"; SECS=16; rc "forceload add 100 460" >/dev/null; rc "effect clear $NAME" >/dev/null; rc "effect give $NAME slow_falling 6 0 true" >/dev/null; rc "item replace entity $NAME armor.chest with elytra" >/dev/null; rc "item replace entity $NAME hotbar.0 with firework_rocket 64" >/dev/null; PRE='X key 1; (sleep 4; for i in $(seq 1 10); do X click 3; sleep 1.2; done) &'; }
measure_boost2(){ rc "clear $NAME firework_rocket 0" | grep -o "[0-9]\+" | head -1; }
setup_dive(){ X0=100 Y0=250 Z0=460; KEYS="space"; SECS=14; PITCH=45; rc "forceload add 100 460" >/dev/null; rc "effect clear $NAME" >/dev/null; rc "effect give $NAME slow_falling 6 0 true" >/dev/null; rc "item replace entity $NAME armor.chest with elytra" >/dev/null; }
measure_dive(){ measure_boost2; }
setup_dive2(){ setup_dive; PITCH=70; }
measure_dive2(){ measure_boost2; }
go none dive
go none dive2
go none boost2
go elytra-boost boost2
go none boost2
echo DONE >> $RES
