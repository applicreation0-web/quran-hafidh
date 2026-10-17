package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Targeted source-level safeguards for the final Atlas interface adjustments.
 * These checks do not claim runtime BOOX or storage validation.
 */
public final class AtlasFinalizationSourceContractTest {
    private static String read(String file) throws Exception {
        Path path = Paths.get(file);
        if (!Files.exists(path)) path=Paths.get("..",file);
        return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
    }
    private static final String ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";

    @Test public void weeklyGoalLabelsUsePagesOnlyWithoutChangingSchedules() throws Exception {
        String home=read(ROOT+"MainActivity.java");
        assertTrue(home.contains("navLine(path, \"Apprentissage\", \"1 page/semaine\""));
        assertTrue(home.contains(": \"2 pages/semaine\";"));
        assertTrue("Post-Nas maintenance remains distinct",
            home.contains("prefs.itqanPostNasMaintenance() ? \"1 hizb · entretien\""));
        assertFalse(home.contains("navLine(path, \"Apprentissage\", \"5 lignes\""));
        assertFalse(home.contains("2 pages / 4 séances"));
    }

    @Test public void qcfParityAndSelectionNeverShareTheSameFillSwitch() throws Exception {
        String pages=read(ROOT+"MapQcfThumbnails.java");
        assertTrue(pages.contains("button.even{background:#121211;color:#faf8f0"));
        assertTrue(pages.contains("button.odd{background:#faf8f0;color:#121211"));
        assertTrue(pages.contains("outline:2px solid #121211;outline-offset:2px"));
        assertTrue(pages.contains("append(p%2==0?\"even\":\"odd\")"));
        assertTrue(pages.contains("if(p==selected)html.append(\" aria-current='page'\")"));
        // The selected style changes only size/border/ring, not pair/impair fill.
        int current=pages.indexOf("button[aria-current=page]{");
        int end=pages.indexOf("}",current);
        assertTrue(current>=0 && end>current);
        assertFalse(pages.substring(current,end).contains("background:"));
        assertTrue(pages.contains("MapPages.step(dx>0?1:-1)"));
    }

    @Test public void atlasUsesSingleSlidingUnitRailAndDirectAuthenticQcf() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue("Exactly five unit positions maximum",
            map.contains("Math.min(5,unitCount())"));
        assertTrue("Every unit remains reachable by sliding",
            map.contains("shiftUnitWindow(dx>0?-1:1)"));
        assertTrue("Active unit remains distinctly circled",
            map.contains("ordinal==activeMacro"));
        assertTrue("Second five-position Ayah Themes rail is retained",
            map.contains("Math.min(5,items.size())"));
        assertTrue("Unit title is larger and bold",map.contains("Ui.text(activity,\"\",16f,true)"));
        assertTrue("Passage title is smaller and italic",
            map.contains("Ui.text(activity,\"\",12.5f,false)")
                &&map.contains("Typeface.ITALIC"));
        assertTrue("Only direct authentic QCF full page is shown",
            map.contains("thumbnails.setMosaic(false)"));
        assertFalse("No redundant third page band above Mushaf",
            map.contains("c.drawText(\"Pages \"+firstPage"));
        assertTrue("QCF page pills stay below Mushaf",
            read(ROOT+"MapQcfThumbnails.java").contains("aria-current='page'"));
        assertTrue("Gray shading uses the entire active Ayah Themes passage",
            map.contains("thumbnails.showPages(first,last,page,surah,focusStart,focusEnd,false,"));
        assertTrue("Existing Lecture action is retained",map.contains("Ui.iconButton(activity,\"\",\"Lecture\""));
        assertTrue("Lecture carries the selected passage, not just the unit",
            map.contains(".putExtra(\"map114PassageId\",current().id)"));
        assertTrue("Open/return QCF page stays in its unit",
            map.contains("activity.startActivityForResult(intent,11436)"));
    }

    @Test public void sourcePassagesRemainUnchangedButNeverAppearAsAtlasNodes() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue("Canonical passage node code remains source-only",map.contains("return titles.titleForPassage(current().id);"));
        assertTrue("Atlas uses only source-backed unit labels",map.contains("passageTitle.setText(unit.label);"));
        assertTrue("No PDF action from selected unit title",map.contains("showTitle(macro().label);"));
        assertFalse("Do not open Islāḥī or Maududi commentaries from unit Atlas",
            map.substring(map.indexOf("private void showCurrentTitle(){"),
                map.indexOf("private void chooseUnit(){")).contains("AtlasSourcePdfActivity"));
    }
}
