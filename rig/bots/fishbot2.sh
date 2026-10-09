# usage: fishbot2.sh <id> <version> <jar> <delay_ms> <player> <N>
id=$1; ver=$2; jar=$3; D=$4; U=$5; N=${6:-8}; RIG=meshac-bot-$id
sh ~/meshac-work/meshac/rig/botrig.sh up $id $ver $jar
rcx() { ip=$(docker inspect $RIG --format "{{(index .NetworkSettings.Networks \"meshac-net\").IPAddress}}"); python3 /tmp/rcon.py $ip 25575 meshacrig "$1"; }
rcx "fill 94 -64 80 106 -58 101 stone" >/dev/null; rcx "fill 96 -62 82 104 -60 88 water" >/dev/null; rcx "fill 94 -59 80 106 -55 101 air" >/dev/null; rcx "fill 94 -60 89 106 -59 89 air" >/dev/null; rcx "fill 96 -61 90 104 -61 92 stone" >/dev/null; rcx "fill 94 -60 90 106 -59 92 air" >/dev/null
rcx "forceload add 90 78 110 100" >/dev/null
cd ~/meshac-work/meshac/rig/bots; rm -f bite.flag; BH=$RIG; [ -n "$LAG" ] && { docker rm -f meshac-lagproxy >/dev/null 2>&1; docker run -d --name meshac-lagproxy --network meshac-net -e STALL_MS=800 -e EVERY_S=4 -v /tmp/lagproxy-f3.py:/p.py python:3-alpine python /p.py >/dev/null; BH=meshac-lagproxy; }
(docker logs -f --since 0s $RIG 2>&1 | grep --line-buffered "$U FISHBITE" | while read l; do echo "$l" >> /tmp/bites-$id.txt; touch bite.flag; done &)
(docker run --rm --network meshac-net --memory 512m --cpus 0.5 -v "$PWD":/w -w /w -e U=$U -e DELAY=$D -e N=$N -e HOST=$BH -e MCV=$ver node:22-slim node fishbot2.mjs > /tmp/fishbot-$id.out 2>&1 &)
sleep 14; rcx "gamemode survival $U" >/dev/null; rcx "tp $U 100.5 -60 90.5 180 0" >/dev/null; rcx "clear $U" >/dev/null; rcx "give $U fishing_rod[enchantments={\"minecraft:lure\":3}]" >/dev/null
echo "$id ready $(date +%T)"
