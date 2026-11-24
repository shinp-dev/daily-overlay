#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" app/src/main/java/com/shinpstudio/dailyoverlay/UsageEngine.java tests/UsageEngineTest.java
java -cp "$out" com.shinpstudio.dailyoverlay.UsageEngineTest
