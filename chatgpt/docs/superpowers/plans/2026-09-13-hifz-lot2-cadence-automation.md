# Hifz Lot 2 Cadence Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Murājaʿah cadence the single workload-sizing reference, expose effective Snowball-grown Itqān ranges read-only, and add a lightweight Wed/Fri recent-Sabqi advisory without changing fixed Hifz schedule semantics.

**Architecture:** Introduce a pure `HifzCadence` helper as the only place for cadence validation, line-target sizing, five-line-safe sizing, and conservative recalibration. Runtime sessions and UI consume that helper; persistence keeps only `murajaahSecondsPerLine` active. Existing schema v3 and Cycle + Snowball cursors remain untouched.

**Tech Stack:** Android Java 17, Kotlin/Gradle Kotlin DSL, JUnit 4, existing Hifz Java/Kotlin domain classes, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-13-hifz-lot2-cadence-automation-design.md`

## Global Constraints

- Base code SHA before Lot 2 behavior: `c23428ccf50e5d5bd658edad47b79bd86e4c630c`.
- Sabqi stays exactly ×37.
- Itqān stays exactly ×40.
- Timed sessions stay exactly on the canonical HifzSchedule durations: 30/60 minutes as already defined.
- No time redistribution.
- `itqanCursor`, `murajaahCursor`, and Sabqi progression remain independent.
- Promotion never jumps either cursor.
- Schema stays v3 unless a genuinely required persistence incompatibility is discovered; no bump is planned.
- `recentSecPerLine` may remain as inert legacy stored data but must not drive runtime planning.
- BOOX reader fit, random mask contract, canonical 604 Mushaf assets, Tafsir/audio/product boundaries must not change.
- No merge, signing, or publication in this plan.

---

### Task 1: Repair the red baseline without behavior change

**Files:**
- Modify: `hifz-app/build.gradle.kts`
- Test: existing Gradle configuration + official CI workflow

**Interfaces:**
- Consumes: current `prepareHifzTafsirRelease` task.
- Produces: a Gradle script that resolves Base64/ceil/min symbols without changing generated asset bytes or task semantics.

- [ ] **Step 1: Capture the exact current failure**

Confirm the existing official run fails only on unresolved Kotlin DSL symbols around:

```kotlin
java.util.Base64.getMimeDecoder().decode(payload)
kotlin.math.ceil(...)
kotlin.math.min(...)
```

Expected: configuration/script compile failure before APK assembly.

- [ ] **Step 2: Apply the minimal namespace-shadowing repair**

Add explicit top-level imports:

```kotlin
import java.util.Base64
import kotlin.math.ceil
import kotlin.math.min
```

Then replace only the affected calls:

```kotlin
Base64.getMimeDecoder().decode(payload)
ceil(decoded.size / corpus.expectedParts.toDouble()).toInt()
min(decoded.size, start + chunkSize)
```

Do not alter corpus lists, expected part counts, asset paths, output names, or chunking formula.

- [ ] **Step 3: Run configuration/build verification through CI**

Expected: Gradle script compiles; workflow progresses past the previous failure and reaches tests/assembly.

- [ ] **Step 4: Compare generated candidate evidence with pre-Lot-2 expectations**

Verify package/version remain:

```text
com.quransafeguard.hifz
versionCode 10
versionName 0.7.3-boox
```

- [ ] **Step 5: Commit**

Commit message:

```text
fix(hifz): repair Gradle DSL release asset preparation
```

---

### Task 2: Add one pure cadence engine with TDD

**Files:**
- Create: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzCadence.java`
- Create: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/HifzCadenceTest.java`
- Read-only reference: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java`

**Interfaces:**
- Produces:
  - `static double sanitizedSecondsPerLine(double value)`
  - `static int targetLines(int minutes, double secondsPerLine)`
  - `static int targetFiveLineCapacity(int minutes, double secondsPerLine)`
  - `static double recalibrate(double oldSecondsPerLine, int lines, long elapsedMs)`
  - `static int[] advisoryFiveLineRange(double secondsPerLine)` returning `[5minLines, 10minLines]`

- [ ] **Step 1: Write failing unit tests**

Tests must assert:

```java
assertEquals(9.0, HifzCadence.sanitizedSecondsPerLine(Double.NaN), 0.0001);
assertEquals(9.0, HifzCadence.sanitizedSecondsPerLine(0.0), 0.0001);
assertEquals(200, HifzCadence.targetLines(30, 9.0));
assertEquals(200, HifzCadence.targetFiveLineCapacity(30, 9.0));
assertEquals(65, HifzCadence.targetFiveLineCapacity(10, 9.0));
assertEquals(30, HifzCadence.targetFiveLineCapacity(5, 9.0));
```

Also assert recalibration rejection:

```java
assertEquals(9.0, HifzCadence.recalibrate(9.0, 19, 600_000L), 0.0001);
assertEquals(9.0, HifzCadence.recalibrate(9.0, 100, 299_000L), 0.0001);
```

And 70/30 + ±10% behavior using values that exercise both unclamped and clamped results.

- [ ] **Step 2: Run the new test and confirm RED**

Run equivalent of:

```text
./gradlew :hifz-app:testDebugUnitTest --tests com.quransafeguard.hifz.preview.HifzCadenceTest
```

Expected: FAIL because `HifzCadence` does not exist.

- [ ] **Step 3: Implement the minimal pure helper**

Core rules:

```java
private static final double INITIAL = PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING;

static double sanitizedSecondsPerLine(double value) {
    return value > 0.0 && Double.isFinite(value) ? value : INITIAL;
}

static int targetLines(int minutes, double secondsPerLine) {
    double safe = sanitizedSecondsPerLine(secondsPerLine);
    return Math.max(1, (int)Math.floor(minutes * 60.0 / safe));
}

static int targetFiveLineCapacity(int minutes, double secondsPerLine) {
    int lines = targetLines(minutes, secondsPerLine);
    int blocks = Math.max(1, lines / PreviewConfig.SABQI_LINES);
    return blocks * PreviewConfig.SABQI_LINES;
}
```

`recalibrate` must preserve the existing thresholds, 70/30 blend, and ±10% clamp exactly.

`advisoryFiveLineRange` must call `targetFiveLineCapacity(5, ...)` and `targetFiveLineCapacity(10, ...)` rather than reimplement formulas.

- [ ] **Step 4: Run targeted tests GREEN**

Expected: all `HifzCadenceTest` tests pass.

- [ ] **Step 5: Run existing `PreviewConfigTest`**

Expected: ×37, ×40 and recent-loop timer tests remain green.

- [ ] **Step 6: Commit**

Commit message:

```text
feat(hifz): centralize cadence workload calculations
```

---

### Task 3: Make Murājaʿah cadence the only active workload reference

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java` only if removing the now-unused recent initial constant is safe
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- Consumes: `HifzCadence` from Task 2.
- Produces: runtime planning where both old Murājaʿah and recent-window capacity consume `prefs.murajaahSecondsPerLine()`.

- [ ] **Step 1: Add a failing regression contract**

Add assertions that runtime source:

```java
assertTrue(session.contains("HifzCadence.targetLines"));
assertTrue(session.contains("HifzCadence.targetFiveLineCapacity"));
assertFalse(session.contains("recentSecondsPerLine()"));
```

And persistence/source contract:

```java
assertFalse("legacy recent cadence must not remain an active API",
    prefs.contains("public double recentSecondsPerLine()"));
assertFalse(prefs.contains("public void setRecentSecondsPerLine"));
```

- [ ] **Step 2: Run regression test RED**

Expected: failure because the old recent cadence API and formulas are still present.

- [ ] **Step 3: Replace runtime formulas**

In recent-window rebalance:

```java
int capacity = HifzCadence.targetFiveLineCapacity(
    recentWindowMinutes,
    prefs.murajaahSecondsPerLine()
);
```

In Murājaʿah plan:

```java
int lines = HifzCadence.targetLines(targetMinutes(), prefs.murajaahSecondsPerLine());
```

In old-speed calibration:

```java
double next = HifzCadence.recalibrate(
    prefs.murajaahSecondsPerLine(), lines, elapsedMs
);
prefs.setMurajaahSecondsPerLine(next);
```

Remove the duplicated `smoothedClamped` method from the Activity.

- [ ] **Step 4: Retire the active recent-cadence API non-destructively**

Remove public runtime getter/setter methods for `recentSecPerLine`. Do not bump schema and do not require deleting the legacy SharedPreferences key. Existing stored data may remain inert.

If `INITIAL_RECENT_SECONDS_PER_LINE_WORKING` has no remaining references, remove it. If a migration compatibility test still references it, retain only as a deprecated legacy constant with a comment stating it must not drive runtime planning.

- [ ] **Step 5: Run targeted tests GREEN**

Run `HifzCadenceTest`, `PreviewConfigTest`, and `OfficialReleaseContractTest`.

- [ ] **Step 6: Commit**

Commit message:

```text
feat(hifz): use Murajaah cadence for all workload sizing
```

---

### Task 4: Add the Wed/Fri advisory without changing the canonical schedule

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java`
- Create or extend test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- Consumes: `HifzCadence.advisoryFiveLineRange(...)`, `prefs.recentSabqi()`, current weekday.
- Produces: one compact informational row; no new session mode or completion state.

- [ ] **Step 1: Add failing source-contract tests**

Require a dedicated method such as:

```java
private String recentSabqiAdvisory(LocalDate date)
```

Contract assertions must ensure:

- Wednesday/Friday check exists;
- `HifzCadence.advisoryFiveLineRange` is used;
- no new `SessionKind` or `HifzSchedule` mutation is introduced;
- advisory is conditioned on non-empty `recentSabqi()`.

- [ ] **Step 2: Run RED**

Expected: advisory method absent.

- [ ] **Step 3: Implement a compact informational row**

Add one `TextView` near Today, visually muted and hidden by default.

Logic:

```java
boolean advisoryDay = date.getDayOfWeek() == DayOfWeek.WEDNESDAY
    || date.getDayOfWeek() == DayOfWeek.FRIDAY;
if (!advisoryDay || prefs.recentSabqi().isEmpty()) return "";
int[] range = HifzCadence.advisoryFiveLineRange(prefs.murajaahSecondsPerLine());
return "Sabqi récent · 5–10 min · " + range[0] + "–" + range[1] + " lignes";
```

The row is not clickable and must not call `openMode`, write completion state, move cursors, promote ranges, or affect Today ordering.

- [ ] **Step 4: Run targeted tests GREEN**

Also verify the existing schedule assertions still see only morning/evening canonical sessions.

- [ ] **Step 5: Commit**

Commit message:

```text
feat(hifz): add cadence-based recent Sabqi advisory
```

---

### Task 5: Show effective Itqān corpus and pending ×40 read-only

**Files:**
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java`
- Modify: `hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java`
- Modify/Test: `hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java`

**Interfaces:**
- Produces from `HifzPrefs`:
  - `public List<VerseRange> effectiveItqanRanges()` = normalized base + promoted ranges.
  - existing `unconsolidatedPromotedRanges()` remains the pending ×40 source.
- Settings consumes these lists read-only.

- [ ] **Step 1: Add failing regression assertions**

Require:

```java
assertTrue(prefs.contains("effectiveItqanRanges()"));
assertTrue(settings.contains("Corpus Itqān réel"));
assertTrue(settings.contains("À consolider ×40"));
assertFalse(settings.contains("Modifier le corpus réel"));
assertFalse(settings.contains("Supprimer du corpus réel"));
```

- [ ] **Step 2: Run RED**

Expected: effective range API/UI absent.

- [ ] **Step 3: Add the normalized effective-range API**

Implementation:

```java
public List<VerseRange> effectiveItqanRanges() {
    ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
    all.addAll(promotedRanges());
    return normalizeRanges(all);
}
```

Then make `itqanWorkCorpus()` consume this method:

```java
return EligibleCorpus.Companion.of(effectiveItqanRanges());
```

This creates one canonical representation for settings and runtime.

- [ ] **Step 4: Add compact read-only Settings section**

Keep existing editable base range rows untouched. Below rotation start, add `Corpus Itqān réel` read-only rows built from `effectiveItqanRanges()` and an `À consolider ×40` summary built from `unconsolidatedPromotedRanges()`.

Show current Itqān and Murājaʿah cursors as read-only status text; no edit/delete controls on effective or pending ranges.

- [ ] **Step 5: Verify base-range edits do not mutate promoted ranges**

Add/retain tests around `setItqanRanges` source semantics: it writes only `itqanRanges`; promoted and unconsolidated keys are separate.

- [ ] **Step 6: Run targeted tests GREEN**

- [ ] **Step 7: Commit**

Commit message:

```text
feat(hifz): expose effective Itqan corpus read only
```

---

### Task 6: Full non-regression gate and contradictory audit

**Files:**
- Modify only if a test proves a defect.
- Update final audit/handoff documentation only after code SHA is frozen.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: one exact candidate SHA with complete CI evidence.

- [ ] **Step 1: Run the complete official candidate workflow**

Require all existing checks plus new Lot 2 tests to pass, including:

- unit tests;
- official release contract;
- Claude audit-fix contracts;
- random source-ink mask regression;
- mask stability/rosette fallback;
- vertical fit/no-crop contract;
- product boundary;
- 604-page/corpus/assets verification;
- APK assembly and inspection.

- [ ] **Step 2: Verify immutable release identity**

Confirm:

```text
package = com.quransafeguard.hifz
versionCode = 10
versionName = 0.7.3-boox
debuggable = false
```

Confirm no bundled MP3/audio payload and no Accessibility/Safeguard blocking surface.

- [ ] **Step 3: Run contradictory source review**

Search explicitly for:

```text
recentSecondsPerLine
setRecentSecondsPerLine
recentSecPerLine runtime reads
SABQI_TOTAL_REPS != 37
ITQAN_TOTAL_REPS != 40
new SessionKind for advisory
hidden cursor writes in promotion
unusedA / availableB / time transfer
```

Expected: no active second cadence, no schedule/repetition regression, no cursor jump.

- [ ] **Step 4: Compare code/assets against pre-Lot-2 baseline**

Expected application changes limited to:

- Gradle namespace fix;
- cadence helper/tests;
- cadence consumption;
- advisory UI;
- effective Itqān read-only UI/API;
- regression contracts/docs.

No Mushaf SVG/geometry/Tafsir/audio/reader-JS changes are expected.

- [ ] **Step 5: Record exact evidence**

Report exact:

- final candidate SHA;
- CI run/job;
- artifact ID/name;
- APK SHA-256;
- package/version/debuggable;
- automated crop status;
- outstanding physical BOOX gate.

- [ ] **Step 6: Final verdict**

Return `GO` only if the entire official CI and contradictory audit are green. Otherwise return `NO-GO` with the exact blocking evidence.
