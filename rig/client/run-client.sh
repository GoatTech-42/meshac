#!/bin/sh
# usage: run-client.sh <wurst-dir> <gradle-home> [username]   Starts a real Wurst client (headless, software GL) against meshac-rig. No internet.
W=$1; G=$2; U=${3:-w_$(head -c3 /dev/urandom | od -An -tx1 | tr -d " \n")}
docker rm -f meshac-wurst >/dev/null 2>&1
docker run -d --name meshac-wurst --network meshac-net --user $(id -u):$(id -g) --memory 3g --cpus 2 \
  -e HOME=/tmp/h -e GRADLE_USER_HOME=/gh -e LIBGL_ALWAYS_SOFTWARE=1 -e DISPLAY=:99 \
  -v "$G":/gh -v "$W":/w -w /w meshac-client sh -c "mkdir -p /tmp/h; Xvfb :99 -screen 0 1280x720x24 & sleep 2; ./gradlew runClient --offline --no-daemon --no-configuration-cache -Dorg.gradle.jvmargs=-Xmx1200m --args=\"--username $U --quickPlayMultiplayer meshac-rig:25565\" > /w/client.log 2>&1" >/dev/null
echo started $U
