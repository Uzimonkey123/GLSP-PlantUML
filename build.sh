#!/bin/bash
set -e

# Author: Norman Babiak
# Description: Building script for Unix systems
# Date: 10.5.2026

TARGET_JAR="target/GLSPPlantUML-1.0-SNAPSHOT.jar"
SERVER_DIR="plantuml-client/server"
CLIENT_DIR="plantuml-client"

if [ -f "./mvnw" ]; then
    MVN="./mvnw"
    echo "[maven] Using Maven Wrapper (mvnw)"
else
    MVN="mvn"
    echo "[maven] Using system Maven (mvn)"
fi

SKIP_TESTS=false
BUILD_CLIENT=false
PACKAGE_VSIX=false

show_help() {
    echo ""
    echo "Usage: ./build.sh [options]"
    echo ""
    echo "Options:"
    echo "  (no flags)     Maven build with tests, copy JAR"
    echo "  -s             Maven build without tests, copy JAR"
    echo "  -c             Maven build + npm run build:all"
    echo "  -f             Maven build + npm run build:all (skip tests)"
    echo "  -p             Full build (skip tests) + npm build + vsce package"
    echo "  -h             Show this help"
    echo ""
}

while getopts "scfph" opt; do
    case $opt in
        s) SKIP_TESTS=true ;;
        c) BUILD_CLIENT=true ;;
        f) SKIP_TESTS=true; BUILD_CLIENT=true ;;
        p) SKIP_TESTS=true; BUILD_CLIENT=true; PACKAGE_VSIX=true ;;
        h) show_help; exit 0 ;;
        *) show_help; exit 1 ;;
    esac
done

if [ "$SKIP_TESTS" = true ]; then
    echo "[1/2] Building server (skip tests)..."
    $MVN clean package -DskipTests
else
    echo "[1/2] Building server (with tests)..."
    $MVN clean package
fi

echo "[2/2] Copying JAR to $SERVER_DIR..."
cp "$TARGET_JAR" "$SERVER_DIR/"
echo "JAR copied."

if [ "$BUILD_CLIENT" = true ]; then
    echo "[+] Building client (npm run build:all)..."
    cd "$CLIENT_DIR"
    npm run build:all
    cd ..
    echo "Client built."
fi

if [ "$PACKAGE_VSIX" = true ]; then
    echo "[+] Packaging VSIX..."
    cd "$CLIENT_DIR"
    npx vsce package --no-dependencies
    cd ..
    echo "VSIX packaged."
fi

echo "Done."