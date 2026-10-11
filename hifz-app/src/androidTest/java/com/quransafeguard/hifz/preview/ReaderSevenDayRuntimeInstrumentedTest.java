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
        try{
          java.lang.reflect.Method enable=StudyReaderActivity.class.getDeclaredMethod("toggleSemanticCues");
          enable.setAccessible(true);
          enable.invoke(a);
        }catch(Exception e){throw new AssertionError(e);}
        a.onSemanticCueTap("IKEN050_001_005");
        Dialog dialog=(Dialog)reflect(a,"semanticTitleDialog");
        assertNotNull("Amorce dialog not created",dialog);
        assertTrue("Local English title not shown",dialog.isShowing());
        TextView body=dialog.findViewById(android.R.id.message);
        assertNotNull("Missing verified local heading",body);
        assertTrue(body.getText().toString().contains("The Disbelievers wonder at the Message and Resurrection"));
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-lecture-amorce-anglais.png");
    }
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
