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

        LinkedHashSet<String> migratedLearned = new LinkedHashSet<>(state.toAnchorLineIds());
        LinkedHashSet<String> migratedStabilized = new LinkedHashSet<>();
        LinkedHashSet<String> migratedAcquired = new LinkedHashSet<>(state.acquiredCreditLineIds());
        LinkedHashSet<String> migratedLegacyPartial = new LinkedHashSet<>(state.legacyPartialAcquiredLineIds());
        LinkedHashSet<String> migratedQuarantine = new LinkedHashSet<>(state.quarantineLineIds());
        LinkedHashMap<String, Long> migratedQuarantineDates = new LinkedHashMap<>(state.quarantineLegacyLastReviewed());
        LinkedHashMap<String, Long> migratedActiveDates = new LinkedHashMap<>(state.activeLastReviewedEpochDays());
        LinkedHashSet<String> migratedUnknownDue = new LinkedHashSet<>(state.unknownDueLineIds());
        LinkedHashSet<String> migratedImported = new LinkedHashSet<>(state.legacyImportedLineIds());
        LinkedHashMap<String, Long> migratedOrphanDates = new LinkedHashMap<>(state.legacyOrphanDates());

        for (String lineId : structurallyCompletedPendingLineIds) {
            Long historicalDate = migratedActiveDates.remove(lineId);
            Long quarantineDate = migratedQuarantineDates.remove(lineId);
            if (historicalDate == null) historicalDate = quarantineDate;
            if (historicalDate != null) migratedOrphanDates.put(lineId, historicalDate);
            migratedLearned.remove(lineId);
            migratedAcquired.remove(lineId);
            migratedLegacyPartial.remove(lineId);
            migratedQuarantine.remove(lineId);
            migratedUnknownDue.remove(lineId);
            migratedImported.remove(lineId);
            migratedStabilized.add(lineId);
        }

        boolean openLegacyStabilization = unitStart != null && unitEnd != null && (
            completedBlocks > 0
                || p.getInt("itqanRep", 0) > 0
                || p.getInt("itqanAssisted", 0) > 0
                || p.getInt("itqanFinalReveals", 0) > 0
                || p.getLong("itqanElapsedMs", 0L) > 0L);
        int migratedBlockIndex = openLegacyStabilization
            ? migratedPhysicalBlockIndex(geometry, unitStart, unitEnd, migratedStabilized)
            : Math.max(0, p.getInt("itqanBlockIndex", 0));
        boolean preserveOpenLegacyProgress = openLegacyStabilization
            && compatibleLegacyOpenStabilization(geometry, unitStart, unitEnd, completedBlocks, hardAnchoringSurahs());

        maybeInterruptMigrationForTest("BEFORE_MAIN_COMMIT");

        SharedPreferences.Editor e = p.edit()
            .putString("v6LearnedLineIds", lineIdsJson(migratedLearned))
            .putString("v6StabilizedLineIds", lineIdsJson(migratedStabilized))
            .putString("v6AcquiredCreditLineIds", lineIdsJson(migratedAcquired))
            .putString("v6LegacyPartialAcquiredLineIds", lineIdsJson(migratedLegacyPartial))
            .putString("v6QuarantineLineIds", lineIdsJson(migratedQuarantine))
            .putString("v6QuarantineLegacyLastReviewed", epochDayMapJson(migratedQuarantineDates))
            .putString("v6ActiveJ10LastReviewed", epochDayMapJson(migratedActiveDates))
            .putString("v6UnknownDueLineIds", lineIdsJson(migratedUnknownDue))
            .putString("v6LegacyImportedLineIds", lineIdsJson(migratedImported))
            .putString("v6LegacyOrphanJ10Dates", epochDayMapJson(migratedOrphanDates))
            .putFloat("murajaahSecPerLine", (float) migrated)
            .putInt("schema", 6);
        if (openLegacyStabilization) {
            e.putInt("itqanBlockIndex", migratedBlockIndex);
            if (!preserveOpenLegacyProgress) {
                e.putInt("itqanRep", 0)
                    .putInt("itqanAssisted", 0)
                    .putInt("itqanFinalReveals", 0)
                    .putLong("itqanElapsedMs", 0L);
            }
        }
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

    private static boolean compatibleLegacyOpenStabilization(
            GeometryRepository geometry,
            VerseRef start,
            VerseRef endInclusive,
            int completedBlocks,
            List<Integer> hardSurahs) {
        if (geometry == null || start == null || endInclusive == null || completedBlocks != 0) return false;
        try {
            if (geometry.pageForVerse(start) != geometry.pageForVerse(endInclusive)) return false;
            List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, endInclusive, geometry);
            if (owned.isEmpty()) return false;
            List<StabilizationHalfPagePolicy.Unit> newPlan = StabilizationHalfPagePolicy.planPage(
                geometry.linesForExactIds(owned));
            if (newPlan.size() != 1) return false;
            List<VerseRef> legacyVerses = geometry.versesForRange(start, endInclusive);
            if (containsHardAnchoringSurah(hardSurahs, legacyVerses)) {
                int[] segments = geometry.surahSegmentLineCounts(start, endInclusive);
                if (PreviewConfig.fractionatedBlockCount(segments) > 1) return false;
            }
            return true;
        } catch (RuntimeException incompatible) {
            return false;
        }
    }

    private static int migratedPhysicalBlockIndex(
            GeometryRepository geometry,
            VerseRef start,
            VerseRef endInclusive,
            java.util.Set<String> stabilizedLineIds) {
        try {
            if (geometry.pageForVerse(start) != geometry.pageForVerse(endInclusive)) return 0;
            List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, endInclusive, geometry);
            List<StabilizationHalfPagePolicy.Unit> planned = StabilizationHalfPagePolicy.planPage(
                geometry.linesForExactIds(owned));
            for (int i = 0; i < planned.size(); i++) {
                if (!stabilizedLineIds.containsAll(planned.get(i).lineIds)) return i;
            }
            return 0;
        } catch (RuntimeException incompatibleLegacyUnit) {
            return 0;
        }
    }

    public enum ProgressState {
        NONE,
        LEARNED,
        STABILIZED,
        ACQUIRED
    }

    public enum ProgressAction {
        LEARNING,
        STABILIZATION,
        CONSOLIDATION,
        REVIEW
    }

    public enum ProgressEvent {
        LEARNING_COMPLETED,
        STABILIZATION_COMPLETED,
        CONSOLIDATION_COMPLETED
    }

    ProgressState progressStateV6(String lineId) {
        if (lineId == null || lineId.isEmpty()) throw new IllegalArgumentException("lineId required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            if (quarantine.contains(lineId) || legacyPartial.contains(lineId)) {
                throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
            }
            return progressStateFromSets(
                lineId,
                v6LineIdSet("v6LearnedLineIds"),
                v6LineIdSet("v6StabilizedLineIds"),
                v6LineIdSet("v6AcquiredCreditLineIds"));
        }
    }

    ProgressAction nextActionV6(String lineId) {
        ProgressState state = progressStateV6(lineId);
        switch (state) {
            case NONE: return ProgressAction.LEARNING;
            case LEARNED: return ProgressAction.STABILIZATION;
            case STABILIZED: return ProgressAction.CONSOLIDATION;
            case ACQUIRED: return ProgressAction.REVIEW;
            default: throw new IllegalStateException("Unsupported schema6 progression state: " + state);
        }
    }

    void transitionV6Lines(List<String> lineIds, ProgressEvent event) {
        if (lineIds == null || lineIds.isEmpty()) throw new IllegalArgumentException("lineIds required");
        if (event == null) throw new IllegalArgumentException("event required");

        LinkedHashSet<String> batch = new LinkedHashSet<>();
        for (String lineId : lineIds) {
            if (lineId == null || lineId.isEmpty()) throw new IllegalArgumentException("lineId required");
            batch.add(lineId);
        }
        if (batch.isEmpty()) throw new IllegalArgumentException("lineIds required");

        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");

            int targetOrdinal = progressionTargetOrdinal(event);
            LinkedHashMap<String, ProgressState> before = new LinkedHashMap<>();
            boolean changed = false;

            for (String lineId : batch) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId)) {
                    throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
                }
                ProgressState current = progressStateFromSets(lineId, learned, stabilized, acquired);
                before.put(lineId, current);
                int currentOrdinal = progressionOrdinal(current);
                if (currentOrdinal < targetOrdinal - 1) {
                    throw new IllegalStateException(
                        "Cannot skip schema6 progression stage for line " + lineId
                            + ": state=" + current + " event=" + event);
                }
                if (currentOrdinal == targetOrdinal - 1) changed = true;
            }

            if (!changed) return;

            for (String lineId : batch) {
                ProgressState current = before.get(lineId);
                if (progressionOrdinal(current) != targetOrdinal - 1) continue;
                learned.remove(lineId);
                stabilized.remove(lineId);
                acquired.remove(lineId);
                switch (event) {
                    case LEARNING_COMPLETED:
                        learned.add(lineId);
                        break;
                    case STABILIZATION_COMPLETED:
                        stabilized.add(lineId);
                        break;
                    case CONSOLIDATION_COMPLETED:
                        acquired.add(lineId);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported schema6 progression event: " + event);
                }
            }

            SharedPreferences.Editor editor = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired));
            if (!editor.commit()) {
                throw new IllegalStateException("Unable to persist schema6 progression transition");
            }
        }
    }

    private void requireSchema6ProgressionState() {
        if (p.getInt("schema", -1) != 6) {
            throw new IllegalStateException("Schema 6 required for progression transitions");
        }
        requiredV6String("v6LearnedLineIds");
        requiredV6String("v6StabilizedLineIds");
        requiredV6String("v6AcquiredCreditLineIds");
        requiredV6String("v6QuarantineLineIds");
        requiredV6String("v6LegacyPartialAcquiredLineIds");
    }

    private static ProgressState progressStateFromSets(
            String lineId,
            LinkedHashSet<String> learned,
            LinkedHashSet<String> stabilized,
            LinkedHashSet<String> acquired) {
        boolean isLearned = learned.contains(lineId);
        boolean isStabilized = stabilized.contains(lineId);
        boolean isAcquired = acquired.contains(lineId);
        int memberships = (isLearned ? 1 : 0) + (isStabilized ? 1 : 0) + (isAcquired ? 1 : 0);
        if (memberships > 1) {
            throw new IllegalStateException("Overlapping schema6 progression states for line " + lineId);
        }
        if (isAcquired) return ProgressState.ACQUIRED;
        if (isStabilized) return ProgressState.STABILIZED;
        if (isLearned) return ProgressState.LEARNED;
        return ProgressState.NONE;
    }

    private static int progressionOrdinal(ProgressState state) {
        switch (state) {
            case NONE: return 0;
            case LEARNED: return 1;
            case STABILIZED: return 2;
            case ACQUIRED: return 3;
            default: throw new IllegalStateException("Unsupported schema6 progression state: " + state);
        }
    }

    private static int progressionTargetOrdinal(ProgressEvent event) {
        switch (event) {
            case LEARNING_COMPLETED: return 1;
            case STABILIZATION_COMPLETED: return 2;
            case CONSOLIDATION_COMPLETED: return 3;
            default: throw new IllegalStateException("Unsupported schema6 progression event: " + event);
        }
    }

    List<String> v6QuarantineLineIds() {
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            return Collections.unmodifiableList(new ArrayList<>(v6LineIdSet("v6QuarantineLineIds")));
        }
    }

    /**
     * Physical lines owned by unconsolidatedPromotedRanges ("à stabiliser") that haven't reached
     * Stabilized or Acquired yet — what Diagnostic's completion estimate divides by
     * PreviewConfig.STABILIZATION_WEEKLY_LINES to project an ETA.
     */
    int stabilizationLinesRemaining(GeometryRepository geometry) {
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            int remaining = 0;
            for (VerseRange range : unconsolidatedPromotedRanges()) {
                for (String lineId : CorpusLinePolicy.ownedLineIdsForRangeOnPage(
                        range.getStart(), range.getEndInclusive(), geometry)) {
                    if (!stabilized.contains(lineId) && !acquired.contains(lineId)) remaining++;
                }
            }
            return remaining;
        }
    }

    /**
     * Physical lines from the current Sabqi cursor to sabqiEnd, still to memorize. Bounded below
     * by sabqiStart, so anything printed before it is never counted as remaining — under the
     * default start of 2:75, that includes all of Al-Fatiha, which was never part of the
     * Apprentissage walk to begin with.
     */
    int sabqiLinesRemaining(GeometryRepository geometry) {
        int first = geometry.firstLineIndex(sabqiStart());
        int last = geometry.lastLineIndex(sabqiEnd());
        int cursor = sabqiLineCursor();
        int from = cursor >= 0 ? Math.max(cursor, first) : first;
        return Math.max(0, last - from + 1);
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

        SharedPreferences.Editor e = p.edit()
            .putString("itqanRanges", rangesJson(normalizeRanges(base)))
            .putString("promotedRanges", rangesJson(normalizeRanges(promoted)))
            .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
            .putString("legacyMurajaahPromotedRanges", rangesJson(normalizeRanges(legacy)))
            .putString("forcedPromotedRanges", "[]")
            .putString("recentSabqi", recent)
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
        List<VerseRange> normalized = sortRangesPreservingBoundaries(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }

    /** Schema-6 manual configuration: replace only the configured pending Stabilisation ranges. */
    public boolean setV6StabilizationRanges(List<VerseRange> ranges, GeometryRepository geometry) {
        if (geometry == null) throw new IllegalArgumentException("Géométrie Mushaf requise.");
        if (ranges != null) {
            ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();
            for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));
            for (VerseRange range : ranges) requireOwnedLineOnEveryPage(range, allLines);
        }
        return setV6ManualRanges(itqanRanges(), ranges, geometry);
    }

    /** Every Mushaf page touched by a Stabilisation range must own at least one physical line. */
    private static void requireOwnedLineOnEveryPage(VerseRange range, List<GeometryRepository.LineMeta> lines) {
        if (range == null) return;
        java.util.TreeMap<Integer, Boolean> pages = new java.util.TreeMap<>();
        for (GeometryRepository.LineMeta line : lines) {
            boolean touched = false;
            for (VerseRef verse : line.verses) {
                if (range.contains(verse)) { touched = true; break; }
            }
            if (!touched) continue;
            boolean owned = range.contains(CorpusLinePolicy.ownerVerse(line));
            pages.merge(line.page, owned, Boolean::logicalOr);
        }
        for (java.util.Map.Entry<Integer, Boolean> page : pages.entrySet()) {
            if (!page.getValue()) {
                throw new IllegalArgumentException("Page " + page.getKey()
                    + " : cette plage ne contient aucune ligne complète. Commencez au premier verset de la ligne.");
            }
        }
    }

    /** Schema-6 manual configuration: replace only the configured Acquired base ranges. */
    public boolean setV6AcquiredRanges(List<VerseRange> ranges, GeometryRepository geometry) {
        return setV6ManualRanges(ranges, unconsolidatedPromotedRanges(), geometry);
    }

    private boolean setV6ManualRanges(List<VerseRange> acquiredRanges,
                                      List<VerseRange> stabilizationRanges,
                                      GeometryRepository geometry) {
        if (geometry == null) throw new IllegalArgumentException("Géométrie Mushaf requise.");
        validateV6ManualRanges(acquiredRanges, stabilizationRanges);
        // Schema-6 manual ranges are ordered but deliberately NOT coalesced. Adjacent
        // ranges may sit on opposite surah boundaries; merging them would destroy the
        // physical boundary that Stabilisation/Consolidation must preserve.
        List<VerseRange> acquiredNormalized = sortRangesPreservingBoundaries(acquiredRanges);
        List<VerseRange> stabilizationNormalized = sortRangesPreservingBoundaries(stabilizationRanges);

        ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));
        LinkedHashSet<String> oldManualAcquired = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(itqanRanges(), allLines));
        LinkedHashSet<String> oldManualStabilization = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(unconsolidatedPromotedRanges(), allLines));
        LinkedHashSet<String> newManualAcquired = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(acquiredNormalized, allLines));
        LinkedHashSet<String> newManualStabilization = new LinkedHashSet<>(
            CorpusLinePolicy.ownedLineIds(stabilizationNormalized, allLines));

        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            for (String lineId : newManualAcquired) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Cette plage contient une ligne en quarantaine.");
            }
            for (String lineId : newManualStabilization) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Cette plage contient une ligne en quarantaine.");
            }

            if ((itqanRep() > 0 || itqanBlockIndex() > 0) && itqanUnitStart() != null && itqanUnitEnd() != null) {
                if (!rangeCoveredBy(stabilizationNormalized, itqanUnitStart(), itqanUnitEnd())) {
                    throw new IllegalStateException("Terminez la Stabilisation en cours avant de retirer sa plage.");
                }
            }

            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            learned.removeAll(oldManualStabilization);
            acquired.removeAll(oldManualAcquired);
            learned.addAll(newManualStabilization);
            acquired.addAll(newManualAcquired);
            for (String lineId : newManualStabilization) {
                acquired.remove(lineId);
                stabilized.remove(lineId);
            }
            for (String lineId : newManualAcquired) {
                learned.remove(lineId);
                stabilized.remove(lineId);
            }

            LinkedHashMap<String, Long> activeJ10 = v6EpochDayMap("v6ActiveJ10LastReviewed");
            LinkedHashSet<String> unknownDue = v6LineIdSet("v6UnknownDueLineIds");
            activeJ10.entrySet().removeIf(entry -> !acquired.contains(entry.getKey()));
            unknownDue.retainAll(acquired);
            for (String lineId : acquired) {
                if (!activeJ10.containsKey(lineId)) unknownDue.add(lineId);
            }
            for (String lineId : newManualStabilization) {
                activeJ10.remove(lineId);
                unknownDue.remove(lineId);
            }

            List<VerseRange> consolidatedPromotions = new ArrayList<>(promotedRanges());
            for (VerseRange oldPending : unconsolidatedPromotedRanges()) {
                consolidatedPromotions = subtractCoverage(
                    consolidatedPromotions, oldPending.getStart(), oldPending.getEndInclusive());
            }
            ArrayList<VerseRange> promotedNext = new ArrayList<>(consolidatedPromotions);
            promotedNext.addAll(stabilizationNormalized);

            SharedPreferences.Editor editor = p.edit()
                .putString("itqanRanges", rangesJson(acquiredNormalized))
                .putString("promotedRanges", rangesJson(sortRangesPreservingBoundaries(promotedNext)))
                .putString("unconsolidatedPromotedRanges", rangesJson(stabilizationNormalized))
                .putBoolean("anchoringQueueInitialized", false)
                .remove("v6ConsolidationStabilizationState")
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putString("v6ActiveJ10LastReviewed", epochDayMapJson(activeJ10))
                .putString("v6UnknownDueLineIds", lineIdsJson(unknownDue));
            return editor.commit();
        }
    }

    private void validateV6ManualRanges(List<VerseRange> acquiredRanges,
                                        List<VerseRange> stabilizationRanges) {
        if (acquiredRanges == null || acquiredRanges.isEmpty())
            throw new IllegalArgumentException("Au moins une plage Acquise est requise.");
        if (stabilizationRanges == null)
            throw new IllegalArgumentException("La liste À stabiliser est requise.");
        validateNoRangeOverlap(acquiredRanges, "Plages Acquises");
        validateNoRangeOverlap(stabilizationRanges, "Plages à stabiliser");
        for (VerseRange acquired : acquiredRanges) {
            for (VerseRange stabilization : stabilizationRanges) {
                if (rangesOverlap(acquired, stabilization))
                    throw new IllegalArgumentException("Une plage ne peut pas être à la fois Acquise et À stabiliser.");
            }
        }
    }

    private static void validateNoRangeOverlap(List<VerseRange> ranges, String label) {
        for (int i = 0; i < ranges.size(); i++) {
            if (ranges.get(i) == null) throw new IllegalArgumentException(label + " : plage absente.");
            for (int j = i + 1; j < ranges.size(); j++) {
                if (ranges.get(j) == null || rangesOverlap(ranges.get(i), ranges.get(j)))
                    throw new IllegalArgumentException(label + " : chevauchement interdit.");
            }
        }
    }

    private static boolean rangesOverlap(VerseRange left, VerseRange right) {
        int leftStart = GeometryRepository.ordinal(left.getStart());
        int leftEnd = GeometryRepository.ordinal(left.getEndInclusive());
        int rightStart = GeometryRepository.ordinal(right.getStart());
        int rightEnd = GeometryRepository.ordinal(right.getEndInclusive());
        return leftStart <= rightEnd && rightStart <= leftEnd;
    }

    private static boolean rangeCoveredBy(List<VerseRange> ranges, VerseRef start, VerseRef endInclusive) {
        if (ranges == null || ranges.isEmpty()) return false;
        int from = GeometryRepository.ordinal(start);
        int to = GeometryRepository.ordinal(endInclusive);
        for (int ordinal = from; ordinal <= to; ordinal++) {
            VerseRef verse = QuranCanon.INSTANCE.fromOrdinal(ordinal);
            boolean covered = false;
            for (VerseRange range : ranges) {
                if (range != null && range.contains(verse)) { covered = true; break; }
            }
            if (!covered) return false;
        }
        return true;
    }

    public VerseRef itqanRotationStart() { return ref("itqanRotationStart"); }
    public void setItqanRotationStart(VerseRef value) { putRef("itqanRotationStart", value); }

    /**
     * A Stabilisation range edit can leave itqanRotationStart outside the new corpus (Settings
     * offers a manual fix, but the anchor is only actually consumed here, in the validation path,
     * so a learner who never revisits Settings would otherwise hit EligibleCorpus.nextAnchored's
     * require() on every validation attempt). Self-heals to the corpus start instead of throwing.
     */
    public VerseRef repairedItqanRotationStart() {
        if (isRotationStartValid()) return itqanRotationStart();
        VerseRef repaired = itqanWorkCorpus().getRanges().get(0).getStart();
        setItqanRotationStart(repaired);
        return repaired;
    }

    public VerseRef itqanCursor() { return ref("itqanCursor"); }
    public VerseRef murajaahCursor() { return ref("murajaahCursor"); }
    public void setItqanCursor(VerseRef value) { putRef("itqanCursor", value); }
    public void setMurajaahCursor(VerseRef value) { putRef("murajaahCursor", value); }

    /** All base Itqan plus every snowball promotion, irrespective of consolidation status. */
    public EligibleCorpus itqanWorkCorpus() {
        return EligibleCorpus.Companion.of(effectiveItqanRanges());
    }

    /** Compatibility alias while the runtime migration is staged. */
    public EligibleCorpus corpus() { return itqanWorkCorpus(); }

    /**
     * Murajaah sees base Itqan, historical promoted ranges retained during v2->v3 migration, and
     * every promoted verse — including freshly learned material still awaiting its Stabilisation
     * pass. That inclusion is deliberate: at the current pace a page can otherwise wait most of a
     * year for its first Itqan turn, with no exposure at all in between (see the "why isn't a
     * just-learned surah in Révision" discussion). A page still counts as "unconsolidated" for
     * AnchoringQueue/promotion bookkeeping (unconsolidatedPromotedRanges) until it clears
     * Stabilisation and the weekly Consolidation snowball; that status just no longer hides it
     * from the daily Entretien / weekly Révision finale reading pool.
     */
    public EligibleCorpus murajaahCorpus() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(legacyMurajaahPromotedRanges());
        all.addAll(promotedRanges());
        return EligibleCorpus.Companion.of(all);
    }

    public boolean isItqanCursorValid() { try { return itqanWorkCorpus().contains(itqanCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isMurajaahCursorValid() { try { return murajaahCorpus().contains(murajaahCursor()); } catch (RuntimeException e) { return false; } }
    public boolean isRotationStartValid() { try { return itqanWorkCorpus().contains(itqanRotationStart()); } catch (RuntimeException e) { return false; } }

    /** Every promoted range that has already cleared Consolidation — "Pages promues" minus "À stabiliser". */
    public List<VerseRange> consolidatedPromotedRanges() {
        List<VerseRange> settled = new ArrayList<>(promotedRanges());
        for (VerseRange pending : unconsolidatedPromotedRanges()) {
            settled = subtractCoverage(settled, pending.getStart(), pending.getEndInclusive());
        }
        return settled;
    }

    /**
     * Révision active only reads genuinely settled material — the declared-acquired base, historical
     * migration ranges, and every promoted range that has already cleared Consolidation — never the
     * "à stabiliser" subset still mid-Stabilisation/Consolidation. Testing recall from memory on
     * material that hasn't finished its own repetitions yet is discouraging, not constructive; passive
     * Entretien is unaffected and keeps reading everything (murajaahCorpus), fresh material included.
     */
    public EligibleCorpus activeMurajaahCorpus() {
        ArrayList<VerseRange> all = new ArrayList<>(itqanRanges());
        all.addAll(legacyMurajaahPromotedRanges());
        all.addAll(consolidatedPromotedRanges());
        return EligibleCorpus.Companion.of(all);
    }

    /**
     * Independent from murajaahCursor: active and passive now read different corpora (active
     * excludes "à stabiliser"), so they can no longer share one traversal position. Defaults to
     * the active corpus's own first range rather than a hardcoded verse, since which surahs are
     * actually settled varies per learner.
     */
    public VerseRef activeMurajaahCursor() {
        String raw = p.getString("activeMurajaahCursor", "");
        if (!raw.isEmpty()) {
            try { return GeometryRepository.parseVerse(raw); } catch (RuntimeException malformed) { /* fall through to default */ }
        }
        return activeMurajaahCorpus().getRanges().get(0).getStart();
    }

    public void setActiveMurajaahCursor(VerseRef value) { putRef("activeMurajaahCursor", value); }

    public boolean isActiveMurajaahCursorValid() {
        try { return activeMurajaahCorpus().contains(activeMurajaahCursor()); } catch (RuntimeException e) { return false; }
    }

    /**
     * Verses flagged during Révision active as a personal weak spot (light E-Ink-safe outline,
     * visible in both active and passive). Global, not scoped to a corpus range: a struggle is a
     * struggle wherever the verse sits. Phase 3 will add automatic decay after clean active
     * recalls; for now a flag only clears by tapping the same verse again while marking.
     */
    public List<VerseRef> murajaahWeakVerses() {
        LinkedHashSet<VerseRef> unique = new LinkedHashSet<>();
        try {
            JSONArray array = new JSONArray(p.getString("murajaahWeakVerses", "[]"));
            for (int i = 0; i < array.length(); i++) {
                try { unique.add(GeometryRepository.parseVerse(array.getString(i))); }
                catch (Exception malformedEntry) { /* drop a single corrupt entry, keep the rest */ }
            }
        } catch (Exception malformed) {
            return Collections.emptyList();
        }
        return new ArrayList<>(unique);
    }

    public boolean toggleMurajaahWeakVerse(VerseRef verse) {
        if (verse == null) return false;
        LinkedHashSet<VerseRef> current = new LinkedHashSet<>(murajaahWeakVerses());
        boolean removed = current.remove(verse);
        if (!removed) current.add(verse);
        JSONArray array = new JSONArray();
        for (VerseRef flagged : current) array.put(flagged.toString());
        SharedPreferences.Editor editor = p.edit().putString("murajaahWeakVerses", array.toString());
        if (removed) {
            java.util.Map<VerseRef, Integer> streaks = weakVerseStreaks();
            if (streaks.remove(verse) != null) editor.putString("murajaahWeakVerseStreaks", weakVerseStreaksJson(streaks));
        }
        return editor.commit();
    }

    /** How many clean active recalls in a row auto-clear a weak-spot flag (see advanceWeakVerseStreaks). */
    public static final int WEAK_VERSE_CLEAN_STREAK_TO_CLEAR = 3;

    /** 0 if never attempted since being flagged (or not flagged at all); otherwise its current clean-recall count, &lt; WEAK_VERSE_CLEAN_STREAK_TO_CLEAR. */
    public int weakVerseStreak(VerseRef verse) {
        Integer streak = weakVerseStreaks().get(verse);
        return streak == null ? 0 : streak;
    }

    private java.util.Map<VerseRef, Integer> weakVerseStreaks() {
        LinkedHashMap<VerseRef, Integer> out = new LinkedHashMap<>();
        try {
            JSONObject object = new JSONObject(p.getString("murajaahWeakVerseStreaks", "{}"));
            java.util.Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                try { out.put(GeometryRepository.parseVerse(key), object.optInt(key, 0)); }
                catch (Exception malformedEntry) { /* drop a single corrupt entry, keep the rest */ }
            }
        } catch (Exception malformed) {
            return new LinkedHashMap<>();
        }
        return out;
    }

    private static String weakVerseStreaksJson(java.util.Map<VerseRef, Integer> streaks) {
        JSONObject object = new JSONObject();
        try {
            for (java.util.Map.Entry<VerseRef, Integer> entry : streaks.entrySet()) object.put(entry.getKey().toString(), entry.getValue());
        } catch (Exception impossible) { /* JSONObject.put(String,int) never throws */ }
        return object.toString();
    }

    /**
     * Advances each flagged verse's clean-active-recall streak after today's Révision active pass
     * — a verse covered without any reveal on its page counts toward automatically clearing its
     * flag after WEAK_VERSE_CLEAN_STREAK_TO_CLEAR such passes; a verse whose page WAS revealed
     * resets its streak to 0 instead. Passive viewing never calls this — only active recall moves
     * the streak, since only active tests whether the verse is actually held from memory. Returns
     * the (possibly now smaller) set of still-flagged verses.
     */
    public List<VerseRef> advanceWeakVerseStreaks(java.util.Collection<VerseRef> cleanThisSession,
                                                  java.util.Collection<VerseRef> revealedThisSession) {
        LinkedHashSet<VerseRef> weak = new LinkedHashSet<>(murajaahWeakVerses());
        if (weak.isEmpty()) return new ArrayList<>(weak);
        java.util.Map<VerseRef, Integer> streaks = weakVerseStreaks();
        boolean changed = false;
        if (cleanThisSession != null) {
            for (VerseRef verse : cleanThisSession) {
                if (!weak.contains(verse)) continue;
                changed = true;
                int next = streaks.getOrDefault(verse, 0) + 1;
                if (next >= WEAK_VERSE_CLEAN_STREAK_TO_CLEAR) {
                    weak.remove(verse);
                    streaks.remove(verse);
                } else {
                    streaks.put(verse, next);
                }
            }
        }
        if (revealedThisSession != null) {
            for (VerseRef verse : revealedThisSession) {
                if (weak.contains(verse) && streaks.remove(verse) != null) changed = true;
            }
        }
        if (!changed) return new ArrayList<>(weak);
        JSONArray array = new JSONArray();
        for (VerseRef verse : weak) array.put(verse.toString());
        p.edit()
            .putString("murajaahWeakVerses", array.toString())
            .putString("murajaahWeakVerseStreaks", weakVerseStreaksJson(streaks))
            .commit();
        return new ArrayList<>(weak);
    }

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
        return p.edit()
            .putString("recentSabqi", recentJson(queue))
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

    /** Atomically closes Apprentissage and advances exactly these physical lines to Appris. */
    public boolean completeSabqiBlockV6(int startLine, int endLine, int nextLineCursor,
                                        List<String> lineIds, String date, String label) {
        if (lineIds == null || lineIds.isEmpty()) throw new IllegalArgumentException("Apprentissage line ids required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            for (String lineId : new LinkedHashSet<>(lineIds)) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
                ProgressState state = progressStateFromSets(lineId, learned, stabilized, acquired);
                if (state == ProgressState.NONE) learned.add(lineId);
            }
            List<RecentSabqi> queue = recentSabqi();
            LocalDate blockDate = safeDate(date, null);
            if (blockDate == null) blockDate = HifzClock.today();
            queue.add(new RecentSabqi(startLine, endLine, blockDate, 0));
            java.util.Map<String, String> snowball = weeklySnowballAppendEntries(
                ConsolidationCycleEngine.Family.LEARNING, lineIds, blockDate);
            SharedPreferences.Editor e = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putString("recentSabqi", recentJson(queue))
                .putInt("sabqiLineCursor", nextLineCursor)
                .putInt("sabqiRep", 0).putInt("sabqiAssisted", 0).putLong("sabqiElapsedMs", 0L)
                .putString("sabqiTodayReviewDate", date).putInt("sabqiTodayReviewStartLine", startLine)
                .putInt("sabqiTodayReviewEndLine", endLine).putLong("sabqi_today_reviewElapsedMs", 0L)
                .putString("lastSabqiDate", date).putString("lastSabqiLabel", label);
            for (java.util.Map.Entry<String, String> entry : snowball.entrySet()) e.putString(entry.getKey(), entry.getValue());
            return e.commit();
        }
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

    /** Atomically advances one frozen Stabilisation half-page and its schema6 line state. */
    boolean completeStabilizationBlockV6(List<String> lineIds, int nextBlockIndex, boolean finalBlock,
                                         VerseRef unitStart, VerseRef unitEnd, VerseRef nextCursor,
                                         String date, String label) {
        if (lineIds == null || lineIds.isEmpty() || unitStart == null || unitEnd == null)
            throw new IllegalArgumentException("Stabilisation unit required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            for (String lineId : new LinkedHashSet<>(lineIds)) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId))
                    throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
                // Ancrage material enters directly from "À ancrer" (the default/bootstrap
                // corpus or a manually added range): it never passes through Apprentissage,
                // so NONE is its normal starting state here, not an error. A LEARNED line is
                // only ever a pre-redesign migration leftover, kept for compatibility.
                ProgressState state = progressStateFromSets(lineId, learned, stabilized, acquired);
                if (state == ProgressState.NONE || state == ProgressState.LEARNED) {
                    learned.remove(lineId);
                    stabilized.add(lineId);
                }
            }
            LocalDate blockDate = safeDate(date, HifzClock.today());
            java.util.Map<String, String> snowball = weeklySnowballAppendEntries(
                ConsolidationCycleEngine.Family.STABILIZATION, lineIds, blockDate);
            SharedPreferences.Editor e = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putInt("itqanRep", 0).putInt("itqanAssisted", 0).putInt("itqanFinalReveals", 0)
                .putInt("itqanBlockIndex", finalBlock ? 0 : Math.max(0, nextBlockIndex))
                .putString("itqanUnitStart", finalBlock ? "" : unitStart.toString())
                .putString("itqanUnitEnd", finalBlock ? "" : unitEnd.toString())
                .putLong("itqanElapsedMs", 0L)
                .putString("lastItqanDate", date).putString("lastItqanLabel", label)
                .putString("lastItqanCreditStart", unitStart.toString())
                .putString("lastItqanCreditEnd", unitEnd.toString());
            for (java.util.Map.Entry<String, String> entry : snowball.entrySet()) e.putString(entry.getKey(), entry.getValue());
            if (finalBlock && nextCursor != null) e.putString("itqanCursor", nextCursor.toString());
            return e.commit();
        }
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
                        AnchoringQueue.ItqanProtocol.valueOf(o.getString("protocol")),
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
                    GeometryRepository.VerseUnit unit = geometry.eligibleWeeklyStabilizationUnit(
                        cursor, range.getEndInclusive(), pendingCorpus);
                    // A page portion whose verses all sit on lines owned by an earlier verse has no
                    // physical Stabilisation unit. Queueing it would block the queue (never complete).
                    if (!CorpusLinePolicy.ownedLineIdsForRangeOnPage(unit.start, unit.end, geometry).isEmpty()) {
                        String key = anchoringKey(unit.start.toString(), unit.end.toString());
                        boolean reconstruction = ordinalBetween(unit.start, new VerseRef(49, 1), new VerseRef(114, 6));
                        boolean forced = !reconstruction && overlaps(forcedRanges, unit.start, unit.end);
                        expected.put(key, new AnchoringQueue.Entry(unit.start.toString(), unit.end.toString(),
                            AnchoringQueue.originFor(reconstruction, forced),
                            reconstruction ? AnchoringQueue.ItqanProtocol.LIGHT : AnchoringQueue.ItqanProtocol.FULL, 0));
                    }
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

    private boolean entryIsFullyStabilizedOrAcquired(AnchoringQueue.Entry entry, GeometryRepository geometry) {
        if (entry == null) return false;
        LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
        LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
        List<String> ids = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end), geometry);
        if (ids.isEmpty()) return true; // ownerless entry: nothing physical to stabilise, never block the queue
        for (String id : ids) if (!stabilized.contains(id) && !acquired.contains(id)) return false;
        return true;
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
            if (!p.edit().putString("anchoringRetryAfterDate", "").commit())
                throw new IllegalStateException("Unable to clear Stabilisation retry deferral");
        }
        List<AnchoringQueue.Entry> queue = anchoringQueue();
        if (!p.getBoolean("anchoringQueueInitialized", false)) {
            if (!reconcileAnchoringQueue(geometry)) throw new IllegalStateException("Unable to repair Stabilisation queue");
            queue = anchoringQueue();
        }
        AnchoringQueue.Entry inProgress = inProgressAnchoringEntry();
        if (inProgress != null) return inProgress;
        if (queue.isEmpty()) return null;
        int start = anchoringQueueIndex(queue.size());
        for (int step = 0; step < queue.size(); step++) {
            int index = (start + step) % queue.size();
            AnchoringQueue.Entry candidate = queue.get(index);
            if (!entryIsFullyStabilizedOrAcquired(candidate, geometry)) {
                if (index != start && !p.edit().putInt("anchoringQueueIndex", index).commit())
                    throw new IllegalStateException("Unable to advance Stabilisation queue");
                return candidate;
            }
        }
        return null;
    }

    private static String snowballAnchorKey(ConsolidationCycleEngine.Family family) {
        return family == ConsolidationCycleEngine.Family.LEARNING
            ? "learningSnowballWeekAnchor" : "stabilizationSnowballWeekAnchor";
    }

    private static String snowballUnitsKey(ConsolidationCycleEngine.Family family) {
        return family == ConsolidationCycleEngine.Family.LEARNING
            ? "learningSnowballUnitIds" : "stabilizationSnowballUnitIds";
    }

    private static String snowballHistoryKey(ConsolidationCycleEngine.Family family) {
        return family == ConsolidationCycleEngine.Family.LEARNING
            ? "learningSnowball8WeekHistory" : "stabilizationSnowball8WeekHistory";
    }

    private static final int SNOWBALL_EXTENDED_WEEKS = 8;

    private static LocalDate mondayOf(LocalDate date) {
        return date.minusDays(date.getDayOfWeek().getValue() - 1L);
    }

    /**
     * This week's (Monday-anchored) accumulated snowball units, or an empty array if the stored
     * accumulator belongs to an earlier, never Sunday-reviewed week — that week's evening work is
     * discarded rather than carried forward or allowed to grow past the configured weekly day
     * count for this family (learningDaysPerWeek/itqanDaysPerWeek).
     */
    private JSONArray rolledSnowballUnits(ConsolidationCycleEngine.Family family, LocalDate today) {
        String weekAnchor = mondayOf(today).toString();
        if (!weekAnchor.equals(p.getString(snowballAnchorKey(family), ""))) return new JSONArray();
        try { return new JSONArray(p.getString(snowballUnitsKey(family), "[]")); }
        catch (Exception error) { return new JSONArray(); }
    }

    /**
     * The {anchor, units, history} triple a state-changing edit should merge in atomically so
     * today's fresh Appris/Stabilisé physical unit joins this week's Renforcement/Consolidation
     * snowball, and the rolling 8-week history Sunday's extended review reads from, in the very
     * same commit that created it.
     */
    private java.util.Map<String, String> weeklySnowballAppendEntries(
            ConsolidationCycleEngine.Family family, List<String> unitLineIds, LocalDate today) {
        JSONArray current = rolledSnowballUnits(family, today);
        int weeklyCap = family == ConsolidationCycleEngine.Family.LEARNING
            ? learningDaysPerWeek() : itqanDaysPerWeek();
        if (current.length() < weeklyCap) current.put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(unitLineIds));
        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
        out.put(snowballAnchorKey(family), mondayOf(today).toString());
        out.put(snowballUnitsKey(family), current.toString());
        out.put(snowballHistoryKey(family), appendToSnowballHistory(family, unitLineIds, today).toString());
        return out;
    }

    /**
     * Rolling 8-calendar-week (Monday-anchored) history of every physical block promoted this
     * family's way, retained so Sunday's extended review (stabilizationSnowballFinalUnits/
     * learningSnowballFinalUnits) can re-read the current + 7 previous weeks' material instead of
     * just the current week. Pruned to the current + 7 previous week-entries on every append —
     * except a week whose blocks haven't all graduated to Acquis yet (weekFullyAcquired), which is
     * kept regardless of age so a long absence spanning an unrun Sunday can never quietly drop a
     * block that was never actually promoted.
     */
    private JSONArray appendToSnowballHistory(
            ConsolidationCycleEngine.Family family, List<String> unitLineIds, LocalDate today) {
        String weekAnchor = mondayOf(today).toString();
        JSONArray history;
        try { history = new JSONArray(p.getString(snowballHistoryKey(family), "[]")); }
        catch (Exception malformed) { history = new JSONArray(); }

        try {
            JSONObject currentWeek = null;
            for (int i = 0; i < history.length(); i++) {
                JSONObject week = history.optJSONObject(i);
                if (week != null && weekAnchor.equals(week.optString("week"))) { currentWeek = week; break; }
            }
            if (currentWeek == null) {
                currentWeek = new JSONObject().put("week", weekAnchor).put("units", new JSONArray());
                history.put(currentWeek);
            }
            currentWeek.getJSONArray("units").put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(unitLineIds));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to update the snowball's 8-week history", error);
        }

        LocalDate cutoff = mondayOf(today).minusWeeks(SNOWBALL_EXTENDED_WEEKS - 1L);
        LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
        JSONArray pruned = new JSONArray();
        for (int i = 0; i < history.length(); i++) {
            JSONObject week = history.optJSONObject(i);
            if (week == null) continue;
            LocalDate weekStart = safeDate(week.optString("week"), null);
            if (weekStart == null) continue;
            if (!weekStart.isBefore(cutoff) || !weekFullyAcquired(week, acquired)) pruned.put(week);
        }
        return pruned;
    }

    /**
     * A week older than the 8-week cutoff is only safe to drop once every block it produced has
     * actually been graduated to Acquis — otherwise a long absence spanning an unrun Sunday could
     * silently retire a block that never got its promotion pass, leaving it stuck at Stabilisé/
     * Appris forever. An ungraduated week is kept past the cutoff and simply keeps resurfacing in
     * extendedSnowballUnits until the next Sunday that actually runs graduates it.
     */
    private boolean weekFullyAcquired(JSONObject week, java.util.Set<String> acquired) {
        JSONArray units = week.optJSONArray("units");
        if (units == null) return true;
        for (int u = 0; u < units.length(); u++) {
            String encoded = units.optString(u, "");
            if (encoded.isEmpty()) continue;
            if (!acquired.containsAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(encoded))) return false;
        }
        return true;
    }

    /**
     * This week's accumulated snowball units, tagged for the given review pass: each day's block
     * on its own, plus — once there are at least two — one extra unit combining every accumulated
     * block's lines into a single continuous pass, so the growing week is also read as one whole,
     * not just as separate blocks.
     */
    private List<ConsolidationCycleEngine.Unit> weeklySnowballUnits(
            ConsolidationCycleEngine.Family family, LocalDate today, ConsolidationCycleEngine.Protocol protocol) {
        JSONArray current = rolledSnowballUnits(family, today);
        ArrayList<String> encodedBlocks = new ArrayList<>();
        for (int i = 0; i < current.length(); i++) encodedBlocks.add(current.optString(i));
        ArrayList<ConsolidationCycleEngine.Unit> out = new ArrayList<>();
        for (String encoded : encodedBlocks) out.add(new ConsolidationCycleEngine.Unit(encoded, protocol));
        if (encodedBlocks.size() > 1) {
            out.add(new ConsolidationCycleEngine.Unit(
                ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(encodedBlocks), protocol));
        }
        return Collections.unmodifiableList(out);
    }

    /** Clears this week's snowball accumulator once its Sunday ×10 final review has graduated it. */
    boolean clearWeeklySnowball(ConsolidationCycleEngine.Family family) {
        return p.edit().putString(snowballUnitsKey(family), "[]").commit();
    }

    /** Tonight's Consolidation snowball: this week's accumulated Stabilisation units, ×10 each. */
    List<ConsolidationCycleEngine.Unit> stabilizedConsolidationUnits(LocalDate today) {
        return weeklySnowballUnits(ConsolidationCycleEngine.Family.STABILIZATION, today,
            ConsolidationCycleEngine.Protocol.SNOWBALL);
    }

    /** Tonight's Renforcement snowball: this week's accumulated Apprentissage units, ×10 each. */
    List<ConsolidationCycleEngine.Unit> learningConsolidationUnits(LocalDate today) {
        return weeklySnowballUnits(ConsolidationCycleEngine.Family.LEARNING, today,
            ConsolidationCycleEngine.Protocol.SNOWBALL);
    }

    /**
     * Sunday's extended review: every physical block from the current + 7 previous calendar weeks
     * (see appendToSnowballHistory), each read ×3, plus one combined continuous pass over all of
     * them — replacing the old this-week-only ×10 final review so recently learned/stabilized
     * material keeps coming back for roughly 8 weeks instead of dropping out of active review the
     * moment its own week ends. A week past that window still contributes its blocks here as long
     * as any of them hasn't graduated yet (see weekFullyAcquired) — covers a long absence that
     * skipped the Sunday session(s) that would otherwise have promoted them.
     */
    private List<ConsolidationCycleEngine.Unit> extendedSnowballUnits(
            ConsolidationCycleEngine.Family family, LocalDate today) {
        JSONArray history;
        try { history = new JSONArray(p.getString(snowballHistoryKey(family), "[]")); }
        catch (Exception malformed) { history = new JSONArray(); }
        LocalDate cutoff = mondayOf(today).minusWeeks(SNOWBALL_EXTENDED_WEEKS - 1L);
        LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");

        ArrayList<String> encodedBlocks = new ArrayList<>();
        for (int i = 0; i < history.length(); i++) {
            JSONObject week = history.optJSONObject(i);
            if (week == null) continue;
            LocalDate weekStart = safeDate(week.optString("week"), null);
            if (weekStart == null) continue;
            boolean withinWindow = !weekStart.isBefore(cutoff);
            if (!withinWindow && weekFullyAcquired(week, acquired)) continue;
            JSONArray units = week.optJSONArray("units");
            if (units == null) continue;
            for (int u = 0; u < units.length(); u++) {
                String encoded = units.optString(u, "");
                if (!encoded.isEmpty()) encodedBlocks.add(encoded);
            }
        }

        // A week is only ever retained past the cutoff for being ungraduated (weekFullyAcquired
        // above), which normally self-limits — but a long enough absence from Sunday specifically,
        // combined with continued weekday practice, could in principle still pile up more blocks
        // than the cycle's cap. History is append-ordered oldest-first, so keep the oldest (most
        // overdue) ones and let the rest wait for a later Sunday rather than overflow the cycle.
        int cap = ConsolidationCycleEngine.maxUnitsFor(ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED) - 1;
        if (encodedBlocks.size() > cap) encodedBlocks = new ArrayList<>(encodedBlocks.subList(0, cap));

        ArrayList<ConsolidationCycleEngine.Unit> out = new ArrayList<>();
        for (String encoded : encodedBlocks) {
            out.add(new ConsolidationCycleEngine.Unit(encoded, ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED));
        }
        if (encodedBlocks.size() > 1) {
            out.add(new ConsolidationCycleEngine.Unit(
                ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(encodedBlocks),
                ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Sunday's extended review: every physical Stabilisation block from the current + 7 previous
     * weeks, ×3 each plus one combined pass, then graduated to Acquis — replacing the old
     * this-week-only ×10 final review (see extendedSnowballUnits).
     */
    List<ConsolidationCycleEngine.Unit> stabilizationSnowballFinalUnits(LocalDate today) {
        return extendedSnowballUnits(ConsolidationCycleEngine.Family.STABILIZATION, today);
    }

    /**
     * Sunday's extended review: every physical Apprentissage block from the current + 7 previous
     * weeks, ×3 each plus one combined pass, then graduated to Acquis — replacing the old
     * this-week-only ×10 final review (see extendedSnowballUnits).
     */
    List<ConsolidationCycleEngine.Unit> learningSnowballFinalUnits(LocalDate today) {
        return extendedSnowballUnits(ConsolidationCycleEngine.Family.LEARNING, today);
    }

    public String lastLearningSnowballEveningDate() { return p.getString("lastLearningSnowballEveningDate", ""); }
    public String lastStabilizationSnowballEveningDate() { return p.getString("lastStabilizationSnowballEveningDate", ""); }

    /** Closes tonight's evening snowball repetitions without graduating anything (Sunday does that). */
    boolean completeSnowballEvening(ConsolidationCycleEngine.Family family, String date) {
        String key = family == ConsolidationCycleEngine.Family.LEARNING
            ? "lastLearningSnowballEveningDate" : "lastStabilizationSnowballEveningDate";
        return p.edit().putString(key, date).remove(consolidationStateKey(family)).commit();
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

    /** Active's own progress markers — independent from passive's, since they now read different corpora. */
    public VerseRef activeMurajaahActualEnd() { return optionalRef("activeMurajaahActualEnd"); }
    public void setActiveMurajaahActualEnd(VerseRef value) {
        p.edit().putString("activeMurajaahActualEnd", value == null ? "" : value.toString()).apply();
    }
    public int activeMurajaahPage() { return p.getInt("activeMurajaahPage", 0); }
    public void setActiveMurajaahPage(int value) {
        if (value >= 1 && value <= 604) p.edit().putInt("activeMurajaahPage", value).apply();
        else p.edit().remove("activeMurajaahPage").apply();
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

    /**
     * Closes the daily Révision active (masked recall test): advances activeMurajaahCursor, its own
     * independent traversal position over activeMurajaahCorpus (settled material only) — no longer
     * tied to murajaahCursor/murajaahCorpus, since active deliberately excludes "à stabiliser"
     * material that passive still reads. Only the day's label is kept; no historical trend.
     */
    public boolean completeActiveMurajaah(VerseRef nextCursor, String date, String label) {
        if (nextCursor == null) return false;
        return p.edit()
            .putString("activeMurajaahCursor", nextCursor.toString())
            .putString("activeMurajaahActualEnd", "")
            .remove("activeMurajaahPage")
            .putString("lastActiveMurajaahDate", date)
            .putString("lastActiveMurajaahLabel", label)
            .commit();
    }

    private static String consolidationStateKey(ConsolidationCycleEngine.Family family) {
        if (family == null) throw new IllegalArgumentException("Consolidation family required");
        switch (family) {
            case LEARNING:
                return "v6ConsolidationLearningState";
            case STABILIZATION:
                return "v6ConsolidationStabilizationState";
            default:
                throw new IllegalArgumentException("Unsupported Consolidation family: " + family);
        }
    }

    /** Persist one OPEN grouped Consolidation session without touching individual counters. */
    boolean persistConsolidationSession(ConsolidationCycleEngine.Session session) {
        if (session == null) throw new IllegalArgumentException("Consolidation session required");
        if (!session.open()) throw new IllegalStateException("Only an OPEN Consolidation session can be persisted");
        ConsolidationCycleEngine.Cycle cycle = session.cycle();
        if (cycle == null || cycle.family() == null || cycle.units().isEmpty()) {
            throw new IllegalStateException("Consolidation cycle/family required");
        }
        int cap = ConsolidationCycleEngine.maxUnitsFor(cycle.units().get(0).protocol());
        if (session.sessionGroupSize() < 1 || session.sessionGroupSize() > cap
                || cycle.units().size() != session.sessionGroupSize()) {
            throw new IllegalStateException("Invalid frozen Consolidation session size");
        }
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("cycleId", cycle.cycleId());
            root.put("family", cycle.family().name());
            root.put("sessionId", session.sessionId());
            root.put("sessionGroupSize", session.sessionGroupSize());
            root.put("stage", session.stage());
            root.put("nextUnitIndex", session.nextUnitIndex());
            root.put("donePerStage", session.donePerStage());
            root.put("open", true);

            JSONArray units = new JSONArray();
            for (ConsolidationCycleEngine.Unit unit : cycle.units()) {
                JSONObject encoded = new JSONObject();
                encoded.put("id", unit.id());
                encoded.put("protocol", unit.protocol().name());
                units.put(encoded);
            }
            root.put("units", units);
            return p.edit()
                .putString(consolidationStateKey(cycle.family()), root.toString())
                .commit();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to persist Consolidation session", error);
        }
    }

    /** Restore the exact frozen OPEN grouped session after process recreation; corruption fails closed. */
    ConsolidationCycleEngine.Session restoreConsolidationSession(
            ConsolidationCycleEngine engine, ConsolidationCycleEngine.Family family) {
        if (engine == null) throw new IllegalArgumentException("Consolidation engine required");
        String raw = p.getString(consolidationStateKey(family), null);
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(raw);
            if (root.getInt("version") != 1) {
                throw new IllegalStateException("Unsupported Consolidation persistence version");
            }
            if (!family.name().equals(root.getString("family"))) {
                throw new IllegalStateException("Consolidation family mismatch");
            }
            if (!root.getBoolean("open")) {
                throw new IllegalStateException("Persisted Consolidation session is not OPEN");
            }

            int groupSize = root.getInt("sessionGroupSize");
            JSONArray units = root.getJSONArray("units");
            if (groupSize < 1 || units.length() != groupSize) {
                throw new IllegalStateException("Corrupt frozen Consolidation group size");
            }

            ConsolidationCycleEngine.Cycle cycle = null;
            for (int i = 0; i < units.length(); i++) {
                JSONObject encoded = units.getJSONObject(i);
                String id = encoded.getString("id");
                ConsolidationCycleEngine.Protocol protocol =
                    ConsolidationCycleEngine.Protocol.valueOf(encoded.getString("protocol"));
                ConsolidationCycleEngine.Unit unit = new ConsolidationCycleEngine.Unit(id, protocol);
                if (i == 0) {
                    cycle = engine.startCycle(root.getString("cycleId"), family, unit);
                } else {
                    cycle = engine.addUnit(cycle, unit);
                }
            }
            if (cycle == null) throw new IllegalStateException("Persisted Consolidation session has no units");

            return engine.restoreOpenSession(
                cycle,
                root.getString("sessionId"),
                groupSize,
                root.getInt("stage"),
                root.getInt("nextUnitIndex"),
                root.getInt("donePerStage"));
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt persisted Consolidation session", error);
        }
    }

    /**
     * Abandons a frozen OPEN grouped session without marking the evening as validated — unlike
     * completeSnowballEvening, this does NOT set lastLearningSnowballEveningDate/
     * lastStabilizationSnowballEveningDate, since the evening's repetitions were not actually
     * finished. Used when the week's own accumulator has grown a new block since this session was
     * opened (e.g. a session frozen at 2 units on Wednesday, still unfinished when Friday's own
     * block appears): the stale session's unfinished progress on its own units is discarded so a
     * fresh session covering every block accumulated so far — including the new one — can open in
     * its place, rather than silently finishing out a session that can no longer see it.
     */
    boolean discardConsolidationSession(ConsolidationCycleEngine.Family family) {
        return p.edit().remove(consolidationStateKey(family)).commit();
    }

    /**
     * The session's real per-day physical blocks, excluding the weekly snowball's synthetic
     * "continuous" pass — both completion methods are only ever reached from a Sunday final review
     * (weeklySnowballUnits' only caller for grouped completion), where a session with more than one
     * unit always has that continuous unit last, already covered line-for-line by the blocks before
     * it, so graduating it again would double-process the same lines.
     */
    private static List<String> physicalUnitIds(ConsolidationCycleEngine.Session session) {
        List<String> unitIds = session.unitIds();
        return unitIds.size() > 1 ? unitIds.subList(0, unitIds.size() - 1) : unitIds;
    }

    /** Atomically closes a frozen 1..3 physical-unit Consolidation group and makes its exact lines Acquired. */
    boolean completeConsolidationSessionV6(ConsolidationCycleEngine.Session session, GeometryRepository geometry,
                                           String date, String label) {
        if (session == null || !session.open() || !session.readyToClose())
            throw new IllegalStateException("Consolidation session must be OPEN and complete");
        if (geometry == null) throw new IllegalArgumentException("Consolidation geometry required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");

            for (String unitId : physicalUnitIds(session)) {
                List<String> ids = ConsolidationPhysicalUnitPolicy.decodeLineUnit(unitId);
                List<GeometryRepository.LineMeta> physical = geometry.linesForExactIds(ids);
                List<StabilizationHalfPagePolicy.Unit> verified = StabilizationHalfPagePolicy.planPage(physical);
                if (verified.size() != 1 || !verified.get(0).lineIds.equals(ids))
                    throw new IllegalStateException("Invalid frozen physical Consolidation unit");
                for (String lineId : ids) {
                    ProgressState state = progressStateFromSets(lineId, learned, stabilized, acquired);
                    if (state == ProgressState.STABILIZED) {
                        stabilized.remove(lineId);
                        acquired.add(lineId);
                    } else if (state != ProgressState.ACQUIRED) {
                        throw new IllegalStateException("Consolidation requires Stabilisé lines: " + lineId);
                    }
                }
            }

            ArrayList<VerseRange> pending = new ArrayList<>(unconsolidatedPromotedRanges());
            ArrayList<VerseRange> forced = new ArrayList<>(forcedPromotedRanges());
            ArrayList<AnchoringQueue.Entry> originalQueue = new ArrayList<>(anchoringQueue());
            int oldIndex = anchoringQueueIndex(originalQueue.size());
            ArrayList<AnchoringQueue.Entry> keptQueue = new ArrayList<>();
            for (AnchoringQueue.Entry entry : originalQueue) {
                List<String> parentIds = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
                    GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end), geometry);
                boolean allAcquired = !parentIds.isEmpty() && acquired.containsAll(parentIds);
                if (allAcquired) {
                    VerseRef start = GeometryRepository.parseVerse(entry.start);
                    VerseRef end = GeometryRepository.parseVerse(entry.end);
                    pending = new ArrayList<>(subtractCoverage(pending, start, end));
                    forced = new ArrayList<>(subtractCoverage(forced, start, end));
                } else {
                    keptQueue.add(entry);
                }
            }

            int nextIndex = 0;
            if (!keptQueue.isEmpty() && !originalQueue.isEmpty()) {
                outer:
                for (AnchoringQueue.Entry visit : AnchoringQueue.visitOrder(originalQueue, oldIndex)) {
                    for (int i = 0; i < keptQueue.size(); i++) {
                        AnchoringQueue.Entry kept = keptQueue.get(i);
                        if (kept.start.equals(visit.start) && kept.end.equals(visit.end)) {
                            nextIndex = i;
                            break outer;
                        }
                    }
                }
            }

            SharedPreferences.Editor editor = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putString("unconsolidatedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(pending)))
                .putString("forcedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(forced)))
                .putString("anchoringQueue", anchoringQueueJson(keptQueue))
                .putBoolean("anchoringQueueInitialized", true).putInt("anchoringQueueIndex", nextIndex)
                .putString("lastRecentSabqiReviewDate", date).putString("lastRecentSabqiReviewLabel", label)
                .putString(snowballUnitsKey(ConsolidationCycleEngine.Family.STABILIZATION), "[]")
                .remove(consolidationStateKey(ConsolidationCycleEngine.Family.STABILIZATION));
            return editor.commit();
        }
    }

    /**
     * Atomically closes a frozen 1..3 unit Sabqi Renforcement group and sends its exact physical
     * lines from Appris directly to Acquis. Sabqi never passes through Stabilisation/À-ancrer:
     * these verses enter the acquired corpus (and leave any pending/forced overlap) in this one
     * step, replacing the old attendance/age-gated rebalanceRecentWindow promotion for the units
     * this session actually reinforced.
     */
    boolean completeLearningConsolidationSessionV6(ConsolidationCycleEngine.Session session, GeometryRepository geometry,
                                                    String date, String label) {
        if (session == null || !session.open() || !session.readyToClose())
            throw new IllegalStateException("Consolidation session must be OPEN and complete");
        if (geometry == null) throw new IllegalArgumentException("Consolidation geometry required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");

            ArrayList<RecentSabqi> processed = new ArrayList<>();
            ArrayList<VerseRef> versesToPromote = new ArrayList<>();

            for (String unitId : physicalUnitIds(session)) {
                List<String> ids = ConsolidationPhysicalUnitPolicy.decodeLineUnit(unitId);
                List<GeometryRepository.LineMeta> physical = geometry.linesForExactIds(ids);
                if (physical.size() != ids.size())
                    throw new IllegalStateException("Invalid frozen physical Renforcement unit");
                for (String lineId : ids) {
                    ProgressState state = progressStateFromSets(lineId, learned, stabilized, acquired);
                    if (state == ProgressState.LEARNED) { learned.remove(lineId); acquired.add(lineId); }
                    else if (state != ProgressState.ACQUIRED) {
                        throw new IllegalStateException("Renforcement requires Appris lines: " + lineId);
                    }
                }
                int startLine = physical.get(0).globalIndex;
                int endLine = physical.get(physical.size() - 1).globalIndex;
                processed.add(new RecentSabqi(startLine, endLine));
                versesToPromote.addAll(geometry.versesFullyCoveredByLines(startLine, endLine));
            }

            List<RecentSabqi> remaining = withoutRecentBlocks(recentSabqi(), processed);

            ArrayList<VerseRef> fresh = new ArrayList<>();
            for (VerseRef verse : versesToPromote) if (!isPromoted(verse)) fresh.add(verse);
            fresh.sort(Comparator.comparingInt(GeometryRepository::ordinal));
            ArrayList<VerseRange> allPromoted = new ArrayList<>(promotedRanges());
            allPromoted.addAll(rangesFromVerses(fresh));
            List<VerseRange> pending = unconsolidatedPromotedRanges();
            List<VerseRange> forced = forcedPromotedRanges();
            for (VerseRef verse : versesToPromote) {
                pending = subtractCoverage(pending, verse, verse);
                forced = subtractCoverage(forced, verse, verse);
            }

            SharedPreferences.Editor editor = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired))
                .putString("recentSabqi", recentJson(remaining))
                .putString("promotedRanges", rangesJson(sortRangesPreservingBoundaries(allPromoted)))
                .putString("unconsolidatedPromotedRanges", rangesJson(pending))
                .putString("forcedPromotedRanges", rangesJson(forced))
                .putBoolean("anchoringQueueInitialized", false)
                .putString("lastLearningConsolidationDate", date)
                .putString("lastLearningConsolidationLabel", label)
                .putString(snowballUnitsKey(ConsolidationCycleEngine.Family.LEARNING), "[]")
                .remove(consolidationStateKey(ConsolidationCycleEngine.Family.LEARNING));
            return editor.commit();
        }
    }

    public String lastLearningConsolidationDate() { return p.getString("lastLearningConsolidationDate", ""); }
    public String lastLearningConsolidationLabel() { return p.getString("lastLearningConsolidationLabel", ""); }

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
    public String lastActiveMurajaahDate() { return p.getString("lastActiveMurajaahDate", ""); }
    public String lastActiveMurajaahLabel() { return p.getString("lastActiveMurajaahLabel", ""); }

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

    /**
     * How many of the six non-Sunday days a week give their morning to Apprentissage instead of
     * Stabilisation (Sunday stays the fixed Révision day regardless). Defaults to 3, the original
     * Mon/Wed/Fri split; see HifzSchedule.actionFor for how this spreads across the week and
     * ConsolidationCycleEngine.maxUnitsFor for the matching weekly-snowball capacity.
     */
    public int learningDaysPerWeek() {
        int stored = p.getInt("learningDaysPerWeek", HifzSchedule.DEFAULT_LEARNING_DAYS_PER_WEEK);
        return Math.max(HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK,
            Math.min(HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK, stored));
    }

    /** The remaining non-Sunday days, given to Stabilisation. */
    public int itqanDaysPerWeek() { return 6 - learningDaysPerWeek(); }

    public boolean setLearningDaysPerWeek(int days) {
        if (days < HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK || days > HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK) {
            throw new IllegalArgumentException("Learning days per week must be "
                + HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK + ".." + HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK);
        }
        return p.edit().putInt("learningDaysPerWeek", days).commit();
    }

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
        p.edit()
            .putString("recentSabqi", recentJson(queue))
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
            .putString("promotedRanges", rangesJson(sortRangesPreservingBoundaries(all)))
            .putString("unconsolidatedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(pending)))
            .putString("forcedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(forced)))
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
        return ranges;
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
        return sortRangesPreservingBoundaries(out);
    }

    private static List<VerseRange> normalizeRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(EligibleCorpus.Companion.of(ranges).getRanges());
    }

    /** Sorts ranges, merges overlap, and merges adjacency only inside the same surah. */
    private static List<VerseRange> sortRangesPreservingBoundaries(List<VerseRange> ranges) {
        ArrayList<VerseRange> sorted = new ArrayList<>();
        if (ranges == null) return sorted;
        for (VerseRange range : ranges) {
            if (range == null) continue;
            if (GeometryRepository.ordinal(range.getEndInclusive()) < GeometryRepository.ordinal(range.getStart()))
                throw new IllegalArgumentException("Reversed range");
            sorted.add(range);
        }
        sorted.sort(Comparator.comparingInt(range -> GeometryRepository.ordinal(range.getStart())));
        ArrayList<VerseRange> out = new ArrayList<>();
        for (VerseRange range : sorted) {
            if (out.isEmpty()) {
                out.add(range);
                continue;
            }
            VerseRange previous = out.get(out.size() - 1);
            int previousEnd = GeometryRepository.ordinal(previous.getEndInclusive());
            int currentStart = GeometryRepository.ordinal(range.getStart());
            boolean overlaps = currentStart <= previousEnd;
            boolean sameSurahAdjacent = currentStart == previousEnd + 1
                && previous.getEndInclusive().getSurah() == range.getStart().getSurah();
            if (overlaps || sameSurahAdjacent) {
                VerseRef mergedEnd = GeometryRepository.ordinal(range.getEndInclusive()) > previousEnd
                    ? range.getEndInclusive() : previous.getEndInclusive();
                out.set(out.size() - 1, new VerseRange(previous.getStart(), mergedEnd));
            } else {
                out.add(range);
            }
        }
        return out;
    }

    private static List<VerseRange> rangesFromVerses(List<VerseRef> orderedVerses) {
        ArrayList<VerseRange> additions = new ArrayList<>();
        if (orderedVerses.isEmpty()) return additions;
        VerseRef start = orderedVerses.get(0);
        VerseRef previous = start;
        for (int i = 1; i < orderedVerses.size(); i++) {
            VerseRef current = orderedVerses.get(i);
            if (current.getSurah() == previous.getSurah()
                    && GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1) {
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
        return sortRangesPreservingBoundaries(out);
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
