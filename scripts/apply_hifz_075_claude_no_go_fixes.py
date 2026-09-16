#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, got {count}\n--- needle ---\n{old[:500]}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# B1 + M1 — MainActivity
MAIN = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java"
replace_once(
    MAIN,
    '''    private String nextMode(ScheduledCadence due){
        if(due==null)return null;
        LocalDate date=due.getScheduledDate();
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return !modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW)?HifzSessionActivity.SABQI_TODAY_REVIEW:null;
            case STABILIZATION:return !modeComplete(date,HifzSessionActivity.ITQAN)?HifzSessionActivity.ITQAN:null;
            case REVISION:return !modeComplete(date,HifzSessionActivity.MURAJAAH)?HifzSessionActivity.MURAJAAH:null;
            default:return null;
        }
    }

    private void openToday() {
        ScheduledCadence due=nextDueCadence(HifzClock.today());
        String mode=nextMode(due);
        if(mode!=null)openMode(mode,due.getScheduledDate());
    }
''',
    '''    private boolean progressionConsolidationDue(GeometryRepository g){
        if(g==null)return false;
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Session open=prefs.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.STABILIZATION);
        return open!=null || !prefs.stabilizedConsolidationUnits(g,3).isEmpty();
    }

    private String nextMode(ScheduledCadence due){
        if(progressionConsolidationDue(geometry))return HifzSessionActivity.RECENT_SABQI_REVIEW;
        if(due==null)return null;
        LocalDate date=due.getScheduledDate();
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return !modeComplete(date,HifzSessionActivity.SABQI_TODAY_REVIEW)?HifzSessionActivity.SABQI_TODAY_REVIEW:null;
            case STABILIZATION:return !modeComplete(date,HifzSessionActivity.ITQAN)?HifzSessionActivity.ITQAN:null;
            case REVISION:return !modeComplete(date,HifzSessionActivity.MURAJAAH)?HifzSessionActivity.MURAJAAH:null;
            default:return null;
        }
    }

    private void openToday() {
        LocalDate current=HifzClock.today();
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null)return;
        LocalDate scheduled=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ? current : due.getScheduledDate();
        openMode(mode,scheduled);
    }
''')
replace_once(
    MAIN,
    '''        ScheduledCadence due=nextDueCadence(current);
        if(due==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String mode=nextMode(due);
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        String prefix=due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(HifzSessionActivity.SABQI.equals(mode)){
                int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · 5 lignes";
            }else if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)){
                detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(HifzSessionActivity.ITQAN.equals(mode)){
                detail=anchoringTodayDetail(prefs,g);
            }else if(HifzSessionActivity.MURAJAAH.equals(mode)){
                detail="Révision · "+HifzSchedule.MAINTENANCE_MINUTES+" min";
            }else detail="Parcours à vérifier";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        today.setText(prefix+detail);todayAction.setEnabled(true);
''',
    '''        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(mode==null){today.setText("Programme à jour");todayAction.setEnabled(false);return;}
        boolean consolidation=HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode);
        String prefix=!consolidation&&due!=null&&due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        String detail;
        try{
            if(consolidation){
                detail="Consolidation · déclenchée par progression";
            }else if(HifzSessionActivity.SABQI.equals(mode)){
                int cursor=prefs.sabqiLineCursor();if(cursor<0)cursor=g.firstLineIndex(prefs.sabqiStart());
                GeometryRepository.FiveLineBlock b=g.fiveLineBlock(cursor);
                detail="Apprentissage · "+shortRange(b.startVerse,b.endVerse)+" · 5 lignes";
            }else if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)){
                detail="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
            }else if(HifzSessionActivity.ITQAN.equals(mode)){
                detail=anchoringTodayDetail(prefs,g);
            }else if(HifzSessionActivity.MURAJAAH.equals(mode)){
                detail="Révision · "+HifzSchedule.MAINTENANCE_MINUTES+" min";
            }else detail="Parcours à vérifier";
        }catch(RuntimeException error){detail="Parcours à vérifier";}
        today.setText(prefix+detail);todayAction.setEnabled(true);
''')
replace_once(
    MAIN,
    '''    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        boolean fractionated=prefs.isFractionatedUnit(geometry.versesForRange(start,end));
        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
        if(!fractionated)return "Stabilisation · "+shortRange(start,end)+" · ×"+reps;
        int[] segments=geometry.surahSegmentLineCounts(start,end);
        int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(segments));
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        return "Stabilisation · "+shortRange(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
    }
''',
    '''    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
        List<String> owned=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
        List<StabilizationHalfPagePolicy.Unit> planned=StabilizationHalfPagePolicy.planPage(
            geometry.linesForExactIds(owned));
        int blocks=Math.max(1,planned.size());
        if(blocks<=1)return "Stabilisation · "+shortRange(start,end)+" · ×"+reps;
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        return "Stabilisation · "+shortRange(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
    }
''')

# Session UI/runtime
SESSION = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
replace_once(SESSION,
    'progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : prefs.lastSabqiTodayReviewLabel());',
    'progress.setText(prefs.lastSabqiTodayReviewLabel().isEmpty() ? "30 min terminées" : HifzDisplayVocabulary.canonicalize(prefs.lastSabqiTodayReviewLabel()));')
replace_once(SESSION, 'addRoundAction("✓", "Valider", v -> validateConsolidationCycle());',
             'addRoundAction("✓", "Valider", v -> runValidationSafely(this::validateConsolidationCycle));')
replace_once(SESSION, 'if (assistancePassed) addRoundAction("✓","Valider",v->validateItqan());',
             'if (assistancePassed) addRoundAction("✓","Valider",v->runValidationSafely(this::validateItqan));')
replace_once(SESSION, 'addRoundAction("✓","Valider",v->validateSabqi());',
             'addRoundAction("✓","Valider",v->runValidationSafely(this::validateSabqi));')
replace_once(SESSION, 'progress.setText("Toutes les pages en attente sont acquises.");',
             'progress.setText("Toutes les unités de Stabilisation sont terminées.");')
replace_once(SESSION,
    '''            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=geometry.lineIdsForVerseRange(savedStart,savedEnd);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
''',
    '''            List<VerseRef> verses=geometry.versesForRange(savedStart,savedEnd);
            List<String> lineIds=CorpusLinePolicy.ownedLineIdsForRangeOnPage(savedStart,savedEnd,geometry);
            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
''')
replace_once(SESSION,
    '''            itqanUnit = new GeometryRepository.VerseUnit(page, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd),
                geometry.lineIdsForVerseRange(entryStart, entryEnd));
''',
    '''            itqanUnit = new GeometryRepository.VerseUnit(page, entryStart, entryEnd,
                geometry.versesForRange(entryStart, entryEnd),
                CorpusLinePolicy.ownedLineIdsForRangeOnPage(entryStart, entryEnd, geometry));
''')
replace_once(SESSION,
    '''    private void configureRevealButton(Button button) {
''',
    '''    private void runValidationSafely(Runnable validation) {
        if (validation == null) return;
        try {
            validation.run();
        } catch (RuntimeException error) {
            android.util.Log.e("QuranHifz", "Validation failed for " + mode, error);
            onError("État de progression à vérifier. Ouvrez Diagnostic si nécessaire.");
        }
    }

    private void configureRevealButton(Button button) {
''')

# Preferences / migration / ranges
PREFS = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java"
old_tail = '''        HifzCorpusState state = HifzV6Migration.classify(new HifzV6Migration.Input(
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
'''
new_tail = '''        HifzCorpusState state = HifzV6Migration.classify(new HifzV6Migration.Input(
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
            e.putInt("itqanBlockIndex", migratedBlockIndex)
                .putInt("itqanRep", 0)
                .putInt("itqanAssisted", 0)
                .putInt("itqanFinalReveals", 0)
                .putLong("itqanElapsedMs", 0L);
        }
        if (!e.commit()) {
            throw new IllegalStateException("Unable to migrate Hifz schema v5 to v6");
        }
'''
replace_once(PREFS, old_tail, new_tail)
replace_once(PREFS, '''    public enum ProgressState {
''', '''    private static int migratedPhysicalBlockIndex(
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
''')
replace_once(PREFS, '''    public boolean setItqanRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return false;
        List<VerseRange> normalized = normalizeRanges(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }
''', '''    public boolean setItqanRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return false;
        List<VerseRange> normalized = sortRangesPreservingBoundaries(ranges);
        return p.edit().putString("itqanRanges", rangesJson(normalized)).commit();
    }
''')
replace_once(PREFS, '''        List<VerseRange> acquiredNormalized = new ArrayList<>(acquiredRanges);
        List<VerseRange> stabilizationNormalized = new ArrayList<>(stabilizationRanges);
        acquiredNormalized.sort(Comparator.comparingInt(range -> GeometryRepository.ordinal(range.getStart())));
        stabilizationNormalized.sort(Comparator.comparingInt(range -> GeometryRepository.ordinal(range.getStart())));
''', '''        List<VerseRange> acquiredNormalized = sortRangesPreservingBoundaries(acquiredRanges);
        List<VerseRange> stabilizationNormalized = sortRangesPreservingBoundaries(stabilizationRanges);
''')
replace_once(PREFS, '''                .putString("promotedRanges", rangesJson(normalizeRanges(promotedNext)))
                .putString("unconsolidatedPromotedRanges", rangesJson(stabilizationNormalized))
                .putBoolean("anchoringQueueInitialized", false)
''', '''                .putString("promotedRanges", rangesJson(sortRangesPreservingBoundaries(promotedNext)))
                .putString("unconsolidatedPromotedRanges", rangesJson(stabilizationNormalized))
                .putBoolean("anchoringQueueInitialized", false)
                .remove("v6ConsolidationStabilizationState")
''')
replace_once(PREFS, '''        List<String> ids = geometry.lineIdsForVerseRange(
            GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end));
''', '''        List<String> ids = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end), geometry);
''')
replace_once(PREFS, '''            List<String> entryIds = geometry.lineIdsForVerseRange(start, end);
            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(entryIds);
''', '''            List<String> entryIds = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end, geometry);
            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(entryIds);
''')
replace_once(PREFS, '''                List<String> parentIds = geometry.lineIdsForVerseRange(
                    GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end));
''', '''                List<String> parentIds = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
                    GeometryRepository.parseVerse(entry.start), GeometryRepository.parseVerse(entry.end), geometry);
''')
replace_once(PREFS, '''            .putString("promotedRanges", rangesJson(normalizeRanges(all)))
            .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
            .putString("forcedPromotedRanges", rangesJson(normalizeRanges(forced)))
''', '''            .putString("promotedRanges", rangesJson(sortRangesPreservingBoundaries(all)))
            .putString("unconsolidatedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(pending)))
            .putString("forcedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(forced)))
''')
replace_once(PREFS, '''                .putString("unconsolidatedPromotedRanges", rangesJson(normalizeRanges(pending)))
                .putString("forcedPromotedRanges", rangesJson(normalizeRanges(forced)))
''', '''                .putString("unconsolidatedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(pending)))
                .putString("forcedPromotedRanges", rangesJson(sortRangesPreservingBoundaries(forced)))
''')
replace_once(PREFS, '''            if (GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1) {
''', '''            if (current.getSurah() == previous.getSurah()
                    && GeometryRepository.ordinal(current) == GeometryRepository.ordinal(previous) + 1) {
''')
replace_once(PREFS, '''    private List<VerseRange> parseRanges(String key) {
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
''', '''    private List<VerseRange> parseRanges(String key) {
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
''')
replace_once(PREFS, '''    private static List<VerseRange> normalizeRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(EligibleCorpus.Companion.of(ranges).getRanges());
    }
''', '''    private static List<VerseRange> normalizeRanges(List<VerseRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(EligibleCorpus.Companion.of(ranges).getRanges());
    }

    /** Sorts and merges overlap only; adjacency remains visible and every surah boundary is explicit. */
    private static List<VerseRange> sortRangesPreservingBoundaries(List<VerseRange> ranges) {
        ArrayList<VerseRange> split = new ArrayList<>();
        if (ranges == null) return split;
        for (VerseRange range : ranges) {
            if (range == null) continue;
            int start = GeometryRepository.ordinal(range.getStart());
            int end = GeometryRepository.ordinal(range.getEndInclusive());
            if (end < start) throw new IllegalArgumentException("Reversed range");
            int segmentStart = start;
            int surah = range.getStart().getSurah();
            for (int ordinal = start + 1; ordinal <= end; ordinal++) {
                VerseRef verse = QuranCanon.INSTANCE.fromOrdinal(ordinal);
                if (verse.getSurah() != surah) {
                    split.add(new VerseRange(
                        QuranCanon.INSTANCE.fromOrdinal(segmentStart),
                        QuranCanon.INSTANCE.fromOrdinal(ordinal - 1)));
                    segmentStart = ordinal;
                    surah = verse.getSurah();
                }
            }
            split.add(new VerseRange(
                QuranCanon.INSTANCE.fromOrdinal(segmentStart),
                QuranCanon.INSTANCE.fromOrdinal(end)));
        }
        split.sort(Comparator.comparingInt(range -> GeometryRepository.ordinal(range.getStart())));
        ArrayList<VerseRange> out = new ArrayList<>();
        for (VerseRange range : split) {
            if (out.isEmpty()) {
                out.add(range);
                continue;
            }
            VerseRange previous = out.get(out.size() - 1);
            int previousEnd = GeometryRepository.ordinal(previous.getEndInclusive());
            int currentStart = GeometryRepository.ordinal(range.getStart());
            if (previous.getStart().getSurah() == range.getStart().getSurah()
                    && currentStart <= previousEnd) {
                VerseRef mergedEnd = GeometryRepository.ordinal(range.getEndInclusive()) > previousEnd
                    ? range.getEndInclusive() : previous.getEndInclusive();
                out.set(out.size() - 1, new VerseRange(previous.getStart(), mergedEnd));
            } else {
                out.add(range);
            }
        }
        return out;
    }
''')
replace_once(PREFS, '        return normalizeRanges(out);\n    }\n\n    private static String defaultItqanRangesJson()',
             '        return sortRangesPreservingBoundaries(out);\n    }\n\n    private static String defaultItqanRangesJson()')

# Update retention expectations for the deliberately rebased obsolete 5-line in-flight protocol.
MIGTEST = "hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzPrefsV6MigrationInstrumentedTest.java"
replace_once(MIGTEST, '        assertEquals(9, prefs.itqanRep());\n        assertEquals(1, prefs.itqanAssisted());\n        assertEquals(2, prefs.itqanFinalReveals());\n        assertEquals(2, prefs.itqanBlockIndex());',
             '        assertEquals(0, prefs.itqanRep());\n        assertEquals(0, prefs.itqanAssisted());\n        assertEquals(0, prefs.itqanFinalReveals());\n        assertEquals(0, prefs.itqanBlockIndex());')
replace_once(MIGTEST, '        assertEquals(54_321L, prefs.elapsedFor(HifzSessionActivity.ITQAN));',
             '        assertEquals(0L, prefs.elapsedFor(HifzSessionActivity.ITQAN));')
replace_once(MIGTEST, '        assertExistingKeysUnchangedExcept(beforeMain, snapshot(main), "schema");',
             '        assertExistingKeysUnchangedExcept(beforeMain, snapshot(main), "schema",\n            "itqanRep", "itqanAssisted", "itqanFinalReveals", "itqanBlockIndex", "itqanElapsedMs");')
replace_once(MIGTEST, '        assertExistingKeysUnchangedExcept(before, snapshot(main), "schema", "murajaahSecPerLine");',
             '        assertExistingKeysUnchangedExcept(before, snapshot(main), "schema", "murajaahSecPerLine",\n            "itqanRep", "itqanAssisted", "itqanFinalReveals", "itqanBlockIndex", "itqanElapsedMs");')

print("Claude 0.7.5 NO-GO source patch applied successfully")
