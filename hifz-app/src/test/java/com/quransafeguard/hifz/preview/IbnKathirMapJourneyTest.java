package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/** CP-03: graphical Ibn Kathir map, exact QCF preview and stateful reader journey. */
public final class IbnKathirMapJourneyTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadGeometry() throws Exception {
        Path path = repo("app/src/main/assets/reader109/geometry.json");
        geometry = GeometryRepository.fromJson(
            new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static Path repo(String relative) {
        Path path = Paths.get(relative);
        return Files.exists(path) ? path : Paths.get("..", relative);
    }

    private static String source(String relative) throws Exception {
        return new String(Files.readAllBytes(repo(relative)), StandardCharsets.UTF_8);
    }

    @Test public void shortBlockUsesOnlyItsExactRealQcfVerses() {
        IbnKathirGroupIndex.Group group = IbnKathirGroupIndex.shared().groupsForSurah(1).get(0);
        IbnKathirMapModel.PageSpan span = IbnKathirMapModel.pageSpan(group, geometry);
        assertEquals(span.first, span.last);
        List<VerseRef> exact = IbnKathirMapModel.highlightedVerses(group, span.first, geometry);
        assertFalse(exact.isEmpty());
        for (VerseRef verse : exact) {
            assertEquals(group.surah, verse.getSurah());
            assertTrue(verse.getAyah() >= group.startAyah);
            assertTrue(verse.getAyah() <= group.endAyah);
        }
    }

    @Test public void multipageBlockClampsPreviewAndHighlightsEveryShownPageExactly() {
        IbnKathirGroupIndex.Group multipage = null;
        IbnKathirMapModel.PageSpan span = null;
        outer: for (int surah = 1; surah <= 114; surah++) {
            for (IbnKathirGroupIndex.Group group : IbnKathirGroupIndex.shared().groupsForSurah(surah)) {
                IbnKathirMapModel.PageSpan candidate = IbnKathirMapModel.pageSpan(group, geometry);
                if (candidate.last > candidate.first) {
                    multipage = group;
                    span = candidate;
                    break outer;
                }
            }
        }
        assertNotNull("The 1,903-group corpus must contain a multipage block", multipage);
        assertEquals(span.first, IbnKathirMapModel.clampPage(multipage, 0, geometry));
        assertEquals(span.last, IbnKathirMapModel.clampPage(multipage, 605, geometry));
        for (int page = span.first; page <= span.last; page++) {
            List<VerseRef> exact = IbnKathirMapModel.highlightedVerses(multipage, page, geometry);
            assertFalse("Selected page has no highlighted passage: " + page, exact.isEmpty());
            for (VerseRef verse : exact) {
                assertEquals(multipage.surah, verse.getSurah());
                assertTrue(verse.getAyah() >= multipage.startAyah);
                assertTrue(verse.getAyah() <= multipage.endAyah);
            }
        }
    }

    @Test public void largeSurahKeepsStableBlockIdentityAndPosition() {
        List<IbnKathirGroupIndex.Group> baqara = IbnKathirGroupIndex.shared().groupsForSurah(2);
        assertTrue("Al-Baqara must exercise a large graphical map", baqara.size() > 50);
        int selected = baqara.size() - 1;
        IbnKathirGroupIndex.Group group = IbnKathirMapModel.groupAt(2, selected,
            IbnKathirGroupIndex.shared());
        assertEquals(baqara.get(selected).id, group.id);
        int page = IbnKathirMapModel.clampPage(group,
            IbnKathirMapModel.pageSpan(group, geometry).last, geometry);
        IbnKathirMapModel.SavedState saved = new IbnKathirMapModel.SavedState(
            2, selected, page, 731);
        assertEquals(2, saved.surah);
        assertEquals(selected, saved.groupIndex);
        assertEquals(page, saved.previewPage);
        assertEquals(731, saved.scrollY);
    }

    @Test public void productionJourneyIsGraphicalOfflineAndRestorable() throws Exception {
        String activity = source(
            "hifz-app/src/main/java/com/quransafeguard/hifz/preview/IbnKathirMapActivity.java");
        String manifest = source("hifz-app/src/main/AndroidManifest.xml");
        String home = source(
            "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");

        assertFalse("The retired numbered ListView must not be the primary map",
            activity.contains("ListView") || activity.contains("ArrayAdapter"));
        assertTrue(activity.contains("buildGraphicalMap"));
        assertTrue(activity.contains("IbnKathirQuranComPrivateTitles.approvedTitle"));
        assertTrue(activity.contains("Sans titre"));
        assertTrue(activity.contains("new MushafView(this)"));
        assertTrue(activity.contains("setHighlightVerses"));
        assertTrue(activity.contains("EXTRA_JUMP_PAGE"));
        assertTrue(activity.contains("EXTRA_JUMP_VERSE"));
        assertTrue(activity.contains("EXTRA_MAP_PREVIEW"));
        assertTrue(activity.contains("onSaveInstanceState"));
        assertTrue(activity.contains("scrollY"));
        assertFalse(activity.contains("IbnKathirOfflineTitles"));
        assertFalse(activity.contains("IbnKathirTitlePack"));
        assertFalse(activity.contains("Intent.ACTION_VIEW"));
        assertTrue(manifest.contains("<activity android:name=\".IbnKathirMapActivity\""));
        assertTrue("Atlas remains a direct home destination",
            home.contains("primaryTile(destinations, \"Atlas\", \"Atlas\""));
        assertTrue(home.contains("new Intent(this, AtlasMapActivity.class)"));
        assertFalse("Atlas must not open legacy Ibn Kathir in the home navigation",
            home.contains("new Intent(this, IbnKathirMapActivity.class)"));
    }
}
