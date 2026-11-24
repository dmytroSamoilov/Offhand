#!/usr/bin/env bash
# Runs the on-device Maestro suite (.maestro/device) on a real Android phone.
#
#   testing/device-suite/run-android.sh [--serial S] [--variant release|debug]
#                                       [--clips "id id"] [--skip-build] [--skip-setup]
#
# Put the phone next to the Mac's speakers in a quiet room, on Wi-Fi, with the
# Mac's volume at a normal speaking level. The first run downloads the on-device
# AI into the devicetest app (about 3 GB); later runs reuse it.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SUITE="$ROOT/testing/device-suite"
FLOWS="$ROOT/.maestro/device"
APP_ID="com.dmytrosamoilov.offhand.devicetest"
PORT=8765

serial=""
variant="release"
clips=""
build=true
setup=true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --serial) serial="$2"; shift 2 ;;
    --variant) variant="$2"; shift 2 ;;
    --clips) clips="$2"; shift 2 ;;
    --skip-build) build=false; shift ;;
    --skip-setup) setup=false; shift ;;
    *) echo "unknown option $1" >&2; exit 2 ;;
  esac
done

if [[ "$variant" != "release" && "$variant" != "debug" ]]; then
  echo "--variant is release or debug" >&2; exit 2
fi

if [[ -z "$serial" ]]; then
  devices=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }' | grep -v '^emulator-' || true)
  if [[ $(echo "$devices" | grep -c .) -ne 1 ]]; then
    echo "connect exactly one physical phone or pass --serial" >&2; exit 2
  fi
  serial="$devices"
fi

if [[ -z "$clips" ]]; then
  clips=$(cd "$SUITE/fixtures" && ls *.json | sed 's/\.json$//' | tr '\n' ' ')
fi

capitalized="$(tr '[:lower:]' '[:upper:]' <<< "${variant:0:1}")${variant:1}"
if $build; then
  (cd "$ROOT" && ./gradlew "assembleDevicetest$capitalized" -q)
fi
apk=$(ls "$ROOT/app/build/outputs/apk/devicetest/$variant/"*.apk | head -1)
adb -s "$serial" install -r "$apk" > /dev/null
adb -s "$serial" shell pm grant "$APP_ID" android.permission.RECORD_AUDIO
adb -s "$serial" shell pm grant "$APP_ID" android.permission.POST_NOTIFICATIONS || true

stay_on=$(adb -s "$serial" shell settings get global stay_on_while_plugged_in | tr -d '\r')
zen_mode=$(adb -s "$serial" shell settings get global zen_mode | tr -d '\r')
restore() {
  adb -s "$serial" shell settings put global stay_on_while_plugged_in "$stay_on" > /dev/null 2>&1 || true
  [[ "$zen_mode" == "0" ]] && adb -s "$serial" shell cmd notification set_dnd off > /dev/null 2>&1 || true
  [[ -n "${bridge_pid:-}" ]] && kill "$bridge_pid" 2> /dev/null || true
}
trap restore EXIT
adb -s "$serial" shell settings put global stay_on_while_plugged_in 7
adb -s "$serial" shell cmd notification set_dnd priority > /dev/null

python3 -I "$SUITE/audio_bridge.py" --serial "$serial" --port "$PORT" &
bridge_pid=$!
sleep 1

reports="$SUITE/results/$(date +%Y-%m-%d_%H%M%S)"
mkdir -p "$reports"

if $setup; then
  maestro --device "$serial" test --format junit --output "$reports/00-setup.xml" "$FLOWS/00-setup.yaml"
fi

status=0
for clip in $clips; do
  maestro --device "$serial" test -e CLIP="$clip" --format junit --output "$reports/$clip.xml" \
    "$FLOWS/01-acoustic-record.yaml" || status=1
  sleep 1
done

echo "JUnit reports: $reports"
echo "Scores: $SUITE/results/device-results.jsonl"
exit $status
