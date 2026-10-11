package com.quransafeguard.hifz.preview;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

/** Screens in the new contract: no Quiz, no passages lists, seven-day week. */
public final class ReaderSevenDayRuntimeInstrumentedTest {
 @Test public void homeHasNoQuizAndNoDeprecatedCarte() {
    try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->{
        View root=a.findViewById(android.R.id.content);
        assertFalse("Quiz must disappear entirely from home",shownText(root,"Quiz"));
        int expectedHeight=-1;
        for(String label:new String[]{"Lecture","Mémorisation libre","Parcours Hifz","Progression"}) {
          View row=rowContainingLabel(root,label);
          assertNotNull("Missing home destination "+label,row);
          assertTrue("No giant cards: "+label,row.getHeight()>0
              && row.getHeight()<=Ui.dp(a,76));
          if(expectedHeight<0)expectedHeight=row.getHeight();
          else assertTrue("Every destination must use the same compact row height",
              Math.abs(row.getHeight()-expectedHeight)<=Ui.dp(a,4));
        }
        android.widget.LinearLayout itqan=(android.widget.LinearLayout)reflect(a,"itqanCard");
        assertEquals("No engine diagnostics inside Parcours Hifz",
            "2 pages / 4 séances",Ui.settingValue(itqan).getText().toString());
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-accueil-sans-quiz.png");
    }
 }
 @Test public void sundayIsItqanAndManualRatioAlwaysSeven() {
    Context context=ApplicationProvider.getApplicationContext();
    HifzPrefs prefs=new HifzPrefs(context);
    for(int n=0;n<=6;n++) {
      int sabqi=0,itqan=0;
      for(java.time.DayOfWeek day:java.time.DayOfWeek.values()){
        com.quransafeguard.hifz.core.CadenceAction action=
          WeeklyProgramPolicy.actionFor(day,HifzPrefs.WeeklyProgram.MANUAL_SEVEN,n);
        if(action==com.quransafeguard.hifz.core.CadenceAction.LEARNING)sabqi++;
        else if(action==com.quransafeguard.hifz.core.CadenceAction.STABILIZATION)itqan++;
        else fail("A day was lost to a six-day programme");
      }
      assertEquals(n,sabqi);assertEquals(7-n,itqan);
    }
    assertEquals(com.quransafeguard.hifz.core.CadenceAction.STABILIZATION,
      WeeklyProgramPolicy.actionFor(java.time.DayOfWeek.SUNDAY,HifzPrefs.WeeklyProgram.MANUAL_SEVEN,3));
    try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)){
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->assertFalse(a.isFinishing()));
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-calendrier-sept-jours.png");
    }
 }
 @Test public void verifiedEnglishTitleAppearsLocallyWithoutBrowser() {
    Context context=ApplicationProvider.getApplicationContext();
    android.content.Intent intent=new android.content.Intent(context,StudyReaderActivity.class)
      .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,518);
    try(ActivityScenario<StudyReaderActivity> s=ActivityScenario.launch(intent)){
      HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Ibn Kathir title Qaf");
      s.onActivity(a->{
        if(Boolean.FALSE.equals(reflect(a,"semanticCuesEnabled"))) {
          try {
            java.lang.reflect.Method enable=StudyReaderActivity.class.getDeclaredMethod("toggleSemanticCues");
            enable.setAccessible(true);enable.invoke(a);
          }catch(Exception e){throw new AssertionError(e);}
        }
        a.onSemanticCueTap("IKEN050_001_005");
        Dialog dialog=(Dialog)reflect(a,"semanticTitleDialog");
        assertNotNull("Amorce dialog not created",dialog);
        assertTrue("Local English title not shown",dialog.isShowing());
        View root=dialog.findViewById(android.R.id.content);
        assertNotNull("Missing Al-Munir-style English title sheet",root);
        assertTrue("Missing verified local heading",containsText(root,
          "The Disbelievers wonder at the Message and Resurrection"));
        assertEquals("English popup must remain bottom-aligned",android.view.Gravity.BOTTOM,
          dialog.getWindow().getAttributes().gravity & android.view.Gravity.VERTICAL_GRAVITY_MASK);
        assertTrue("Caption positioning must be toolbar-anchored, not a fixed offset",
          dialog.getWindow().getAttributes().y>=Ui.dp(a,68));
        assertTrue("Caption must not prevent taps outside from reaching the Mushaf",
          (dialog.getWindow().getAttributes().flags
            & android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)!=0);
        TextView title=findTitle(root,
          "The Disbelievers wonder at the Message and Resurrection");
        assertNotNull(title);
        assertTrue("English heading is secondary micro-typography, not a giant bold banner",
          title.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity<=12.0f);
        assertFalse("English heading must not be bold",title.getTypeface().isBold());
      });
      // Measure actual on-device screen coordinates AFTER layout, not only an
      // arbitrary dialog gravity/margin: the complete caption stays over the
      // visible icons as the original Al-Munir design required.
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->{
        Dialog popup=(Dialog)reflect(a,"semanticTitleDialog");
        assertNotNull(popup);
        View iconRow=(View)reflect(a,"readerActions");
        assertEquals(View.VISIBLE,iconRow.getVisibility());
        View caption=popup.findViewById(android.R.id.content);
        assertNotNull(caption);
        int[] iconTop=new int[2],captionTop=new int[2];
        iconRow.getLocationOnScreen(iconTop);
        caption.getLocationOnScreen(captionTop);
        assertTrue("Title sheet must have measured height",caption.getHeight()>0);
        assertTrue("Al-Munir contract: heading BELOW Mushaf and strictly ABOVE icons",
          captionTop[1]+caption.getHeight()+Ui.dp(a,4)<=iconTop[1]);
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-lecture-amorce-anglais.png");
      // Different part of the Mushaf. The title MUST come from the pinned
      // source asset, not the 16 Qaf/Najm editorial checkpoint methods.
      s.onActivity(a->{
        try{
          java.lang.reflect.Method jump=StudyReaderActivity.class.getDeclaredMethod("setPage",int.class);
          jump.setAccessible(true);jump.invoke(a,2);
        }catch(Exception error){throw new AssertionError("Cannot navigate to real Al-Baqarah page",error);}
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(s,"mushaf","Ibn Kathir title Al-Baqarah");
      s.onActivity(a->{
        IbnKathirGroupIndex.Group baqarah=IbnKathirGroupIndex.shared().containing(2,1);
        assertNotNull(baqarah);
        String[] source=IbnKathirTitlePack.sourceHeadings(a,baqarah);
        assertEquals("The Virtues of Surat Al-Baqarah",source[0]);
        a.onSemanticCueTap("IKEN002_001_001");
        Dialog popup=(Dialog)reflect(a,"semanticTitleDialog");
        assertNotNull("Missing attested Al-Baqarah heading",popup);
        View panel=popup.findViewById(android.R.id.content);
        assertTrue("The verse-specific original title overrides general surah virtues",
            containsText(panel,"The Discussion of the Individual Letters"));
        assertFalse("Do not show a generic surah-virtue title for 2:1",
            containsText(panel,source[0]));
        View row=(View)reflect(a,"readerActions");
        assertTrue(row.getVisibility()==View.VISIBLE);
      });
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->{
        Dialog popup=(Dialog)reflect(a,"semanticTitleDialog");
        View row=(View)reflect(a,"readerActions");
        View panel=popup.findViewById(android.R.id.content);
        int[] barTop=new int[2],panelTop=new int[2];
        row.getLocationOnScreen(barTop);panel.getLocationOnScreen(panelTop);
        assertTrue("Actual Al-Baqarah heading must remain ABOVE the three icons",
            panelTop[1]+panel.getHeight()+Ui.dp(a,4)<=barTop[1]);
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-lecture-amorce-baqarah-source.png");
    }
 }
 @Test public void liveItqanFourDateWindowSurvivesRealTapAndRecreation() {
    Context context=ApplicationProvider.getApplicationContext();
    android.content.SharedPreferences disk=context.getSharedPreferences(
        "quran_hifz_preview_v1",Context.MODE_PRIVATE);
    java.util.Map<String,?> backup=new java.util.HashMap<>(disk.getAll());
    try {
      assertTrue(disk.edit().clear().commit());
      GeometryRepository geo=GeometryRepository.get(context);
      HifzPrefs prefs=new HifzPrefs(context);
      GeometryRepository.LineMeta first=geo.linesForExactIds(geo.lineIdsOnPage(520)).get(0);
      assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
          ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,first.verses.get(0),false)));
      java.time.LocalDate date=HifzClock.today().plusDays(1);
      android.content.Intent launch=new android.content.Intent(context,HifzSessionActivity.class)
          .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.ITQAN)
          .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,date.toString());
      try(ActivityScenario<HifzSessionActivity> s=ActivityScenario.launch(launch)) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        s.onActivity(a->{
          ItqanFourSessionSnapshot snapshot=(ItqanFourSessionSnapshot)reflect(a,"frozenItqan");
          assertNotNull("Itqan screen did not open frozen 2-page model",snapshot);
          assertEquals(0,snapshot.sessionIndex);
          assertEquals(0,snapshot.reps);
          assertEquals(520,snapshot.plan.firstPage);
          assertEquals(521,snapshot.plan.lastPage);
          assertTrue(((TextView)reflect(a,"program")).getText().toString().contains("séance 1/4"));
          android.widget.LinearLayout actions=(android.widget.LinearLayout)reflect(a,"actions");
          assertTrue(actions.getChildCount()>0);
          ViewGroup rep=(ViewGroup)actions.getChildAt(0);
          assertTrue(rep.getChildAt(0) instanceof android.widget.Button);
          assertTrue("Tap must reach actual repetition engine",rep.getChildAt(0).performClick());
        });
        s.recreate();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        s.onActivity(a->{
          ItqanFourSessionSnapshot restored=(ItqanFourSessionSnapshot)reflect(a,"frozenItqan");
          assertNotNull(restored);
          assertEquals("One real click persisted through Activity recreation",1,restored.reps);
          assertEquals(520,restored.plan.firstPage);
          assertEquals(521,restored.plan.lastPage);
          assertEquals(1,new HifzPrefs(a).frozenItqanSnapshot(
              GeometryRepository.get(a)).reps);
        });
        HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-itqan-4dates.png");
      }
    } finally {
      android.content.SharedPreferences.Editor restore=disk.edit().clear();
      for(java.util.Map.Entry<String,?> e:backup.entrySet()) {
        String key=e.getKey();Object val=e.getValue();
        if(val instanceof String)restore.putString(key,(String)val);
        else if(val instanceof Integer)restore.putInt(key,(Integer)val);
        else if(val instanceof Long)restore.putLong(key,(Long)val);
        else if(val instanceof Boolean)restore.putBoolean(key,(Boolean)val);
        else if(val instanceof Float)restore.putFloat(key,(Float)val);
        else if(val instanceof java.util.Set) {
          @SuppressWarnings("unchecked")
          java.util.Set<String> values=(java.util.Set<String>)val;
          restore.putStringSet(key,values);
        } else throw new AssertionError("Unknown Hifz preference type: "+key);
      }
      assertTrue("Restore original user state after Android proof",restore.commit());
    }
 }
 private static View rowContainingLabel(View node,String exact) {
   if(!(node instanceof ViewGroup))return null;
   ViewGroup group=(ViewGroup)node;
   for(int i=0;i<group.getChildCount();i++) {
     View child=group.getChildAt(i);
     if(child instanceof TextView && exact.contentEquals(((TextView)child).getText()))
       return node;
   }
   for(int i=0;i<group.getChildCount();i++) {
     View found=rowContainingLabel(group.getChildAt(i),exact);
     if(found!=null)return found;
   }
   return null;
 }
 private static TextView findTitle(View node,String target) {
   if(node instanceof TextView && target.contentEquals(((TextView)node).getText()))
     return (TextView)node;
   if(node instanceof ViewGroup) {
     ViewGroup group=(ViewGroup)node;
     for(int i=0;i<group.getChildCount();i++) {
       TextView result=findTitle(group.getChildAt(i),target);
       if(result!=null)return result;
     }
   }
   return null;
 }
 private static boolean containsText(View v,String value){
   if(v instanceof TextView && ((TextView)v).getText().toString().contains(value))return true;
   if(v instanceof ViewGroup){
     ViewGroup g=(ViewGroup)v;
     for(int i=0;i<g.getChildCount();i++)if(containsText(g.getChildAt(i),value))return true;
   }
   return false;
 }
 private static boolean shownText(View v,String value){
   if(v.getVisibility()!=View.VISIBLE)return false;
   if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return true;
   if(v instanceof ViewGroup){
     ViewGroup g=(ViewGroup)v;
     for(int i=0;i<g.getChildCount();i++)if(shownText(g.getChildAt(i),value))return true;
   }
   return false;
 }
 private static Object reflect(Object obj,String key){
   try{java.lang.reflect.Field f=obj.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(obj);}
   catch(Exception e){throw new AssertionError(e);}
 }
}
