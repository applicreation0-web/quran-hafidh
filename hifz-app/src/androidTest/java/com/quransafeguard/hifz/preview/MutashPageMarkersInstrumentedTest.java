package com.quransafeguard.hifz.preview;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.junit.Test;
import static org.junit.Assert.*;

/** Scan every QCF page, without a four-page whitelist, and show only verified cases. */
public final class MutashPageMarkersInstrumentedTest {
    @Test public void all604PagesResolveLinksContextually() throws Exception {
        Context ctx=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(ctx);
        int found=0;
        for(int page=1;page<=604;page++){
            JSONArray refs=AtlasDiscriminantMarkers.onPage(page,geometry);
            for(int i=0;i<refs.length();i++){
                String key=refs.getString(i);
                String[] parts=key.split(":");
                VerseRef verse=new VerseRef(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
                assertEquals("Icon misplaced on page "+page,page,geometry.pageForVerse(verse));
                assertTrue("Unverified link displayed "+key,
                    AtlasDiscriminantCases.indexFor(verse.getSurah(),verse.getAyah())>=0);
                found++;
            }
        }
        assertEquals("Five verified pairs should have two contextual endpoints each",10,found);
        assertEquals(-1,AtlasDiscriminantCases.indexFor(2,64));
        assertEquals(-1,AtlasDiscriminantCases.indexFor(2,92));
    }
    @Test public void bothAlBaqarahPagesHaveAnAutomaticLink() throws Exception {
        GeometryRepository geometry=GeometryRepository.get(ApplicationProvider.getApplicationContext());
        int p63=geometry.pageForVerse(new VerseRef(2,63));
        int p93=geometry.pageForVerse(new VerseRef(2,93));
        assertTrue(AtlasDiscriminantMarkers.onPage(p63,geometry).toString().contains("2:63"));
        assertTrue(AtlasDiscriminantMarkers.onPage(p93,geometry).toString().contains("2:93"));
        assertTrue(p63>=1&&p93<=604);
    }
}
