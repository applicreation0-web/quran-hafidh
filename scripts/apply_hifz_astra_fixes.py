from pathlib import Path

ROOT = Path('.')


def replace_once(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected exactly one match, got {count}')
    p.write_text(text.replace(old, new, 1), encoding='utf-8')


def insert_before_final_brace(path, block):
    p = ROOT / path
    text = p.read_text(encoding='utf-8')
    at = text.rfind('\n}')
    if at < 0:
        raise SystemExit(f'{path}: final brace not found')
    p.write_text(text[:at] + block + text[at:], encoding='utf-8')


# B3 + M6: preserve exact in-progress protocol and repair corrupt queue entries.
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java',
    '''    public List<AnchoringQueue.Entry> anchoringQueue() {
        ArrayList<AnchoringQueue.Entry> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString("anchoringQueue", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(new AnchoringQueue.Entry(
                    o.getString("start"), o.getString("end"),
                    AnchoringQueue.Origin.valueOf(o.getString("origin")),
                    AnchoringQueue.Protocol.valueOf(o.getString("protocol")),
                    o.optInt("failures", 0)));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt anchoring queue", error);
        }
        return out;
    }
''',
    '''    public List<AnchoringQueue.Entry> anchoringQueue() {
        ArrayList<AnchoringQueue.Entry> out = new ArrayList<>();
        boolean repaired = false;
        try {
            JSONArray array = new JSONArray(p.getString("anchoringQueue", "[]"));
            for (int i = 0; i < array.length(); i++) {
                try {
                    JSONObject o = array.getJSONObject(i);
                    String start = o.getString("start");
                    String end = o.getString("end");
                    VerseRef startRef = GeometryRepository.parseVerse(start);
                    VerseRef endRef = GeometryRepository.parseVerse(end);
                    if (GeometryRepository.ordinal(startRef) > GeometryRepository.ordinal(endRef)) {
                        repaired = true;
                        continue;
                    }
                    out.add(new AnchoringQueue.Entry(
                        start, end,
                        AnchoringQueue.Origin.valueOf(o.getString("origin")),
                        AnchoringQueue.Protocol.valueOf(o.getString("protocol")),
                        Math.max(0, o.optInt("failures", 0))));
                } catch (Exception malformedEntry) {
                    repaired = true;
                }
            }
        } catch (Exception malformedQueue) {
            repaired = true;
        }
        if (repaired) {
            p.edit()
                .putString("anchoringQueue", anchoringQueueJson(out))
                .putBoolean("anchoringQueueInitialized", false)
                .commit();
        }
        return out;
    }
'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java',
    '''    public int anchoringQueueIndex() { return anchoringQueueIndex(anchoringQueue().size()); }

    public boolean anchoringDeferredToday() {''',
    '''    public int anchoringQueueIndex() { return anchoringQueueIndex(anchoringQueue().size()); }

    public AnchoringQueue.Entry anchoringEntryFor(VerseRef start, VerseRef endInclusive) {
        if (start == null || endInclusive == null) return null;
        return AnchoringQueue.findByRange(anchoringQueue(), start.toString(), endInclusive.toString());
    }

    public boolean anchoringDeferredToday() {'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java',
    '''        List<AnchoringQueue.Entry> queue = anchoringQueue();
        if (!queue.isEmpty() && p.getInt("itqanRep", 0) > 0) {''',
    '''        List<AnchoringQueue.Entry> queue = anchoringQueue();
        if (!p.getBoolean("anchoringQueueInitialized", false)) {
            if (!reconcileAnchoringQueue(geometry)) {
                throw new IllegalStateException("Unable to repair anchoring queue");
            }
            queue = anchoringQueue();
        }
        if (!queue.isEmpty() && p.getInt("itqanRep", 0) > 0) {'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java',
    '''            AnchoringQueue.Entry inProgress = AnchoringQueue.findByRange(
                prefs.anchoringQueue(), savedStart.toString(), savedEnd.toString());''',
    '''            AnchoringQueue.Entry inProgress = prefs.anchoringEntryFor(savedStart, savedEnd);'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java',
    '''                    boolean fractionated = prefs.isFractionatedUnit(g.versesForRange(start, end));
                    if (fractionated) {''',
    '''                    AnchoringQueue.Entry inProgress = prefs.anchoringEntryFor(start, end);
                    if (inProgress != null) entry = inProgress;
                    boolean fractionated = prefs.isFractionatedUnit(g.versesForRange(start, end));
                    if (fractionated) {'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java',
    '''        List<WeeklyDashboardPlanner.Row> rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, hostBudgetStore).week(LocalDate.now());
        for (int i = 0; i < rows.size(); i++) {''',
    '''        List<WeeklyDashboardPlanner.Row> rows;
        try {
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, hostBudgetStore).week(LocalDate.now());
        } catch (RuntimeException error) {
            dashboard.removeAllViews();
            TextView unavailable = Ui.text(this, "Semaine indisponible", 10.8f, false);
            unavailable.setTextColor(Ui.MUTED);
            unavailable.setPadding(Ui.dp(this, 4), Ui.dp(this, 3), Ui.dp(this, 4), Ui.dp(this, 3));
            dashboard.addView(unavailable);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {'''
)

# M1/M2: production J10 cap is five physical lines; Ancrage is reusable capacity/host time.
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java',
    '    static final int MAX_PRIORITY_LINES = 15;',
    '    static final int MAX_PRIORITY_LINES = 5;'
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java',
    '''        return kind == SessionKind.SABQI_TODAY_REVIEW
            || kind == SessionKind.RECENT_SABQI_REVIEW''',
    '''        return kind == SessionKind.SABQI_TODAY_REVIEW
            || kind == SessionKind.ITQAN
            || kind == SessionKind.RECENT_SABQI_REVIEW'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10HostBudgetStore.java',
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)''',
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.ITQAN.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java',
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(hostMode)) kind = SessionKind.SABQI_TODAY_REVIEW;
        else if (HifzSessionActivity.RECENT_SABQI_REVIEW.equals(hostMode)) kind = SessionKind.RECENT_SABQI_REVIEW;
        else kind = SessionKind.OLD_ITQAN_MURAJAAH;''',
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(hostMode)) kind = SessionKind.SABQI_TODAY_REVIEW;
        else if (HifzSessionActivity.ITQAN.equals(hostMode)) kind = SessionKind.ITQAN;
        else if (HifzSessionActivity.RECENT_SABQI_REVIEW.equals(hostMode)) kind = SessionKind.RECENT_SABQI_REVIEW;
        else kind = SessionKind.OLD_ITQAN_MURAJAAH;'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java',
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
                || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)''',
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
                || HifzSessionActivity.ITQAN.equals(mode)
                || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)'''
)

# Minor m3: no timer-based immediate reopen after returning from J10; suppress exactly one host resume.
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    'import java.util.concurrent.ExecutorService;',
    'import java.util.Collections;\nimport java.util.Set;\nimport java.util.WeakHashMap;\nimport java.util.concurrent.ExecutorService;'
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''    private volatile boolean pendingReconcile;
    private LocalDate lastAlertDate;''',
    '''    private volatile boolean pendingReconcile;
    private final Set<Activity> suppressPriorityOnce = Collections.newSetFromMap(new WeakHashMap<>());
    private LocalDate lastAlertDate;'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''    @Override public void onActivityResumed(Activity activity) {
        if (activity instanceof J10ReviewActivity) return;

        if (activity instanceof HifzSessionActivity) {''',
    '''    @Override public void onActivityResumed(Activity activity) {
        if (activity instanceof J10ReviewActivity) {
            openingPriority = false;
            return;
        }

        if (activity instanceof HifzSessionActivity) {'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''                if (new J10HostBudgetStore(this).isSlotConsumed(hostMode, LocalDate.now())) {
                    activity.finish();
                    return;
                }

                J10ReviewPlanner p = ensurePlanner();''',
    '''                if (new J10HostBudgetStore(this).isSlotConsumed(hostMode, LocalDate.now())) {
                    suppressPriorityOnce.remove(activity);
                    activity.finish();
                    return;
                }
                if (suppressPriorityOnce.remove(activity)) return;

                J10ReviewPlanner p = ensurePlanner();'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''                    openingPriority = true;
                    Intent intent = new Intent(activity, J10ReviewActivity.class)
                        .putExtra(J10ReviewActivity.EXTRA_HOST_MODE, hostMode);
                    activity.startActivity(intent);
                    activity.getWindow().getDecorView().postDelayed(() -> openingPriority = false, 500L);''',
    '''                    openingPriority = true;
                    suppressPriorityOnce.add(activity);
                    Intent intent = new Intent(activity, J10ReviewActivity.class)
                        .putExtra(J10ReviewActivity.EXTRA_HOST_MODE, hostMode);
                    activity.startActivity(intent);'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''            } catch (RuntimeException error) {
                openingPriority = false;
                Log.e("QuranHifz", "Unable to evaluate J10 priority", error);''',
    '''            } catch (RuntimeException error) {
                openingPriority = false;
                suppressPriorityOnce.remove(activity);
                Log.e("QuranHifz", "Unable to evaluate J10 priority", error);'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '''    private static boolean isReusableJ10Host(String mode) {
        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)''',
    '''    static boolean isReusableJ10Host(String mode) {
        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)
            || HifzSessionActivity.ITQAN.equals(mode)
            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)'''
)
replace_once(
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java',
    '    @Override public void onActivityDestroyed(Activity activity) {}',
    '    @Override public void onActivityDestroyed(Activity activity) { suppressPriorityOnce.remove(activity); }'
)

# Behavioral tests required by Claude.
replace_once(
    'hifz-app/src/test/java/com/quransafeguard/hifz/preview/J10CapacityTest.java',
    '''    @Test public void tenDayCapacityUsesOnlyReusableTimedReviewSessions() {
        LocalDate sunday = LocalDate.of(2026, 9, 13);
        assertEquals(450, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 36, true));
        assertEquals(390, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 0, false));
    }
''',
    '''    @Test public void tenDayCapacityIncludesAnchoringAsReusableJ10Time() {
        LocalDate sunday = LocalDate.of(2026, 9, 13);
        assertEquals(690, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 36, true));
        assertEquals(750, J10ReviewPlanner.scheduledCapacityMinutes(sunday, 10, 0, false));
    }

    @Test public void productionPriorityIsCappedAtFivePhysicalLines() {
        assertEquals(5, J10ReviewPlanner.MAX_PRIORITY_LINES);
    }
'''
)
replace_once(
    'hifz-app/src/test/java/com/quransafeguard/hifz/preview/J10CapacityTest.java',
    'import static org.junit.Assert.assertEquals;',
    'import static org.junit.Assert.assertEquals;\nimport static org.junit.Assert.assertTrue;'
)
insert_before_final_brace(
    'hifz-app/src/test/java/com/quransafeguard/hifz/preview/J10CapacityTest.java',
    '''
    @Test public void anchoringIsAReusableJ10HostInProductionPolicy() {
        assertTrue(J10ReviewPlanner.isReusableJ10Kind(com.quransafeguard.hifz.core.SessionKind.ITQAN));
        assertTrue(QuranHifzApp.isReusableJ10Host(HifzSessionActivity.ITQAN));
    }
'''
)

insert_before_final_brace(
    'hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzPrefsV4MigrationInstrumentedTest.java',
    '''
    @Test public void corruptAnchoringEntryIsIgnoredPersistedAndRebuiltFromPendingCorpus() {
        HifzPrefs prefs = new HifzPrefs(context);
        GeometryRepository geometry = GeometryRepository.get(context);
        AnchoringQueue.Entry initial = prefs.currentAnchoringEntry(geometry);
        assertTrue(initial != null);

        String mixed = "[{\\\"start\\\":\\\"49:1\\\",\\\"end\\\":\\\"49:5\\\",\\\"origin\\\":\\\"RECONSTRUCTION\\\",\\\"protocol\\\":\\\"LIGHT\\\",\\\"failures\\\":0},"
            + "{\\\"start\\\":\\\"bad\\\",\\\"end\\\":\\\"bad\\\",\\\"origin\\\":\\\"FUTURE_ENUM\\\",\\\"protocol\\\":\\\"LIGHT\\\"}]";
        raw.edit().putString("anchoringQueue", mixed).putBoolean("anchoringQueueInitialized", true).commit();

        java.util.List<AnchoringQueue.Entry> recovered = prefs.anchoringQueue();
        assertEquals(1, recovered.size());
        assertFalse(raw.getBoolean("anchoringQueueInitialized", true));

        AnchoringQueue.Entry rebuilt = prefs.currentAnchoringEntry(geometry);
        assertTrue(rebuilt != null);
        assertTrue(raw.getBoolean("anchoringQueueInitialized", false));
    }

    @Test public void malformedAnchoringQueueFailsOpenAndReconcilesInsteadOfCrashing() {
        HifzPrefs prefs = new HifzPrefs(context);
        raw.edit().putString("anchoringQueue", "not-json").putBoolean("anchoringQueueInitialized", true).commit();

        assertTrue(prefs.anchoringQueue().isEmpty());
        assertFalse(raw.getBoolean("anchoringQueueInitialized", true));
        assertTrue(prefs.currentAnchoringEntry(GeometryRepository.get(context)) != null);
    }
'''
)
insert_before_final_brace(
    'hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzFractionatedAnchoringInstrumentedTest.java',
    '''
    @Test public void threeBlockFractionatedProgressionResetsEachBlockAndLeavesQueueOnlyAtTheEnd() {
        HifzPrefs prefs = new HifzPrefs(context);
        VerseRef start = new VerseRef(53, 1);
        VerseRef end = new VerseRef(53, 26);
        String range = "[{\\\"start\\\":\\\"53:1\\\",\\\"end\\\":\\\"53:26\\\"}]";
        String queue = "[{\\\"start\\\":\\\"53:1\\\",\\\"end\\\":\\\"53:26\\\",\\\"origin\\\":\\\"RECONSTRUCTION\\\",\\\"protocol\\\":\\\"LIGHT\\\",\\\"failures\\\":0}]";
        raw.edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("itqanCursor", "53:1")
            .commit();

        assertTrue(prefs.setItqanProgress(35, 3, 1, start, end));
        assertTrue(prefs.advanceItqanBlock(1, "2026-09-13", "bloc 1/3"));
        assertEquals(1, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(1, prefs.anchoringQueue().size());

        assertTrue(prefs.setItqanProgress(35, 2, 0, start, end));
        assertTrue(prefs.advanceItqanBlock(2, "2026-09-14", "bloc 2/3"));
        assertEquals(2, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertEquals(1, prefs.anchoringQueue().size());

        assertTrue(prefs.setItqanProgress(35, 1, 0, start, end));
        assertTrue(prefs.completeItqanUnitAndConsolidate(start, end, new VerseRef(2, 1),
            "2026-09-15", "bloc 3/3"));
        assertEquals(0, prefs.itqanBlockIndex());
        assertEquals(0, prefs.itqanRep());
        assertTrue(prefs.anchoringQueue().isEmpty());
        assertTrue(prefs.murajaahCorpus().contains(start));
    }
'''
)

(ROOT / 'hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/J10HostBudgetStoreInstrumentedTest.java').write_text('''package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10HostBudgetStoreInstrumentedTest {
    private static final String STORE = "quran_hifz_j10_host_v1";
    private Context context;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After public void tearDown() {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void anchoringCanHostJ10WithoutCreditingNormalAnchoringCompletion() {
        J10HostBudgetStore store = new J10HostBudgetStore(context);
        LocalDate day = LocalDate.of(2026, 9, 15);

        assertTrue(store.addConsumed(HifzSessionActivity.ITQAN, day, 90_000L));
        assertEquals(90_000L, store.consumedMs(HifzSessionActivity.ITQAN, day));
        assertTrue(store.markSlotConsumed(HifzSessionActivity.ITQAN, day));
        assertTrue(store.isSlotConsumed(HifzSessionActivity.ITQAN, day));
        assertFalse(store.isSlotConsumed(HifzSessionActivity.ITQAN, day.plusDays(1)));
    }
}
''', encoding='utf-8')

# Minor m4: align class/test names with the CDC naming without changing calibration behavior.
for rel in [
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSpeedStore.java',
    'hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java',
]:
    p = ROOT / rel
    p.write_text(p.read_text(encoding='utf-8').replace('SpeedCalibrationPolicy', 'SpeedCalibration'), encoding='utf-8')
old = ROOT / 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpeedCalibrationPolicy.java'
new = ROOT / 'hifz-app/src/main/java/com/quransafeguard/hifz/preview/SpeedCalibration.java'
new.write_text(old.read_text(encoding='utf-8').replace('SpeedCalibrationPolicy', 'SpeedCalibration'), encoding='utf-8')
old.unlink()
old = ROOT / 'hifz-app/src/test/java/com/quransafeguard/hifz/preview/SpeedCalibrationPolicyTest.java'
new = ROOT / 'hifz-app/src/test/java/com/quransafeguard/hifz/preview/SpeedCalibrationTest.java'
new.write_text(old.read_text(encoding='utf-8').replace('SpeedCalibrationPolicyTest', 'SpeedCalibrationTest').replace('SpeedCalibrationPolicy', 'SpeedCalibration'), encoding='utf-8')
old.unlink()

# m2 is now an instrumented JUnit gate; remove the old source-string script.
old_script = ROOT / 'scripts/test_j10_host_slot_override.py'
if old_script.exists():
    old_script.unlink()

# M5: remove all temporary TDD/Astra audit workflows from the final candidate tree.
for name in [
    'hifz-j10-tdd.yml',
    'hifz-astra-red.yml',
    'hifz-astra-micro.yml',
    'hifz-astra-fix-fast.yml',
    'hifz-astra-apply.yml',
    'hifz-claude-audit-fix.yml',
]:
    p = ROOT / '.github/workflows' / name
    if p.exists():
        p.unlink()

print('CLAUDE_COMPLETE_CORRECTION_SET_APPLIED')
