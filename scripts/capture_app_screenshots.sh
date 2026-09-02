#!/usr/bin/env bash
set -euo pipefail

apk_path="${1:?APK path is required}"
output_dir="${2:?Output directory is required}"
package_name="com.applicreation0.quransafeguard"
launcher="${package_name}/.ScreenshotLauncherActivity"

mkdir -p "${output_dir}"
adb install -r "${apk_path}"

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put system font_scale 1.0
adb shell wm dismiss-keyguard || true
adb shell pm grant "${package_name}" android.permission.POST_NOTIFICATIONS || true
adb shell pm grant "${package_name}" android.permission.ACCESS_COARSE_LOCATION || true

capture_screen() {
    local screen="$1"
    local filename="$2"

    adb shell am force-stop "${package_name}"
    adb shell am start -W -n "${launcher}" --es screenshot_screen "${screen}"
    sleep 3

    if ! adb shell dumpsys window windows | grep -q "${package_name}"; then
        adb shell dumpsys activity activities
        echo "Quran Safeguard did not become visible for screen: ${screen}" >&2
        exit 1
    fi

    adb exec-out screencap -p > "${output_dir}/${filename}.png"
    test -s "${output_dir}/${filename}.png"
}

capture_screen dashboard 01-dashboard
capture_screen settings 02-settings
capture_screen protection 03-protection-setup
capture_screen library 04-spiritual-library
capture_screen hikam 05-hikma-10
capture_screen adhkar 06-adhkar-morning
capture_screen selection 07-juz-hizb-selection
capture_screen reader 08-mushaf-reader

file "${output_dir}"/*.png
