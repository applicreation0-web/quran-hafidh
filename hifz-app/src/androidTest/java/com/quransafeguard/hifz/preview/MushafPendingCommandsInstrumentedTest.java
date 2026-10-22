package com.quransafeguard.hifz.preview;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Uses a real WebView, real folio and its actual JS-ready callback. */
public final class MushafPendingCommandsInstrumentedTest {
 @Test public void everyCommandSubmittedBeforeReadyRunsInOrder() throws Exception {
  List<Integer> observed=Collections.synchronizedList(new ArrayList<>());
  CountDownLatch done=new CountDownLatch(1);
  MushafView[] view=new MushafView[1];
  try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
   activity.onActivity(a->{
    try{
     view[0]=new MushafView(a);a.setContentView(view[0]);
     view[0].show(14,Collections.emptyList(),GeometryRepository.get(a).lineIdsOnPage(14),0);
     java.lang.reflect.Method enqueue=MushafView.class.getDeclaredMethod("runWhenReady",Runnable.class);
     enqueue.setAccessible(true);
     enqueue.invoke(view[0],(Runnable)()->observed.add(1));
     enqueue.invoke(view[0],(Runnable)()->observed.add(2));
     enqueue.invoke(view[0],(Runnable)()->{observed.add(3);done.countDown();});
    }catch(Exception e){throw new AssertionError(e);}
   });
   assertTrue("Real JS never became ready",done.await(30,TimeUnit.SECONDS));
   assertEquals("Required pre-ready commands were overwritten",Arrays.asList(1,2,3),observed);
   activity.onActivity(a->view[0].destroySafely());
  }
 }
}
