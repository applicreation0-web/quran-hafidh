package com.quransafeguard.hifz.preview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.VerseRange;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real reader, actual buttons and fully painted pages; synthetic progress only. */
public final class FragilityReaderVisualInstrumentedTest {
    @Test public void languageRecreationKeepsOutstandingAssistedRecall() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        int originalLanguage=HaafidhActivity.languageIndex(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        try {
            assertTrue(raw.edit().clear().commit());
            assertTrue(HaafidhActivity.setLanguage(context,0));
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1), end=new VerseRef(2,74);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geo));
            prefs.setActiveMurajaahCursor(start);
            VerseRef aid=new VerseRef(2,6);
            int page=geo.pageForVerse(aid);
            assertTrue(raw.edit().putString(ActiveReviewCorrectionStore.KEY_QUEUE,"["+page+"]").commit());
            ActiveReviewCorrectionStore store=prefs.activeReviewSlots().manualCorrection();
            int acquired=prefs.acquiredLineCountV6();
            int receipts=prefs.activeReviewSlots().receipts().size();
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_FRAGILITY_PAGE,page);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Visible weak spot");
                assertEquals("READ",store.pending().stage);
                activity.onActivity(a->click(a,"Masquer la page"));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Masked weak spot");
                assertEquals("RECALL",store.pending().stage);
                activity.onActivity(a->a.onVerseTap(aid));
                assertTrue(store.pending().assisted);
                String[] tags={"fr","en","wo"};
                String[] labels={"Fragilités","Weak spots","Pàcc yu jafe"};
                for(int language:new int[]{1,2,0,2,1,0}){
                    activity.onActivity(a->chooseLanguage(a,language));
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Recreated weak spot");
                    var pending = store.pending();
                    assertNotNull(pending);
                    assertEquals(page,pending.page());
                    assertEquals(0,pending.pageIndex);
                    assertEquals("RECALL",pending.stage);
                    assertTrue(pending.assisted);
                    activity.onActivity(a->{
                        assertEquals(tags[language],a.getResources().getConfiguration().getLocales().get(0).getLanguage());
                        assertEquals(labels[language],((TextView)field(a,"program")).getText().toString());
                        assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                        assertFalse(control((View)field(a,"actions"),a.getString(R.string.fragility_validate)).isEnabled());
                        assertFragilityTargetsFit(a);
                    });
                    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragility-"+tags[language]+".png");
                }
            }
            assertEquals(acquired,prefs.acquiredLineCountV6());
            assertEquals(start,prefs.activeMurajaahCursor());
            assertEquals(receipts,prefs.activeReviewSlots().receipts().size());
        } finally {
            assertTrue(HaafidhActivity.setLanguage(context,originalLanguage));
            HifzBackup.restore(context,original);
        }
    }

    @Test public void languageRecreationKeepsPausedTimedRecall() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        int originalLanguage=HaafidhActivity.languageIndex(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        java.time.LocalDate day=java.time.LocalDate.of(2026,11,28);
        ActiveReviewSlots.Slot slot=ActiveReviewSlots.Slot.MORNING;
        try {
            HifzClock.setClockForTests(java.time.Clock.fixed(
                day.atTime(6,0).toInstant(java.time.ZoneOffset.UTC),java.time.ZoneOffset.UTC));
            assertTrue(raw.edit().clear().commit());
            assertTrue(HaafidhActivity.setLanguage(context,0));
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(
                new VerseRange(start,new VerseRef(2,74))),geo));
            prefs.setActiveMurajaahCursor(start);
            ActiveReviewSlots slots=prefs.activeReviewSlots();
            int page=geo.pageForVerse(start);
            long elapsed=30_000L;
            assertTrue(slots.checkpoint(day,slot,start,start,page,elapsed,true,40));
            Intent intent=new Intent(context,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,slot.mode);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Paused timed review");
                for(int language:new int[]{1,2,0,2,1,0}) {
                    activity.onActivity(a->chooseLanguage(a,language));
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Recreated timed review");
                    activity.onActivity(a->{
                        assertTrue((Boolean)field(a,"activeClockPaused"));
                        assertEquals(elapsed,((SessionClock)field(a,"clock")).elapsedMs());
                        assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    });
                    var saved=slots.draft(day,slot);
                    assertNotNull(saved);
                    assertTrue(saved.paused);
                    assertEquals(elapsed,saved.elapsedMs);
                    assertEquals(40,saved.targetMinutes);
                    assertEquals(start,saved.start);
                    assertEquals(start,prefs.activeMurajaahCursor());
                    assertTrue(slots.receipts().isEmpty());
                }
            }
        } finally {
            HifzClock.resetClockForTests();
            assertTrue(HaafidhActivity.setLanguage(context,originalLanguage));
            HifzBackup.restore(context,original);
        }
    }

    private static void chooseLanguage(HifzSessionActivity activity,int language) {
        View languageButton=control(activity.getWindow().getDecorView(),activity.getString(R.string.app_language));
        assertNotNull(languageButton);
        assertTrue(languageButton.performClick());
        AlertDialog picker=(AlertDialog)field(activity,"fragilityLanguagePicker");
        android.widget.ListView choices=picker.getListView();
        assertEquals(HaafidhActivity.LANGUAGE_LABELS[language],choices.getAdapter().getItem(language));
        assertTrue(choices.performItemClick(null,language,choices.getItemIdAtPosition(language)));
    }

    private static void assertFragilityTargetsFit(HifzSessionActivity activity) {
        View commands = (View) field(activity,"actions");
        android.graphics.Rect frame = new android.graphics.Rect();
        activity.getWindow().getDecorView().getGlobalVisibleRect(frame);
        int target = Ui.dp(activity,48);
        for (int label : new int[]{R.string.fragility_previous,R.string.fragility_hide,R.string.fragility_reveal_verse,
                R.string.fragility_audio,R.string.fragility_validate,R.string.fragility_defer,R.string.fragility_next}) {
            View button = control(commands,activity.getString(label));
            assertNotNull(activity.getString(label),button);
            android.graphics.Rect bounds = new android.graphics.Rect();
            assertTrue(button.getGlobalVisibleRect(bounds));
            assertTrue(bounds.toShortString(),bounds.width() >= target && bounds.height() >= target);
            assertTrue(bounds.toShortString(),frame.contains(bounds));
        }
    }

    @Test public void standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        try {
            assertTrue(raw.edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1),end=new VerseRef(2,74);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geo));
            prefs.setActiveMurajaahCursor(start);
            int page=geo.pageForVerse(new VerseRef(2,6));
            assertTrue(raw.edit().putString(ActiveReviewCorrectionStore.KEY_QUEUE,"["+page+"]").commit());
            int acquired=prefs.acquiredLineCountV6();
            int receipts=prefs.activeReviewSlots().receipts().size();
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_FRAGILITY_PAGE,page);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité visible");
                activity.onActivity(a->{
                    assertEquals("Fragilités",((TextView)field(a,"program")).getText().toString());
                    assertEquals(0,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"fragilityPageReady"));
                    assertNotNull(control((View)field(a,"actions"),"Masquer la page"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-visible.png");
                double visibleInk=inkFraction(context,"fragilite-visible.png");
                assertTrue("Visible folio is blank or not fully painted: "+visibleInk,visibleInk>0.02);
                activity.onActivity(a->click(a,"Masquer la page"));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                    assertTrue(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-masquee.png");
                double maskedInk=inkFraction(context,"fragilite-masquee.png");
                assertTrue("Masked folio exposes too much ink: "+maskedInk,maskedInk<visibleInk*0.25);
                // Pick the verse with the largest central word-box area. It must reveal
                // real ink, but not replace the mask by an entirely visible folio.
                java.util.Map<VerseRef,Double> areas=new java.util.LinkedHashMap<>();
                for(WordGeometryRepository.WordBox w:WordGeometryRepository.shared(context).wordsForPage(page)){
                    double area=Math.max(0,Math.min(w.x1,276)-Math.max(w.x0,69))
                        *Math.max(0,Math.min(w.y1,440)-Math.max(w.y0,110));
                    VerseRef v=new VerseRef(w.surah,w.ayah);
                    areas.put(v,areas.getOrDefault(v,0d)+area);
                }
                VerseRef target=null;double largest=0;
                for(java.util.Map.Entry<VerseRef,Double> entry:areas.entrySet())
                    if(entry.getValue()>largest){largest=entry.getValue();target=entry.getKey();}
                assertNotNull("Precise verse geometry required",target);
                final VerseRef chosen=target;
                activity.onActivity(a->{
                    a.onVerseTap(chosen);
                    assertTrue(prefs.activeReviewSlots().manualCorrection().pending().assisted);
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Atomic targeted verse paint");
                activity.onActivity(a->{
                    assertTrue((Boolean)field(a,"fragilityPageReady"));
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-cible.png");
                double partialInk=inkFraction(context,"fragilite-cible.png");
                assertTrue("Targeted verse must reveal actual ink",partialInk>maskedInk+0.0002);
                assertTrue("Targeted aid must not expose the whole page",partialInk<visibleInk*0.8);
                activity.onActivity(a->click(a,"Masquer la page"));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fresh full mask after targeted help");
                activity.recreate();
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée reprise");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-reprise.png");
                assertTrue("Resumed masked folio exposes too much ink",inkFraction(context,"fragilite-reprise.png")<visibleInk*0.25);
                activity.onActivity(a->click(a,"Reporter cette page"));
            }
            assertTrue(prefs.activeReviewSlots().manualCorrection().queuedPages().contains(page));
            assertEquals(acquired,prefs.acquiredLineCountV6());
            assertEquals(start,prefs.activeMurajaahCursor());
            assertEquals(receipts,prefs.activeReviewSlots().receipts().size());
            for(int ayah=1;ayah<=74;ayah++)
                assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(2,ayah)));
        }finally{HifzBackup.restore(context,original);}
    }

    @Test public void threePageBatchAdvancesResumesDefersAndReturnsWithoutActiveCredit() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        try {
            assertTrue(raw.edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1),end=new VerseRef(2,74);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geo));
            prefs.setActiveMurajaahCursor(start);
            assertTrue(raw.edit().putString(ActiveReviewCorrectionStore.KEY_QUEUE,"[4,6,10]").commit());
            int acquired=prefs.acquiredLineCountV6();
            int receipts=prefs.activeReviewSlots().receipts().size();
            ActiveReviewCorrectionStore store=prefs.activeReviewSlots().manualCorrection();
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_FRAGILITY_PAGE,6);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)){
                assertPhase(activity,store,6,0,"READ");
                assertEquals(java.util.Arrays.asList(6,10,4),store.pending().pages);
                activity.onActivity(a->click(a,"Page suivante"));
                assertPhase(activity,store,10,0,"READ");
                activity.onActivity(a->assertTrue(a.dispatchKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP))));
                assertPhase(activity,store,6,0,"READ");
                assertEquals(java.util.Arrays.asList(6,10,4),store.pending().pages);
                activity.onActivity(a->assertEquals("Libre",((TextView)field(a,"timerText")).getText().toString()));
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,6,0,"RECALL");
                activity.onActivity(a->click(a,"Afficher la page"));
                assertPhase(activity,store,6,0,"READ");
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,6,0,"RECALL");
                VerseRef aid=null;
                for(WordGeometryRepository.WordBox w:WordGeometryRepository.shared(context).wordsForPage(6)){
                    aid=new VerseRef(w.surah,w.ayah);break;
                }
                assertNotNull(aid);
                final VerseRef selected=aid;
                activity.onActivity(a->{
                    a.onVerseTap(selected);
                    assertTrue(store.pending().assisted);
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    assertNotNull(control((View)field(a,"actions"),"Masquer la page"));
                });
                activity.recreate();
                assertPhase(activity,store,6,0,"RECALL");
                activity.onActivity(a->{
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    click(a,"Masquer la page");
                });
                assertPhase(activity,store,6,0,"RECALL");
                assertFalse(store.pending().assisted);
                activity.onActivity(a->click(a,"Page récitée sans aide"));
                assertPhase(activity,store,10,1,"READ");
                activity.recreate();
                assertPhase(activity,store,10,1,"READ");
                activity.onActivity(a->click(a,"Reporter cette page"));
                assertPhase(activity,store,4,2,"READ");
                assertEquals(Collections.singletonList(10),store.pending().deferred);
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,4,2,"RECALL");
                activity.onActivity(a->click(a,"Page récitée sans aide"));
                for(int attempt=0;attempt<40
                        &&activity.getState()!=androidx.lifecycle.Lifecycle.State.DESTROYED;attempt++)
                    android.os.SystemClock.sleep(100);
                assertEquals("Manual batch returns instead of leaving masked completion screen",
                    androidx.lifecycle.Lifecycle.State.DESTROYED,activity.getState());
            }
            assertNull(store.pending());
            assertEquals(Collections.singletonList(10),store.queuedPages());
            assertEquals(acquired,prefs.acquiredLineCountV6());
            assertEquals(start,prefs.activeMurajaahCursor());
            assertEquals(receipts,prefs.activeReviewSlots().receipts().size());
            for(int ayah=1;ayah<=74;ayah++)
                assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(2,ayah)));
        }finally{HifzBackup.restore(context,original);}
    }

    @Test public void programmedBudgetsExpireWithoutCreditOrLosingAnyOfTwelvePages() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original = HifzBackup.export(context);
        SharedPreferences raw = context.getSharedPreferences("quran_hifz_preview_v1", 0);
        java.time.LocalDate date = java.time.LocalDate.of(2026, 11, 23);
        ActiveReviewSlots.Slot slot = ActiveReviewSlots.Slot.MORNING;
        GeometryRepository geometry = GeometryRepository.get(context);
        VerseRef start = new VerseRef(2, 1);
        VerseRef end = new VerseRef(2, 74);
        VerseRef audioVerse = new VerseRef(2, 6);
        int firstPage = geometry.pageForVerse(audioVerse);
        File audioFolder = new File(context.getFilesDir(), "audio/husary-muallim");
        assertFalse("The test requires an isolated Android fixture without an audio pack", audioFolder.exists());
        java.util.List<Integer> pages = new java.util.ArrayList<>();
        for (int page = firstPage; page < firstPage + 12; page++) pages.add(page);

        try {
            HifzClock.setClockForTests(java.time.Clock.fixed(
                date.atTime(6, 0).toInstant(java.time.ZoneOffset.UTC), java.time.ZoneOffset.UTC));
            for (int[] band : new int[][]{{40, 5}, {60, 10}, {70, 15}}) {
                assertTrue(raw.edit().clear().commit());
                HifzPrefs prefs = new HifzPrefs(context);
                assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start, end)), geometry));
                prefs.setActiveMurajaahCursor(start);
                int acquired = prefs.acquiredLineCountV6();
                ActiveReviewSlots slots = prefs.activeReviewSlots();
                long activeMs = band[0] * 60_000L;
                long budgetMs = band[1] * 60_000L;
                ActiveReviewCorrectionStore store = slots.correction(date, slot);
                assertTrue(slots.checkpoint(date, slot, start, start, firstPage, activeMs, false, band[0]));
                assertTrue(store.begin(date.toString(), start, start, activeMs, pages, firstPage));
                VerseRef next = prefs.activeMurajaahCorpus().next(start);
                assertTrue(slots.completeRecall(date, slot, start, next, "Révision fixture"));

                if (band[1] == 15) seedSyntheticAudio(audioFolder);
                Intent intent = new Intent(context, HifzSessionActivity.class)
                    .putExtra(HifzSessionActivity.EXTRA_MODE, slot.mode);
                try (ActivityScenario<HifzSessionActivity> activity = ActivityScenario.launch(intent)) {
                    HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Programmed fragility");
                    activity.onActivity(a -> {
                        String label = ((TextView) field(a, "timerText")).getContentDescription().toString();
                        long elapsed = ((SessionClock) field(a, "fragilityClock")).elapsedMs();
                        long shown = Long.parseLong(label.substring(label.length() - 5, label.length() - 3)) * 60L
                            + Long.parseLong(label.substring(label.length() - 2));
                        assertTrue(Math.abs(shown - (budgetMs - elapsed) / 1000L) <= 2);
                        assertEquals(firstPage, store.pending().page());
                        if (band[1] == 10) click(a, "Page suivante");
                    });
                    if (band[1] == 10) {
                        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Next unresolved page");
                        assertEquals(firstPage + 1, store.pending().page());
                    }
                    if (band[1] != 10) {
                        activity.onActivity(a -> click(a, "Masquer la page"));
                        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "Full mask");
                        activity.onActivity(a -> selectAudioVerse(a, audioVerse));
                        if (band[1] == 5) {
                            activity.onActivity(a -> {
                                assertNull(field(a, "audioPlayer"));
                                assertFalse(store.pending().assisted);
                                a.onVerseTap(audioVerse);
                                assertTrue(store.pending().assisted);
                            });
                            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity, "mushaf", "First targeted reveal");
                            activity.onActivity(a -> a.onVerseTap(new VerseRef(2, 7)));
                        } else {
                            activity.onActivity(a -> {
                                assertEquals(Collections.singletonList(audioVerse),
                                    field(field(a, "audioPlayer"), "queue"));
                                assertTrue(store.pending().assisted);
                                assertFalse(control((View) field(a, "actions"), "Page récitée sans aide").isEnabled());
                                View audio = (View) field(a, "audioHost");
                                assertNull(control(audio, "Verset suivant"));
                                assertNull(control(audio, "Verset précédent"));
                                assertTrue(control(audio, "Lire").performClick());
                            });
                            awaitPlayback(activity);
                            activity.onActivity(a -> assertTrue(control((View) field(a, "audioHost"), "Répéter le verset").performClick()));
                            awaitPlayback(activity);
                        }
                    }
                    activity.onActivity(a -> {
                        ((SessionClock) field(a, "fragilityClock")).syncPersistedElapsed(budgetMs);
                        View previous = control((View) field(a, "actions"), "Page précédente");
                        if (previous != null) previous.performClick();
                    });
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    activity.onActivity(a -> {
                        assertNotNull(control((View) field(a, "actions"), "Continuer"));
                        assertNull(field(a, "audioPlayer"));
                        assertEquals(View.GONE, ((View) field(a, "audioHost")).getVisibility());
                    });
                }

                assertNull(store.pending());
                java.util.List<Integer> remaining = slots.manualCorrection().queuedPages();
                assertEquals(pages.size(), remaining.size());
                assertTrue(remaining.containsAll(pages));
                assertEquals(acquired, prefs.acquiredLineCountV6());
                assertEquals(next, prefs.activeMurajaahCursor());
                assertEquals(1, slots.receipts().size());
            }
        } finally {
            File[] audioFiles = audioFolder.listFiles();
            if (audioFiles != null) {
                for (File file : audioFiles) Files.delete(file.toPath());
                Files.delete(audioFolder.toPath());
            }
            HifzClock.resetClockForTests();
            HifzBackup.restore(context, original);
        }
    }

    private static void selectAudioVerse(HifzSessionActivity activity, VerseRef verse) {
        click(activity, "Audio ciblé");
        AlertDialog picker = (AlertDialog) field(activity, "fragilityVersePicker");
        android.widget.ListView verses = picker.getListView();
        for (int row = 0; row < verses.getCount(); row++) {
            if (("Verset " + verse).equals(verses.getAdapter().getItem(row))) {
                assertTrue(verses.performItemClick(verses.getChildAt(row), row, row));
                return;
            }
        }
        fail("Missing audio verse " + verse);
    }

    private static void awaitPlayback(ActivityScenario<HifzSessionActivity> activity) {
        java.util.concurrent.atomic.AtomicBoolean playing = new java.util.concurrent.atomic.AtomicBoolean();
        for (int attempt = 0; attempt < 40; attempt++) {
            activity.onActivity(a -> {
                MediaPlayer player = (MediaPlayer) field(field(a, "audioPlayer"), "player");
                playing.set(player != null && !((Boolean) field(field(a, "audioPlayer"), "preparing")) && player.isPlaying());
            });
            if (playing.get()) return;
            android.os.SystemClock.sleep(100);
        }
        fail("Synthetic Android playback did not start");
    }

    /** A generated PCM clip exercises MediaPlayer without copying the private recitation pack. */
    private static void seedSyntheticAudio(File folder) throws Exception {
        Files.createDirectories(folder.toPath());
        for (String file : new String[]{".verified-6236-v1", "source.json", "sha256.txt", "001001.mp3", "114006.mp3"})
            Files.write(folder.toPath().resolve(file), new byte[]{1});
        int sampleRate = 8000;
        int pcmBytes = sampleRate * 2 * 8;
        ByteBuffer wave = ByteBuffer.allocate(44 + pcmBytes).order(ByteOrder.LITTLE_ENDIAN);
        wave.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + pcmBytes);
        wave.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        wave.putShort((short) 1).putShort((short) 1).putInt(sampleRate).putInt(sampleRate * 2);
        wave.putShort((short) 2).putShort((short) 16);
        wave.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(pcmBytes);
        Files.write(folder.toPath().resolve("002006.mp3"), wave.array());
    }

    @Test public void continuationCannotCrossTheMorningEveningBoundary(){
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.SABQI));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.ITQAN));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.LEARNING_FINAL));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.CONSOLIDATION_FINAL));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.LEARNING_CONSOLIDATION));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.RECENT_SABQI_REVIEW));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.CONSOLIDATION_FINAL));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.SABQI));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,ActiveReviewSlots.Slot.MORNING.mode));
    }

    private static void assertPhase(ActivityScenario<HifzSessionActivity> activity,
            ActiveReviewCorrectionStore store,int page,int index,String stage){
        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Batch "+page+" "+stage);
        ActiveReviewCorrectionStore.State pending=store.pending();
        assertNotNull(pending);
        assertEquals(page,pending.page());assertEquals(index,pending.pageIndex);assertEquals(stage,pending.stage);
        activity.onActivity(a->{
            assertEquals(page,((Integer)field(a,"currentPage")).intValue());
            assertTrue(((TextView)field(a,"progress")).getText().toString().contains("Page "+page+" · "+index+"/3 traitées"));
            assertEquals("READ".equals(stage)?0:100,((Integer)field(a,"currentMask")).intValue());
        });
    }

    /** Pixel proof from the actual screenshot: the central 60% excludes header,
     * footer and most margins. A blank cream page previously passed DOM tests. */
    private static double inkFraction(Context context,String name){
        java.io.File file=new java.io.File(context.getExternalFilesDir(null),name);
        android.graphics.Bitmap image=android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
        assertNotNull("Missing actual frame "+name,image);
        long dark=0,count=0;
        try{
            int w=image.getWidth(),h=image.getHeight();
            int[] row=new int[w];
            for(int y=h/5;y<4*h/5;y++){
                image.getPixels(row,0,w,0,y,w,1);
                for(int x=w/5;x<4*w/5;x++){
                    int p=row[x];count++;
                    if(android.graphics.Color.red(p)<100&&android.graphics.Color.green(p)<100
                        &&android.graphics.Color.blue(p)<100)dark++;
                }
            }
            return (double)dark/count;
        }finally{image.recycle();}
    }

    private static Object field(Object target,String name){
        try {
            java.lang.reflect.Field f=target.getClass().getDeclaredField(name);
            f.setAccessible(true);return f.get(target);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static View control(View v,String label){
        if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View hit=control(g.getChildAt(i),label);if(hit!=null)return hit;
            }
        }
        return null;
    }
    private static void click(HifzSessionActivity a,String label){
        View button=control((View)field(a,"actions"),label);
        assertNotNull(label,button);
        assertTrue(label,button.isEnabled());
        assertTrue(label,button.performClick());
    }
}
