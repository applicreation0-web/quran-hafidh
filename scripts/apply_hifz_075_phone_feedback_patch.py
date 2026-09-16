from pathlib import Path
import re


def exact(path, old, new, expected=1):
    p=Path(path); text=p.read_text(encoding="utf-8")
    count=text.count(old)
    if count==0 and new in text: return False
    if count!=expected: raise SystemExit(f"{path}: expected {expected}, found {count}: {old[:90]!r}")
    p.write_text(text.replace(old,new),encoding="utf-8"); return True


def regex(path, pattern, replacement, expected=1):
    p=Path(path); text=p.read_text(encoding="utf-8")
    out,count=re.subn(pattern,replacement,text,flags=re.S)
    if count==0 and replacement in text: return False
    if count!=expected: raise SystemExit(f"{path}: regex expected {expected}, found {count}: {pattern[:90]!r}")
    p.write_text(out,encoding="utf-8"); return True

BUILD="hifz-app/build.gradle.kts"
SETTINGS="hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java"
MAIN="hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java"
UI="hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java"
SOURCE="hifz-app/src/test/java/com/quransafeguard/hifz/preview/CanonicalProgressUiSourceContractTest.java"
PERSIST="hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzV6PersistentStateInstrumentedTest.java"

exact(BUILD,
'''        check(session.contains("Ancrage fractionné")) {
            "Le libellé visible de l'Ancrage fractionné est requis."
        }
        check(!session.contains("Consolidation fractionnée")) {
            "Consolidation désigne RECENT_SABQI_REVIEW et ne doit pas nommer l'Ancrage."
        }
        check(settings.contains("Sourates difficiles à ancrer")) {
            "Le drapeau durable d'Ancrage fractionné doit rester accessible dans les Paramètres."
        }
''',
'''        check(session.contains("Stabilisation") && !session.contains("Ancrage fractionné")) {
            "Le libellé visible du mode structuré doit rester Stabilisation."
        }
        check(!session.contains("Consolidation fractionnée")) {
            "Consolidation désigne RECENT_SABQI_REVIEW et ne doit pas nommer la Stabilisation."
        }
        check(settings.contains("Sourates difficiles à stabiliser")) {
            "Le réglage durable du protocole fractionné doit rester accessible dans les Paramètres."
        }
''')
exact(BUILD,
'        check(settings.contains("Ajouter") && settings.contains("Début de rotation d’ancrage"))\n',
'        check(settings.contains("Ajouter") && settings.contains("Début de rotation de stabilisation"))\n')

exact(UI,
'''        String cue = lower.contains("leçon") || lower.contains("lecon") || lower.contains("sabqi") ? "5 lignes"
            : lower.contains("reprise") ? "30 min"
            : lower.contains("consolidation") ? "30 min"
            : lower.contains("ancrage") || lower.contains("itq") ? "Répétitions"
            : lower.contains("entretien") || lower.contains("mur") ? "45 min" : "";
''',
'''        String cue = lower.contains("apprentissage") || lower.contains("leçon") || lower.contains("lecon") || lower.contains("sabqi") ? "5 lignes"
            : lower.contains("reprise") ? "30 min"
            : lower.contains("consolidation") ? "Cycle 1–3"
            : lower.contains("stabilisation") || lower.contains("ancrage") || lower.contains("itq") ? "Répétitions"
            : lower.contains("révision") || lower.contains("revision") || lower.contains("entretien") || lower.contains("mur") ? "45 min" : "";
''')

regex(MAIN,
r'''    private void refreshRecentSabqiAdvisory\(\) \{.*?\n    \}\n\n    private void refreshDashboard''',
'''    private void refreshRecentSabqiAdvisory() {
        if (recentSabqiAdvisory == null) return;
        recentSabqiAdvisory.setText("");
        recentSabqiAdvisory.setVisibility(View.GONE);
    }

    private void refreshDashboard''')

exact(SETTINGS,"Terminez la unité de Stabilisation en cours avant de modifier ce réglage.","Terminez l’unité de Stabilisation en cours avant de modifier ce réglage.")

regex(SOURCE,
r'''    @Test public void settingsExposeIndependentMultiRangeViewsForStabilizationAndAcquiredCorpus\(\) throws Exception \{.*?\n    \}\n\n    @Test public void repereLexicon''',
'''    @Test public void settingsExposeIndependentEditableMultiRangesForStabilizationAndAcquiredCorpus() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
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

    @Test public void repereLexicon''')

exact(PERSIST,'import java.util.Iterator;\n','import java.util.Arrays;\nimport java.util.Iterator;\nimport java.util.List;\n')
marker='    private void seedSingleConflictSchemaFive() {'
new_tests='''    @Test public void manualMultiRangesPersistDisjointStatePreserveCursorsAndNeverInventJ10Dates() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String itqanCursorBefore = main.getString("itqanCursor", null);
        String murajaahCursorBefore = main.getString("murajaahCursor", null);
        int sabqiCursorBefore = main.getInt("sabqiLineCursor", -1);

        List<com.quransafeguard.hifz.core.VerseRange> stabilization = Arrays.asList(
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(49,1), new VerseRef(49,18)),
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(50,1), new VerseRef(50,16)));
        List<com.quransafeguard.hifz.core.VerseRange> acquired = Arrays.asList(
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,1), new VerseRef(2,20)),
            new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,30), new VerseRef(2,40)));

        assertTrue(prefs.setV6StabilizationRanges(stabilization, geometry));
        assertTrue(prefs.setV6AcquiredRanges(acquired, geometry));
        assertEquals(2, prefs.unconsolidatedPromotedRanges().size());
        assertEquals(2, prefs.itqanRanges().size());
        assertEquals(itqanCursorBefore, main.getString("itqanCursor", null));
        assertEquals(murajaahCursorBefore, main.getString("murajaahCursor", null));
        assertEquals(sabqiCursorBefore, main.getInt("sabqiLineCursor", -1));

        String acquiredLine = firstOwned(new VerseRef(2,1), new VerseRef(2,20));
        String pendingLine = firstOwned(new VerseRef(49,1), new VerseRef(49,18));
        assertTrue(stringSet(ACQUIRED).contains(acquiredLine));
        assertTrue(stringSet(UNKNOWN_DUE).contains(acquiredLine));
        assertFalse(longMap(ACTIVE_J10).containsKey(acquiredLine));
        assertTrue(stringSet(LEARNED).contains(pendingLine));
        assertFalse(stringSet(ACQUIRED).contains(pendingLine));

        Map<String, ?> beforeReopen = snapshot(main);
        HifzPrefs reopened = new HifzPrefs(context);
        assertEquals(beforeReopen, snapshot(main));
        assertEquals(2, reopened.unconsolidatedPromotedRanges().size());
        assertEquals(2, reopened.itqanRanges().size());
    }

    @Test public void manualMultiRangesRejectCrossListOverlapAtomically() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        Map<String, ?> before = snapshot(main);
        try {
            prefs.setV6StabilizationRanges(Arrays.asList(
                new com.quransafeguard.hifz.core.VerseRange(new VerseRef(2,10), new VerseRef(2,30))), geometry);
            fail("cross-list overlap must fail closed");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("à la fois Acquise et À stabiliser"));
        }
        assertEquals("rejected edit must be atomic", before, snapshot(main));
    }

'''+marker
exact(PERSIST,marker,new_tests)

print("PHONE_FEEDBACK_REMAINING_PATCH_APPLIED")
