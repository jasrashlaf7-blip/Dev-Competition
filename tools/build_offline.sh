#!/usr/bin/env bash
# Reproducible offline build script for FIRSTSTEP (Module C.4)
set -euo pipefail

if [ ! -f "debug.keystore" ] && [ -f "debug.keystore.base64" ]; then
  base64 -d debug.keystore.base64 > debug.keystore
fi

echo "Running offline unit tests & assembling Debug APK..."
gradle --offline :app:testDebugUnitTest :app:assembleDebug
echo "Done. APK output: app/build/outputs/apk/debug/app-debug.apk"
