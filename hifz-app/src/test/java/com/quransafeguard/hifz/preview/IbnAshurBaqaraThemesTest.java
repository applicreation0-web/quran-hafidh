package com.quransafeguard.hifz.preview;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** CP-08P: source-faithful thematic overlap, no documentary boundary changes. */
public final class IbnAshurBaqaraThemesTest {
    private IbnAshurBaqaraThemes.Theme find(String id) {
        for (IbnAshurBaqaraThemes.Theme t : IbnAshurBaqaraThemes.all())
            if (t.id.equals(id)) return t;
        throw new AssertionError(id);
    }
    @Test public void thirteenSourceThemesIncludeDiscreteAndOverlappingReferences() {
        assertEquals(13, IbnAshurBaqaraThemes.all().size());
        assertEquals("2:1, 2:23", find("opening").references());
        assertTrue(find("opening").contains(1));
        assertFalse(find("opening").contains(22));
        assertTrue(find("opening").contains(23));
        for (String id : new String[]{"israelites", "disputes", "abrogation"})
            assertTrue(find(id).contains(106));
        assertTrue(find("legislation").contains(177));
        assertTrue(find("righteousness").contains(177));
        assertEquals("2:18–19, 2:74, 2:153, 2:189, 2:217, 2:243, 2:255",
                find("reminders").references());
    }
    @Test public void usesUnmodifiedCanonical173Blocks() {
        List<IbnKathirGroupIndex.Group> groups =
                IbnKathirGroupIndex.shared().groupsForSurah(2);
        assertEquals(173, groups.size());
        List<Integer> selected = IbnAshurBaqaraThemes.matchingIndices(find("qiblah"), groups);
        assertFalse(selected.isEmpty());
        for (int i : selected) {
            IbnKathirGroupIndex.Group block = groups.get(i);
            assertEquals(2, block.surah);
            assertTrue(block.startAyah <= 150 && block.endAyah >= 142);
            assertTrue(block.id.startsWith("IKEN002_"));
        }
        assertEquals(173, groups.size()); // All Passages remains complete
        assertFalse(find("qiblah").intersects(
                IbnKathirGroupIndex.shared().groupsForSurah(50).get(0)));
    }
}
