#!/bin/sh
# Boots each dist jar on a real server of its Minecraft version and checks meshac loads. usage: rig/smoke.sh [version...]
cd "$(dirname "$0")/.."; ROOT=~/meshac-work/rig
LOADER=0.19.5; INST=1.0.1
for v in ${@:-1.21.11 26.1 26.2 26.3}; do
  . ./versions/$v.properties 2>/dev/null || eval "$(sed 's/^\([a-z_]*\)=\(.*\)$/\1="\2"/' versions/$v.properties)"
  d=$ROOT/smoke-$v; mkdir -p $d/mods; JRE=25; [ "$v" = 1.21.11 ] && JRE=21
  [ -f $d/fabric-server.jar ] || curl -sfL -o $d/fabric-server.jar "https://meta.fabricmc.net/v2/versions/loader/$minecraft_version/$LOADER/$INST/server/jar"
  [ -f $d/mods/fabric-api.jar ] || curl -sfL -o $d/mods/fabric-api.jar "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$fabric_api_version/fabric-api-$fabric_api_version.jar"
  cp dist/meshac-$v.jar $d/mods/meshac.jar; echo eula=true > $d/eula.txt
  printf 'online-mode=false\nlevel-name=flat\nlevel-type=minecraft\\:flat\nspawn-protection=0\nserver-port=25565\n' > $d/server.properties
  docker rm -f meshac-smoke >/dev/null 2>&1
  docker run -d --name meshac-smoke --memory 2g --cpus 2 -v $d:/srv -w /srv eclipse-temurin:$JRE-jre java -Xmx1400M -jar fabric-server.jar nogui >/dev/null
  r=FAIL; for i in $(seq 1 45); do sleep 4
    docker logs meshac-smoke 2>&1 | grep -q 'Done (' && { r=BOOTED; break; }
    docker ps -q -f name=meshac-smoke | grep -q . || break
  done
  log=$(docker logs meshac-smoke 2>&1)
  m=$(echo "$log" | grep -c "meshac loaded"); err=$(echo "$log" | grep -ci 'meshac.*\(error\|exception\)\|mixin.*fail')
  echo "$v mc=$minecraft_version $r meshac_lines=$m errors=$err"
  echo "$log" | grep -i meshac | head -3
  docker rm -f meshac-smoke >/dev/null
done
