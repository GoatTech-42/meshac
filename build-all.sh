#!/bin/sh
# Builds one jar per supported Minecraft version into dist/. Needs JAVA_HOME on JDK 25.
set -e
cd "$(dirname "$0")"; mkdir -p dist
for f in versions/*.properties; do
  v=$(basename $f .properties)
  args=""; while IFS== read k val; do case "$k" in ""|\#*) continue;; esac; args="$args -P$k=$val"; done < $f # skip blank lines (they made a bare -P= arg; Box filtered these by hand on 10-17) and comment lines
  ./gradlew clean build -q --no-daemon $args
  cp build/libs/meshac-*[0-9].jar dist/meshac-$v.jar
  echo "built dist/meshac-$v.jar"
done
