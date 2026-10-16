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

    @Test public void commentaryAndReaderActionsStayCompactCenteredAndAccessible() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        assertTrue(map.contains("icons=Ui.row(activity);icons.setGravity(Gravity.CENTER);"));
        assertTrue(map.contains("icons.setMinimumHeight(Ui.dp(activity,56));"));
        assertTrue(map.contains("new LinearLayout.LayoutParams(Ui.dp(activity,72),Ui.dp(activity,48))"));
        assertTrue(map.contains("Ui.iconButton(activity,\"\",\"Tafsir\",v->openCommentary())"));
        assertTrue(map.contains("Ui.iconButton(activity,\"\",\"Lecture\",v->openReader())"));
        String actions=map.substring(map.indexOf("icons=Ui.row(activity)"),map.indexOf("root.addView(icons);"));
        assertFalse(actions.contains("new LinearLayout.LayoutParams(0,Ui.dp(activity,48),1)"));
    }

    @Test public void canonicalAnNamlUsesItsOwnTitleWhileExactSubnodesRemain() throws Exception {
        String map=read(ROOT+"MawdudiMapScreen.java");
        int start=map.indexOf("String selectedTitle(){");
        int end=map.indexOf("@Override protected void onMeasure",start);
        assertTrue(start>=0&&end>start);
        String method=map.substring(start,end);
        assertTrue(method.contains("n.index==selected&&n.first==focusStart&&n.last==focusEnd"));
        assertTrue(method.contains("return n.title;"));
        assertTrue(method.contains("return titles.titleForPassage(current().id);"));
        assertTrue(map.contains("if(surah==27&&focusStart>=67&&focusEnd<=82&&subnodes.length()==3)"));
        assertTrue("Expanded title must retain parent fallback",
            map.contains("showTitle(titles.titleForPassage(current().id));"));
    }
}
