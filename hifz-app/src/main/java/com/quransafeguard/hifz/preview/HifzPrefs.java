package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Versioned local persistence. Structured Hifz and free memorization are intentionally isolated. */
public final class HifzPrefs {
    public static final class RecentSabqi {
        public final int startLine;
        public final int endLine;
        public final LocalDate addedOn;
        public final int reviewStreak;

        RecentSabqi(int startLine, int endLine) {
            this(startLine, endLine, HifzClock.today(), 0);
        }

        RecentSabqi(int startLine, int endLine, LocalDate addedOn) {
            this(startLine, endLine, addedOn, 0);
        }

        RecentSabqi(int startLine, int endLine, LocalDate addedOn, int reviewStreak) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.addedOn = addedOn == null ? HifzClock.today() : addedOn;
            this.reviewStreak = Math.max(0, reviewStreak);
        }

        @Override public String toString() { return startLine + "–" + endLine; }
    }


    private static final String NAME = "quran_hifz_preview_v1";
    private static final String LEGACY_GATES = "hifz_preview_session_gates";
    private static final Object V6_STATE_LOCK = new Object();
    private static volatile String migrationFaultPointForTest;
    private final SharedPreferences p;

    static void setMigrationFaultPointForTest(String point) {
        if (point != null
                && !"BEFORE_MAIN_COMMIT".equals(point)
                && !"AFTER_MAIN_COMMIT".equals(point)) {
            throw new IllegalArgumentException("Unknown migration fault point: " + point);
        }
        migrationFaultPointForTest = point;
    }

    private static void maybeInterruptMigrationForTest(String point) {
        if (point != null && point.equals(migrationFaultPointForTest)) {
            throw new IllegalStateException("Injected Hifz migration interruption at " + point);
        }
    }

    public HifzPrefs(Context context) {
        p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        ensureSchema(context);
        migrateLegacyGates(context);
    }

    private void ensureSchema(Context context) {
        int schema = p.getInt("schema", 0);
        if (schema == 0) {
            SharedPreferences.Editor e = p.edit()
                .putInt("schema", PreviewConfig.SCHEMA_VERSION)
                .putString("programStartDate", HifzClock.today().toString())
                .putString("lowerBound", "2:1")
                .putString("promotedFrontier", "2:74")
                .putString("upperTailStart", "49:1")
                .putString("sabqiStart", "2:75")
                .putString("sabqiEnd", "2:286")
                .putString("itqanRanges", defaultItqanRangesJson())
                .putString("promotedRanges", bootstrapReconstructionJson())
                .putString("unconsolidatedPromotedRanges", bootstrapReconstructionJson())
                .putString("legacyMurajaahPromotedRanges", "[]")
                .putString("forcedPromotedRanges", "[]")
                .putString("anchoringQueue", "[]")
                .putBoolean("anchoringQueueInitialized", false)
                .putInt("anchoringQueueIndex", 0)
                .putString("anchoringRetryAfterDate", "")
                .putString("hardAnchoringSurahs", "[]")
                .putInt("itqanBlockIndex", 0)
                .putString("itqanRotationStart", "49:1")
                .putString("itqanCursor", "49:1")
                .putString("murajaahCursor", "2:1")
                .putInt("sabqiLineCursor", -1)
                .putInt("sabqiRep", 0)
                .putInt("sabqiAssisted", 0)
                .putInt("itqanRep", 0)
                .putInt("itqanAssisted", 0)
                .putInt("itqanFinalReveals", 0)
                .putString("itqanUnitStart", "")
                .putString("itqanUnitEnd", "")
                .putString("recentSabqi", "[]")
                .putString("recentConsolidationActivatedOn", "")
                .putString("consolidationAttendanceDates", "[]")
                .putLong("sabqiElapsedMs", 0L)
                .putLong("itqanElapsedMs", 0L)
                .putLong("sabqi_today_reviewElapsedMs", 0L)
                .putLong("recent_sabqi_reviewElapsedMs", 0L)
                .putLong("murajaahElapsedMs", 0L)
                .putString("sabqiTodayReviewDate", "")
                .putInt("sabqiTodayReviewStartLine", -1)
                .putInt("sabqiTodayReviewEndLine", -1)
                .putString("lastSabqiTodayReviewDate", "")
                .putString("lastSabqiTodayReviewLabel", "")
                .putInt("recentSabqiReviewIndex", 0)
                .putString("lastRecentSabqiReviewDate", "")
                .putString("lastRecentSabqiReviewLabel", "")
                .putString("murajaahActualEnd", "")
                .putFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING)
                .putFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING)
                .putBoolean("murajaahSpeedCalibrated", false)
                .putInt("murajaahSpeedSamples", 0)
                .putBoolean("recentSpeedCalibrated", false)
                .putInt("recentSpeedSamples", 0)
                .putBoolean("forceEink", false)
                .putString("lastSabqiDate", "")
                .putString("lastSabqiLabel", "")
                .putString("lastItqanDate", "")
                .putString("lastItqanLabel", "")
                .putString("lastItqanCreditStart", "")
                .putString("lastItqanCreditEnd", "")
                .putInt("lastItqanCreditBlockIndex", -1)
                .putString("lastMurajaahDate", "")
                .putString("lastMurajaahLabel", "")
                .putString("lastMurajaahCreditStart", "")
                .putString("lastMurajaahCreditEnd", "")
                .putString("v6LearnedLineIds", "[]")
                .putString("v6StabilizedLineIds", "[]")
                .putString("v6AcquiredCreditLineIds", "[]")
                .putString("v6LegacyPartialAcquiredLineIds", "[]")
                .putString("v6QuarantineLineIds", "[]")
                .putString("v6QuarantineLegacyLastReviewed", "{}")
                .putString("v6ActiveJ10LastReviewed", "{}")
                .putString("v6UnknownDueLineIds", "[]")
                .putString("v6LegacyImportedLineIds", "[]")
                .putString("v6LegacyOrphanJ10Dates", "{}");
            if (!e.commit()) throw new IllegalStateException("Unable to initialize Hifz schema");
            return;
        }
        if (schema == 1) {
            migrateV1ToV2();
            schema = 2;
        }
        if (schema == 2) {
            migrateV2ToV3();
            schema = 3;
        }
        if (schema == 3) {
            migrateV3ToV4();
            schema = 4;
        }
        if (schema == 4) {
            migrateV4ToV5();
            schema = 5;
        }
        if (schema == 5) {
            synchronized (J10ReviewStore.LEGACY_STORE_LOCK) {
                migrateV5ToV6(context);
                schema = 6;
            }
        }
        if (schema != PreviewConfig.SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported Hifz preview schema: " + schema);
        }
        boolean repairV5 = p.contains("stableRecentLines")
            || !p.contains("consolidationAttendanceDates")
            || !p.contains("forcedPromotedRanges")
            || !p.contains("anchoringRetryAfterDate")
            || !p.contains("hardAnchoringSurahs")
            || !p.contains("itqanBlockIndex")
            || !p.contains("lastItqanCreditStart")
            || !p.contains("lastItqanCreditEnd")
            || !p.contains("lastItqanCreditBlockIndex")
            || !p.contains("lastMurajaahCreditStart")
            || !p.contains("lastMurajaahCreditEnd");
        if (repairV5) {
            SharedPreferences.Editor repair = p.edit().remove("stableRecentLines");
            if (!p.contains("consolidationAttendanceDates")) repair.putString("consolidationAttendanceDates", "[]");
            if (!p.contains("forcedPromotedRanges")) repair.putString("forcedPromotedRanges", "[]");
            if (!p.contains("anchoringRetryAfterDate")) repair.putString("anchoringRetryAfterDate", "");
            if (!p.contains("hardAnchoringSurahs")) repair.putString("hardAnchoringSurahs", "[]");
            if (!p.contains("itqanBlockIndex")) repair.putInt("itqanBlockIndex", 0);
            if (!p.contains("lastItqanCreditStart")) repair.putString("lastItqanCreditStart", "");
            if (!p.contains("lastItqanCreditEnd")) repair.putString("lastItqanCreditEnd", "");
            if (!p.contains("lastItqanCreditBlockIndex")) repair.putInt("lastItqanCreditBlockIndex", -1);
            if (!p.contains("lastMurajaahCreditStart")) repair.putString("lastMurajaahCreditStart", "");
            if (!p.contains("lastMurajaahCreditEnd")) repair.putString("lastMurajaahCreditEnd", "");
            if (!repair.commit()) throw new IllegalStateException("Unable to repair Hifz schema v5 optional state");
        }
    }

    private void migrateV5ToV6(Context context) {
        if (context == null) throw new IllegalStateException("Context required for schema v5 to v6 migration");

        boolean calibrated = p.getBoolean("murajaahSpeedCalibrated", false);
        double existing = p.getFloat("murajaahSecPerLine", 9.0f);
        double migrated = HifzV6Migration.migratedMaintenanceSecondsPerLine(existing, calibrated);

        GeometryRepository geometry = GeometryRepository.get(context);
        ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));

        LinkedHashSet<String> pendingLineIds = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(unconsolidatedPromotedRanges(), allLines));

        LinkedHashSet<String> legacyStableLineIds = new LinkedHashSet<>();
        EligibleCorpus legacyStableCorpus = murajaahCorpus();
        for (GeometryRepository.LineMeta line : allLines) {
            if (legacyStableCorpus.contains(CorpusLinePolicy.ownerVerse(line))) {
                legacyStableLineIds.add(line.id);
            }
        }

        LinkedHashMap<String, Long> recentSabqiAddedOnEpochDays = new LinkedHashMap<>();
        try {
            JSONArray recent = new JSONArray(p.getString("recentSabqi", "[]"));
            for (int itemIndex = 0; itemIndex < recent.length(); itemIndex++) {
                JSONObject item = recent.getJSONObject(itemIndex);
                int startLine = Math.max(0, item.getInt("start"));
                int endLine = Math.min(geometry.lineCount() - 1, item.getInt("end"));
                LocalDate addedOn = safeDate(item.optString("addedOn", ""), null);
                for (int lineIndex = startLine; lineIndex <= endLine; lineIndex++) {
                    String lineId = geometry.line(lineIndex).id;
                    legacyStableLineIds.add(lineId);
                    if (addedOn != null) recentSabqiAddedOnEpochDays.put(lineId, addedOn.toEpochDay());
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt recent Sabqi queue during schema v6 migration", error);
        }

        LinkedHashMap<String, Long> legacyJ10EpochDays = new LinkedHashMap<>();
        for (java.util.Map.Entry<String, LocalDate> entry : new J10ReviewStore(context).snapshot().entrySet()) {
            legacyJ10EpochDays.put(entry.getKey(), entry.getValue().toEpochDay());
        }

        LinkedHashSet<String> structurallyCompletedPendingLineIds = new LinkedHashSet<>();
        int completedBlocks = Math.max(0, p.getInt("itqanBlockIndex", 0));
        VerseRef unitStart = optionalRef("itqanUnitStart");
        VerseRef unitEnd = optionalRef("itqanUnitEnd");
        if (completedBlocks > 0 && unitStart != null && unitEnd != null) {
            List<VerseRef> unitVerses = geometry.versesForRange(unitStart, unitEnd);
            if (containsHardAnchoringSurah(hardAnchoringSurahs(), unitVerses)) {
                List<String> unitLines = geometry.lineIdsForVerseRange(unitStart, unitEnd);
                int[] segments = geometry.surahSegmentLineCounts(unitStart, unitEnd);
                int blockCount = PreviewConfig.fractionatedBlockCount(segments);
                for (int block = 0; block < Math.min(completedBlocks, blockCount); block++) {
                    int from = PreviewConfig.fractionatedBlockStart(segments, block);
                    int length = PreviewConfig.fractionatedBlockLength(segments, block);
                    int through = Math.min(unitLines.size(), from + length);
                    for (int lineIndex = Math.max(0, from); lineIndex < through; lineIndex++) {
                        String lineId = unitLines.get(lineIndex);
                        if (pendingLineIds.contains(lineId)) structurallyCompletedPendingLineIds.add(lineId);
                    }
                }
            }
        }

        HifzCorpusState state = HifzV6Migration.classify(new HifzV6Migration.Input(
            pendingLineIds,
            structurallyCompletedPendingLineIds,
            legacyStableLineIds,
            legacyJ10EpochDays,
            recentSabqiAddedOnEpochDays));

        maybeInterruptMigrationForTest("BEFORE_MAIN_COMMIT");

        SharedPreferences.Editor e = p.edit()
            .putString("v6LearnedLineIds", lineIdsJson(state.toAnchorLineIds()))
            .putString("v6StabilizedLineIds", "[]")
            .putString("v6AcquiredCreditLineIds", lineIdsJson(state.acquiredCreditLineIds()))
            .putString("v6LegacyPartialAcquiredLineIds", lineIdsJson(state.legacyPartialAcquiredLineIds()))
            .putString("v6QuarantineLineIds", lineIdsJson(state.quarantineLineIds()))
            .putString("v6QuarantineLegacyLastReviewed", epochDayMapJson(state.quarantineLegacyLastReviewed()))
            .putString("v6ActiveJ10LastReviewed", epochDayMapJson(state.activeLastReviewedEpochDays()))
            .putString("v6UnknownDueLineIds", lineIdsJson(state.unknownDueLineIds()))
            .putString("v6LegacyImportedLineIds", lineIdsJson(state.legacyImportedLineIds()))
            .putString("v6LegacyOrphanJ10Dates", epochDayMapJson(state.legacyOrphanDates()))
            .putFloat("murajaahSecPerLine", (float) migrated)
            .putInt("schema", 6);
        if (!e.commit()) {
            throw new IllegalStateException("Unable to migrate Hifz schema v5 to v6");
        }

        maybeInterruptMigrationForTest("AFTER_MAIN_COMMIT");

        if (p.getInt("schema", -1) != 6
                || !p.contains("v6LearnedLineIds")
                || !p.contains("v6AcquiredCreditLineIds")
                || !p.contains("v6ActiveJ10LastReviewed")) {
            throw new IllegalStateException("Incomplete Hifz schema v6 migration commit");
        }
    }

    void resolveV6Quarantine(
            String lineId,
            HifzV6Migration.QuarantineResolution resolution) {
        if (lineId == null || lineId.isEmpty()) throw new IllegalArgumentException("lineId required");
        if (resolution == null) throw new IllegalArgumentException("resolution required");

        synchronized (V6_STATE_LOCK) {
            if (p.getInt("schema", -1) != 6) {
                throw new IllegalStateException("Schema 6 required for persisted conflict resolution");
            }

            HifzCorpusState current = schema6CorpusState();
            HifzCorpusState resolved = HifzV6Migration.resolveQuarantine(current, lineId, resolution);
            String stabilized = requiredV6String("v6StabilizedLineIds");

            SharedPreferences.Editor e = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(resolved.toAnchorLineIds()))
                .putString("v6StabilizedLineIds", stabilized)
                .putString("v6AcquiredCreditLineIds", lineIdsJson(resolved.acquiredCreditLineIds()))
                .putString("v6LegacyPartialAcquiredLineIds", lineIdsJson(resolved.legacyPartialAcquiredLineIds()))
                .putString("v6QuarantineLineIds", lineIdsJson(resolved.quarantineLineIds()))
                .putString("v6QuarantineLegacyLastReviewed", epochDayMapJson(resolved.quarantineLegacyLastReviewed()))
                .putString("v6ActiveJ10LastReviewed", epochDayMapJson(resolved.activeLastReviewedEpochDays()))
                .putString("v6UnknownDueLineIds", lineIdsJson(resolved.unknownDueLineIds()))
                .putString("v6LegacyImportedLineIds", lineIdsJson(resolved.legacyImportedLineIds()))
                .putString("v6LegacyOrphanJ10Dates", epochDayMapJson(resolved.legacyOrphanDates()));
            if (!e.commit()) {
                throw new IllegalStateException("Unable to persist schema v6 conflict resolution");
            }
            if (p.getInt("schema", -1) != 6) {
                throw new IllegalStateException("Schema changed during persisted conflict resolution");
            }
        }
    }

    private HifzCorpusState schema6CorpusState() {
        return new HifzCorpusState(
            v6LineIdSet("v6LearnedLineIds"),
            v6LineIdSet("v6LegacyPartialAcquiredLineIds"),
            v6LineIdSet("v6QuarantineLineIds"),
            v6EpochDayMap("v6QuarantineLegacyLastReviewed"),
            v6EpochDayMap("v6ActiveJ10LastReviewed"),
            v6LineIdSet("v6UnknownDueLineIds"),
            v6LineIdSet("v6LegacyImportedLineIds"),
            v6EpochDayMap("v6LegacyOrphanJ10Dates"),
            v6LineIdSet("v6AcquiredCreditLineIds")
        );
    }

    private String requiredV6String(String key) {
        if (!p.contains(key)) throw new IllegalStateException("Missing schema v6 state: " + key);
        String raw = p.getString(key, null);
        if (raw == null) throw new IllegalStateException("Missing schema v6 state: " + key);
        return raw;
    }

    private LinkedHashSet<String> v6LineIdSet(String key) {
        try {
            JSONArray array = new JSONArray(requiredV6String(key));
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (int i = 0; i < array.length(); i++) result.add(array.getString(i));
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt schema v6 line state: " + key, error);
        }
    }

    private LinkedHashMap<String, Long> v6EpochDayMap(String key) {
        try {
            JSONObject object = new JSONObject(requiredV6String(key));
            LinkedHashMap<String, Long> result = new LinkedHashMap<>();
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String lineId = keys.next();
                result.put(lineId, object.getLong(lineId));
            }
            return result;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt schema v6 date state: " + key, error);
        }
    }

    private static String lineIdsJson(Iterable<String> lineIds) {
        JSONArray array = new JSONArray();
        if (lineIds != null) for (String lineId : lineIds) array.put(lineId);
        return array.toString();
    }

    private static String epochDayMapJson(java.util.Map<String, Long> values) {
        JSONObject object = new JSONObject();
        try {
            if (values != null) {
                for (java.util.Map.Entry<String, Long> entry : values.entrySet()) {
                    object.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception error) {
            throw new IllegalStateException("Unable to serialize schema v6 J10 state", error);
        }
        return object.toString();
    }

    private void migrateV1ToV2() {
        VerseRef legacyFrontier = safeRef(p.getString("promotedFrontier", "2:74"), new VerseRef(2,74));
        ArrayList<VerseRange> promoted = new ArrayList<>();
        if (GeometryRepository.ordinal(legacyFrontier) > GeometryRepository.ordinal(new VerseRef(2,74))) {
            promoted.add(new VerseRange(new VerseRef(2,75), legacyFrontier));
        }
        SharedPreferences.Editor e = p.edit()
            .putString("sabqiStart", p.getString("sabqiStart", "2:75"))
            .putString("sabqiEnd", p.getString("sabqiEnd", "2:286"))
            .putString("itqanRanges", defaultItqanRangesJson())
            .putString("promotedRanges", rangesJson(promoted))
            .putString("itqanRotationStart", p.getString("itqanRotationStart", "49:1"))
            .putFloat("recentSecPerLine", p.getFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING))
            .putString("murajaahPhase", p.getString("murajaahPhase", "A"))
            .putString("murajaahActualEnd", p.getString("murajaahActualEnd", ""))
            .putInt("murajaahRecentLinesDone", p.getInt("murajaahRecentLinesDone", 0))
            .putLong("murajaahBlockAElapsedMs", p.getLong("murajaahBlockAElapsedMs", 0L))
            .putLong("murajaahBlockBElapsedMs", p.getLong("murajaahBlockBElapsedMs", 0L))
            .putInt("schema", 2);
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v1 to v2");
    }

    /**
     * Schema v3 introduces snowball consolidation state without moving any existing cursor.
     * Existing promoted ranges stay visible to Murajaah for migration compatibility, but are
     * also marked unconsolidated so their next natural-cycle Itqan encounter performs a real ×40.
     */
    private void migrateV2ToV3() {
        String historical = p.getString("promotedRanges", "[]");
        long legacyRecentElapsed = p.getLong("murajaahBlockAElapsedMs", 0L);
        long legacyOldElapsed = p.getLong("murajaahBlockBElapsedMs", 0L);
        String legacyActualEnd = p.getString("murajaahActualEnd", "");
        int legacyRecentIndex = Math.max(0, p.getInt("murajaahRecentLinesDone", 0) / PreviewConfig.SABQI_LINES);
        SharedPreferences.Editor e = p.edit()
            .putString("unconsolidatedPromotedRanges", historical == null ? "[]" : historical)
            .putString("legacyMurajaahPromotedRanges", historical == null ? "[]" : historical)
            .putLong("recent_sabqi_reviewElapsedMs", legacyRecentElapsed)
            .putInt("recentSabqiReviewIndex", legacyRecentIndex)
            .putLong("murajaahElapsedMs", legacyOldElapsed)
            .putString("murajaahActualEnd", legacyActualEnd == null ? "" : legacyActualEnd)
            .remove("murajaahPhase")
            .remove("murajaahRecentLinesDone")
            .remove("murajaahBlockAElapsedMs")
            .remove("murajaahBlockBElapsedMs")
            .putInt("schema", 3);
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v2 to v3");
    }

    /**
     * Schema v4 turns the historical upper tail into pending reconstruction work. The single
     * editor commit makes the corpus split, cursor repair and new persistent state atomic.
     */
    private void migrateV3ToV4() {
        VerseRef tailStart = new VerseRef(49, 1);
        VerseRef tailEnd = new VerseRef(114, 6);
        List<VerseRange> base = subtractCoverage(parseRanges("itqanRanges"), tailStart, tailEnd);
        if (base.isEmpty()) base = defaultItqanRanges();

        ArrayList<VerseRange> promoted = new ArrayList<>(promotedRanges());
        promoted.add(new VerseRange(tailStart, tailEnd));
        ArrayList<VerseRange> pending = new ArrayList<>(unconsolidatedPromotedRanges());
        pending.add(new VerseRange(tailStart, tailEnd));
        List<VerseRange> legacy = subtractCoverage(legacyMurajaahPromotedRanges(), tailStart, tailEnd);

        VerseRef murajaah = safeRef(p.getString("murajaahCursor", "2:1"), new VerseRef(2, 1));
        if (ordinalBetween(murajaah, tailStart, tailEnd)) murajaah = new VerseRef(2, 1);
        LocalDate migrationDay = HifzClock.today();
        String recent = normalizeRecentJson(p.getString("recentSabqi", "[]"), migrationDay);
        String activation = p.getString("recentConsolidationActivatedOn", "");
        if ((activation == null || activation.isEmpty())
                && recentCount(recent) >= HifzSchedule.RECENT_BLOCKS_FOR_SUNDAY_CONSOLIDATION) {
            activation = migrationDay.toString();
        }

        SharedPreferences.Editor e = p.edit()
            .putString("itqanRanges", rangesJson(normalizeRanges(base)))
            .putString("promotedRanges", rangesJson(normalizeRanges(promoted)))
            .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
            .putString("legacyMurajaahPromotedRanges", rangesJson(normalizeRanges(legacy)))
            .putString("forcedPromotedRanges", "[]")
            .putString("recentSabqi", recent)
            .putString("recentConsolidationActivatedOn", activation == null ? "" : activation)
            .putString("consolidationAttendanceDates", "[]")
            .putString("murajaahCursor", murajaah.toString())
            .putString("anchoringQueue", p.getString("anchoringQueue", "[]"))
            .putBoolean("anchoringQueueInitialized", p.getBoolean("anchoringQueueInitialized", false))
            .putInt("anchoringQueueIndex", Math.max(0, p.getInt("anchoringQueueIndex", 0)))
            .putString("anchoringRetryAfterDate", "")
            .putString("hardAnchoringSurahs", "[]")
            .putInt("itqanBlockIndex", 0)
            .putInt("itqanFinalReveals", Math.max(0, p.getInt("itqanFinalReveals", 0)))
            .putBoolean("murajaahSpeedCalibrated", p.getBoolean("murajaahSpeedCalibrated", false))
            .putInt("murajaahSpeedSamples", Math.max(0, p.getInt("murajaahSpeedSamples", 0)))
            .putBoolean("recentSpeedCalibrated", p.getBoolean("recentSpeedCalibrated", false))
            .putInt("recentSpeedSamples", Math.max(0, p.getInt("recentSpeedSamples", 0)))
            .remove("stableRecentLines")
            .putInt("schema", 4);
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v3 to v4");
    }

    /**
     * Schema v5 introduces structured J10 credit. Because v5 has never been published, a
     * fractionated cross-surah unit started under v4 is restarted at its unit boundary so a
     * legacy block index can never point at different physical lines after C23 segmentation.
     * Queue/corpus/unit bounds and all global cursors are deliberately left untouched.
     */
    private void migrateV4ToV5() {
        VerseRef start = optionalRef("itqanUnitStart");
        VerseRef end = optionalRef("itqanUnitEnd");
        boolean started = p.getInt("itqanBlockIndex", 0) > 0
            || p.getInt("itqanRep", 0) > 0
            || p.getInt("itqanAssisted", 0) > 0
            || p.getInt("itqanFinalReveals", 0) > 0
            || p.getLong("itqanElapsedMs", 0L) > 0L;
        boolean crossSurahFractionated = started && start != null && end != null
            && start.getSurah() != end.getSurah();

        SharedPreferences.Editor e = p.edit()
            .remove("stableRecentLines")
            .putString("lastItqanCreditStart", "")
            .putString("lastItqanCreditEnd", "")
            .putInt("lastItqanCreditBlockIndex", -1)
            .putString("lastMurajaahCreditStart", "")
            .putString("lastMurajaahCreditEnd", "")
            .putInt("schema", 5);
        if (!p.contains("consolidationAttendanceDates")) e.putString("consolidationAttendanceDates", "[]");
        if (!p.contains("forcedPromotedRanges")) e.putString("forcedPromotedRanges", "[]");
        if (!p.contains("anchoringRetryAfterDate")) e.putString("anchoringRetryAfterDate", "");
        if (!p.contains("hardAnchoringSurahs")) e.putString("hardAnchoringSurahs", "[]");
        if (!p.contains("itqanBlockIndex")) e.putInt("itqanBlockIndex", 0);
        if (crossSurahFractionated) {
            e.putInt("itqanBlockIndex", 0)
                .putInt("itqanRep", 0)
                .putInt("itqanAssisted", 0)
                .putInt("itqanFinalReveals", 0)
                .putLong("itqanElapsedMs", 0L);
        }
        if (!e.commit()) throw new IllegalStateException("Unable to migrate Hifz schema v4 to v5");
    }

    private static boolean rangeTouchesHardAnchoringSurah(VerseRef start, VerseRef end, List<Integer> hardSurahs) {
        if (start == null || end == null || hardSurahs == null || hardSurahs.isEmpty()) return false;
        int low = Math.min(start.getSurah(), end.getSurah());
        int high = Math.max(start.getSurah(), end.getSurah());
        for (Integer surah : hardSurahs) {
            if (surah != null && surah >= low && surah <= high) return true;
        }
        return false;
    }

    private void migrateLegacyGates(Context context) {
        SharedPreferences legacy = context.getSharedPreferences(LEGACY_GATES, Context.MODE_PRIVATE);
        if (legacy.getAll().isEmpty()) return;
        SharedPreferences.Editor e = p.edit();
        copyLegacyIfMissing(legacy, e, "lastSabqiDate");
        copyLegacyIfMissing(legacy, e, "lastSabqiLabel");
        copyLegacyIfMissing(legacy, e, "lastItqanDate");
        copyLegacyIfMissing(legacy, e, "lastItqanLabel");
        copyLegacyIfMissing(legacy, e, "lastMurajaahDate");
        copyLegacyIfMissing(legacy, e, "lastMurajaahLabel");
        e.apply();
    }

    private void copyLegacyIfMissing(SharedPreferences legacy, SharedPreferences.Editor e, String key) {
        if (p.contains(key)) return;
        String value = legacy.getString(key, "");
        if (value != null && !value.isEmpty()) e.putString(key, value);
    }

    public int schema() { return p.getInt("schema", 0); }
    public LocalDate programStartDate() { return LocalDate.parse(required("programStartDate")); }
    public void setProgramStartDate(LocalDate value) { p.edit().putString("programStartDate", value.toString()).apply(); }
    public LocalDate recentConsolidationActivatedOn() {
        String value = p.getString("recentConsolidationActivatedOn", "");
        return value == null || value.isEmpty() ? null : safeDate(value, null);
    }

    private static String maskEntropyKey(String mode) {
        if (mode == null || mode.trim().isEmpty()) throw new IllegalArgumentException("mask entropy mode required");
        return "maskEntropy_" + mode.toLowerCase(Locale.ROOT);
    }

    public String maskEntropyFor(String mode) {
        String key = maskEntropyKey(mode);
        String current = p.getString(key, "");
        if (current != null && !current.isEmpty()) return current;
        String created = UUID.randomUUID().toString();
        if (!p.edit().putString(key, created).commit()) throw new IllegalStateException("Unable to persist Hifz mask entropy");
        return created;
    }

    public void clearMaskEntropy(String mode) {
        if (!p.edit().remove(maskEntropyKey(mode)).commit()) throw new IllegalStateException("Unable to clear Hifz mask entropy");
    }

    // Legacy diagnostics retained for migration visibility; schema v3 corpus APIs below are authoritative.
    public VerseRef lowerBound() { return safeRef(p.getString("lowerBound", "2:1"), new VerseRef(2,1)); }
    public VerseRef promotedFrontier() { return safeRef(p.getString("promotedFrontier", "2:74"), new VerseRef(2,74)); }
    public VerseRef upperTailStart() { return safeRef(p.getString("upperTailStart", "49:1"), new VerseRef(49,1)); }
    public void setPromotedFrontier(VerseRef value) { putRef("promotedFrontier", value); }

    public VerseRef sabqiStart() { return ref("sabqiStart"); }
    public VerseRef sabqiEnd() { return ref("sabqiEnd"); }
    public void setSabqiStart(VerseRef value) { putRef("sabqiStart", value); }
    public void setSabqiEnd(VerseRef value) { putRef("sabqiEnd", value); }

    public List<VerseRange> itqanRanges() { return parseRanges("itqanRanges"); }
    public List<VerseRange> promotedRanges() { return parseRangesAllowEmpty("promotedRanges"); }
    public List<VerseRange> unconsolidatedPromotedRanges() { return parseRangesAllowEmpty("unconsolidatedPromotedRanges"); }
    public List<VerseRange> legacyMurajaahPromotedRanges() { return parseRangesAllowEmpty("legacyMurajaahPromotedRanges"); }
    public List<VerseRange> forcedPromotedRanges() { return parseRangesAllowEmpty("forcedPromotedRanges"); }

    public List<LocalDate> consolidationAttendanceDates() {
        ArrayList<LocalDate> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString("consolidationAttendanceDates", "[]"));
            for (int i = 0; i < array.length(); i++) {
                LocalDate date = safeDate(array.getString(i), null);
                if (date != null) out = new ArrayList<>(ConsolidationAttendance.add(out, date));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt Consolidation attendance", error);
        }
        return Collections.unmodifiableList(out);
    }

    public List<LocalDate> consolidationAttendanceDates(LocalDate fromInclusive, LocalDate throughInclusive) {
        return Collections.unmodifiableList(ConsolidationAttendance.between(
            consolidationAttendanceDates(), fromInclusive, throughInclusive));
    }

    /** Effective Itqan work corpus: configured base ranges plus every snowball promotion. Read-only to UI callers. */
    public List<VerseRange> effectiveItqanRanges() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(promotedRanges());
        return Collections.unmodifiableList(normalizeRanges(all));
    }

    public boolean setItqanRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return false;
        List<VerseRange> normalized = normalizeRanges(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }

    public VerseRef itqanRotationStart() { return ref("itqanRotationStart"); }
    public void setItqanRotationStart(VerseRef value) { putRef("itqanRotationStart", value); }

    public VerseRef itqanCursor() { return ref("itqanCursor"); }
    public VerseRef murajaahCursor() { return ref("murajaahCursor"); }
    public void setItqanCursor(VerseRef value) { putRef("itqanCursor", value); }

    /** All base Itqan plus every snowball promotion, irrespective of consolidation status. */
    public EligibleCorpus itqanWorkCorpus() {
        return EligibleCorpus.Companion.of(effectiveItqanRanges());
    }

    /** Compatibility alias while the runtime migration is staged. */
    public EligibleCorpus corpus() { return itqanWorkCorpus(); }

    /**
     * Murajaah sees base Itqan, all already-consolidated promotions, and historical promoted
     * ranges retained during v2->v3 migration. Fresh unconsolidated promotions stay hidden.
     */
    public EligibleCorpus murajaahCorpus() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(legacyMurajaahPromotedRanges());
        List<VerseRange> consolidated = new ArrayList<>(promotedRanges());
        for (VerseRange pending : unconsolidatedPromotedRanges()) {
            consolidated = subtractCoverage(consolidated, pending.getStart(), pending.getEndInclusive());
        }
        all.addAll(consolidated);
        return EligibleCorpus.Companion.of(all);
    }

    public boolean isItqanCursorValid() { try { return itqanWorkCorpus().contains(itqanCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isMurajaahCursorValid() { try { return murajaahCorpus().contains(murajaahCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isRotationStartValid() { try { return itqanWorkCorpus().contains(itqanRotationStart()); } catch (RuntimeException e) { return false; } }

    public int sabqiLineCursor() { return p.getInt("sabqiLineCursor", -1); }
    public void setSabqiLineCursor(int value) { p.edit().putInt("sabqiLineCursor", value).apply(); }
    public int sabqiRep() { return p.getInt("sabqiRep", 0); }
    public int sabqiAssisted() { return p.getInt("sabqiAssisted", 0); }
    public boolean setSabqiProgress(int rep, int assisted) {
        return p.edit().putInt("sabqiRep", rep).putInt("sabqiAssisted", assisted).commit();
    }

    /** Sabqi completion queues the five-line block; promotion is deliberately deferred to calendar/attendance policy. */
    public boolean completeSabqiBlock(int startLine, int endLine, int nextLineCursor, String date, String label) {
        List<RecentSabqi> queue = recentSabqi();
        LocalDate blockDate = safeDate(date, null);
        if (blockDate == null) blockDate = HifzClock.today();
        queue.add(new RecentSabqi(startLine, endLine, blockDate, 0));
        String activation = p.getString("recentConsolidationActivatedOn", "");
        if ((activation == null || activation.isEmpty())
                && queue.size() >= HifzSchedule.RECENT_BLOCKS_FOR_SUNDAY_CONSOLIDATION) {
            activation = blockDate.toString();
        }
        return p.edit()
            .putString("recentSabqi", recentJson(queue))
            .putString("recentConsolidationActivatedOn", activation == null ? "" : activation)
            .putInt("sabqiLineCursor", nextLineCursor)
            .putInt("sabqiRep", 0)
            .putInt("sabqiAssisted", 0)
            .putLong("sabqiElapsedMs", 0L)
            .putString("sabqiTodayReviewDate", date)
            .putInt("sabqiTodayReviewStartLine", startLine)
            .putInt("sabqiTodayReviewEndLine", endLine)
            .putLong("sabqi_today_reviewElapsedMs", 0L)
            .putString("lastSabqiDate", date)
            .putString("lastSabqiLabel", label)
            .commit();
    }

    /** Compatibility overload: promotion is handled by the calendar/attendance rebalance. */
    public boolean completeSabqiBlock(int startLine, int endLine, VerseRef ignoredPromotion,
                                      int nextLineCursor, String date, String label) {
        return completeSabqiBlock(startLine, endLine, nextLineCursor, date, label);
    }

    public int itqanRep() { return p.getInt("itqanRep", 0); }
    public int itqanAssisted() { return p.getInt("itqanAssisted", 0); }
    public int itqanFinalReveals() { return p.getInt("itqanFinalReveals", 0); }
    public int itqanBlockIndex() { return Math.max(0, p.getInt("itqanBlockIndex", 0)); }
    public VerseRef itqanUnitStart() { return optionalRef("itqanUnitStart"); }
    public VerseRef itqanUnitEnd() { return optionalRef("itqanUnitEnd"); }

    public List<Integer> hardAnchoringSurahs() {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        try {
            JSONArray array = new JSONArray(p.getString("hardAnchoringSurahs", "[]"));
            for (int i = 0; i < array.length(); i++) {
                int surah = array.optInt(i, -1);
                if (surah >= 1 && surah <= 114) unique.add(surah);
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        ArrayList<Integer> out = new ArrayList<>(unique);
        Collections.sort(out);
        return out;
    }

    public boolean setHardAnchoringSurahs(List<Integer> surahs) {
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        if (surahs != null) {
            for (Integer value : surahs) if (value != null && value >= 1 && value <= 114) unique.add(value);
        }
        ArrayList<Integer> ordered = new ArrayList<>(unique);
        Collections.sort(ordered);
        JSONArray array = new JSONArray();
        for (int value : ordered) array.put(value);
        return p.edit().putString("hardAnchoringSurahs", array.toString()).commit();
    }

    static boolean containsHardAnchoringSurah(List<Integer> hardSurahs, List<VerseRef> unitVerses) {
        if (hardSurahs == null || hardSurahs.isEmpty() || unitVerses == null || unitVerses.isEmpty()) return false;
        for (VerseRef verse : unitVerses) if (verse != null && hardSurahs.contains(verse.getSurah())) return true;
        return false;
    }

    public boolean isFractionatedUnit(List<VerseRef> unitVerses) {
        return containsHardAnchoringSurah(hardAnchoringSurahs(), unitVerses);
    }

    /** Atomically closes one fractionated sub-block and advances the persisted block cursor. */
    public boolean advanceItqanBlock(int nextBlockIndex, String date, String label) {
        return p.edit()
            .putInt("itqanBlockIndex", Math.max(0, nextBlockIndex))
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .commit();
    }

    /** Atomically closes a fractionated sub-block while pinning the exact open unit. */
    public boolean advanceItqanBlock(int nextBlockIndex, VerseRef unitStart, VerseRef unitEnd,
                                     String date, String label) {
        if (unitStart == null || unitEnd == null) return false;
        int completedBlockIndex = Math.max(0, nextBlockIndex - 1);
        return p.edit()
            .putInt("itqanBlockIndex", Math.max(0, nextBlockIndex))
            .putInt("itqanRep", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", unitStart.toString())
            .putString("itqanUnitEnd", unitEnd.toString())
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .putString("lastItqanCreditStart", unitStart.toString())
            .putString("lastItqanCreditEnd", unitEnd.toString())
            .putInt("lastItqanCreditBlockIndex", completedBlockIndex)
            .commit();
    }

    public boolean setItqanProgress(int rep, int assisted, VerseRef unitStart, VerseRef unitEnd) {
        return setItqanProgress(rep, assisted, itqanFinalReveals(), unitStart, unitEnd);
    }

    public boolean setItqanProgress(int rep, int assisted, int finalReveals,
                                     VerseRef unitStart, VerseRef unitEnd) {
        return p.edit()
            .putInt("itqanRep", rep)
            .putInt("itqanAssisted", assisted)
            .putInt("itqanFinalReveals", Math.max(0, finalReveals))
            .putString("itqanUnitStart", unitStart == null ? "" : unitStart.toString())
            .putString("itqanUnitEnd", unitEnd == null ? "" : unitEnd.toString())
            .commit();
    }

    public boolean completeItqanUnit(VerseRef nextCursor, String date, String label) {
        VerseRef completedStart = itqanUnitStart();
        VerseRef completedEnd = itqanUnitEnd();
        return p.edit()
            .putString("itqanCursor", nextCursor.toString())
            .putInt("itqanRep", 0)
            .putInt("itqanBlockIndex", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .putString("lastItqanCreditStart", completedStart == null ? "" : completedStart.toString())
            .putString("lastItqanCreditEnd", completedEnd == null ? "" : completedEnd.toString())
            .putInt("lastItqanCreditBlockIndex", -1)
            .commit();
    }

    /** Atomically advances the natural Itqan cycle and consolidates the exact completed queue entry. */
    public boolean completeItqanUnitAndConsolidate(VerseRef start, VerseRef endInclusive,
                                                   VerseRef nextCursor, String date, String label) {
        return completeItqanUnitAndConsolidate(start, endInclusive, nextCursor, date, label, -1);
    }

    public boolean completeItqanUnitAndConsolidate(VerseRef start, VerseRef endInclusive,
                                                   VerseRef nextCursor, String date, String label,
                                                   int creditBlockIndex) {
        List<AnchoringQueue.Entry> queue = anchoringQueue();
        int completedIndex = findAnchoringEntry(queue, start, endInclusive);
        if (completedIndex < 0) return false;
        List<VerseRange> pending = subtractCoverage(unconsolidatedPromotedRanges(), start, endInclusive);
        List<VerseRange> forced = subtractCoverage(forcedPromotedRanges(), start, endInclusive);
        int currentIndex = anchoringQueueIndex(queue.size());
        queue.remove(completedIndex);
        if (!queue.isEmpty()) currentIndex = Math.min(completedIndex, queue.size() - 1);
        else currentIndex = 0;
        VerseRef storedNext = queue.isEmpty() ? nextCursor
            : GeometryRepository.parseVerse(queue.get(currentIndex).start);
        return p.edit()
            .putString("unconsolidatedPromotedRanges", rangesJson(pending))
            .putString("forcedPromotedRanges", rangesJson(forced))
            .putString("anchoringQueue", anchoringQueueJson(queue))
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", currentIndex)
            .putString("anchoringRetryAfterDate", "")
            .putString("itqanCursor", storedNext.toString())
            .putInt("itqanRep", 0)
            .putInt("itqanBlockIndex", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putLong("itqanElapsedMs", 0L)
            .putString("lastItqanDate", date)
            .putString("lastItqanLabel", label)
            .putString("lastItqanCreditStart", start.toString())
            .putString("lastItqanCreditEnd", endInclusive.toString())
            .putInt("lastItqanCreditBlockIndex", creditBlockIndex)
            .commit();
    }

    public List<AnchoringQueue.Entry> anchoringQueue() {
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

    public int anchoringQueueIndex(int size) {
        if (size <= 0) return 0;
        return Math.floorMod(p.getInt("anchoringQueueIndex", 0), size);
    }

    public int anchoringQueueIndex() { return anchoringQueueIndex(anchoringQueue().size()); }

    public AnchoringQueue.Entry anchoringEntryFor(VerseRef start, VerseRef endInclusive) {
        if (start == null || endInclusive == null) return null;
        return AnchoringQueue.findByRange(anchoringQueue(), start.toString(), endInclusive.toString());
    }

    /** Persisted identity of the anchoring unit actually started, or null when none is open. */
    public AnchoringQueue.Entry inProgressAnchoringEntry() {
        if (p.getInt("itqanRep", 0) <= 0 && p.getInt("itqanBlockIndex", 0) <= 0) return null;
        VerseRef start = itqanUnitStart();
        VerseRef end = itqanUnitEnd();
        if (start == null || end == null) return null;
        return AnchoringQueue.findByRange(anchoringQueue(), start.toString(), end.toString());
    }

    public boolean anchoringDeferredToday() {
        String value = p.getString("anchoringRetryAfterDate", "");
        LocalDate retry = safeDate(value, null);
        return retry != null && !HifzClock.today().isAfter(retry);
    }

    private static boolean overlaps(List<VerseRange> ranges, VerseRef start, VerseRef endInclusive) {
        int a = GeometryRepository.ordinal(start), b = GeometryRepository.ordinal(endInclusive);
        for (VerseRange range : ranges) {
            int ra = GeometryRepository.ordinal(range.getStart());
            int rb = GeometryRepository.ordinal(range.getEndInclusive());
            if (a <= rb && b >= ra) return true;
        }
        return false;
    }

    /** Reconcile pending page units while retaining failure/protocol state and explicit deferrals. */
    public boolean reconcileAnchoringQueue(GeometryRepository geometry) {
        List<VerseRange> pendingRanges = unconsolidatedPromotedRanges();
        List<VerseRange> forcedRanges = forcedPromotedRanges();
        LinkedHashMap<String, AnchoringQueue.Entry> expected = new LinkedHashMap<>();
        if (!pendingRanges.isEmpty()) {
            EligibleCorpus pendingCorpus = EligibleCorpus.Companion.of(pendingRanges);
            for (VerseRange range : pendingRanges) {
                VerseRef cursor = range.getStart();
                while (range.contains(cursor)) {
                    GeometryRepository.VerseUnit unit = geometry.eligiblePageUnit(cursor, pendingCorpus);
                    String key = anchoringKey(unit.start.toString(), unit.end.toString());
                    boolean reconstruction = ordinalBetween(unit.start, new VerseRef(49, 1), new VerseRef(114, 6));
                    boolean forced = !reconstruction && overlaps(forcedRanges, unit.start, unit.end);
                    expected.put(key, new AnchoringQueue.Entry(unit.start.toString(), unit.end.toString(),
                        AnchoringQueue.originFor(reconstruction, forced),
                        reconstruction ? AnchoringQueue.Protocol.LIGHT : AnchoringQueue.Protocol.FULL, 0));
                    VerseRef next = pendingCorpus.next(unit.end);
                    if (GeometryRepository.ordinal(next) <= GeometryRepository.ordinal(unit.end)) break;
                    cursor = next;
                }
            }
        }

        ArrayList<AnchoringQueue.Entry> next = new ArrayList<>();
        for (AnchoringQueue.Entry existing : anchoringQueue()) {
            String key = anchoringKey(existing.start, existing.end);
            AnchoringQueue.Entry wanted = expected.remove(key);
            if (wanted != null) {
                next.add(new AnchoringQueue.Entry(existing.start, existing.end, wanted.origin,
                    existing.protocol, existing.failures));
            }
        }
        next.addAll(expected.values());
        int index = anchoringQueueIndex(next.size());
        boolean keepCurrent = p.getInt("itqanBlockIndex", 0) > 0 || p.getInt("itqanRep", 0) > 0;
        if (keepCurrent) {
            VerseRef savedStart = itqanUnitStart();
            VerseRef savedEnd = itqanUnitEnd();
            if (savedStart != null && savedEnd != null) {
                int savedIndex = findAnchoringEntry(next, savedStart, savedEnd);
                if (savedIndex >= 0) index = savedIndex;
            }
        }
        next = new ArrayList<>(AnchoringQueue.mergeWithPromotionPriority(
            next, index, keepCurrent, Collections.emptyList()));
        index = 0;
        return p.edit()
            .putString("anchoringQueue", anchoringQueueJson(next))
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", index)
            .commit();
    }

    public AnchoringQueue.Entry currentAnchoringEntry(GeometryRepository geometry) {
        if (!p.getBoolean("anchoringQueueInitialized", false)
                && !reconcileAnchoringQueue(geometry)) {
            throw new IllegalStateException("Unable to persist anchoring queue");
        }
        String retryText = p.getString("anchoringRetryAfterDate", "");
        LocalDate retry = safeDate(retryText, null);
        if (retry != null) {
            if (!HifzClock.today().isAfter(retry)) return null;
            if (!p.edit().putString("anchoringRetryAfterDate", "").commit()) {
                throw new IllegalStateException("Unable to clear Ancrage retry deferral");
            }
        }
        List<AnchoringQueue.Entry> queue = anchoringQueue();
        if (!p.getBoolean("anchoringQueueInitialized", false)) {
            if (!reconcileAnchoringQueue(geometry)) {
                throw new IllegalStateException("Unable to repair anchoring queue");
            }
            queue = anchoringQueue();
        }
        AnchoringQueue.Entry inProgress = inProgressAnchoringEntry();
        if (inProgress != null) return inProgress;
        return queue.isEmpty() ? null : queue.get(anchoringQueueIndex(queue.size()));
    }

    /** Fail and defer the exact page displayed, even when it is not the physical queue head. */
    public boolean failAndDeferAnchoring(VerseRef displayedStart, VerseRef displayedEnd) {
        List<AnchoringQueue.Entry> queue = anchoringQueue();
        int displayedIndex = findAnchoringEntry(queue, displayedStart, displayedEnd);
        if (displayedIndex < 0) return false;
        AnchoringQueue.Deferral deferred = AnchoringQueue.failAndDefer(queue, displayedIndex, 3);
        int storedIndex = deferred.nextIndex < 0 ? 0 : deferred.nextIndex;
        VerseRef next = GeometryRepository.parseVerse(deferred.entries.get(storedIndex).start);
        String retryAfter = deferred.nextIndex < 0 ? HifzClock.today().toString() : "";
        return p.edit()
            .putString("anchoringQueue", anchoringQueueJson(deferred.entries))
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", storedIndex)
            .putString("anchoringRetryAfterDate", retryAfter)
            .putString("itqanCursor", next.toString())
            .putInt("itqanRep", 0)
            .putInt("itqanBlockIndex", 0)
            .putInt("itqanAssisted", 0)
            .putInt("itqanFinalReveals", 0)
            .putString("itqanUnitStart", "")
            .putString("itqanUnitEnd", "")
            .putLong("itqanElapsedMs", 0L)
            .commit();
    }

    public String sabqiTodayReviewDate() { return p.getString("sabqiTodayReviewDate", ""); }
    public int sabqiTodayReviewStartLine() { return p.getInt("sabqiTodayReviewStartLine", -1); }
    public int sabqiTodayReviewEndLine() { return p.getInt("sabqiTodayReviewEndLine", -1); }
    public String lastSabqiTodayReviewDate() { return p.getString("lastSabqiTodayReviewDate", ""); }
    public String lastSabqiTodayReviewLabel() { return p.getString("lastSabqiTodayReviewLabel", ""); }
    public boolean completeSabqiTodayReview(String date, String label) {
        return p.edit()
            .putLong("sabqi_today_reviewElapsedMs", 0L)
            .putString("lastSabqiTodayReviewDate", date)
            .putString("lastSabqiTodayReviewLabel", label)
            .commit();
    }

    public int recentSabqiReviewIndex() { return Math.max(0, p.getInt("recentSabqiReviewIndex", 0)); }
    public void setRecentSabqiReviewIndex(int value) {
        p.edit().putInt("recentSabqiReviewIndex", Math.max(0, value)).apply();
    }
    public String lastRecentSabqiReviewDate() { return p.getString("lastRecentSabqiReviewDate", ""); }
    public String lastRecentSabqiReviewLabel() { return p.getString("lastRecentSabqiReviewLabel", ""); }
    public boolean completeRecentSabqiReview(String date, int nextIndex, String label) {
        LocalDate completed = safeDate(date, null);
        if (completed == null) completed = HifzClock.today();
        List<LocalDate> attendance = ConsolidationAttendance.add(consolidationAttendanceDates(), completed);
        return p.edit()
            .putLong("recent_sabqi_reviewElapsedMs", 0L)
            .putInt("recentSabqiReviewIndex", Math.max(0, nextIndex))
            .putString("lastRecentSabqiReviewDate", date)
            .putString("lastRecentSabqiReviewLabel", label)
            .putString("consolidationAttendanceDates", attendanceJson(attendance))
            .commit();
    }

    public VerseRef murajaahActualEnd() { return optionalRef("murajaahActualEnd"); }
    public void setMurajaahActualEnd(VerseRef value) {
        p.edit().putString("murajaahActualEnd", value == null ? "" : value.toString()).apply();
    }
    public int murajaahPage() { return p.getInt("murajaahPage", 0); }
    public void setMurajaahPage(int value) {
        if (value >= 1 && value <= 604) p.edit().putInt("murajaahPage", value).apply();
        else p.edit().remove("murajaahPage").apply();
    }

    public boolean completeMurajaah(VerseRef nextCursor, String date, String label) {
        return completeMurajaah(nextCursor, null, null, date, label);
    }

    public boolean completeMurajaah(VerseRef nextCursor, VerseRef reviewedStart, VerseRef reviewedEnd,
                                    String date, String label) {
        if (nextCursor == null) return false;
        return p.edit()
            .putString("murajaahCursor", nextCursor.toString())
            .putLong("murajaahElapsedMs", 0L)
            .putString("murajaahActualEnd", "")
            .remove("murajaahPage")
            .putString("lastMurajaahDate", date)
            .putString("lastMurajaahLabel", label)
            .putString("lastMurajaahCreditStart", reviewedStart == null ? "" : reviewedStart.toString())
            .putString("lastMurajaahCreditEnd", reviewedEnd == null ? "" : reviewedEnd.toString())
            .commit();
    }

    public String lastSabqiDate() { return p.getString("lastSabqiDate", ""); }
    public String lastSabqiLabel() { return p.getString("lastSabqiLabel", ""); }
    public String lastItqanDate() { return p.getString("lastItqanDate", ""); }
    public String lastItqanLabel() { return p.getString("lastItqanLabel", ""); }
    public VerseRef lastItqanCreditStart() { return optionalRef("lastItqanCreditStart"); }
    public VerseRef lastItqanCreditEnd() { return optionalRef("lastItqanCreditEnd"); }
    public int lastItqanCreditBlockIndex() { return p.getInt("lastItqanCreditBlockIndex", -1); }
    public String lastMurajaahDate() { return p.getString("lastMurajaahDate", ""); }
    public String lastMurajaahLabel() { return p.getString("lastMurajaahLabel", ""); }
    public VerseRef lastMurajaahCreditStart() { return optionalRef("lastMurajaahCreditStart"); }
    public VerseRef lastMurajaahCreditEnd() { return optionalRef("lastMurajaahCreditEnd"); }

    static String elapsedKey(String mode) {
        if (mode == null || mode.trim().isEmpty()) throw new IllegalArgumentException("elapsed mode required");
        return mode.toLowerCase(Locale.ROOT) + "ElapsedMs";
    }
    public long elapsedFor(String mode) { return p.getLong(elapsedKey(mode), 0L); }
    public void setElapsedFor(String mode, long value) { p.edit().putLong(elapsedKey(mode), Math.max(0L, value)).apply(); }

    public double murajaahSecondsPerLine() { return p.getFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING); }
    public void setMurajaahSecondsPerLine(double value) {
        if (value > 0.0 && Double.isFinite(value)) p.edit().putFloat("murajaahSecPerLine", (float) value).apply();
    }

    public boolean forceEink() { return p.getBoolean("forceEink", false); }
    public void setForceEink(boolean value) { p.edit().putBoolean("forceEink", value).apply(); }

    public List<RecentSabqi> recentSabqi() {
        ArrayList<RecentSabqi> out = new ArrayList<>();
        boolean needsRewrite = false;
        LocalDate fallback = HifzClock.today();
        try {
            JSONArray array = new JSONArray(p.getString("recentSabqi", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                String addedText = o.optString("addedOn", "");
                LocalDate addedOn = safeDate(addedText, fallback);
                if (addedText == null || addedText.isEmpty() || !o.has("reviewStreak")) needsRewrite = true;
                out.add(new RecentSabqi(o.getInt("start"), o.getInt("end"), addedOn,
                    o.optInt("reviewStreak", 0)));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt recent Sabqi queue", error);
        }
        if (needsRewrite) p.edit().putString("recentSabqi", recentJson(out)).apply();
        return out;
    }

    public void addRecentSabqi(int start, int end) {
        List<RecentSabqi> queue = recentSabqi();
        LocalDate now = HifzClock.today();
        queue.add(new RecentSabqi(start, end, now, 0));
        String activation = p.getString("recentConsolidationActivatedOn", "");
        if ((activation == null || activation.isEmpty())
                && queue.size() >= HifzSchedule.RECENT_BLOCKS_FOR_SUNDAY_CONSOLIDATION) {
            activation = now.toString();
        }
        p.edit()
            .putString("recentSabqi", recentJson(queue))
            .putString("recentConsolidationActivatedOn", activation == null ? "" : activation)
            .apply();
    }

    public void removeFirstRecentSabqi() {
        List<RecentSabqi> queue = recentSabqi();
        if (!queue.isEmpty()) queue.remove(0);
        saveRecent(queue);
    }

    static List<RecentSabqi> markReviewed(List<RecentSabqi> source, int index) {
        ArrayList<RecentSabqi> out = new ArrayList<>(source == null ? Collections.emptyList() : source);
        if (out.isEmpty()) return out;
        int at = Math.floorMod(index, out.size());
        RecentSabqi item = out.get(at);
        out.set(at, new RecentSabqi(item.startLine, item.endLine, item.addedOn, item.reviewStreak + 1));
        return out;
    }

    static List<RecentSabqi> deferRecent(List<RecentSabqi> source, int index) {
        ArrayList<RecentSabqi> out = new ArrayList<>(source == null ? Collections.emptyList() : source);
        if (out.isEmpty()) return out;
        int at = Math.floorMod(index, out.size());
        RecentSabqi item = out.remove(at);
        out.add(new RecentSabqi(item.startLine, item.endLine, item.addedOn, 0));
        return out;
    }

    static List<RecentSabqi> canonicalRecentOrder(List<RecentSabqi> source) {
    ArrayList<RecentSabqi> out = new ArrayList<>(source == null ? Collections.emptyList() : source);
    out.sort(Comparator.comparingInt((RecentSabqi item) -> item.startLine)
        .thenComparingInt(item -> item.endLine));
    return out;
}

private static boolean sameRecentBlock(RecentSabqi left, RecentSabqi right) {
    return left.startLine == right.startLine && left.endLine == right.endLine;
}

static List<RecentSabqi> withoutRecentBlocks(List<RecentSabqi> source, List<RecentSabqi> removed) {
    ArrayList<RecentSabqi> out = new ArrayList<>();
    List<RecentSabqi> safeRemoved = removed == null ? Collections.emptyList() : removed;
    for (RecentSabqi item : source == null ? Collections.<RecentSabqi>emptyList() : source) {
        boolean drop = false;
        for (RecentSabqi candidate : safeRemoved) {
            if (sameRecentBlock(item, candidate)) { drop = true; break; }
        }
        if (!drop) out.add(item);
    }
    return out;
}

public boolean removeRecentBlocks(List<RecentSabqi> removed) {
    List<RecentSabqi> source = recentSabqi();
    if (source.isEmpty()) return true;
    int currentAt = Math.floorMod(p.getInt("recentSabqiReviewIndex", 0), source.size());
    RecentSabqi displayed = source.get(currentAt);
    List<RecentSabqi> next = withoutRecentBlocks(source, removed);
    int nextIndex = 0;
    for (int i = 0; i < next.size(); i++) {
        if (sameRecentBlock(next.get(i), displayed)) { nextIndex = i; break; }
    }
    return p.edit()
        .putString("recentSabqi", recentJson(next))
        .putInt("recentSabqiReviewIndex", next.isEmpty() ? 0 : nextIndex)
        .commit();
}

    static int indexAfterDeferral(int index, int size) {
        if (size <= 0) return 0;
        int at = Math.floorMod(index, size);
        return at >= size - 1 ? 0 : at;
    }

    public boolean markRecentReviewed(int index) {
        List<RecentSabqi> source = recentSabqi();
        if (source.isEmpty()) return false;
        int at = Math.floorMod(index, source.size());
        List<RecentSabqi> next = markReviewed(source, at);
        int nextIndex = PreviewConfig.nextRecentReviewIndex(at, next.size());
        return p.edit()
            .putString("recentSabqi", recentJson(next))
            .putInt("recentSabqiReviewIndex", Math.max(0, nextIndex))
            .commit();
    }

    public boolean deferRecentBlock(int index) {
        List<RecentSabqi> source = recentSabqi();
        if (source.isEmpty()) return false;
        int at = Math.floorMod(index, source.size());
        List<RecentSabqi> next = deferRecent(source, at);
        int nextIndex = indexAfterDeferral(at, next.size());
        return p.edit()
            .putString("recentSabqi", recentJson(next))
            .putInt("recentSabqiReviewIndex", nextIndex)
            .commit();
    }

    /**
     * Idempotently grows the Itqan snowball. Forced overflow promotion is retained separately so
     * the user can see why that material entered Ancrage.
     */
    public boolean addPromotedVerses(List<VerseRef> verses) {
        return addPromotedVerses(verses, false);
    }

    public boolean addPromotedVerses(List<VerseRef> verses, boolean forcedPromotion) {
        if (verses == null || verses.isEmpty()) return true;
        ArrayList<VerseRef> fresh = new ArrayList<>();
        for (VerseRef verse : verses) if (!isPromoted(verse)) fresh.add(verse);
        if (fresh.isEmpty()) return true;
        fresh.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        List<VerseRange> additions = rangesFromVerses(fresh);

        ArrayList<VerseRange> all = new ArrayList<>(promotedRanges());
        all.addAll(additions);
        ArrayList<VerseRange> pending = new ArrayList<>(unconsolidatedPromotedRanges());
        pending.addAll(additions);
        ArrayList<VerseRange> forced = new ArrayList<>(forcedPromotedRanges());
        if (forcedPromotion) forced.addAll(additions);
        return p.edit()
            .putString("promotedRanges", rangesJson(normalizeRanges(all)))
            .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
            .putString("forcedPromotedRanges", rangesJson(normalizeRanges(forced)))
            .putBoolean("anchoringQueueInitialized", false)
            .commit();
    }

    public boolean isPromoted(VerseRef verse) {
        for (VerseRange range : promotedRanges()) if (range.contains(verse)) return true;
        return false;
    }

    public boolean isUnconsolidatedPromoted(VerseRef verse) {
        for (VerseRange range : unconsolidatedPromotedRanges()) if (range.contains(verse)) return true;
        return false;
    }

    public boolean isForcedPromoted(VerseRef verse) {
        for (VerseRange range : forcedPromotedRanges()) if (range.contains(verse)) return true;
        return false;
    }

    /** Mark only the completed overlap consolidated; unrelated pending promotions remain queued in-place. */
    public boolean markPromotedConsolidated(VerseRef start, VerseRef endInclusive) {
        List<VerseRange> next = subtractCoverage(unconsolidatedPromotedRanges(), start, endInclusive);
        List<VerseRange> forced = subtractCoverage(forcedPromotedRanges(), start, endInclusive);
        return p.edit()
            .putString("unconsolidatedPromotedRanges", rangesJson(next))
            .putString("forcedPromotedRanges", rangesJson(forced))
            .putBoolean("anchoringQueueInitialized", false)
            .commit();
    }

    private void saveRecent(List<RecentSabqi> queue) {
        p.edit().putString("recentSabqi", recentJson(queue)).apply();
    }

    private String recentJson(List<RecentSabqi> queue) {
        JSONArray array = new JSONArray();
        try {
            for (RecentSabqi item : queue) {
                JSONObject o = new JSONObject();
                o.put("start", item.startLine);
                o.put("end", item.endLine);
                o.put("addedOn", item.addedOn.toString());
                o.put("reviewStreak", item.reviewStreak);
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    private static String normalizeRecentJson(String raw, LocalDate fallback) {
        JSONArray out = new JSONArray();
        try {
            JSONArray source = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.getJSONObject(i);
                JSONObject normalized = new JSONObject();
                normalized.put("start", item.getInt("start"));
                normalized.put("end", item.getInt("end"));
                normalized.put("addedOn", safeDate(item.optString("addedOn", ""), fallback).toString());
                normalized.put("reviewStreak", Math.max(0, item.optInt("reviewStreak", 0)));
                out.put(normalized);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt recent Sabqi queue", error);
        }
        return out.toString();
    }

    private static int recentCount(String raw) {
        try { return new JSONArray(raw == null ? "[]" : raw).length(); }
        catch (Exception error) { throw new IllegalStateException("Corrupt recent Sabqi queue", error); }
    }

    private List<VerseRange> parseRanges(String key) {
        List<VerseRange> ranges = parseRangesAllowEmpty(key);
        if (ranges.isEmpty()) throw new IllegalStateException("No Itqan range configured");
        return normalizeRanges(ranges);
    }

    private List<VerseRange> parseRangesAllowEmpty(String key) {
        ArrayList<VerseRange> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString(key, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                out.add(new VerseRange(GeometryRepository.parseVerse(o.getString("start")), GeometryRepository.parseVerse(o.getString("end"))));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt range list: " + key, error);
        }
        return out;
    }

    private static List<VerseRange> normalizeRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(EligibleCorpus.Companion.of(ranges).getRanges());
    }

    private static List<VerseRange> rangesFromVerses(List<VerseRef> orderedVerses) {
        ArrayList<VerseRange> additions = new ArrayList<>();
        if (orderedVerses.isEmpty()) return additions;
        VerseRef start = orderedVerses.get(0);
        VerseRef previous = start;
        for (int i = 1; i < orderedVerses.size(); i++) {
            VerseRef current = orderedVerses.get(i);
            if (GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1) {
                previous = current;
            } else {
                additions.add(new VerseRange(start, previous));
                start = previous = current;
            }
        }
        additions.add(new VerseRange(start, previous));
        return additions;
    }

    private static List<VerseRange> subtractCoverage(List<VerseRange> source, VerseRef cutStart, VerseRef cutEndInclusive) {
        int cutA = GeometryRepository.ordinal(cutStart);
        int cutB = GeometryRepository.ordinal(cutEndInclusive);
        if (cutB < cutA) { int swap = cutA; cutA = cutB; cutB = swap; }
        ArrayList<VerseRange> out = new ArrayList<>();
        for (VerseRange range : source) {
            int a = GeometryRepository.ordinal(range.getStart());
            int b = GeometryRepository.ordinal(range.getEndInclusive());
            if (cutB < a || cutA > b) {
                out.add(range);
                continue;
            }
            if (a < cutA) {
                out.add(new VerseRange(range.getStart(), QuranCanon.INSTANCE.fromOrdinal(cutA - 1)));
            }
            if (cutB < b) {
                out.add(new VerseRange(QuranCanon.INSTANCE.fromOrdinal(cutB + 1), range.getEndInclusive()));
            }
        }
        return normalizeRanges(out);
    }

    private static String defaultItqanRangesJson() {
        return rangesJson(defaultItqanRanges());
    }

    private static List<VerseRange> defaultItqanRanges() {
        ArrayList<VerseRange> ranges = new ArrayList<>();
        ranges.add(new VerseRange(new VerseRef(2,1), new VerseRef(2,74)));
        return ranges;
    }

    private static String bootstrapReconstructionJson() {
        return rangesJson(Collections.singletonList(
            new VerseRange(new VerseRef(49,1), new VerseRef(114,6))));
    }

    private static boolean ordinalBetween(VerseRef value, VerseRef start, VerseRef endInclusive) {
        int ordinal = GeometryRepository.ordinal(value);
        return ordinal >= GeometryRepository.ordinal(start) && ordinal <= GeometryRepository.ordinal(endInclusive);
    }

    private static String attendanceJson(List<LocalDate> dates) {
        JSONArray array = new JSONArray();
        for (LocalDate date : ConsolidationAttendance.add(dates, null)) array.put(date.toString());
        return array.toString();
    }

    private static String rangesJson(List<VerseRange> ranges) {
        JSONArray array = new JSONArray();
        try {
            for (VerseRange range : ranges) {
                JSONObject o = new JSONObject();
                o.put("start", range.getStart().toString());
                o.put("end", range.getEndInclusive().toString());
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    private static int findAnchoringEntry(List<AnchoringQueue.Entry> queue,
                                          VerseRef start, VerseRef endInclusive) {
        String wanted = anchoringKey(start.toString(), endInclusive.toString());
        for (int i = 0; i < queue.size(); i++) {
            AnchoringQueue.Entry entry = queue.get(i);
            if (wanted.equals(anchoringKey(entry.start, entry.end))) return i;
        }
        return -1;
    }

    private static String anchoringKey(String start, String end) { return start + "|" + end; }

    private static String anchoringQueueJson(List<AnchoringQueue.Entry> entries) {
        JSONArray array = new JSONArray();
        try {
            for (AnchoringQueue.Entry entry : entries) {
                JSONObject o = new JSONObject();
                o.put("start", entry.start);
                o.put("end", entry.end);
                o.put("origin", entry.origin.name());
                o.put("protocol", entry.protocol.name());
                o.put("failures", entry.failures);
                array.put(o);
            }
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        return array.toString();
    }

    public void resetPreviewState() {
        p.edit().clear().commit();
        ensureSchema(null);
    }

    private VerseRef ref(String key) { return GeometryRepository.parseVerse(required(key)); }
    private VerseRef optionalRef(String key) {
        String value = p.getString(key, "");
        return value == null || value.isEmpty() ? null : GeometryRepository.parseVerse(value);
    }
    private void putRef(String key, VerseRef value) { p.edit().putString(key, value.toString()).apply(); }
    private String required(String key) {
        String value = p.getString(key, null);
        if (value == null) throw new IllegalStateException("Missing Hifz state key: " + key);
        return value;
    }
    private static VerseRef safeRef(String value, VerseRef fallback) {
        try { return value == null || value.isEmpty() ? fallback : GeometryRepository.parseVerse(value); }
        catch (RuntimeException error) { return fallback; }
    }
    private static LocalDate safeDate(String value, LocalDate fallback) {
        try { return value == null || value.isEmpty() ? fallback : LocalDate.parse(value); }
        catch (RuntimeException error) { return fallback; }
    }
}
