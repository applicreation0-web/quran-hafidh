package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Android runtime gates absent from the previous audit-only CI.
 * Pure opening/navigation smoke tests: never mark a Hifz unit complete. */
public final class HifzAndMapRuntimeSmokeInstrumentedTest {
    @Test public void eachOfficialHifzModeOpensWithoutCrash() {
        Context context=ApplicationProvider.getApplicationContext();
        String[] modes={
            HifzSessionActivity.SABQI,
            HifzSessionActivity.ITQAN,
            HifzSessionActivity.LEARNING_CONSOLIDATION,
            HifzSessionActivity.RECENT_SABQI_REVIEW,
            HifzSessionActivity.LEARNING_FINAL,
            HifzSessionActivity.CONSOLIDATION_FINAL,
            HifzSessionActivity.MURAJAAH_ACTIVE,
            HifzSessionActivity.MURAJAAH
        };
        for(String mode:modes) {
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,mode);
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(intent)) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity->{
                    assertFalse("Unexpected close of Hifz "+mode,activity.isFinishing());
                    assertNotNull("Missing Hifz view "+mode,
                        activity.findViewById(android.R.id.content));
                    TextView program=(TextView)reflect(activity,"program");
                    String title=program.getText().toString();
                    assertFalse("Hifz "+mode+" rendered an error instead of a session: "+title,
                        title.contains("état à vérifier"));
                });
            }
        }
    }

    @Test public void ibnKathirMapMustShowFullBlockListThenSeparatePassageSheet() {
        Context context=ApplicationProvider.getApplicationContext();
        Intent intent=new Intent(context,IbnKathirMapActivity.class)
            .putExtra(IbnKathirMapActivity.EXTRA_SURAH,50)
            .putExtra(IbnKathirMapActivity.EXTRA_AYAH,1);
        try(ActivityScenario<IbnKathirMapActivity> scenario=ActivityScenario.launch(intent)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                ListView groups=(ListView)reflect(activity,"groupsList");
                View details=(View)reflect(activity,"detailPane");
                assertEquals("Block list should own screen initially",View.VISIBLE,groups.getVisibility());
                assertEquals("No oversized half-screen preview",View.GONE,details.getVisibility());
                assertTrue("Qaf should have multiple Ibn Kathir passages",groups.getCount()>1);
                groups.performItemClick(groups.getAdapter().getView(0,null,groups),0,groups.getAdapter().getItemId(0));
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                ListView groups=(ListView)reflect(activity,"groupsList");
                View details=(View)reflect(activity,"detailPane");
                assertEquals(View.GONE,groups.getVisibility());
                assertEquals(View.VISIBLE,details.getVisibility());
                assertFalse(activity.isFinishing());
                activity.onBackPressed();
                assertEquals("Retour must restore the block list",View.VISIBLE,groups.getVisibility());
            });
        }
    }

    private static Object reflect(Object owner,String name) {
        try {
            Field f=owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch(Exception x) {throw new AssertionError("Cannot inspect runtime view "+name,x);}
    }
}
