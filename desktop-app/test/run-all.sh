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
    if [ "$mode" = display ]; then
        out=$(xvfb-run -a java -cp "build/classes:$OUT" "$probe" 2>&1)
    else
        out=$(java -Djava.awt.headless=true -cp "build/classes:$OUT" "$probe" 2>&1)
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
run kitnapper.SkinProbe headless
run kitnapper.FontProbe headless
run kitnapper.view.EdgeProbe headless
run kitnapper.view.SizeProbe headless
run kitnapper.FrameProbe display
run kitnapper.CloseProbe display
run kitnapper.view.CatalogUiProbe display

if [ "$fail" -eq 0 ]; then
    echo "ALL PROBES GREEN"
else
    echo "PROBE SUITE FAILED"
fi
exit "$fail"
