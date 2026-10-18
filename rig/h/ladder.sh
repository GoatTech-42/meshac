# Ladder progression with one fixed name: kick, tempban x3, permaban; then unban/pardon. Flight hack on the walk scenario.
D="$(dirname "$0")"; . "$D/run.sh"; RES=/tmp/ladder.csv; : > $RES; NAME=ladder1
for r in 1 2 3 4 5; do
  rc "mesh unban $NAME" >/dev/null 2>&1
  start_client || { echo "round $r: client could not join"; continue; }
  run_case walk Flight
  echo "round $r done; connected=$(connected && echo yes || echo no)"
  sleep 3; tail -c 600 ~/meshac-work/rig/server261/config/meshac-cases.json | tr -d "\n " | cut -c1-300; echo
done
echo "--- join while permabanned (must be refused)"
timeout 80 sh -c ". $D/lib.sh; NAME=ladder1; start_client" && echo "JOINED (BAD)" || echo "refused (good)"
rc "mesh unban $NAME"; sleep 1
timeout 100 sh -c ". $D/lib.sh; NAME=ladder1; start_client" && echo "joined after unban (good)" || echo "still refused after unban (BAD)"
echo LADDERDONE
