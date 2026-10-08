# usage: signbot.sh <id> <version> <jar> <gap_ms> <player>
id=$1; ver=$2; jar=$3; G=$4; U=$5; RIG=meshac-bot-$id
sh ~/meshac-work/meshac/rig/botrig.sh up $id $ver $jar
rcx() { ip=$(docker inspect $RIG --format '{{(index .NetworkSettings.Networks "meshac-net").IPAddress}}'); python3 /tmp/rcon.py $ip 25575 meshacrig "$1"; }
cd ~/meshac-work/meshac/rig/bots; cp /tmp/signbot.mjs .
(docker run --rm --network meshac-net --memory 512m --cpus 0.5 -v "$PWD":/w -w /w -e U=$U -e GAP=$G -e N=${N:-3} -e HOST=$RIG -e MCV=$ver node:22-slim timeout 75 node signbot.mjs > /tmp/signbot-$id.out 2>&1 &)
sleep 8
rcx "forceload add 92 972 108 990" >/dev/null
rcx "fill 92 -61 972 108 -55 990 air" >/dev/null; rcx "fill 92 -61 972 108 -61 990 stone" >/dev/null
rcx "give $U oak_sign 16" >/dev/null
sleep 1; rcx "tp $U 100.5 -60 980.5 0 0" >/dev/null; rcx "gamemode survival $U" >/dev/null
sleep 38; rcx "data get block 97 -60 982" | cut -c1-400
cat /tmp/signbot-$id.out
echo "RESULT $id gap=$G: signals=$(docker logs $RIG 2>&1 | grep -c "SIGNAL $U")"; docker logs $RIG 2>&1 | grep "SIGNAL $U" | sed "s/.*SIGNAL//" | sort | uniq -c
docker logs $RIG 2>&1 | grep -iE "sign text" | cut -c1-170 | tail -8
sh ~/meshac-work/meshac/rig/botrig.sh down $id
