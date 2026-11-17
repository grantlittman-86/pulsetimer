#!/bin/bash
set -euo pipefail
watch_root="$(cd "$(dirname "$0")/.." && pwd)"
check_dir="$(mktemp -d)"
trap 'rm -rf "$check_dir"' EXIT
app_source="$watch_root/PulseTimer/PulseTimer Watch App"
# Pass -disable-sandbox when running inside an existing sandbox that prevents nested Swift macro sandboxes.
xcrun swiftc "$@" -module-cache-path "$check_dir/cache" \
  "$app_source"/Models/*.swift \
  "$app_source/Timer/TimerState.swift" \
  "$app_source/Timer/TimerEngine.swift" \
  "$watch_root/tests/TimerEngineChecks.swift" -o "$check_dir/timer-checks"
"$check_dir/timer-checks"
