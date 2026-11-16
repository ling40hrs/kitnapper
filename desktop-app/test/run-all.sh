#!/usr/bin/env sh
# KitNapper probe suite (plan §6.4, WP0.5). JDK only, no test framework:
# builds the app, compiles every probe into test/out, runs them, and prints
# one status line per probe. Exits non-zero on any failure.
#
# Probes that never open a window run headless; frame/paint probes run
# under xvfb-run -a so they work with or without a live display.

set -u
cd "$(dirname "$0")/.."

./build.sh build || exit 1

OUT=test/out
rm -rf "$OUT"
mkdir -p "$OUT"
find test -name '*.java' | sort > "$OUT/sources.txt"
javac -Xlint:all -encoding UTF-8 -cp build/classes -d "$OUT" @"$OUT/sources.txt" || exit 1

fail=0

run() {
    probe=$1
    mode=$2
    jvm=${3:-}
    if [ "$mode" = display ]; then
        out=$(xvfb-run -a java $jvm -cp "build/classes:$OUT" "$probe" 2>&1)
    else
        out=$(java -Djava.awt.headless=true $jvm -cp "build/classes:$OUT" "$probe" 2>&1)
    fi
    if printf '%s\n' "$out" | grep -q '^FAIL=[1-9]'; then
        fail=1
        status=FAIL
    elif printf '%s\n' "$out" | grep -q '^PASS=.*FAIL=0$'; then
        status=PASS
    elif [ "$probe" = "kitnapper.CloseProbe" ] \
        && printf '%s\n' "$out" | grep -q '^EXITED_OK$'; then
        status=PASS
    else
        fail=1
        status=FAIL
    fi
    printf '%-36s %s\n' "$probe" "$status"
    if [ "$status" = FAIL ]; then
        printf '%s\n' "$out" | sed 's/^/    /'
    fi
}

run kitnapper.DataProbe headless
run kitnapper.CatalogProbe headless
run kitnapper.LoanProbe headless
run kitnapper.InventoryProbe headless
run kitnapper.SkinProbe headless
run kitnapper.FontProbe headless
run kitnapper.view.EdgeProbe headless
run kitnapper.view.SizeProbe headless
# Motion off for every display probe: the fades would corrupt paint
# assertions (UiProbeSupport.settle() does not wait). MotionProbe runs with
# motion on and is the only probe that measures the fades.
MOTION_OFF=-Dkitnapper.motion=off

# Optional DPI proxy (plan §4.4, §8.1): KITNAPPER_UI_SCALE=1.25 or 1.5 adds
# -Dsun.java2d.uiScale to every display probe. Unset by default; the Windows
# 100/125/150 % pass stays the authoritative check.
SCALE_ARG=
if [ -n "${KITNAPPER_UI_SCALE:-}" ]; then
    SCALE_ARG="-Dsun.java2d.uiScale=${KITNAPPER_UI_SCALE}"
fi
DISPLAY_ARGS="$MOTION_OFF $SCALE_ARG"

run kitnapper.FrameProbe display "$DISPLAY_ARGS"
run kitnapper.CloseProbe display "$DISPLAY_ARGS"
run kitnapper.view.CatalogUiProbe display "$DISPLAY_ARGS"
run kitnapper.view.PhotoUiProbe display "$DISPLAY_ARGS"
run kitnapper.view.InventoryEditorProbe display "$DISPLAY_ARGS"
run kitnapper.view.InventoryUiProbe display "$DISPLAY_ARGS"
run kitnapper.view.SettingsUiProbe display "$DISPLAY_ARGS"
run kitnapper.view.M5FlowProbe display "$DISPLAY_ARGS"
run kitnapper.view.LoansLayoutProbe display "$DISPLAY_ARGS"
run kitnapper.view.LoansUiProbe display "$DISPLAY_ARGS"
run kitnapper.view.GeometryProbe display "$DISPLAY_ARGS"
run kitnapper.view.MotionProbe display "$SCALE_ARG"

if [ "$fail" -eq 0 ]; then
    echo "ALL PROBES GREEN"
else
    echo "PROBE SUITE FAILED"
fi
exit "$fail"
