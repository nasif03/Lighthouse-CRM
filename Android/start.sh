#!/usr/bin/env bash

# https://arlean-stratagemical-kimberlie.ngrok-free.dev/

# cd /home/niloy/vs_code/course/nbm327/third/Lighthouse-CRM/Android && API_BASE_URL='https://fca03e6338da.ngrok-free.app' ./start.sh
#  ngrok --config /home/niloy/snap/ngrok/325/.config/ngrok/ngrok.yml http 3000
# Everytime build fails u can do only @start.sh (10-12) this till it fixes the build.



set -euo pipefail

API_BASE_URL="${API_BASE_URL:-${1:-http://10.0.2.2:3000/}}"
echo "▶ Building with API_BASE_URL=${API_BASE_URL}"

./gradlew assembleDebug -PAPI_BASE_URL="${API_BASE_URL}"
/mnt/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/mnt/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.project.lighthouse/.MainActivity