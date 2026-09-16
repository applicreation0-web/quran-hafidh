from pathlib import Path


def exact(path, old, new, expected=1):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count == 0 and new in text:
        return False
    if count != expected:
        raise SystemExit(f"{path}: expected {expected} occurrences, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new), encoding="utf-8")
    return True

UI = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java"
MAP = "hifz-app/src/test/java/com/quransafeguard/hifz/preview/UiIconMappingTest.java"
AUDIT = "hifz-app/src/test/java/com/quransafeguard/hifz/preview/AuditClosureSourceContractTest.java"
FINAL = "hifz-app/src/test/java/com/quransafeguard/hifz/preview/FinalUiPolishSourceContractTest.java"
LOT2 = "hifz-app/src/test/java/com/quransafeguard/hifz/preview/Lot2SourceContractTest.java"
OFFICIAL = "hifz-app/src/test/java/com/quransafeguard/hifz/preview/OfficialReleaseContractTest.java"

# Real regression exposed by the historical C22 contract: canonical visible mode labels need icons.
exact(UI,
'''        if (s.contains("leçon neuve") || s.contains("lecon neuve")) return R.drawable.ic_hifz_new_lesson;
        if (s.contains("reprise")) return R.drawable.ic_hifz_reprise;
        if (s.contains("consolidation")) return R.drawable.ic_hifz_consolidation;
        if (s.contains("ancrage")) return R.drawable.ic_hifz_anchor;
        if (s.contains("entretien")) return R.drawable.ic_hifz_maintenance;
''',
'''        if (s.contains("apprentissage") || s.contains("leçon neuve") || s.contains("lecon neuve")) return R.drawable.ic_hifz_new_lesson;
        if (s.contains("reprise")) return R.drawable.ic_hifz_reprise;
        if (s.contains("consolidation")) return R.drawable.ic_hifz_consolidation;
        if (s.contains("stabilisation") || s.contains("ancrage")) return R.drawable.ic_hifz_anchor;
        if (s.contains("révision") || s.contains("revision") || s.contains("entretien")) return R.drawable.ic_hifz_maintenance;
''')
exact(MAP,
'''            {"À renforcer", R.drawable.ic_hifz_strengthen},
            {"Leçon neuve", R.drawable.ic_hifz_new_lesson},
            {"Ancrage", R.drawable.ic_hifz_anchor},
            {"Entretien", R.drawable.ic_hifz_maintenance},
''',
'''            {"À renforcer", R.drawable.ic_hifz_strengthen},
            {"Apprentissage", R.drawable.ic_hifz_new_lesson},
            {"Stabilisation", R.drawable.ic_hifz_anchor},
            {"Révision", R.drawable.ic_hifz_maintenance},
            {"Leçon neuve", R.drawable.ic_hifz_new_lesson},
            {"Ancrage", R.drawable.ic_hifz_anchor},
            {"Entretien", R.drawable.ic_hifz_maintenance},
''')

# Persisted queue-order invariant remains valid; only the old visible noun changed.
exact(AUDIT,
'        assertTrue(dashboard.contains("Ancrage · page reportée"));\n',
'        assertTrue(dashboard.contains("Stabilisation · unité reportée"));\n')

# Canonical visible vocabulary. Internal legacy constants/persistence names are deliberately untouched.
old_vocab = '''    @Test public void userFacingHifzVocabularyUsesPlainFrenchNames() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(main.contains("\\\"Leçon neuve\\\""));
        assertTrue(main.contains("\\\"Ancrage\\\""));
        assertTrue(main.contains("\\\"Entretien\\\""));
        assertTrue(main.contains("Ancrage fractionné"));
        assertTrue(session.contains("return \\\"Leçon neuve\\\""));
        assertTrue(session.contains("return \\\"Reprise du soir\\\""));
        assertTrue(session.contains("return \\\"Consolidation\\\""));
        assertTrue(session.contains("return \\\"Ancrage\\\""));
        assertTrue(session.contains("return \\\"Entretien\\\""));
        assertTrue(settings.contains("section(root,\\\"Repères\\\")"));
        assertTrue(settings.contains("Une page entière travaillée en profondeur"));
        assertTrue(settings.contains("addRepere(root,\\\"Ancrage fractionné\\\""));
        assertTrue(settings.contains("addRepere(root,\\\"J10\\\""));
    }
'''
new_vocab = '''    @Test public void userFacingHifzVocabularyUsesCanonicalProgressionNames() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(main.contains("\\\"Apprentissage\\\""));
        assertTrue(main.contains("\\\"Stabilisation\\\""));
        assertTrue(main.contains("\\\"Révision\\\""));
        assertTrue(session.contains("return \\\"Apprentissage\\\""));
        assertTrue(session.contains("return \\\"Stabilisation\\\""));
        assertTrue(session.contains("return \\\"Consolidation\\\""));
        assertTrue(session.contains("return \\\"Révision\\\""));
        assertFalse(session.contains("return \\\"Leçon neuve\\\""));
        assertFalse(session.contains("return \\\"Ancrage\\\""));
        assertFalse(session.contains("return \\\"Entretien\\\""));
        assertTrue(settings.contains("section(root,\\\"Repères\\\")"));
        for (String name : new String[]{"Apprentissage","Appris","Stabilisation","Stabilisé","Consolidation","Acquis","Révision","J10"}) {
            assertTrue(name, settings.contains("addRepere(root,\\\"" + name + "\\\""));
        }
    }
'''
exact(FINAL, old_vocab, new_vocab)

old_fractionated = '''    @Test public void fractionatedAnchoringIsExplicitOnHomeAndWeeklyProjection() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("Ancrage fractionné"));
        assertTrue(main.contains("PreviewConfig.ITQAN_LIGHT_TOTAL_REPS"));
        assertTrue(main.contains("bloc \\\" + (block + 1) + \\\"/\\\" + blocks"));
        assertTrue(week.contains("Ancrage fractionné"));
        assertTrue(week.contains("PreviewConfig.ITQAN_LIGHT_TOTAL_REPS"));
        assertTrue(week.contains("projectedItqanBlockIndex"));
    }
'''
new_fractionated = '''    @Test public void fractionatedStabilizationIsExplicitOnHomeAndWeeklyProjection() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("Stabilisation · "));
        assertTrue(main.contains("PreviewConfig.itqanTotalReps(entry.protocol)"));
        assertTrue(main.contains("bloc \\\"+(block+1)+\\\"/\\\"+blocks"));
        assertTrue(week.contains("Stabilisation · "));
        assertTrue(week.contains("PreviewConfig.itqanTotalReps(entry.protocol)"));
        assertTrue(week.contains("projectedItqanBlockIndex"));
        assertFalse(main.contains("Ancrage fractionné"));
        assertFalse(week.contains("Ancrage fractionné"));
    }
'''
exact(FINAL, old_fractionated, new_fractionated)

old_j10 = '''    @Test public void j10ConsumedSlotsStayDistinctFromNormalValidatedSessions() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("normalProtocolComplete"));
        assertTrue(main.contains("Créneau J10 utilisé"));
        assertTrue(week.contains("J10 · créneau utilisé"));
        assertTrue(week.contains("Terminé · J10"));
    }
'''
new_j10 = '''    @Test public void j10ConsumedSlotsStayDistinctFromNormalValidatedSessions() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("if(ledger.find(date,mode)!=null)return true"));
        assertTrue(main.contains("hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(mode,date)"));
        assertTrue(week.contains("J10 · créneau utilisé"));
        assertTrue(week.contains("actual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed"));
        assertTrue(week.contains("morningActual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed"));
    }
'''
exact(FINAL, old_j10, new_j10)

old_advisory = '''    @Test public void advisoryIsWedFriOnlyAndNeverBecomesScheduledSession() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(main.contains("recentSabqiAdvisory"));
        assertTrue(main.contains("DayOfWeek.WEDNESDAY") && main.contains("DayOfWeek.FRIDAY"));
        assertTrue(main.contains("HifzCadence.advisoryFiveLineRange"));
        assertTrue(main.contains("speedStore.consolidationSecondsPerLine()"));
        assertTrue(main.contains("prefs.recentSabqi().isEmpty()"));
        assertTrue(core.contains("EVENING_REVIEW_MINUTES = 30"));
        assertTrue(core.contains("ANCHORING_ENVELOPE_MINUTES = 60"));
        assertTrue(core.contains("CONSOLIDATION_MINUTES = 30"));
        assertTrue(core.contains("MAINTENANCE_MINUTES = 45"));
        assertFalse(core.contains("MICRO_REVIEW"));
    }
'''
new_advisory = '''    @Test public void consolidationIsProgressionDrivenAndNeverScheduledByWeekday() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(main.contains("recentSabqiAdvisory.setVisibility(View.GONE)"));
        assertFalse(main.contains("DayOfWeek.WEDNESDAY") || main.contains("DayOfWeek.FRIDAY"));
        assertFalse(main.contains("HifzCadence.advisoryFiveLineRange"));
        assertTrue(core.contains("EVENING_REVIEW_MINUTES = 30"));
        assertTrue(core.contains("ANCHORING_ENVELOPE_MINUTES = 60"));
        assertTrue(core.contains("CONSOLIDATION_MINUTES = 30"));
        assertTrue(core.contains("MAINTENANCE_MINUTES = 45"));
        assertFalse(core.contains("MICRO_REVIEW"));
    }
'''
exact(LOT2, old_advisory, new_advisory)

old_corpus = '''    @Test public void effectiveAnchoringCorpusIsVisibleButNotDirectlyEditable() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(prefs.contains("effectiveItqanRanges()"));
        assertTrue(settings.contains("Corpus d’ancrage"));
        assertTrue(settings.contains("En attente d’ancrage"));
        assertTrue(settings.contains("prefs.effectiveItqanRanges()"));
        assertTrue(settings.contains("prefs.unconsolidatedPromotedRanges()"));
        assertFalse(settings.contains("Modifier le corpus réel"));
        assertFalse(settings.contains("Supprimer du corpus réel"));
    }
'''
new_corpus = '''    @Test public void stabilizationAndAcquiredCorporaAreIndependentEditableMultiLists() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("section(root,\\\"Plages à stabiliser\\\")"));
        assertTrue(settings.contains("section(root,\\\"Plages acquises\\\")"));
        assertTrue(settings.contains("chooseStabilizationRange"));
        assertTrue(settings.contains("chooseAcquiredRange"));
        assertTrue(settings.contains("removeStabilizationRange"));
        assertTrue(settings.contains("removeAcquiredRange"));
        assertTrue(prefs.contains("setV6StabilizationRanges"));
        assertTrue(prefs.contains("setV6AcquiredRanges"));
        assertTrue(prefs.contains("validateV6ManualRanges"));
        assertTrue(prefs.contains("Une plage ne peut pas être à la fois Acquise et À stabiliser."));
    }
'''
exact(LOT2, old_corpus, new_corpus)

old_resume = '''        assertTrue("expired timed work must be committed after process death", session.contains("completeExpiredTimedSession"));
        assertTrue("empty recent Sabqi must complete truthfully without opening old Itqan", session.contains("completeEmptyRecentSabqiSession"));
        assertTrue("Today must open the first incomplete morning/evening plan entry", main.contains("firstIncompleteMode") && main.contains("planFor"));
        assertTrue("dashboard must share the domain schedule", dashboard.contains("planFor"));
        assertFalse("dashboard must not retain A/B transfer projection", dashboard.contains("secondsA") || dashboard.contains("availableB") || dashboard.contains("Murājaʿah A") || dashboard.contains("Murājaʿah B"));
        assertFalse("settings must not offer daily cursor reposition buttons", settings.contains("Repositionner Murājaʿah"));
'''
new_resume = '''        assertTrue("expired timed work must be committed after process death", session.contains("completeExpiredTimedSession"));
        assertTrue("empty recent Sabqi must complete truthfully without opening old Itqan", session.contains("completeEmptyRecentSabqiSession"));
        assertTrue("Today must resolve the oldest due canonical cadence", main.contains("nextDueCadence") && main.contains("HifzSchedule.INSTANCE.nextDue"));
        assertTrue("Today must preserve the scheduled carryover date", main.contains("HifzSessionActivity.EXTRA_SCHEDULED_DATE"));
        assertTrue("dashboard must share the canonical domain schedule", dashboard.contains("HifzSchedule.INSTANCE.actionFor"));
        assertTrue("weekend cadence must be Révision", dashboard.contains("case REVISION"));
        assertFalse("dashboard must not retain A/B transfer projection", dashboard.contains("secondsA") || dashboard.contains("availableB") || dashboard.contains("Murājaʿah A") || dashboard.contains("Murājaʿah B"));
        assertFalse("settings must not offer daily cursor reposition buttons", settings.contains("Repositionner Murājaʿah"));
'''
exact(OFFICIAL, old_resume, new_resume)

print("CANONICAL_LEGACY_CONTRACT_ALIGNMENT_APPLIED")
