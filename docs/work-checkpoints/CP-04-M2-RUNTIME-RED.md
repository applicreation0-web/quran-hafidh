# CP-04-M2-RUNTIME-RED — Execute the complete Android suite

## Evidence carried forward

- Fixture repair SHA: `527dc480f1ef897275e88fda8552f726e016976c`.
- Same-SHA run: `38177602466`, conclusion `success`.
- Meaning of that success: all 25 Android test files compiled after the fixture repair, but the runtime script still passed `-e class` with only two classes and therefore executed seven tests.

## Change

Remove the runtime class filter and require AndroidJUnitRunner to report exactly `OK (98 tests)` and 98 successful test status records. Keep existing failure detection and authorized screenshot collection. No APK is published.

## Expected result

This is the first run that can reveal runtime failures in the 91 tests previously excluded. A failing result is diagnostic RED evidence, not a regression introduced by the workflow. No test or production behavior will be changed until named failures and logs are available.

## Protected surfaces

No production Java/Kotlin, Hifz core, persistence schema, Quran asset, icon, or private title content changes. Only the CI runner script, workflow label, and checkpoint documents change.

