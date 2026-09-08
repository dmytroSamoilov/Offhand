#!/usr/bin/env bash
# Fetches the sherpa-onnx iOS release (which bundles onnxruntime) and unpacks
# the two xcframeworks project.yml links from iosApp/Frameworks. They are
# gitignored, so a fresh clone and CI both need this once.
set -euo pipefail

VERSION="${SHERPA_ONNX_VERSION:-1.13.4}"
SHA256="${SHERPA_ONNX_IOS_SHA256:-596f33bff80046a52144745745fe54d55e8b23659d92209f5ab7d94c1259fe6d}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FRAMEWORKS="$ROOT/Frameworks"
CACHE="${SHERPA_ONNX_CACHE:-$HOME/Library/Caches/offhand-models/sherpa-ios}"
ARCHIVE="$CACHE/sherpa-onnx-v$VERSION-ios.tar.bz2"
URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v$VERSION/sherpa-onnx-v$VERSION-ios.tar.bz2"

if [ -d "$FRAMEWORKS/sherpa-onnx.xcframework" ] && [ -d "$FRAMEWORKS/onnxruntime.xcframework" ]; then
  echo "Frameworks already present in $FRAMEWORKS"
  exit 0
fi

mkdir -p "$CACHE" "$FRAMEWORKS"
if [ ! -f "$ARCHIVE" ]; then
  echo "Downloading $URL"
  curl -fsSL --retry 3 -o "$ARCHIVE" "$URL"
fi
if [ -n "$SHA256" ]; then
  echo "$SHA256  $ARCHIVE" | shasum -a 256 -c -
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
tar -xjf "$ARCHIVE" -C "$WORK"
cp -R "$WORK/build-ios/sherpa-onnx.xcframework" "$FRAMEWORKS/"
ONNX="$(find "$WORK/build-ios/ios-onnxruntime" -maxdepth 2 -type d -name onnxruntime.xcframework | head -1)"
cp -R "$ONNX" "$FRAMEWORKS/"
echo "Installed sherpa-onnx.xcframework and onnxruntime.xcframework into $FRAMEWORKS"
