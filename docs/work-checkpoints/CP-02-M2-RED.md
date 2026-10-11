# CP-02-M2-RED — Restore honest Android-suite discovery

## Objective

Obtain the first reproducible unfiltered result for all 25 instrumented-test files and all 98 declared tests. This checkpoint deliberately does not repair any failure: it establishes the RED evidence required before fixture or production changes.

## Starting state

- Parent checkpoint: CP-01-M1 at remote SHA `c07ce7137dd361a8d683172527b5c97cb48db02f`.
- Same-SHA M1 Android run: `38176731635`, conclusion `success` for the seven tests retained by the old workflow.
- No application, Hifz engine, persistence, Quran geometry, icon, or private title file changed.

## Effective change

The workflow no longer deletes 23 Java test files. It now fails early unless the checkout contains exactly 25 instrumented-test files and 98 `@Test` annotations, then invokes the existing Android runtime script against the complete source set.

## Expected RED evidence

The prior audit says obsolete `HifzPrefs` APIs prevent the full source set from compiling. The next same-SHA run must show the actual compiler/runtime failures by filename and symbol. No test will be removed, disabled, stubbed, or rewritten before that evidence exists.

## Tests

- Local workflow inventory command: expected 25 files / 98 tests.
- Local Gradle/Android execution: `NOT RUN` because Gradle and Android SDK are unavailable in this workspace.
- GitHub Actions full-suite result: pending until this checkpoint is pushed.

## Next action

Read the full same-SHA failure logs, classify every compile error against the official contract, then apply the smallest test-fixture corrections. Production changes remain prohibited unless an executable behavioral test first demonstrates a real product defect.

