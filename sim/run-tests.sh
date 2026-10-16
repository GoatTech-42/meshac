#!/bin/sh
# Pure-Java tests, no Minecraft. Needs a JDK 17+ (javac). Run from the repo root.
set -e
out=$(mktemp -d)
V=src/main/java/dev/meshac/veil
javac -d "$out" src/main/java/dev/meshac/CheckNames.java src/main/java/dev/meshac/SeenNames.java sim/SeenNamesTest.java $V/PacketPlan.java $V/PlanFresh.java $V/XrayCore.java $V/EspCore.java $V/RayBlocking.java \
  sim/CheckNamesTest.java sim/veil/PlanFreshTest.java sim/veil/PacketPlanFallbackTest.java sim/veil/SizeBytesInvariantTest.java sim/veil/VeilSelfTest.java
for t in CheckNamesTest SeenNamesTest PlanFreshTest PacketPlanFallbackTest SizeBytesInvariantTest VeilSelfTest; do echo "== $t"; java -cp "$out" $t; done
echo "all pure tests ran"
