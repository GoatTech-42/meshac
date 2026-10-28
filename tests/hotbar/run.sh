#!/bin/sh
set -eu
: "${JAVA_HOME:?Set JAVA_HOME to a JDK supporting release 25}"
cd "$(dirname "$0")/../.."
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT HUP INT TERM
"$JAVA_HOME/bin/javac" -proc:none --release 25 -d "$out" src/main/java/dev/meshac/Inventory.java tests/hotbar/dev/meshac/*.java tests/hotbar/net/minecraft/server/level/ServerPlayer.java
failed=0
for mode in creative spectator exempt survival; do
    "$JAVA_HOME/bin/java" -cp "$out" dev.meshac.InventoryHotbarTest "$mode" || failed=1
done
exit "$failed"
