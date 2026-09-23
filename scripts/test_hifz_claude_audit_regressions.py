from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def test_runtime_uses_domain_schedule_as_single_source_of_truth():
    session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")
    main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java")
    core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt")
    config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java")

    # Calendar/session selection stays schedule-driven, while a mode opened directly must obtain
    # its duration from the same domain constants without depending on whether that mode happens
    # to be scheduled today. This prevents quick-access and midnight rollover crashes.
    assert "HifzSchedule.INSTANCE.planFor" in main, (
        "Today/dashboard must keep consuming HifzSchedule.planFor()"
    )
    assert "fun targetMinutesFor(kind: SessionKind)" in core, (
        "HifzSchedule must expose the canonical per-mode target duration"
    )
    assert "HifzSchedule.INSTANCE.targetMinutesFor" in session, (
        "HifzSessionActivity must use schedule-owned per-mode duration constants"
    )
    assert "scheduledTargetMinutes(" not in session, (
        "direct session duration must not depend on today's DailyPlan"
    )
    assert "absent du planning" not in session, (
        "off-schedule quick access must not throw from duration resolution"
    )

    assert "WEEKDAY_MURAJAAH_MINUTES" not in session
    assert "WEEKEND_MURAJAAH_MINUTES" not in session
    assert "WEEKEND_RECENT_REVIEW_MINUTES" not in session
    assert "SABQI_TODAY_REVIEW_MINUTES" not in session

    for duplicate in (
        "MURAJAAH_MINUTES_WORKING",
        "MURAJAAH_RECENT_SABQI_MINUTES_WORKING",
        "MURAJAAH_ITQAN_MINUTES_WORKING",
        "WEEKDAY_MURAJAAH_MINUTES",
        "WEEKEND_MURAJAAH_MINUTES",
        "WEEKEND_RECENT_REVIEW_MINUTES",
        "SABQI_TODAY_REVIEW_MINUTES",
    ):
        assert duplicate not in config, f"duplicate duration constant remains: {duplicate}"


def test_reader_fit_cannot_be_overridden_by_user_zoom():
    mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java")
    index = read("hifz-app/src/main/assets/hifzreader/index.html")

    assert "setBuiltInZoomControls(false)" in mushaf
    assert "setBuiltInZoomControls(true)" not in mushaf
    assert "maximum-scale=1" in index
    assert "user-scalable=no" in index


def test_mask_entropy_is_session_persistent_not_view_instance_random():
    mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java")
    prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
    session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")

    assert "UUID.randomUUID" not in mushaf
    assert "setMaskEntropy" in mushaf
    assert "maskEntropyFor" in prefs
    assert "clearMaskEntropy" in prefs
    assert "prefs.maskEntropyFor(mode)" in session
    assert "prefs.clearMaskEntropy(mode)" in session


def test_rosettes_are_redrawn_even_when_mask_has_no_ayah_selection():
    reader = read("hifz-app/src/main/assets/hifzreader/reader.js")
    assert "if(polys.length)layer.appendChild(markerLayer" not in reader
    assert "markerLayer(svg,polys,lines)" in reader


def test_dead_cursor_setter_and_test_only_reset_copy_are_removed():
    prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
    settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java")

    assert "void setMurajaahCursor" not in prefs
    assert '"État de test"' not in settings
    assert "Efface la progression Hifz locale" in settings


if __name__ == "__main__":
    tests = [
        test_runtime_uses_domain_schedule_as_single_source_of_truth,
        test_reader_fit_cannot_be_overridden_by_user_zoom,
        test_mask_entropy_is_session_persistent_not_view_instance_random,
        test_rosettes_are_redrawn_even_when_mask_has_no_ayah_selection,
        test_dead_cursor_setter_and_test_only_reset_copy_are_removed,
    ]
    failures = []
    for test in tests:
        try:
            test()
            print(f"PASS {test.__name__}")
        except AssertionError as error:
            failures.append((test.__name__, str(error)))
            print(f"FAIL {test.__name__}: {error}")
    if failures:
        raise SystemExit(f"{len(failures)} regression contract(s) failing")