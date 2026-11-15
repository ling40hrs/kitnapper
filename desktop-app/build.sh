#!/usr/bin/env sh
# KitNapper build helper — needs only the JDK (17+), no Ant and no NetBeans.
#
#   ./build.sh clean    remove build output
#   ./build.sh build    compile every source file into build/classes
#   ./build.sh run      build, then launch kitnapper.Main
#
# NetBeans still owns the canonical Ant build (build.xml + nbproject/);
# this script exists so a machine without Ant can compile and smoke-test.

set -eu

cd "$(dirname "$0")"

OUT=build/classes
SOURCES=build/sources.txt

clean() {
    rm -rf build dist
}

build() {
    mkdir -p "$OUT"
    find src -name '*.java' | sort > "$SOURCES"
    javac -Xlint:all -encoding UTF-8 -d "$OUT" @"$SOURCES"
    echo "BUILD OK -> $OUT ($(find "$OUT" -name '*.class' | wc -l) classes)"
}

run() {
    build
    java -cp "$OUT" kitnapper.Main
}

case "${1:-build}" in
    clean) clean ;;
    build) build ;;
    run) run ;;
    *)
        echo "usage: $0 [clean|build|run]" >&2
        exit 2
        ;;
esac
