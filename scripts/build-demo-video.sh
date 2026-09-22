#!/usr/bin/env bash

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEMO_TMP="$(mktemp -d)"
OUTPUT="$PROJECT_ROOT/docs/settleup-demo.mp4"

cleanup() {
  rm -r "$DEMO_TMP"
}
trap cleanup EXIT

render_slide() {
  local input="$1"
  local output="$2"
  local background="$3"

  ffmpeg -hide_banner -loglevel error -y \
    -loop 1 -framerate 30 -i "$input" \
    -f lavfi -i anullsrc=r=48000:cl=stereo \
    -t 10 \
    -vf "scale=1920:1080:force_original_aspect_ratio=decrease,pad=1920:1080:(ow-iw)/2:(oh-ih)/2:color=$background,format=yuv420p" \
    -c:v libx264 -preset medium -crf 20 -r 30 \
    -c:a aac -b:a 128k -shortest \
    "$output"
}

render_card() {
  local input="$1"
  local output="$2"

  ffmpeg -hide_banner -loglevel error -y \
    -loop 1 -framerate 30 -i "$input" \
    -f lavfi -i anullsrc=r=48000:cl=stereo \
    -t 10 \
    -vf "format=yuv420p" \
    -c:v libx264 -preset medium -crf 20 -r 30 \
    -c:a aac -b:a 128k -shortest \
    "$output"
}

command -v rsvg-convert >/dev/null || {
  echo "rsvg-convert is required (install librsvg)." >&2
  exit 1
}

rsvg-convert -w 1920 -h 1080 \
  -o "$DEMO_TMP/intro.png" "$PROJECT_ROOT/docs/demo/intro.svg"
rsvg-convert -w 1920 -h 1080 \
  -o "$DEMO_TMP/metrics.png" "$PROJECT_ROOT/docs/demo/metrics.svg"

render_card "$DEMO_TMP/intro.png" "$DEMO_TMP/01.mp4"
render_slide "$PROJECT_ROOT/.impeccable/review/hero-phase7-final.png" "$DEMO_TMP/02.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/.impeccable/review/phase7-auth.png" "$DEMO_TMP/03.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/.impeccable/review/phase7-dashboard-final.png" "$DEMO_TMP/04.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/.impeccable/review/phase7-group-top.png" "$DEMO_TMP/05.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/.impeccable/review/phase7-group-expenses.png" "$DEMO_TMP/06.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/.impeccable/review/phase7-mobile-final.png" "$DEMO_TMP/07.mp4" "0xf5e6d8"
render_slide "$PROJECT_ROOT/docs/grafana-dashboard.png" "$DEMO_TMP/08.mp4" "0x101216"
render_card "$DEMO_TMP/metrics.png" "$DEMO_TMP/09.mp4"

for segment in "$DEMO_TMP"/*.mp4; do
  printf "file '%s'\n" "$segment" >> "$DEMO_TMP/segments.txt"
done

ffmpeg -hide_banner -loglevel error -y \
  -f concat -safe 0 -i "$DEMO_TMP/segments.txt" \
  -c copy -movflags +faststart "$OUTPUT"

echo "Created $OUTPUT"
