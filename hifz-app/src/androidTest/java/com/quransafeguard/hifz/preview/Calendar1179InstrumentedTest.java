package com.quransafeguard.hifz.preview;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Test;
import java.lang.reflect.*;
import java.time.LocalDate;
import static org.junit.Assert.*;
public final class Calendar1179InstrumentedTest {
    private static Object field(Object a,String name)throws Exception{Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    private static void call(Object a,String name,Class<?>[] types,Object... args)throws Exception{Method m=a.getClass().getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(a,args);}
    private void ready(ActivityScenario<MainActivity> s)throws Exception{
        for(int i=0;i<100;i++){final boolean[] ready={false};s.onActivity(a->{try{ready[0]=field(a,"todayRow")!=null;}catch(Exception e){throw new AssertionError(e);}});
            if(ready[0])return;Thread.sleep(100);}
        fail("Real geometry and planner must load");
    }
    @Test public void selectedWeekDoesNotChangeRealTodayOrProgress()throws Exception{
        try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){ready(s);
            s.onActivity(a->{try{
                HifzPrefs prefs=(HifzPrefs)field(a,"prefs");
                int acquired=prefs.acquiredLineCountV6();LocalDate actual=((WeeklyDashboardPlanner.Row)field(a,"todayRow")).date;
                LocalDate first=(LocalDate)field(a,"displayedWeekStart");
                call(a,"moveCalendarWeek",new Class<?>[]{int.class},1);
                assertEquals(first.plusWeeks(1),field(a,"displayedWeekStart"));
                assertEquals(actual,((WeeklyDashboardPlanner.Row)field(a,"todayRow")).date);
                assertEquals(acquired,prefs.acquiredLineCountV6());
                assertEquals(7,((ViewGroup)field(a,"weekStrip")).getChildCount());
                call(a,"moveCalendarWeek",new Class<?>[]{int.class},-1);
                assertEquals(first,field(a,"displayedWeekStart"));
            }catch(Exception e){throw new AssertionError(e);}});
        }
    }
    @Test public void recreationKeepsSelectedWeekAndAllSixDestinations()throws Exception{
        try(ActivityScenario<MainActivity> s=ActivityScenario.launch(MainActivity.class)){ready(s);
            final LocalDate[] selected={null};
            s.onActivity(a->{try{call(a,"showParcours",new Class<?>[]{});call(a,"moveCalendarWeek",new Class<?>[]{int.class},1);selected[0]=(LocalDate)field(a,"displayedWeekStart");}catch(Exception e){throw new AssertionError(e);}});
            s.recreate();ready(s);
            s.onActivity(a->{try{assertEquals(selected[0],field(a,"displayedWeekStart"));assertEquals(true,field(a,"showingParcours"));
                assertNotNull(field(a,"sabqiQuickAccess"));assertNotNull(field(a,"itqanQuickAccess"));
                assertNotNull(field(a,"consolidationCard"));assertEquals(7,((ViewGroup)field(a,"weekStrip")).getChildCount());
            }catch(Exception e){throw new AssertionError(e);}});
        }
    }
    @Test public void closedScreenRejectsLateCalendarRefreshWithoutSubmittingWork()throws Exception{
        java.util.concurrent.atomic.AtomicReference<MainActivity> closed=new java.util.concurrent.atomic.AtomicReference<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            ready(scenario);scenario.onActivity(closed::set);
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            try{assertTrue(closed.get().isDestroyed());call(closed.get(),"refreshDashboard",new Class<?>[]{});}
            catch(Exception error){throw new AssertionError("A late geometry callback must not resubmit to a closed executor",error);}
        });
    }
}
