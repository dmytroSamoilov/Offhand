"""Renders every fixture's text to clips/<id>.m4a with the macOS voice it names.

The clips are committed: a different macOS version can change a voice, and the
thresholds in the fixtures were measured against these exact files.
Run with: python3 -I testing/device-suite/generate_clips.py [clip-id ...]
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

SUITE = Path(__file__).resolve().parent
FIXTURES = SUITE / "fixtures"
CLIPS = SUITE / "clips"


def render(fixture: dict) -> Path:
    target = CLIPS / f"{fixture['id']}.m4a"
    with tempfile.TemporaryDirectory() as scratch:
        aiff = Path(scratch) / "speech.aiff"
        subprocess.run(
            ["say", "-v", fixture["voice"], "-r", str(fixture["rate"]), "-o", str(aiff), fixture["text"]],
            check=True,
        )
        subprocess.run(["afconvert", "-f", "m4af", "-d", "aac", str(aiff), str(target)], check=True)
    return target


def main(ids: list[str]) -> None:
    CLIPS.mkdir(exist_ok=True)
    for path in sorted(FIXTURES.glob("*.json")):
        fixture = json.loads(path.read_text())
        if ids and fixture["id"] not in ids:
            continue
        print(f"rendered {render(fixture).relative_to(SUITE)}")


if __name__ == "__main__":
    main(sys.argv[1:])
