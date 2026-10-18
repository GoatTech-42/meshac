#!/usr/bin/env bash
# 0107: end-to-end DRY control-flow test of the movement-integration fixture using DECLARED tool doubles.
# Fake javac/java on PATH + a fake Gson jar; the REAL fixture script runs its entire python control flow
# (prereqs, version probes, source staging + clock-rebind pins, manifest, capped subprocess plumbing) against
# the doubles. This catches def/use and wiring bugs by running the real flow - helpers alone cannot. It proves
# NOTHING about compilation or harness semantics: the tools are doubles. The real run remains Box's job with a
# real JDK + real Gson.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
D="$(mktemp -d)"; trap 'rm -rf "$D"' EXIT
mkdir -p "$D/bin"

cat > "$D/bin/javac" <<'SH'
#!/usr/bin/env bash
# DECLARED DOUBLE - never compiles anything
for a in "$@"; do [ "$a" = "-version" ] && { echo "javac 17.0.0-drydouble"; exit 0; }; done
echo "DRYDOUBLE javac $*" >> "$DRYLOG"
exit 0
SH

cat > "$D/bin/java" <<'SH'
#!/usr/bin/env bash
# DECLARED DOUBLE - never runs anything
for a in "$@"; do [ "$a" = "-version" ] && { echo 'openjdk version "17.0.0-drydouble" 2026-01-01'; exit 0; }; done
echo "DRYDOUBLE java $*" >> "$DRYLOG"
mode="plain"; for a in "$@"; do [ "$a" = "classify" ] && mode="classify"; done
echo "MovementHarness PASS mode=$mode (1 checks)"
exit 0
SH
chmod +x "$D/bin/javac" "$D/bin/java"

printf 'declared-double-gson-bytes' > "$D/gson-0.0.0-dry.jar"
export DRYLOG="$D/dry.log"; : > "$DRYLOG"
PATH="$D/bin:$PATH" GSON_JAR="$D/gson-0.0.0-dry.jar" python3 "$HERE/test-movement-integration.py"

grep -q 'DRYDOUBLE javac .*MovementHarness.java' "$DRYLOG" || { echo "DRY FAIL: javac double never received the compile"; exit 1; }
grep -q 'DRYDOUBLE java .*MovementHarness plain' "$DRYLOG" || { echo "DRY FAIL: java double never received the plain harness run"; exit 1; }
grep -q 'DRYDOUBLE java .*MovementHarness classify' "$DRYLOG" || { echo "DRY FAIL: java double never received the classify harness run"; exit 1; }
echo "DRY CONTROL-FLOW PASS: full fixture flow executed end to end against declared doubles (compile + both harness invocations recorded in the dry log)"
