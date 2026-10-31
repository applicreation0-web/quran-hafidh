package com.quransafeguard.hifz.preview;
import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

/** Every canonical page has exactly its admitted endpoints, grouped in one real margin control. */
public final class MutashPageMarkersInstrumentedTest {
    @Test public void all604PagesResolveLinksContextually() throws Exception {
        GeometryRepository geometry=GeometryRepository.get(ApplicationProvider.getApplicationContext());
        int total=0,expectedTotal=0;
        for(int page=1;page<=604;page++){
            Set<String> expected=new LinkedHashSet<>();
            for(AtlasDiscriminantCases.Entry e:AtlasDiscriminantCases.CATALOGUE){
                if(e.firstPage==page)expected.add(e.first.toString());
                if(e.secondPage==page)expected.add(e.second.toString());
            }
            JSONArray refs=AtlasDiscriminantMarkers.onPage(page,geometry);
            Set<String> actual=new LinkedHashSet<>();
            for(int i=0;i<refs.length();i++){
                String key=refs.getString(i);String[] parts=key.split(":");
                VerseRef verse=new VerseRef(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
                assertEquals("Icon misplaced "+key,page,geometry.pageForVerse(verse));
                assertTrue(actual.add(key));total++;
            }
            assertEquals("Exact endpoints on QCF page "+page,expected,actual);
            expectedTotal+=expected.size();
        }
        assertEquals(expectedTotal,total);
    }
    @Test public void bothAlBaqarahPriorityPagesHaveAutomaticLinks() throws Exception {
        GeometryRepository geometry=GeometryRepository.get(ApplicationProvider.getApplicationContext());
        assertTrue(AtlasDiscriminantMarkers.onPage(10,geometry).toString().contains("2:63"));
        assertTrue(AtlasDiscriminantMarkers.onPage(14,geometry).toString().contains("2:93"));
    }
    private static String js(ActivityScenario<StudyReaderActivity> scene,String script)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> value=new AtomicReference<>();
        scene.onActivity(a->{
            try{
                java.lang.reflect.Field field=StudyReaderActivity.class.getDeclaredField("mushaf");
                field.setAccessible(true);((MushafView)field.get(a)).evaluateJavascript(script,v->{value.set(v);done.countDown();});
            }catch(Exception e){throw new AssertionError(e);}
        });
        assertTrue(done.await(15,TimeUnit.SECONDS));return value.get();
    }
    private static Intent reader(int page){
        Context ctx=ApplicationProvider.getApplicationContext();
        return new Intent(ctx,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,page);
    }
    @Test public void multiplePageComparisonsUseOneValidOutsideMarginChainAndNoBottomChain()throws Exception{
        assertTrue("Representative page has multiple retained comparisons",AtlasDiscriminantCases.indicesForPage(9).size()>1);
        try(ActivityScenario<StudyReaderActivity> scene=ActivityScenario.launch(reader(9))){
            String count="0";
            for(int attempt=0;attempt<60;attempt++){
                count=js(scene,"document.querySelectorAll('#atlas-discriminant-marks button').length");
                if("1".equals(count))break;Thread.sleep(150L);
            }
            assertEquals("One grouped contextual link", "1",count);
            String measured=js(scene,"(function(){var b=document.querySelector('#atlas-discriminant-marks button');"
                +"var svg=b.querySelector('svg'),r=b.getBoundingClientRect(),m=document.querySelector('#mushaf').getBoundingClientRect(),s=getComputedStyle(b);"
                +"return JSON.stringify({viewBox:svg.getAttribute('viewBox'),paths:svg.querySelectorAll('path').length,"
                +"cssWidth:s.width,cssHeight:s.height,width:r.width,height:r.height,"
                +"outside:r.right<=m.left+1||r.left>=m.right-1,buttonLeft:r.left,buttonRight:r.right,mushafLeft:m.left,mushafRight:m.right});})()");
            org.json.JSONObject metric=new org.json.JSONObject(new JSONArray("["+measured+"]").getString(0));
            assertEquals(metric.toString(),"0 0 24 24",metric.getString("viewBox"));
            assertEquals(metric.toString(),2,metric.getInt("paths"));
            assertEquals(metric.toString(),"44px",metric.getString("cssWidth"));
            assertEquals(metric.toString(),"44px",metric.getString("cssHeight"));
            // Fractional native WebView density may quantize a 44-CSS-pixel DOMRect.
            assertEquals(metric.toString(),44.0,metric.getDouble("width"),0.02);
            assertEquals(metric.toString(),44.0,metric.getDouble("height"),0.02);
            assertTrue(metric.toString(),metric.getBoolean("outside"));
            scene.onActivity(a->{
                assertTrue(a.discriminantAvailableForTest());
                // The removed bottom action is not replaced with a detached button for tests.
                assertEquals(0,countNativeChainButtons(a.findViewById(android.R.id.content)));
            });
        }
    }
    private static int countNativeChainButtons(android.view.View view){
        int count=view instanceof android.widget.Button&&view.getContentDescription()!=null
            &&view.getContentDescription().toString().startsWith("Versets ressemblants")?1:0;
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)count+=countNativeChainButtons(group.getChildAt(i));}
        return count;
    }
    @Test public void pageWithoutRetainedComparisonHasNoMarker()throws Exception{
        int page=1;while(page<=604&&!AtlasDiscriminantCases.indicesForPage(page).isEmpty())page++;
        assertTrue(page<=604);final int target=page;
        try(ActivityScenario<StudyReaderActivity> scene=ActivityScenario.launch(reader(target))){
            for(int attempt=0;attempt<30;attempt++){
                if("true".equals(js(scene,"!!document.querySelector('#mushaf svg .ayahPolygon')")))break;
                Thread.sleep(150L);
            }
            assertEquals("true",js(scene,"!!document.querySelector('#mushaf svg .ayahPolygon')"));
            assertEquals("0",js(scene,"document.querySelectorAll('#atlas-discriminant-marks button').length"));
            scene.onActivity(a->assertFalse(a.discriminantAvailableForTest()));
        }
    }
}
