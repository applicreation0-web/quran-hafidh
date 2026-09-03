#!/usr/bin/env bash
set -euo pipefail

apk_path="${1:?APK path is required}"
output_dir="${2:?Output directory is required}"
package_name="com.applicreation0.quransafeguard"
launcher="${package_name}/.ScreenshotLauncherActivity"

mkdir -p "${output_dir}"

wait_for_android() {
    adb wait-for-device
    for _ in $(seq 1 60); do
        if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
            return 0
        fi
        sleep 2
    done
    echo "Android did not finish booting" >&2
    exit 1
}

dismiss_system_ui_anr() {
    local dump
    local bounds

    for _ in $(seq 1 4); do
        if ! timeout 12s adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1; then
            adb shell input keyevent KEYCODE_DPAD_DOWN || true
            adb shell input keyevent KEYCODE_ENTER || true
            sleep 4
            continue
        fi

        dump="$(adb shell cat /sdcard/window.xml 2>/dev/null || true)"
        if ! grep -Eq 'android:id/aerr_wait|System UI isn.t responding' <<< "${dump}"; then
            return 0
        fi

        bounds="$(grep -oE '<node[^>]*(resource-id="android:id/aerr_wait"|text="Wait")[^>]*bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' <<< "${dump}"             | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"'             | head -n 1 || true)"

        if [[ "${bounds}" =~ \[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\] ]]; then
            adb shell input tap "$(( (BASH_REMATCH[1] + BASH_REMATCH[3]) / 2 ))" "$(( (BASH_REMATCH[2] + BASH_REMATCH[4]) / 2 ))"
        else
            adb shell input keyevent KEYCODE_DPAD_DOWN || true
            adb shell input keyevent KEYCODE_ENTER || true
        fi
        sleep 4
    done

    if ! timeout 12s adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1; then
        echo "Android UI could not be inspected after dismiss attempts" >&2
        exit 1
    fi

    dump="$(adb shell cat /sdcard/window.xml 2>/dev/null || true)"
    if grep -Eq 'android:id/aerr_wait|System UI isn.t responding' <<< "${dump}"; then
        echo "System UI ANR dialog could not be dismissed" >&2
        exit 1
    fi
}

wait_for_android
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
sleep 8
dismiss_system_ui_anr

adb install -r "${apk_path}"

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put system font_scale 1.0
adb shell pm grant "${package_name}" android.permission.POST_NOTIFICATIONS || true
adb shell pm grant "${package_name}" android.permission.ACCESS_COARSE_LOCATION || true

capture_screen() {
    local screen="$1"
    local filename="$2"
    local dump

    adb shell am force-stop "${package_name}"
    adb shell am start -W -n "${launcher}" --es screenshot_screen "${screen}"
    sleep 3
    dismiss_system_ui_anr
    sleep 2

    if ! timeout 12s adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1; then
        echo "Android UI could not be inspected for screen: ${screen}" >&2
        exit 1
    fi

    dump="$(adb shell cat /sdcard/window.xml 2>/dev/null || true)"
    if grep -Eq 'android:id/aerr_wait|System UI isn.t responding' <<< "${dump}"; then
        echo "System UI error dialog is still visible for screen: ${screen}" >&2
        exit 1
    fi

    if ! adb shell dumpsys activity activities | grep -q "mResumedActivity.*${package_name}"; then
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
