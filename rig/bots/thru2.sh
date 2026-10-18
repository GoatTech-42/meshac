# usage: thru2.sh <id> <version> <jar> <behind|front> <player>   runs on bot rig meshac-bot-<id>
id=$1; ver=$2; jar=$3; T=$4; U=$5; RIG=meshac-bot-$id
sh ~/meshac-work/meshac/rig/botrig.sh up $id $ver $jar
rcx() { ip=$(docker inspect $RIG --format '{{(index .NetworkSettings.Networks "meshac-net").IPAddress}}'); python3 /tmp/rcon.py $ip 25575 meshacrig "$1"; }
cd ~/meshac-work/meshac/rig/bots
(docker run --rm --network meshac-net --memory 512m --cpus 0.5 -v "$PWD":/w -w /w -e U=$U -e T=$T -e HOST=$RIG -e MCV=$ver node:22-slim timeout 80 node thru.mjs > /tmp/thru2-$id.out 2>&1 &)
sleep 8
rcx "forceload add 92 952 108 970" >/dev/null
rcx "fill 96 -61 956 104 -55 966 air" >/dev/null; rcx "fill 96 -61 956 104 -61 966 stone" >/dev/null
rcx "setblock 100 -59 961 stone" >/dev/null; rcx "setblock 100 -59 962 stone" >/dev/null; rcx "give $U diamond_pickaxe" >/dev/null
sleep 1; rcx "tp $U 100.5 -60 960.5 0 0" >/dev/null; rcx "gamemode survival $U" >/dev/null
sleep 40
cat /tmp/thru2-$id.out
echo "RESULT $id $T $(basename $jar): signals=$(docker logs $RIG 2>&1 | grep -c "SIGNAL $U")"; docker logs $RIG 2>&1 | grep "SIGNAL $U" | sed "s/.*SIGNAL//" | sort | uniq -c
sh ~/meshac-work/meshac/rig/botrig.sh down $id
