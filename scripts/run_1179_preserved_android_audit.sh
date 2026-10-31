#!/usr/bin/env bash
# Synthetic disposable emulator only. Re-exercises the preserved 1.17.8 gates
# against the reviewed 1.17.9 adapters; never touches user progression.
set -euo pipefail
python3 scripts/verify_1179_invariants.py
adb shell wm size 1404x1872
adb shell wm density 226
gradle --no-daemon :hifz-app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.quransafeguard.hifz.preview.BackupSchemaRejectedInstrumentedTest,com.quransafeguard.hifz.preview.MushafPendingCommandsInstrumentedTest,com.quransafeguard.hifz.preview.AyahThemesReviewDisplayInstrumentedTest#independentTitlesLeaveFullMaskAndHistoricalHolesExactlyUnchanged,com.quransafeguard.hifz.preview.HifzV6PersistentStateInstrumentedTest,com.quransafeguard.hifz.preview.HifzV6StateTransitionsInstrumentedTest,com.quransafeguard.hifz.preview.ProcessDeathPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalPersistenceInstrumentedTest,com.quransafeguard.hifz.preview.SundayMondayResumeInstrumentedTest,com.quransafeguard.hifz.preview.SundayFinalMaskReadinessInstrumentedTest,com.quransafeguard.hifz.preview.PostFinalRecallInstrumentedTest,com.quransafeguard.hifz.preview.PhoneProgrammeMigrationInstrumentedTest,com.quransafeguard.hifz.preview.HifzPrefsV6MigrationInstrumentedTest,com.quransafeguard.hifz.preview.HifzUiLayoutInstrumentedTest,com.quransafeguard.hifz.preview.MapQcfLifecycleInstrumentedTest
bash scripts/run_1175_process_death_audit.sh
