./gradlew assembleDebug
/mnt/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
/mnt/c/Users/User/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.project.lighthouse/.MainActivity