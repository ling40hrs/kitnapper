#!/usr/bin/env sh
# Assembles the KitNapper deck and renders it with weasyprint.
# Each slide lives in its own file so no source file grows past the 200-line
# ceiling; this script glues them into one document with the shared stylesheets.
#
# Usage: deck/build.sh   (from anywhere; writes assets/kitnapper-presentation.pdf)

set -eu
cd "$(dirname "$0")"

OUT=../assets/kitnapper-presentation.pdf
TMP=build.tmp.html
trap 'rm -f "$TMP"' EXIT

{
    printf '<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8" />\n'
    printf '<title>KitNapper — presentation</title>\n<style>\n'
    cat base.css components.css panels.css slides.css
    printf '</style>\n</head>\n<body>\n'
    for slide in slide-*.html; do
        cat "$slide"
    done
    printf '</body>\n</html>\n'
} > "$TMP"

python3 -m weasyprint "$TMP" "$OUT"
printf 'deck written to %s\n' "$OUT"
