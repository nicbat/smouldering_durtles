#!/usr/bin/env bash
# Check/build this checkout and update Smoky Durtles - Dev on one USB-connected phone.
set -euo pipefail

usage() {
    printf '%s\n' 'Usage: ./install-phone.sh' \
        'Reuses the current APK when up to date; otherwise builds, then installs and opens Smoky Durtles - Dev.' \
        'Requires JDK 17, the Android SDK, and exactly one phone with USB debugging authorized.' \
        'Preserves app data. Does not pull Git changes, uninstall apps, or run phone tests.'
}
if [[ $# -gt 0 ]]; then
    case "$1" in
        -h|--help) usage; exit 0 ;;
        *) usage >&2; exit 2 ;;
    esac
fi

fail() { printf 'Error: %s\n' "$*" >&2; exit 1; }
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd -- "$project_dir"

sdk_root=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [[ -z "$sdk_root" ]]; then
    for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
        if [[ -d "$candidate/platform-tools" ]]; then
            sdk_root=$candidate
            break
        fi
    done
fi
if [[ -n "$sdk_root" ]]; then export ANDROID_HOME="$sdk_root"; fi
if [[ -n "$sdk_root" && -x "$sdk_root/platform-tools/adb" ]]; then
    adb_bin="$sdk_root/platform-tools/adb"
else
    adb_bin=$(command -v adb) || fail 'adb not found. Set ANDROID_HOME to your Android SDK directory.'
fi

# -d deliberately excludes emulators and wireless-debugging connections.
serial=$("$adb_bin" -d get-serialno) || fail 'Connect exactly one phone by USB, enable USB debugging, and accept its authorization prompt.'
[[ -n "$serial" && "$serial" != unknown ]] || fail 'No USB phone found. Check the cable and USB debugging.'
state=$("$adb_bin" -s "$serial" get-state) || fail 'Unlock the phone and accept the USB debugging prompt, then rerun this command.'
[[ "$state" == device ]] || fail "USB phone is not ready (state: $state)."

printf 'Checking the local build for USB phone %s (unchanged builds are reused)…\n' "$serial"
# Gradle tracks source, resources, dependencies and build configuration. Keep its
# incremental/up-to-date checks enabled; APK timestamps alone cannot prove freshness.
./gradlew --console=plain :app:assembleDebug
apk="$project_dir/app/build/outputs/apk/debug/app-debug.apk"
[[ -f "$apk" ]] || fail "Build completed without the expected APK: $apk"

current_serial=$("$adb_bin" -d get-serialno) || fail 'The USB connection changed during the build. Reconnect the phone and rerun.'
[[ "$current_serial" == "$serial" ]] || fail 'The connected phone changed during the build; installation stopped.'
printf 'Updating Smoky Durtles - Dev (keeping app data)…\n'
"$adb_bin" -s "$serial" install -r "$apk" || fail 'Installation failed; no uninstall or data reset was attempted. See the adb error above.'
"$adb_bin" -s "$serial" shell am start -n com.smoky_durtles.wk.debug/com.smouldering_durtles.wk.activities.MainActivity
printf 'Done — latest local build installed and launched.\n'
