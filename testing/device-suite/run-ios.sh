#!/usr/bin/env bash
# Runs the on-device suite on a real iPhone or iPad: the UI test in
# iosApp/DeviceTests drives Offhand Dev with its real on-device AI while this
# Mac plays each fixture clip through its speakers, then the note the test
# hands back is scored like the Android run.
#
#   testing/device-suite/run-ios.sh --device <udid> [--clips "id id"] [--skip-build]
#                                   [--lead-seconds N] [--project name.xcodeproj]
#
# Put the device next to the Mac's speakers in a quiet room, unlocked, with
# the on-device AI already downloaded in Offhand Dev (the suite never resets
# the app). A USB cable is steadier than Wi-Fi. `xcrun devicectl list devices`
# shows the UDID.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SUITE="$ROOT/testing/device-suite"
SCHEME="Offhand-devicetest"
TEST="OffhandDeviceTests/AcousticNoteTests/testRecordedClipBecomesANote"

device=""
clips=""
build=true
lead=75
project="Offhand.xcodeproj"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) device="$2"; shift 2 ;;
    --clips) clips="$2"; shift 2 ;;
    --skip-build) build=false; shift ;;
    --lead-seconds) lead="$2"; shift 2 ;;
    --project) project="$2"; shift 2 ;;
    *) echo "unknown option $1" >&2; exit 2 ;;
  esac
done

if [[ -z "$device" ]]; then
  echo "pass --device <udid>" >&2; exit 2
fi
if [[ -z "$clips" ]]; then
  clips=$(cd "$SUITE/fixtures" && ls *.json | sed 's/\.json$//' | tr '\n' ' ')
fi

derived="$ROOT/iosApp/build/DerivedDataDeviceTests"
destination="platform=iOS,id=$device"
devices_json=$(mktemp)
xcrun devicectl list devices --json-output "$devices_json" > /dev/null 2>&1 || true
device_name=$(python3 -I - "$devices_json" "$device" <<'PY'
import json, sys
try:
    devices = json.load(open(sys.argv[1]))["result"]["devices"]
except (OSError, ValueError, KeyError):
    devices = []
for entry in devices:
    hardware = entry.get("hardwareProperties", {})
    if hardware.get("udid") == sys.argv[2]:
        print(hardware.get("marketingName", ""))
PY
)
rm -f "$devices_json"
reports="$SUITE/results/$(date +%Y-%m-%d_%H%M%S)-ios"
mkdir -p "$reports"

if $build; then
  (cd "$ROOT/iosApp" && GIT_LFS_SKIP_SMUDGE=1 xcodebuild build-for-testing -project "$project" -scheme "$SCHEME" \
    -destination "$destination" -derivedDataPath "$derived" -quiet)
fi

# xcodebuild waits minutes on a locked device before giving up, so say it now.
if xcrun devicectl device info lockState --device "$device" 2> /dev/null | grep -q "passcodeRequired: true"; then
  echo "unlock the device and keep its screen on, then run this again" >&2; exit 2
fi

describe() {
  python3 -I "$SUITE/audio_bridge.py" --describe-clip "$1" | python3 -I -c "import json, sys; print(json.load(sys.stdin)['$2'])"
}

status=0
for clip in $clips; do
  clip_file=$(describe "$clip" clip)
  play_at=$(( $(date +%s) + lead ))
  ( sleep "$lead"; afplay "$clip_file" ) &
  player=$!
  result="$reports/$clip.xcresult"
  (cd "$ROOT/iosApp" && TEST_RUNNER_PLAY_AT="$play_at" \
    TEST_RUNNER_CLIP_DURATION_MS="$(describe "$clip" durationMs)" \
    TEST_RUNNER_LIVE_PHRASE="$(describe "$clip" livePhrase)" \
    xcodebuild test-without-building -project "$project" -scheme "$SCHEME" -destination "$destination" \
      -derivedDataPath "$derived" -only-testing:"$TEST" -resultBundlePath "$result" \
      > "$reports/$clip.log" 2>&1) || status=1
  kill "$player" 2> /dev/null || true
  attachments="$reports/$clip-attachments"
  xcrun xcresulttool export attachments --path "$result" --output-path "$attachments" > /dev/null 2>&1 || true
  export_file=$(python3 -I - "$attachments" <<'PY'
import json, sys
from pathlib import Path
folder = Path(sys.argv[1])
manifest = folder / "manifest.json"
if manifest.is_file():
    for test in json.loads(manifest.read_text()):
        for attachment in test.get("attachments", []):
            if attachment.get("suggestedHumanReadableName", "").startswith("note-export"):
                print(folder / attachment["exportedFileName"])
                raise SystemExit
PY
)
  if [[ -n "$export_file" ]]; then
    python3 -I "$SUITE/audio_bridge.py" --score-export "$export_file" --clip "$clip" \
      --device-name "${device_name:-iOS device}" || status=1
  else
    echo "$clip: the test handed back no note, see $reports/$clip.log" >&2
    status=1
  fi
done

echo "Reports: $reports"
echo "Scores: $SUITE/results/device-results.jsonl"
exit $status
