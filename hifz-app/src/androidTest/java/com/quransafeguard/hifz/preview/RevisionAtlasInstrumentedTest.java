package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.VerseRange;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real session, real clock and QCF DOM; the fixture never touches a user's device. */
public final class RevisionAtlasInstrumentedTest {
    private static Object field(Object a,String name){
        try{Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);}
        catch(Exception e){throw new AssertionError(e);}
    }
    private static void invoke(Object a,String name,Class<?>[] types,Object...args){
        try{Method m=a.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(a,args);}
        catch(Exception e){throw new AssertionError(e);}
    }
    private static View find(View root,String label){
        if(label.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){
            View v=find(g.getChildAt(i),label);if(v!=null)return v;}}
        return null;
    }
    private static HifzPrefs fixture(Context c){
        c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).edit().clear().commit();
        HifzPrefs p=new HifzPrefs(c);GeometryRepository g=GeometryRepository.get(c);
        VerseRef first=new VerseRef(30,35),last=new VerseRef(30,40);
        assertTrue(p.setV6AcquiredRanges(Collections.singletonList(new VerseRange(first,last)),g));
        p.setMurajaahCursor(first);p.setActiveMurajaahCursor(first);
        p.setMurajaahPage(g.pageForVerse(new VerseRef(30,37)));
        p.setActiveMurajaahPage(g.pageForVerse(new VerseRef(30,37)));
        return p;
    }
    private static JSONObject snapshot(Context c)throws Exception{
        JSONObject result=HifzBackup.export(c);result.remove("exportedAtEpochMs");return result;
    }

    private static AyahThemesCueIndex recallTitleFixture(Context c)throws Exception{
        // CI deliberately excludes private headings. Exercise the real mask with
        // canonical passage boundaries and exact shipped QCF word geometry.
        // Synthetic labels are test metadata only; no Quran text is generated.
        MawdudiPassageIndex canonical=MawdudiPassageIndex.load(c);
        org.json.JSONArray rows=new org.json.JSONArray();
        for(MawdudiPassageIndex.Passage p:canonical.all())
            rows.put(new JSONObject().put("id",p.id).put("surah",p.surah)
                .put("start",p.firstAyah).put("end",p.lastAyah)
                .put("source_immutability","LOCKED").put("display_label","TEST "+p.id)
                .put("source",p.editorialAdaptation?JSONObject.NULL:"Ayah Themes")
                .put("source_title_exact",p.editorialAdaptation?JSONObject.NULL:"TEST "+p.id)
                .put("display_label_origin",p.editorialAdaptation?AyahThemesCueIndex.EDITORIAL:AyahThemesCueIndex.ORIGINAL)
                .put("display_label_approved_by_user",p.editorialAdaptation));
        return AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA)
            .put("passages",rows).toString(),canonical,WordGeometryRepository.shared(c));
    }
    private static void prepareRealRecallMask(ActivityScenario<HifzSessionActivity> s,AyahThemesCueIndex titles){
        s.onActivity(a->{
            try{Field f=HifzSessionActivity.class.getDeclaredField("ayahThemesTitles");
                f.setAccessible(true);f.set(a,titles);}
            catch(ReflectiveOperationException e){throw new AssertionError(e);}
            invoke(a,"renderMurajaahActive",new Class<?>[0]);
        });
        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Actual QCF recall mask");
    }
    private static void assertRealRecallMask(ActivityScenario<HifzSessionActivity> s)throws Exception{
        assertEquals("A Java field of 100 is not evidence of a painted mask", "true",js(s,
            "(function(){return mask===100 && document.querySelectorAll('g.masklayer').length>0"
            +" && !!document.querySelector('#hifz-exact-word-holes');})()"));
    }

    private static void revealWithRealTouch(HifzSessionActivity a){
        View reveal=find((View)field(a,"actions"),"Révéler");
        assertNotNull(reveal);
        long now=android.os.SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,1,1,0);
        MotionEvent up=MotionEvent.obtain(now,now,MotionEvent.ACTION_UP,1,1,0);
        try{reveal.dispatchTouchEvent(down);reveal.dispatchTouchEvent(up);}
        finally{down.recycle();up.recycle();}
    }
    private static void assertCorrectiveRecallMask(ActivityScenario<HifzSessionActivity> s)throws Exception{
        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Corrective recall before validation");
        s.onActivity(a->{
            assertEquals(100,field(a,"currentMask"));
            assertEquals(View.GONE,((View)field(a,"discriminantButton")).getVisibility());
            assertEquals(View.GONE,((View)field(a,"annotationOverlay")).getVisibility());
        });
        assertEquals("Corrective recall has a painted full-page mask without hints", "true",js(s,
            "(function(){var m=document.querySelector('#mushaf');var layer=m.querySelector('svg g.masklayer');"
            +"return mask===100&&m.style.visibility==='visible'&&!!layer&&layer.children.length>0"
            +"&&protectedWordBoxes().length===0&&!layer.hasAttribute('mask')"
            +"&&!document.querySelector('#hifz-exact-word-holes')"
            +"&&!document.querySelector('#atlas-discriminant-marks');})()"));
    }

    private static void saveAuditCapture(String name)throws Exception{
        HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot(name);
        Context c=ApplicationProvider.getApplicationContext();
        // UTP uninstalls the target package after the suite. Preserve this real
        // screenshot in MediaStore before app-specific external files disappear.
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,name);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,
            android.os.Environment.DIRECTORY_PICTURES+"/HaafidhAudit");
        android.net.Uri uri=c.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull("MediaStore audit capture destination",uri);
        try(java.io.InputStream in=new java.io.FileInputStream(new java.io.File(c.getExternalFilesDir(null),name));
            java.io.OutputStream out=c.getContentResolver().openOutputStream(uri)){
            assertNotNull(out);byte[] bytes=new byte[16384];int n;
            while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
        }
    }
    private static boolean sameJson(Object a,Object b)throws Exception{
        if(a instanceof JSONObject && b instanceof JSONObject){
            JSONObject x=(JSONObject)a,y=(JSONObject)b;
            if(x.length()!=y.length())return false;
            java.util.Iterator<String> keys=x.keys();
            while(keys.hasNext()){String key=keys.next();
                if(!y.has(key)||!sameJson(x.get(key),y.get(key)))return false;}
            return true;
        }
        if(a instanceof org.json.JSONArray && b instanceof org.json.JSONArray){
            org.json.JSONArray x=(org.json.JSONArray)a,y=(org.json.JSONArray)b;
            if(x.length()!=y.length())return false;
            for(int i=0;i<x.length();i++)if(!sameJson(x.get(i),y.get(i)))return false;
            return true;
        }
        return java.util.Objects.equals(a,b);
    }
    private static String js(ActivityScenario<HifzSessionActivity> s,String script)throws Exception{
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        s.onActivity(a->((MushafView)field(a,"mushaf")).evaluateJavascript(script,v->{result.set(v);done.countDown();}));
        assertTrue(done.await(10,TimeUnit.SECONDS));return result.get();
    }
    @Test public void passiveNestedComparisonPausesClockAndRestoresExactSessionAndBackup()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();JSONObject original=HifzBackup.export(c);
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor atlas=ins.addMonitor(AtlasDiscriminantActivity.class.getName(),null,false);
        Instrumentation.ActivityMonitor lecture=ins.addMonitor(StudyReaderActivity.class.getName(),null,false);
        try{
            fixture(c);
            try(ActivityScenario<HifzSessionActivity> s=ActivityScenario.launch(new Intent(c,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH))){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Passive Atlas entry");
                AtomicReference<HifzSessionActivity> same=new AtomicReference<>();
                AtomicReference<SessionClock> clock=new AtomicReference<>();
                s.onActivity(a->{
                    same.set(a);clock.set((SessionClock)field(a,"clock"));
                    a.onVerseTap(new VerseRef(30,36));
                    assertEquals(View.VISIBLE,((View)field(a,"discriminantButton")).getVisibility());
                });
                saveAuditCapture("atlas-revision-passive.png");
                s.onActivity(a->a.onAtlasDiscriminantTap(new VerseRef(30,37)));
                Activity comparison=ins.waitForMonitorWithTimeout(atlas,20000);assertNotNull(comparison);
                ins.waitForIdleSync();
                long paused=clock.get().elapsedMs();JSONObject before=snapshot(c);
                Thread.sleep(1400L);
                assertEquals("Atlas consultation cannot count as revision time",paused,clock.get().elapsedMs());
                assertTrue("Consultation must not change any backup store",sameJson(before,snapshot(c)));
                int page=GeometryRepository.get(c).pageForVerse(new VerseRef(30,37));
                ins.runOnMainSync(()->invoke(comparison,"openReader",new Class<?>[]{boolean.class,int.class},true,page));
                Activity reader=ins.waitForMonitorWithTimeout(lecture,20000);assertNotNull(reader);
                ins.waitForIdleSync();
                JSONObject afterReaderBootstrap=snapshot(c);
                assertTrue("Nested Lecture must preserve every Hifz backup store",sameJson(before,afterReaderBootstrap));
                Thread.sleep(1200L);
                assertEquals("Nested Lecture cannot resume the revision clock",paused,clock.get().elapsedMs());
                assertTrue(sameJson(afterReaderBootstrap,snapshot(c)));
                ins.runOnMainSync(reader::finish);ins.waitForIdleSync();
                assertEquals(paused,clock.get().elapsedMs());
                ins.runOnMainSync(comparison::finish);ins.waitForIdleSync();
                s.onActivity(a->{
                    assertSame(same.get(),a);assertEquals(page,field(a,"currentPage"));
                    assertEquals(new VerseRef(30,36),field(a,"murajaahActualEnd"));
                    assertEquals(0,field(a,"currentMask"));
                    assertEquals(new VerseRef(30,37),field(a,"discriminantVerse"));
                });
                Thread.sleep(1100L);assertTrue(clock.get().elapsedMs()>=paused+900L);
                // onPause legitimately checkpoints elapsed time. Compare progression separately.
                JSONObject after=snapshot(c);
                String elapsedKey=HifzPrefs.elapsedKey(HifzSessionActivity.MURAJAAH);
                afterReaderBootstrap.getJSONObject("values").remove(elapsedKey);
                after.getJSONObject("values").remove(elapsedKey);
                assertTrue("Only resumed session time may change; no cursor, receipt or credit",
                    sameJson(afterReaderBootstrap,after));
            }
        }finally{ins.removeMonitor(atlas);ins.removeMonitor(lecture);HifzBackup.restore(c,original);}
    }
    @Test public void activeHasNoHintUntilRealValidationAndKeepsMaskAcrossRecreation()throws Exception{
        Context c=ApplicationProvider.getApplicationContext();JSONObject original=HifzBackup.export(c);
        Instrumentation ins=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor atlas=ins.addMonitor(AtlasDiscriminantActivity.class.getName(),null,false);
        try{
            HifzPrefs p=fixture(c);
            ActiveReviewCorrectionStore corrections=new ActiveReviewCorrectionStore(c);
            String sessionDate="2026-10-29";
            VerseRef start=p.activeMurajaahCursor(),end=new VerseRef(30,37);
            VerseRef passiveCursor=p.murajaahCursor();
            int acquiredLines=p.acquiredLineCountV6();
            String acquiredCredit=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE)
                .getString("v6AcquiredCreditLineIds",null);
            AyahThemesCueIndex titles=recallTitleFixture(c);
            try(ActivityScenario<HifzSessionActivity> s=ActivityScenario.launch(new Intent(c,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,sessionDate))){
                prepareRealRecallMask(s,titles);
                assertRealRecallMask(s);
                s.onActivity(a->{
                    assertEquals(100,field(a,"currentMask"));
                    assertEquals(View.GONE,((View)field(a,"discriminantButton")).getVisibility());
                    a.onAtlasDiscriminantTap(new VerseRef(30,37));
                    a.onVerseTap(new VerseRef(30,37)); // Endpoint selection is NOT validation.
                    assertEquals(View.GONE,((View)field(a,"discriminantButton")).getVisibility());
                });
                assertEquals("No active relation markers", "0",js(s,"document.querySelectorAll('#atlas-discriminant-marks button').length"));
                // Instrumentation.waitForMonitorWithTimeout removes its monitor even
                // on timeout; query the monitor directly to keep observing the later launch.
                assertNull(atlas.waitForActivityWithTimeout(500));
                // Exercise a real reveal explicitly, so this replay is required
                // even when private headings are installed. CI also records the
                // initial fail-open page before the synthetic titles are injected.
                s.onActivity(RevisionAtlasInstrumentedTest::revealWithRealTouch);
                assertEquals(Collections.singletonList(GeometryRepository.get(c).pageForVerse(end)),
                    new java.util.ArrayList<>(corrections.revealed(sessionDate,start)));
                saveAuditCapture("atlas-revision-active-masked.png");
                s.recreate();
                prepareRealRecallMask(s,titles);
                assertRealRecallMask(s);
                s.onActivity(a->{
                    assertEquals(100,field(a,"currentMask"));
                    assertEquals(View.GONE,((View)field(a,"discriminantButton")).getVisibility());
                    View validate=find(a.findViewById(android.R.id.content),"Valider jusqu’ici");
                    assertNotNull(validate);validate.performClick();
                });
                ins.waitForIdleSync();
                // A small acquired corpus loops in the time-budgeted plan. Its
                // existing partial-validation confirmation must be accepted by
                // the real user action, never bypassed by a test-only receipt.
                if(corrections.pending()==null){
                    android.view.accessibility.AccessibilityNodeInfo root=ins.getUiAutomation().getRootInActiveWindow();
                    assertNotNull("Partial validation dialog missing",root);
                    java.util.List<android.view.accessibility.AccessibilityNodeInfo> buttons=
                        root.findAccessibilityNodeInfosByText("Valider quand même");
                    assertFalse("Expected the real partial-validation confirmation",buttons.isEmpty());
                    assertTrue(buttons.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK));
                    ins.waitForIdleSync();
                }
                // Validation starts mandatory correction, not the Hifz receipt.
                assertNotNull(corrections.pending());
                assertEquals("READ",corrections.pending().stage);
                long originalElapsed=corrections.pending().activeElapsedMs;
                assertEquals("",p.lastActiveMurajaahDate());
                assertEquals(start,p.activeMurajaahCursor());
                assertEquals(end,p.activeMurajaahActualEnd());
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Corrective visible reread");
                assertEquals("true",js(s,"mask===0&&protectedWordBoxes().length===0"));
                s.onActivity(a->{
                    assertEquals(View.GONE,((View)field(a,"discriminantButton")).getVisibility());
                    assertEquals(View.GONE,((View)field(a,"annotationOverlay")).getVisibility());
                    find((View)field(a,"actions"),"Page relue").performClick();
                });
                assertCorrectiveRecallMask(s);
                s.onActivity(a->{
                    revealWithRealTouch(a);
                    assertTrue(corrections.pending().assisted);
                    View done=find((View)field(a,"actions"),"Page récitée sans aide");
                    assertFalse(done.isEnabled());
                    done.performClick(); // The handler must reject assistance too.
                    // A later scheduling date cannot relabel the pending replay.
                    a.getIntent().putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,"2026-10-30");
                });
                assertEquals("",p.lastActiveMurajaahDate());
                assertEquals("RECALL",corrections.pending().stage);
                s.recreate();
                assertCorrectiveRecallMask(s);
                s.onActivity(a->{
                    assertEquals(sessionDate,field(a,"sessionDate").toString());
                    a.getIntent().putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,sessionDate);
                    assertTrue(corrections.pending().assisted);
                    assertEquals(originalElapsed,corrections.pending().activeElapsedMs);
                    assertFalse(find((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    assertEquals(start,p.activeMurajaahCursor());
                    assertEquals(end,p.activeMurajaahActualEnd());
                    assertEquals("",p.lastActiveMurajaahDate());
                    find((View)field(a,"actions"),"Recommencer la page").performClick();
                });
                assertEquals("READ",corrections.pending().stage);
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Restarted visible reread");
                s.onActivity(a->find((View)field(a,"actions"),"Page relue").performClick());
                assertCorrectiveRecallMask(s);
                s.onActivity(a->{
                    assertFalse(corrections.pending().assisted);
                    View done=find((View)field(a,"actions"),"Page récitée sans aide");
                    assertTrue("Only the painted unaided recall can finish",done.isEnabled());
                    done.performClick();
                    assertEquals(sessionDate,p.lastActiveMurajaahDate());
                    assertEquals(p.activeMurajaahCorpus().next(end),p.activeMurajaahCursor());
                    assertEquals(passiveCursor,p.murajaahCursor());
                    assertEquals(acquiredLines,p.acquiredLineCountV6());
                    assertEquals(acquiredCredit,c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE)
                        .getString("v6AcquiredCreditLineIds",null));
                    assertTrue("Correction time cannot add active credit",p.lastActiveMurajaahLabel()
                        .endsWith("L/"+(originalElapsed/1000L)+"s"));
                    assertNull(corrections.pending());
                    assertTrue(corrections.revealed(sessionDate,start).isEmpty());
                    assertEquals(View.VISIBLE,((View)field(a,"discriminantButton")).getVisibility());
                });
                saveAuditCapture("atlas-revision-active-validated.png");
                s.recreate();
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Validated closing Fatiha");
                s.onActivity(a->{
                    assertEquals(View.VISIBLE,((View)field(a,"discriminantButton")).getVisibility());
                    ((View)field(a,"discriminantButton")).performClick();
                });
                Activity comparison=ins.waitForMonitorWithTimeout(atlas,20000);assertNotNull(comparison);
                ins.waitForIdleSync();JSONObject before=snapshot(c);Thread.sleep(1100L);
                assertTrue("Post-validation Atlas cannot create a second credit",sameJson(before,snapshot(c)));
                ins.runOnMainSync(comparison::finish);ins.waitForIdleSync();
                assertTrue("Returning cannot create a second receipt",sameJson(before,snapshot(c)));
            }
        }finally{ins.removeMonitor(atlas);HifzBackup.restore(c,original);}
    }
}
