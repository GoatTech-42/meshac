#!/bin/bash
# Resolve the workspace without baking a user name into the script.
if [ "${WORKSPACE+x}" != x ]; then
  if [ -n "${SUDO_USER:-}" ] || [ -z "${HOME:-}" ]; then
    printf '%s\n' 'set WORKSPACE explicitly when HOME is unset or running through sudo' >&2
    exit 1
  fi
  WORKSPACE="$HOME"
fi
case "$WORKSPACE" in
  ''|*[\"\'\$\`]*|*'
'*)
    printf '%s\n' 'WORKSPACE is empty or contains unsafe shell characters' >&2
    exit 1 ;;
  /*) ;;
  *) printf '%s\n' 'WORKSPACE must be an absolute path' >&2; exit 1 ;;
esac
if [ ! -d "$WORKSPACE/meshac-work/meshac/rig/h" ] ||
   [ ! -r "$WORKSPACE/meshac-work/meshac/rig/h/run.sh" ] ||
   [ ! -r "$WORKSPACE/meshac-work/meshac/rig/h/lib.sh" ]; then
  printf '%s\n' 'WORKSPACE must contain the prepared rig/h directory and readable run.sh/lib.sh' >&2
  exit 1
fi
export WORKSPACE
set -e
cd -- "${WORKSPACE}/meshac-work/meshac/rig/h" || exit 1
. ./lib.sh
NAME=mcTimer66
trap 'docker stop meshac-wurst >/dev/null 2>&1' EXIT
: > /tmp/timer66-results.log
for run in 1 2 3 4 5 6;do
 docker rm -f meshac-wurst >/dev/null 2>&1 || true
 d="${WORKSPACE}/meshac-work/clients/meteor/run/meteor-client"
 if [ $((run % 2)) = 1 ];then
  [ ! -f "$d/modules.nbt" ] || cp "$d/modules.nbt" /tmp/timer66-before-$run.nbt
  rm -f "$d/modules.nbt"
 fi
 stamp=$(date -u +%Y-%m-%dT%H:%M:%S)
 docker run -d --name meshac-wurst --network meshac-net --user 1000:1000 --memory 3g --cpus 2 -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 -v "${WORKSPACE}/meshac-work/clients/gh:/gh" -v "${WORKSPACE}/meshac-work/clients/meteor:/w" -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $NAME --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
 valid=0
 for i in $(seq 1 45);do sleep 4;h=$(hp);if connected && [ -n "$h" ] && [ "$h" != 0.0 ];then valid=1;break;fi;done
 [ "$valid" = 1 ] || { echo "FAIL join $run" >> /tmp/timer66-results.log;exit 1; }
 grep '\[rig66\] GAME_JOIN active=\[\]' "${WORKSPACE}/meshac-work/clients/meteor/client.log" || { echo "FAIL module evidence $run" >> /tmp/timer66-results.log;exit 1; }
 say '.t all off'
 sleep 8
 p0=$(pos);X keydown w;sleep 15;X keyup w;sleep 5;p1=$(pos)
 docker exec -e DISPLAY=:99 meshac-wurst import -window root /tmp/end.png
 docker cp meshac-wurst:/tmp/end.png "${WORKSPACE}/meshac-work/shots/timer66-$run.png"
 cp "${WORKSPACE}/meshac-work/clients/meteor/client.log" /tmp/timer66-client-$run.log
 docker logs -t --since "$stamp" meshac-rig > /tmp/timer66-server-$run.log 2>&1
 n=$(grep -c "SIGNAL $NAME" /tmp/timer66-server-$run.log || true)
 echo "run=$run reset=$((run % 2)) modules=[] hp=$(hp) pos=$p0>$p1 signals=$n" >> /tmp/timer66-results.log
 docker stop -t 15 meshac-wurst >/dev/null
 sleep 3
done
