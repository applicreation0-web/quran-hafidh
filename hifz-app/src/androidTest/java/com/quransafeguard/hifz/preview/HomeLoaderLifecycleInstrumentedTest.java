package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic reproduction of the two PR9 late-home-callback crashes. */
public final class HomeLoaderLifecycleInstrumentedTest {
    @Test public void finishedHomeMustNotRefreshFromQueuedLoaderCompletion() throws Exception {
        Application app=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=app.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        new HifzPrefs(app);
        CountDownLatch destroyed=new CountDownLatch(1);
        AtomicReference<Throwable> error=new AtomicReference<>();
        Application.ActivityLifecycleCallbacks observer=new Application.ActivityLifecycleCallbacks(){
            @Override public void onActivityPostCreated(Activity activity,Bundle state){
                if(!(activity instanceof MainActivity))return;
                try{
                    Field field=MainActivity.class.getDeclaredField("localLoader");
                    field.setAccessible(true);
                    // The main thread is deliberately held while the real loader
                    // finishes and queues its UI callback. No production stub.
                    ((ExecutorService)field.get(activity)).submit(()->{}).get(30,TimeUnit.SECONDS);
                    activity.finish();
                    assertTrue(raw.edit().clear().commit());
                }catch(Throwable failure){error.set(failure);activity.finish();}
            }
            @Override public void onActivityDestroyed(Activity activity){
                if(activity instanceof MainActivity)destroyed.countDown();
            }
            @Override public void onActivityCreated(Activity a,Bundle b){}
            @Override public void onActivityStarted(Activity a){}
            @Override public void onActivityResumed(Activity a){}
            @Override public void onActivityPaused(Activity a){}
            @Override public void onActivityStopped(Activity a){}
            @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
        };
        app.registerActivityLifecycleCallbacks(observer);
        try{
            app.startActivity(new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            assertTrue("Closed home must reach destruction",destroyed.await(40,TimeUnit.SECONDS));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            if(error.get()!=null)throw new AssertionError(error.get());
            assertTrue("Late completion must not recreate state or credits",raw.getAll().isEmpty());
        }finally{
            app.unregisterActivityLifecycleCallbacks(observer);
            assertTrue(raw.edit().clear().commit());
        }
    }
}
