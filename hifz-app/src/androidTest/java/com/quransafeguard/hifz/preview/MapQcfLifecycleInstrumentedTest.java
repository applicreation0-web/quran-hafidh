package com.quransafeguard.hifz.preview;

import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** A JavaScript bridge request queued before teardown must never navigate afterward. */
public final class MapQcfLifecycleInstrumentedTest {
 @Test public void queuedAndLateBridgeRequestsCannotNavigateAfterWebViewDestruction(){
  AtomicInteger callbacks=new AtomicInteger();
  Object[] bridge={null};
  int[] fullGeneration={0},mosaicGeneration={0};
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(StudyReaderActivity.class)){
   scenario.onActivity(activity->{
    MapQcfThumbnails map=new MapQcfThumbnails(activity,new MapQcfThumbnails.Listener(){
     @Override public void onPageChosen(int page){callbacks.incrementAndGet();}
     @Override public void onOpenReading(int page){callbacks.incrementAndGet();}
    });
    FrameLayout root=new FrameLayout(activity);root.addView(map);activity.setContentView(root);
    map.showPages(1,2,1);
    bridge[0]=newBridge(map);
    fullGeneration[0]=(Integer)field(map,"fullGeneration");
    mosaicGeneration[0]=(Integer)field(map,"mosaicGeneration");
    // All callbacks are queued on the UI thread, then the same UI turn destroys
    // the WebView. Without the disposal guard, choose/open/step still notify.
    invoke(bridge[0],"choose",new Class<?>[]{int.class},2);
    invoke(bridge[0],"open",new Class<?>[]{int.class},1);
    invoke(bridge[0],"step",new Class<?>[]{int.class},1);
    map.destroy();
   });
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   assertEquals("Queued map bridge navigation escaped the destroyed host",0,callbacks.get());
   // Simulate a bridge worker already holding its old Java interface. These
   // requests must return safely without decoding/reinjecting a dead document.
   invoke(bridge[0],"full",new Class<?>[]{int.class,int.class},1,fullGeneration[0]);
   invoke(bridge[0],"thumb",new Class<?>[]{int.class,int.class},1,mosaicGeneration[0]);
   invoke(bridge[0],"choose",new Class<?>[]{int.class},2);
   invoke(bridge[0],"open",new Class<?>[]{int.class},1);
   InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   assertEquals("Late map bridge navigation escaped the destroyed host",0,callbacks.get());
  }
 }
 private static Object newBridge(MapQcfThumbnails map){
  try{
   for(Class<?> type:MapQcfThumbnails.class.getDeclaredClasses())if(type.getSimpleName().equals("Bridge")){
    Constructor<?> constructor=type.getDeclaredConstructor(MapQcfThumbnails.class);constructor.setAccessible(true);return constructor.newInstance(map);
   }
   throw new AssertionError("Map bridge not found");
  }catch(Exception error){throw new AssertionError(error);}
 }
 private static Object field(Object target,String name){
  try{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
  catch(Exception error){throw new AssertionError(error);}
 }
 private static void invoke(Object target,String name,Class<?>[] types,Object... values){
  try{Method method=target.getClass().getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(target,values);}
  catch(Exception error){throw new AssertionError(error);}
 }
}
