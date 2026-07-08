#!/bin/sh
# builds the jackc native binary - needs a GraalVM JDK (javac + native-image) on PATH
# used both locally and by the release CI
set -e

cd "$(dirname "$0")"

rm -rf build
mkdir -p build/OS build/META-INF/native-image/jackc

javac -d build Compiler.java JackCompiler.java VMTranslator.java Assembler.java JackParser.java

# -L dereferences the OS/*.vm symlinks so real file contents get baked into the binary
cp -L OS/*.vm build/OS/
cp resource-config.json build/META-INF/native-image/jackc/

# on windows, git can check symlinks out as plain text files holding the target path -
# catch that here instead of shipping a binary with garbage where the OS should be
grep -q "function Sys.init" build/OS/Sys.vm || {
    echo "error: build/OS/Sys.vm does not contain VM code - were the OS/ symlinks checked out as text files?" >&2
    exit 1
}

# on windows the launcher is native-image.cmd, which bash won't find under the bare name
NATIVE_IMAGE="native-image"
command -v native-image >/dev/null 2>&1 || NATIVE_IMAGE="native-image.cmd"

# --no-fallback = fail the build instead of silently producing a binary that still needs a JVM
"$NATIVE_IMAGE" -cp build Compiler -o jackc --no-fallback

echo "built: $(pwd)/jackc"
