# usage: fishbot.sh <id> <version> <jar> <delay_ms> <player>
id=$1; ver=$2; jar=$3; D=$4; U=$5; RIG=meshac-bot-$id
sh ~/meshac-work/meshac/rig/botrig.sh up $id $ver $jar
rcx() { ip=$(docker inspect $RIG --format "{{(index .NetworkSettings.Networks \"meshac-net\").IPAddress}}"); python3 /tmp/rcon.py $ip 25575 meshacrig "$1"; }
rcx "fill 94 -64 80 106 -58 89 stone" >/dev/null; rcx "fill 96 -62 82 104 -60 88 water" >/dev/null; rcx "fill 94 -59 80 106 -55 93 air" >/dev/null; rcx "fill 94 -60 89 106 -59 89 air" >/dev/null; rcx "fill 96 -61 90 104 -61 92 stone" >/dev/null; rcx "fill 94 -64 93 106 -58 101 stone" >/dev/null; rcx "fill 94 -59 93 106 -55 101 air" >/dev/null; rcx "fill 94 -60 93 106 -59 93 air" >/dev/null; rcx "fill 96 -62 94 104 -60 100 water" >/dev/null
rcx "forceload add 90 78 110 96" >/dev/null
cd ~/meshac-work/meshac/rig/bots
(docker run --rm --network meshac-net --memory 512m --cpus 0.5 -v "$PWD":/w -w /w -e U=$U -e DELAY=$D -e HOST=$RIG -e MCV=$ver node:22-slim node fishbot.mjs > /tmp/fishbot-$id.out 2>&1 &)
sleep 14; rcx "gamemode survival $U" >/dev/null; rcx "tp $U 100.5 -60 90.5 180 20" >/dev/null; rcx "clear $U" >/dev/null; rcx "give $U fishing_rod[enchantments={\"minecraft:lure\":3}]" >/dev/null
sleep ${W:-100}
tail -6 /tmp/fishbot-$id.out
echo "RESULT $id delay=$D: signals=$(docker logs $RIG 2>&1 | grep -c "SIGNAL $U")"; docker logs $RIG 2>&1 | grep "SIGNAL $U" | sed "s/.*SIGNAL//" | head -3
sh ~/meshac-work/meshac/rig/botrig.sh down $id
