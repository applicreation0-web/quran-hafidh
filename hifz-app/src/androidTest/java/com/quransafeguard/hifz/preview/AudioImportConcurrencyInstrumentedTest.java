package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.net.Uri;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import static org.junit.Assert.*;

/** Real Android test: no external audio corpus, network or Hifz mutations. */
public final class AudioImportConcurrencyInstrumentedTest {
    @Test public void secondImportRefusesConcurrentActivationAndConstructorNeverBlocks() throws Exception {
        Context app=ApplicationProvider.getApplicationContext();
        HifzAudioPack pack=new HifzAudioPack(app);
        Field field=HifzAudioPack.class.getDeclaredField("IMPORT_LOCK");
        field.setAccessible(true);
        ReentrantLock lock=(ReentrantLock)field.get(null);
        AtomicReference<HifzAudioPack.ImportResult> attempted=new AtomicReference<>();
        AtomicReference<Throwable> problem=new AtomicReference<>();
        lock.lock();
        try {
            Thread worker=new Thread(()->{
                try {
                    // A contending importer must fail before touching even an invalid URI.
                    new HifzAudioPack(app);
                    attempted.set(pack.importZip(Uri.parse("content://test.invalid/not-opened")));
                }catch(Throwable error){problem.set(error);}
            },"audio-import-contention-test");
            worker.start();
            worker.join(4000);
            assertFalse("Audio pack constructor/import blocked Android thread",worker.isAlive());
            if(problem.get()!=null)throw new AssertionError(problem.get());
            HifzAudioPack.ImportResult result=attempted.get();
            assertNotNull(result);
            assertFalse(result.ok);
            assertTrue(result.message, result.message.contains("déjà en cours"));
        }finally {
            lock.unlock();
        }
    }
}
