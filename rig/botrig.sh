#!/bin/sh
# Bot rig: a throwaway server on its own world copy, no GL client. For packet-level tests driven by mineflayer bots.
# usage: botrig.sh up <id> <version e.g. 26.1> <jar>     (container meshac-bot-<id>, world copied from rig/smoke-<version>)
#        botrig.sh down <id>
R=$HOME/meshac-work/rig
case "$1" in
up)
  id=$2; v=$3; jar=$4; d=$R/bot-$id
  docker rm -f meshac-bot-$id >/dev/null 2>&1
  docker run --rm --user root -v $R:/r eclipse-temurin:25-jre sh -c "rm -rf /r/bot-$id; cp -a /r/smoke-$v /r/bot-$id; rm -rf /r/bot-$id/logs; chown -R $(id -u):$(id -g) /r/bot-$id"; cp "$jar" $d/mods/meshac.jar
  sed -i "s/^rcon.password=.*/rcon.password=meshacrig/; s/^enable-rcon=.*/enable-rcon=true/; s/^white-list=.*/white-list=false/" $d/server.properties
  docker network inspect meshac-net >/dev/null 2>&1 || docker network create --internal meshac-net
  docker run -d -e MESHAC_TRACE=1 --name meshac-bot-$id --network meshac-net --memory 1g --cpus 1 -v $d:/srv -w /srv eclipse-temurin:25-jre java -Xmx700M -jar fabric-server.jar nogui >/dev/null
  for i in $(seq 1 40); do sleep 2; docker logs meshac-bot-$id 2>&1 | grep -q "Done (" && break; done
  docker logs meshac-bot-$id 2>&1 | grep -E "Done \(" | tail -1 ;;
down) docker rm -f meshac-bot-$2 >/dev/null 2>&1; docker run --rm --user root -v $R:/r eclipse-temurin:25-jre rm -rf /r/bot-$2; echo down $2 ;;
esac
