#!/usr/bin/env python3
from pathlib import Path

path = Path("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "        ensureSchema();\n        migrateLegacyGates(context);",
        "        ensureSchema(context);\n        migrateLegacyGates(context);",
    ),
    (
        "    private void ensureSchema() {",
        "    private void ensureSchema(Context context) {",
    ),
    (
        "            migrateV5ToV6();\n            schema = 6;",
        "            migrateV5ToV6(context);\n            schema = 6;",
    ),
    (
        "        ensureSchema();\n    }",
        "        ensureSchema(null);\n    }",
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"replacement target count={count}: {old!r}")
    text = text.replace(old, new, 1)

start_marker = "    private void migrateV5ToV6() {\n"
end_marker = "\n    private void migrateV1ToV2() {"
if text.count(start_marker) != 1:
    raise SystemExit(f"migrate start count={text.count(start_marker)}")
if text.count(end_marker) != 1:
    raise SystemExit(f"migrate end count={text.count(end_marker)}")
start = text.index(start_marker)
end = text.index(end_marker, start)

new_method = '''    private void migrateV5ToV6(Context context) {
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

        if (p.getInt("schema", -1) != 6
                || !p.contains("v6LearnedLineIds")
                || !p.contains("v6AcquiredCreditLineIds")
                || !p.contains("v6ActiveJ10LastReviewed")) {
            throw new IllegalStateException("Incomplete Hifz schema v6 migration commit");
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
'''

text = text[:start] + new_method + text[end:]

assert text.count("migrateV5ToV6(context);") == 1
assert text.count("private void migrateV5ToV6(Context context)") == 1
assert "migrateV5ToV6();" not in text
assert text.count("ensureSchema(context);") == 1

path.write_text(text, encoding="utf-8")
