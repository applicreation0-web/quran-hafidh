package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual source-QCF snippets for the three reported cases on each display profile. */
public final class SimilitudesMandatInstrumentedTest {
    @Test public void authenticReportedSnippetsHaveExactReferencesAndHonestRelation()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();
        for(int id:new int[]{161,166,168}){
            int index=-1;
            for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++)if(AtlasDiscriminantCases.PILOT.get(i).auditId==id)index=i;
            assertTrue(index>=0);AtlasDiscriminantCases.Entry pair=AtlasDiscriminantCases.PILOT.get(index);
            if(id==166){assertEquals(new VerseRef(2,232),pair.first);assertEquals(new VerseRef(65,2),pair.second);}
            if(id==168)assertEquals("Conclusion commune",AtlasDiscriminantActivity.displayRelation(pair));
            Intent intent=new Intent(context,AtlasDiscriminantActivity.class)
                .putExtra(AtlasDiscriminantActivity.EXTRA_SURAH,pair.first.getSurah())
                .putExtra(AtlasDiscriminantActivity.EXTRA_AYAH,pair.first.getAyah())
                .putExtra(AtlasDiscriminantActivity.EXTRA_CASE_INDEX,index);
            try(ActivityScenario<AtlasDiscriminantActivity> scenario=ActivityScenario.launch(intent)){
                AtomicBoolean ready=new AtomicBoolean();long deadline=System.currentTimeMillis()+45000;
                while(System.currentTimeMillis()<deadline){
                    scenario.onActivity(a->{assertEquals(a.qcfFailureDetailForTest(),0,a.qcfPagesFailedForTest());ready.set(a.qcfPagesReadyForTest()&&a.qcfStripsReadyForTest());});
                    if(ready.get())break;Thread.sleep(100);
                }
                assertTrue("Both original Quran excerpts must actually render "+id,ready.get());
                MandatCaptureInstrumentedTest.capture("sim-"+id);
            }
        }
    }
}
