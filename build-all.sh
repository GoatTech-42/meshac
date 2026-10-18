#!/bin/sh
# Builds one jar per supported Minecraft version into dist/. Needs JAVA_HOME on JDK 25.
set -e
cd "$(dirname "$0")"; mkdir -p dist
for f in versions/*.properties; do
  v=$(basename $f .properties)
  args=""; while IFS== read k val; do args="$args -P$k=$val"; done < $f
  ./gradlew clean build -q --no-daemon $args
  cp build/libs/meshac-*[0-9].jar dist/meshac-$v.jar
  echo "built dist/meshac-$v.jar"
done
