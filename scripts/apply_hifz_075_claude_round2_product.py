from pathlib import Path
import re


def sub_once(text, pattern, replacement, label, flags=0):
    out, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 replacement, got {count}")
    return out

# --- HifzPrefs.java ---
p = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java')
s = p.read_text()

s = sub_once(
    s,
    r'''    public boolean setV6StabilizationRanges\(List<VerseRange> ranges, GeometryRepository geometry\) \{\n        return setV6ManualRanges\(itqanRanges\(\), ranges, geometry\);\n    \}\n''',
    '''    public boolean setV6StabilizationRanges(List<VerseRange> ranges, GeometryRepository geometry) {\n        if (geometry == null) throw new IllegalArgumentException("Géométrie Mushaf requise.");\n        if (ranges != null) {\n            ArrayList<GeometryRepository.LineMeta> allLines = new ArrayList<>();\n            for (int i = 0; i < geometry.lineCount(); i++) allLines.add(geometry.line(i));\n            for (VerseRange range : ranges) requireOwnedLineOnEveryPage(range, allLines);\n        }\n        return setV6ManualRanges(itqanRanges(), ranges, geometry);\n    }\n\n    /** Every Mushaf page touched by a Stabilisation range must own at least one physical line. */\n    private static void requireOwnedLineOnEveryPage(VerseRange range, List<GeometryRepository.LineMeta> lines) {\n        if (range == null) return;\n        java.util.TreeMap<Integer, Boolean> pages = new java.util.TreeMap<>();\n        for (GeometryRepository.LineMeta line : lines) {\n            boolean touched = false;\n            for (VerseRef verse : line.verses) {\n                if (range.contains(verse)) { touched = true; break; }\n            }\n            if (!touched) continue;\n            boolean owned = range.contains(CorpusLinePolicy.ownerVerse(line));\n            pages.merge(line.page, owned, Boolean::logicalOr);\n        }\n        for (java.util.Map.Entry<Integer, Boolean> page : pages.entrySet()) {\n            if (!page.getValue()) {\n                throw new IllegalArgumentException("Page " + page.getKey()\n                    + " : cette plage ne contient aucune ligne complète. Commencez au premier verset de la ligne.");\n            }\n        }\n    }\n''',
    'setV6StabilizationRanges')

s = sub_once(
    s,
    r'''    private static boolean rangeCoveredBy\(List<VerseRange> ranges, VerseRef start, VerseRef endInclusive\) \{.*?\n    \}\n\n    public VerseRef itqanRotationStart''',
    '''    private static boolean rangeCoveredBy(List<VerseRange> ranges, VerseRef start, VerseRef endInclusive) {\n        if (ranges == null || ranges.isEmpty()) return false;\n        int from = GeometryRepository.ordinal(start);\n        int to = GeometryRepository.ordinal(endInclusive);\n        for (int ordinal = from; ordinal <= to; ordinal++) {\n            VerseRef verse = QuranCanon.INSTANCE.fromOrdinal(ordinal);\n            boolean covered = false;\n            for (VerseRange range : ranges) {\n                if (range != null && range.contains(verse)) { covered = true; break; }\n            }\n            if (!covered) return false;\n        }\n        return true;\n    }\n\n    public VerseRef itqanRotationStart''',
    'rangeCoveredBy', re.S)

old = '''                    String key = anchoringKey(unit.start.toString(), unit.end.toString());\n                    boolean reconstruction = ordinalBetween(unit.start, new VerseRef(49, 1), new VerseRef(114, 6));\n                    boolean forced = !reconstruction && overlaps(forcedRanges, unit.start, unit.end);\n                    expected.put(key, new AnchoringQueue.Entry(unit.start.toString(), unit.end.toString(),\n                        AnchoringQueue.originFor(reconstruction, forced),\n                        reconstruction ? AnchoringQueue.Protocol.LIGHT : AnchoringQueue.Protocol.FULL, 0));\n'''
new = '''                    // A page portion whose verses all sit on lines owned by an earlier verse has no\n                    // physical Stabilisation unit. Queueing it would block the queue (never complete).\n                    if (!CorpusLinePolicy.ownedLineIdsForRangeOnPage(unit.start, unit.end, geometry).isEmpty()) {\n                        String key = anchoringKey(unit.start.toString(), unit.end.toString());\n                        boolean reconstruction = ordinalBetween(unit.start, new VerseRef(49, 1), new VerseRef(114, 6));\n                        boolean forced = !reconstruction && overlaps(forcedRanges, unit.start, unit.end);\n                        expected.put(key, new AnchoringQueue.Entry(unit.start.toString(), unit.end.toString(),\n                            AnchoringQueue.originFor(reconstruction, forced),\n                            reconstruction ? AnchoringQueue.Protocol.LIGHT : AnchoringQueue.Protocol.FULL, 0));\n                    }\n'''
if s.count(old) != 1:
    raise SystemExit(f'reconcile ownerless: expected 1, got {s.count(old)}')
s = s.replace(old, new, 1)

if s.count('        if (ids.isEmpty()) return false;') != 1:
    raise SystemExit('entryIsFully ownerless guard not unique')
s = s.replace('        if (ids.isEmpty()) return false;',
              '        if (ids.isEmpty()) return true; // ownerless entry: nothing physical to stabilise, never block the queue', 1)

old = '            List<String> entryIds = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end, geometry);\n            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(entryIds);'
new = '            List<String> entryIds = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end, geometry);\n            if (entryIds.isEmpty()) continue;\n            List<GeometryRepository.LineMeta> physicalLines = geometry.linesForExactIds(entryIds);'
if s.count(old) != 1:
    raise SystemExit('stabilizedConsolidationUnits guard source mismatch')
s = s.replace(old, new, 1)

s = sub_once(
    s,
    r'''    /\*\* Sorts and merges overlap only; adjacency remains visible and every surah boundary is explicit\. \*/\n    private static List<VerseRange> sortRangesPreservingBoundaries\(List<VerseRange> ranges\) \{.*?\n    \}\n\n    private static List<VerseRange> rangesFromVerses''',
    '''    /** Sorts ranges, merges overlap, and merges adjacency only inside the same surah. */\n    private static List<VerseRange> sortRangesPreservingBoundaries(List<VerseRange> ranges) {\n        ArrayList<VerseRange> sorted = new ArrayList<>();\n        if (ranges == null) return sorted;\n        for (VerseRange range : ranges) {\n            if (range == null) continue;\n            if (GeometryRepository.ordinal(range.getEndInclusive()) < GeometryRepository.ordinal(range.getStart()))\n                throw new IllegalArgumentException("Reversed range");\n            sorted.add(range);\n        }\n        sorted.sort(Comparator.comparingInt(range -> GeometryRepository.ordinal(range.getStart())));\n        ArrayList<VerseRange> out = new ArrayList<>();\n        for (VerseRange range : sorted) {\n            if (out.isEmpty()) {\n                out.add(range);\n                continue;\n            }\n            VerseRange previous = out.get(out.size() - 1);\n            int previousEnd = GeometryRepository.ordinal(previous.getEndInclusive());\n            int currentStart = GeometryRepository.ordinal(range.getStart());\n            boolean overlaps = currentStart <= previousEnd;\n            boolean sameSurahAdjacent = currentStart == previousEnd + 1\n                && previous.getEndInclusive().getSurah() == range.getStart().getSurah();\n            if (overlaps || sameSurahAdjacent) {\n                VerseRef mergedEnd = GeometryRepository.ordinal(range.getEndInclusive()) > previousEnd\n                    ? range.getEndInclusive() : previous.getEndInclusive();\n                out.set(out.size() - 1, new VerseRange(previous.getStart(), mergedEnd));\n            } else {\n                out.add(range);\n            }\n        }\n        return out;\n    }\n\n    private static List<VerseRange> rangesFromVerses''',
    'sortRangesPreservingBoundaries', re.S)

# Migration: preserve in-flight counters only when old and new physical identity are safely compatible.
anchor = '''        int migratedBlockIndex = openLegacyStabilization\n            ? migratedPhysicalBlockIndex(geometry, unitStart, unitEnd, migratedStabilized)\n            : Math.max(0, p.getInt("itqanBlockIndex", 0));\n\n        maybeInterruptMigrationForTest("BEFORE_MAIN_COMMIT");\n'''
replacement = '''        int migratedBlockIndex = openLegacyStabilization\n            ? migratedPhysicalBlockIndex(geometry, unitStart, unitEnd, migratedStabilized)\n            : Math.max(0, p.getInt("itqanBlockIndex", 0));\n        boolean preserveOpenLegacyProgress = openLegacyStabilization\n            && compatibleLegacyOpenStabilization(geometry, unitStart, unitEnd, completedBlocks, hardAnchoringSurahs());\n\n        maybeInterruptMigrationForTest("BEFORE_MAIN_COMMIT");\n'''
if s.count(anchor) != 1:
    raise SystemExit('migration preserve anchor mismatch')
s = s.replace(anchor, replacement, 1)

old = '''        if (openLegacyStabilization) {\n            e.putInt("itqanBlockIndex", migratedBlockIndex)\n                .putInt("itqanRep", 0)\n                .putInt("itqanAssisted", 0)\n                .putInt("itqanFinalReveals", 0)\n                .putLong("itqanElapsedMs", 0L);\n        }\n'''
new = '''        if (openLegacyStabilization) {\n            e.putInt("itqanBlockIndex", migratedBlockIndex);\n            if (!preserveOpenLegacyProgress) {\n                e.putInt("itqanRep", 0)\n                    .putInt("itqanAssisted", 0)\n                    .putInt("itqanFinalReveals", 0)\n                    .putLong("itqanElapsedMs", 0L);\n            }\n        }\n'''
if s.count(old) != 1:
    raise SystemExit('migration reset block mismatch')
s = s.replace(old, new, 1)

helper_anchor = '''    private static int migratedPhysicalBlockIndex(\n            GeometryRepository geometry,\n            VerseRef start,\n            VerseRef endInclusive,\n            java.util.Set<String> stabilizedLineIds) {\n'''
helper = '''    private static boolean compatibleLegacyOpenStabilization(\n            GeometryRepository geometry,\n            VerseRef start,\n            VerseRef endInclusive,\n            int completedBlocks,\n            List<Integer> hardSurahs) {\n        if (geometry == null || start == null || endInclusive == null || completedBlocks != 0) return false;\n        try {\n            if (geometry.pageForVerse(start) != geometry.pageForVerse(endInclusive)) return false;\n            List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, endInclusive, geometry);\n            if (owned.isEmpty()) return false;\n            List<StabilizationHalfPagePolicy.Unit> newPlan = StabilizationHalfPagePolicy.planPage(\n                geometry.linesForExactIds(owned));\n            if (newPlan.size() != 1) return false;\n            List<VerseRef> legacyVerses = geometry.versesForRange(start, endInclusive);\n            if (containsHardAnchoringSurah(hardSurahs, legacyVerses)) {\n                int[] segments = geometry.surahSegmentLineCounts(start, endInclusive);\n                if (PreviewConfig.fractionatedBlockCount(segments) > 1) return false;\n            }\n            return true;\n        } catch (RuntimeException incompatible) {\n            return false;\n        }\n    }\n\n'''
if s.count(helper_anchor) != 1:
    raise SystemExit('migration helper anchor mismatch')
s = s.replace(helper_anchor, helper + helper_anchor, 1)

p.write_text(s)

# --- MainActivity.java ---
p = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java')
s = p.read_text()
s = sub_once(
    s,
    r'''    private boolean progressionConsolidationDue\(GeometryRepository g\)\{\n        if\(g==null\)return false;\n        ConsolidationCycleEngine engine=new ConsolidationCycleEngine\(\);\n        ConsolidationCycleEngine\.Session open=prefs\.restoreConsolidationSession\(\n            engine,ConsolidationCycleEngine\.Family\.STABILIZATION\);\n        return open!=null \|\| !prefs\.stabilizedConsolidationUnits\(g,3\)\.isEmpty\(\);\n    \}\n''',
    '''    private boolean progressionConsolidationDue(GeometryRepository g){\n        if(g==null)return false;\n        if(HifzClock.today().toString().equals(prefs.lastRecentSabqiReviewDate()))return false;\n        try{\n            ConsolidationCycleEngine engine=new ConsolidationCycleEngine();\n            ConsolidationCycleEngine.Session open=prefs.restoreConsolidationSession(\n                engine,ConsolidationCycleEngine.Family.STABILIZATION);\n            return open!=null || !prefs.stabilizedConsolidationUnits(g,3).isEmpty();\n        }catch(RuntimeException error){\n            android.util.Log.e("QuranHifz","Unable to evaluate progression Consolidation",error);\n            return false;\n        }\n    }\n''',
    'progressionConsolidationDue')
p.write_text(s)

# --- WeeklyDashboardPlanner.java ---
p = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java')
s = p.read_text()
old = '''        List<AnchoringQueue.Entry> projectedAnchoring=AnchoringQueue.visitOrder(\n            prefs.anchoringQueue(),prefs.anchoringQueueIndex());\n'''
new = '''        List<AnchoringQueue.Entry> projectedAnchoring=new ArrayList<>();\n        for(AnchoringQueue.Entry entry:AnchoringQueue.visitOrder(prefs.anchoringQueue(),prefs.anchoringQueueIndex())){\n            VerseRef entryStart=GeometryRepository.parseVerse(entry.start),entryEnd=GeometryRepository.parseVerse(entry.end);\n            if(!CorpusLinePolicy.ownedLineIdsForRangeOnPage(entryStart,entryEnd,geometry).isEmpty())projectedAnchoring.add(entry);\n        }\n'''
if s.count(old) != 1:
    raise SystemExit('Weekly projectedAnchoring mismatch')
p.write_text(s.replace(old, new, 1))

print('Applied Claude round2 production fixes + migration zero-loss compatibility rule')
