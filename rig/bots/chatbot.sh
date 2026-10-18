# usage: chatbot.sh <id> <version> <jar> <gap_ms> <mode same|diff> <player>
id=$1; ver=$2; jar=$3; G=$4; M=$5; U=$6; RIG=meshac-bot-$id
sh ~/meshac-work/meshac/rig/botrig.sh up $id $ver $jar
rcx() { ip=$(docker inspect $RIG --format '{{(index .NetworkSettings.Networks "meshac-net").IPAddress}}'); python3 /tmp/rcon.py $ip 25575 meshacrig "$1"; }
cd ~/meshac-work/meshac/rig/bots; cp /tmp/chatbot.mjs .
(docker run --rm --network meshac-net --memory 512m --cpus 0.5 -v "$PWD":/w -w /w -e U=$U -e GAP=$G -e MODE=$M -e HOST=$RIG -e MCV=$ver node:22-slim timeout 75 node chatbot.mjs > /tmp/chatbot-$id.out 2>&1 &)
sleep ${W:-38}
tail -3 /tmp/chatbot-$id.out
echo "RESULT $id gap=$G mode=$M: signals=$(docker logs $RIG 2>&1 | grep -c "SIGNAL $U")"; docker logs $RIG 2>&1 | grep "SIGNAL $U" | sed "s/.*SIGNAL//" | sort | uniq -c | head -4
docker logs $RIG 2>&1 | grep -iE "chat" | head -3 | cut -c1-170
sh ~/meshac-work/meshac/rig/botrig.sh down $id
