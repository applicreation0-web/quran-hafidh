package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Real Android evidence: the live screen, not only its pure planner, uses
 * immutable physical QCF windows and resumes the same ×40 count on recreation. */
public final class ItqanFourDateLiveRuntimeInstrumentedTest {
  @Test public void screenOpensFrozenFourDateWindowAndRetainsRealRepetitionAfterRecreation() {
    Context context=ApplicationProvider.getApplicationContext();
    SharedPreferences disk=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
    Map<String,?> before=new HashMap<>(disk.getAll());
    try {
      assertTrue(disk.edit().clear().commit());
      GeometryRepository geo=GeometryRepository.get(context);
      HifzPrefs prefs=new HifzPrefs(context);
      GeometryRepository.LineMeta first=geo.linesForExactIds(geo.lineIdsOnPage(520)).get(0);
      VerseRef start=first.verses.get(0);
      assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
          ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,start,false)));
      LocalDate scheduled=HifzClock.today().plusDays(1);
      Intent launch=new Intent(context,HifzSessionActivity.class)
          .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.ITQAN)
          .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduled.toString());
      try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(launch)) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(a->{
          ItqanFourSessionSnapshot snapshot=(ItqanFourSessionSnapshot)field(a,"frozenItqan");
          assertNotNull("Production screen did not use the 2-page/4-date plan",snapshot);
          assertEquals(0,snapshot.sessionIndex);
          assertEquals(0,snapshot.reps);
          assertEquals(520,snapshot.plan.firstPage);
          assertEquals(521,snapshot.plan.lastPage);
          assertTrue(((TextView)field(a,"program")).getText().toString().contains("séance 1/4"));
          LinearLayout actions=(LinearLayout)field(a,"actions");
          assertTrue("Missing real Itqan repetition control",actions.getChildCount()>0);
          ViewGroup repBox=(ViewGroup)actions.getChildAt(0);
          assertTrue(repBox.getChildAt(0) instanceof Button);
          assertTrue("The repetition tap must be wired",repBox.getChildAt(0).performClick());
        });
        scenario.recreate();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(a->{
          ItqanFourSessionSnapshot after=(ItqanFourSessionSnapshot)field(a,"frozenItqan");
          assertNotNull(after);
          assertEquals("Real tap must survive Activity recreation",1,after.reps);
          assertEquals(1,new HifzPrefs(a).frozenItqanSnapshot(
              GeometryRepository.get(a)).reps);
          assertEquals(520,after.plan.firstPage);
          assertEquals(521,after.plan.lastPage);
        });
        HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-itqan-4dates.png");
      }
    } finally {
      SharedPreferences.Editor restore=disk.edit().clear();
      for(Map.Entry<String,?> item:before.entrySet()) {
        Object v=item.getValue(),k=item.getKey();
        String key=(String)k;
        if(v instanceof String)restore.putString(key,(String)v);
        else if(v instanceof Integer)restore.putInt(key,(Integer)v);
        else if(v instanceof Long)restore.putLong(key,(Long)v);
        else if(v instanceof Boolean)restore.putBoolean(key,(Boolean)v);
        else if(v instanceof Float)restore.putFloat(key,(Float)v);
        else if(v instanceof Set) {
          @SuppressWarnings("unchecked")
          Set<String> saved=(Set<String>)v;
          restore.putStringSet(key,saved);
        } else throw new AssertionError("Unsupported persisted type for safe restore: "+key);
      }
      assertTrue("Never leave test data inside Hifz progress",restore.commit());
    }
  }
  private static Object field(Object host,String name) {
    try {
      Field f=host.getClass().getDeclaredField(name);
      f.setAccessible(true);return f.get(host);
    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
  }
}
