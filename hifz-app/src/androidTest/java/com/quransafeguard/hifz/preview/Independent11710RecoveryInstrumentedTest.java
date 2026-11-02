package com.quransafeguard.hifz.preview;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.VerseRange;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.Collections;
import static org.junit.Assert.*;

public final class Independent11710RecoveryInstrumentedTest {
 @Test public void damagedCheckpointExitPreservesUnvalidatedActiveTime() throws Exception {
  Context c=ApplicationProvider.getApplicationContext();
  SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",0);
  assertTrue(raw.edit().clear().commit());
  try {
   GeometryRepository g=GeometryRepository.get(c);HifzPrefs p=new HifzPrefs(c);
   assertTrue(p.setV6AcquiredRanges(Collections.singletonList(new VerseRange(new VerseRef(30,1),new VerseRef(30,60))),g));
   p.setActiveMurajaahCursor(new VerseRef(30,20));p.setActiveMurajaahActualEnd(new VerseRef(30,30));
   p.setElapsedFor(HifzSessionActivity.MURAJAAH_ACTIVE,12345L);
   assertTrue(raw.edit().putString("activeReviewRevealedPagesV1","{invalid").commit());
   String credits=raw.getString("v6AcquiredCreditLineIds",null);
   String receipt=p.lastActiveMurajaahDate();
   Intent i=new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE);
   try(ActivityScenario<HifzSessionActivity> s=ActivityScenario.launch(i)) {
    s.onActivity(a->{try {
     Field f=HifzSessionActivity.class.getDeclaredField("program");f.setAccessible(true);
     assertTrue(((TextView)f.get(a)).getText().toString().contains("état à vérifier"));
    }catch(Exception e){throw new AssertionError(e);}});
    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("independent-recovery-error.png");
   }
   assertEquals("An unvalidated recovery screen must not erase elapsed time",12345L,new HifzPrefs(c).elapsedFor(HifzSessionActivity.MURAJAAH_ACTIVE));
   assertEquals("{invalid",raw.getString("activeReviewRevealedPagesV1",""));
   assertEquals(credits,raw.getString("v6AcquiredCreditLineIds",null));
   assertEquals(receipt,new HifzPrefs(c).lastActiveMurajaahDate());
   assertEquals(new VerseRef(30,20),new HifzPrefs(c).activeMurajaahCursor());
   assertEquals(new VerseRef(30,30),new HifzPrefs(c).activeMurajaahActualEnd());
  }finally{assertTrue(raw.edit().clear().commit());}
 }
}
