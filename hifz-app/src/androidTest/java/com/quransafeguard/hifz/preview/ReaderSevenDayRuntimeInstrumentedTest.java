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

/** Reader and home regression coverage for the six-day cadence. */
public final class ReaderSevenDayRuntimeInstrumentedTest {
 @Test public void homeHasNoQuizAndNoDeprecatedCarte() {
    try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->{
        View root=a.findViewById(android.R.id.content);
        assertFalse("Quiz must disappear entirely from home",shownText(root,"Quiz"));
        int expectedHeight=-1;
        for(String label:new String[]{"Lecture","Atlas","Parcours","Progression"}) {
          java.util.ArrayList<View> candidates=new java.util.ArrayList<>();
          root.findViewsWithText(candidates,label,View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
          View tile=candidates.stream().filter(v->label.equals(v.getContentDescription())
              &&v.isShown()&&v.isClickable()).findFirst().orElse(null);
          assertNotNull("Missing home destination "+label,tile);
          assertTrue("Destination must be measured: "+label,tile.getHeight()>0);
          if(expectedHeight<0)expectedHeight=tile.getHeight();
          else assertTrue("Every destination must use the same tile height",
              Math.abs(tile.getHeight()-expectedHeight)<=Ui.dp(a,4));
        }
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-accueil-sans-quiz.png");
    }
 }
 @Test public void sundayIsReservedForFinalsAndManualCadenceUsesSixDays() {
    for(int n=0;n<=6;n++) {
      int sabqi=0,itqan=0;
      for(java.time.DayOfWeek day:java.time.DayOfWeek.values()){
        com.quransafeguard.hifz.core.CadenceAction action=
          WeeklyProgramPolicy.actionFor(day,HifzPrefs.WeeklyProgram.MANUAL_SEVEN,n);
        if(day==java.time.DayOfWeek.SUNDAY) {
          assertEquals(com.quransafeguard.hifz.core.CadenceAction.REVISION,action);
          continue;
        }
        if(action==com.quransafeguard.hifz.core.CadenceAction.LEARNING)sabqi++;
        else if(action==com.quransafeguard.hifz.core.CadenceAction.STABILIZATION)itqan++;
        else fail("Unexpected weekday cadence: "+day);
      }
      assertEquals(n,sabqi);assertEquals(6-n,itqan);
    }
    try(ActivityScenario<SettingsActivity> s=ActivityScenario.launch(SettingsActivity.class)){
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      s.onActivity(a->assertFalse(a.isFinishing()));
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-calendrier-six-jours.png");
    }
 }
 @Test public void ayahThemesPrivateTitleIsPageScopedOrFailsClosedWhenNotBundled() {
    Context context=ApplicationProvider.getApplicationContext();
    AyahThemesCueIndex cues=AyahThemesCueIndex.load(context);
    MawdudiPassageIndex.Passage qaf=MawdudiPassageIndex.load(context).at(50,1);
    String expected=cues==null?null:cues.titleForVisibleStart(518,qaf.id);
    android.content.Intent intent=new android.content.Intent(context,StudyReaderActivity.class)
      .putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,518);
    try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)){
      HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Ayah Themes Qaf title");
      scenario.onActivity(a->{
        if(Boolean.FALSE.equals(reflect(a,"semanticCuesEnabled"))) {
          try {
            java.lang.reflect.Method enable=StudyReaderActivity.class.getDeclaredMethod("toggleSemanticCues");
            enable.setAccessible(true);enable.invoke(a);
          }catch(Exception error){throw new AssertionError(error);}
        }
        a.onSemanticCueTap(qaf.id);
        Dialog dialog=(Dialog)reflect(a,"semanticTitleDialog");
        if(expected==null) {
          assertNull("Never fall back to a legacy title when private corpus is absent",dialog);
          // A prior popup must never masquerade as the label of an untitled cue.
          Dialog stale=new Dialog(a);
          stale.setTitle("SYNTHETIC_STALE_TITLE");
          stale.show();
          try {
            java.lang.reflect.Field field=StudyReaderActivity.class.getDeclaredField("semanticTitleDialog");
            field.setAccessible(true);
            field.set(a,stale);
          }catch(Exception bad){throw new AssertionError(bad);}
          a.onSemanticCueTap(qaf.id);
          assertFalse("Untitled cue must dismiss stale English caption",stale.isShowing());
          Dialog staleOnPage=new Dialog(a);
          staleOnPage.setTitle("SYNTHETIC_PREVIOUS_PAGE");
          staleOnPage.show();
          try {
            java.lang.reflect.Field field=StudyReaderActivity.class.getDeclaredField("semanticTitleDialog");
            field.setAccessible(true);
            field.set(a,staleOnPage);
          }catch(Exception bad){throw new AssertionError(bad);}
          a.onPageSwipe(1);
          assertFalse("Page change must dismiss stale semantic title",staleOnPage.isShowing());
          return;
        }
        assertNotNull(dialog);
        assertTrue(dialog.isShowing());
        assertTrue(containsText(dialog.findViewById(android.R.id.content),expected));
        TextView title=findTitle(dialog.findViewById(android.R.id.content),expected);
        assertNotNull(title);
        assertFalse("Regular typeface required",title.getTypeface().isBold());
        float sp=title.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity;
        assertTrue("BOOX title must remain legible in English",sp>=13.9f&&sp<=14.1f);
      });
      if(expected==null)return; // Public CI: no private title; personal APK verifies the local pack.
      InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(a->{
        Dialog dialog=(Dialog)reflect(a,"semanticTitleDialog");
        View icons=(View)reflect(a,"readerActions");
        View caption=dialog.findViewById(android.R.id.content);
        int[] i=new int[2],c=new int[2];
        icons.getLocationOnScreen(i);caption.getLocationOnScreen(c);
        assertTrue("Caption height must be measured",caption.getHeight()>0);
        assertTrue("Title bottom never overlaps toolbar",
          c[1]+caption.getHeight()+Ui.dp(a,4)<=i[1]);
        assertEquals("Title visible after measured anchoring",
          1f,dialog.getWindow().getAttributes().alpha,0.01f);
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-qurancom-qaf-private.png");

      scenario.onActivity(a->{
        try{
          java.lang.reflect.Method jump=StudyReaderActivity.class.getDeclaredMethod("setPage",int.class);
          jump.setAccessible(true);jump.invoke(a,2);
        }catch(Exception ex){throw new AssertionError(ex);}
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Quran.com Baqarah");
      scenario.onActivity(a->{
        MawdudiPassageIndex.Passage b=MawdudiPassageIndex.load(a).at(2,1);
        String heading=cues.titleForVisibleStart(2,b.id);
        assertNotNull("Private Ayah Themes pack must contain approved contextual 2:1",heading);
        a.onSemanticCueTap(b.id);
        Dialog popup=(Dialog)reflect(a,"semanticTitleDialog");
        assertNotNull(popup);
        assertTrue(containsText(popup.findViewById(android.R.id.content),heading));
        a.onSemanticCueTap("SYNTHETIC_UNKNOWN_ID");
        assertFalse("Unknown cue must clear a valid prior title",popup.isShowing());
        assertNull(reflect(a,"semanticTitleDialog"));
      });
      HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-qurancom-baqarah-private.png");
    }
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
