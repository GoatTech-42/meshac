# usage: thru.sh <jar> <behind|front> <name>
JAR=$1; T=$2; U=$3
cp $JAR ~/meshac-work/rig/smoke-26.1/mods/meshac.jar
docker rm -f meshac-rig >/dev/null 2>&1
MESHAC_TRACE=1 sh ~/meshac-work/meshac/rig/run-server.sh ~/meshac-work/rig/smoke-26.1 >/dev/null
sleep 40
. ~/meshac-work/meshac/rig/h/lib.sh
NAME=$U
cd ~/meshac-work/meshac/rig/bots
(docker run --rm --network meshac-net --memory 512m --cpus 1 -v "$PWD":/w -w /w -e U=$U -e T=$T node:22-slim timeout 70 node thru.mjs > /tmp/thru-$U.out 2>&1 &) 
sleep 8
rc "forceload add 92 952 108 970" >/dev/null
rc "fill 96 -61 956 104 -55 966 air" >/dev/null; rc "fill 96 -61 956 104 -61 966 stone" >/dev/null
rc "setblock 100 -59 961 stone" >/dev/null; rc "setblock 100 -59 962 stone" >/dev/null; rc "give $U diamond_pickaxe" >/dev/null
sleep 1
rc "tp $U 100.5 -60 960.5 0 0" >/dev/null; rc "gamemode survival $U" >/dev/null; rc "clear $U" >/dev/null
sleep 30
cat /tmp/thru-$U.out
echo "signals: $(docker logs meshac-rig 2>&1 | grep -c "SIGNAL $U")"; docker logs meshac-rig 2>&1 | grep "SIGNAL $U" | sed "s/.*SIGNAL//" | sort | uniq -c
docker rm -f meshac-rig >/dev/null 2>&1
