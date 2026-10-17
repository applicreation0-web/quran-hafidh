package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real QCF WebView, synthetic labels only: independent titles cannot open new recall holes. */
public final class AyahThemesReviewDisplayInstrumentedTest {
 @Test public void independentTitlesLeaveFullMaskAndHistoricalHolesExactlyUnchanged() throws Exception {
  Context c=ApplicationProvider.getApplicationContext();
  Intent intent=new Intent(c,StudyReaderActivity.class).putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE,518);
  try(ActivityScenario<StudyReaderActivity> scenario=ActivityScenario.launch(intent)) {
   HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Independent synthetic title targets");
   scenario.onActivity(a->{
    MushafView view=mushaf(a);GeometryRepository g=GeometryRepository.get(a);
    SemanticPassageRepository old=SemanticPassageRepository.shared(a);
    view.setMaskFollowsSelection(false);
    view.setSelection(Collections.emptyList(),g.lineIdsOnPage(518));
    view.setPageLandmarkBoxes(old.pageLandmarkBoxes(518));
    view.setSemanticCues(old.readerCuesForPage(518),true,false);
    view.setMask(100);
   });
   String before=js(scenario,"JSON.stringify({mask:mask,holes:protectedWordBoxes(),layers:Array.from(document.querySelectorAll('.masklayer')).map(n=>n.outerHTML)})");
   JSONObject state=new JSONObject(before);
   assertEquals(100,state.getInt("mask"));
   assertTrue("Real historical recall holes must exist",state.getJSONArray("holes").length()>0);
   assertTrue("Real full-page mask must exist",state.getJSONArray("layers").length()>0);
   scenario.onActivity(a->{
    try {
     MawdudiPassageIndex canonical=MawdudiPassageIndex.load(a);JSONArray rows=new JSONArray();
     for(MawdudiPassageIndex.Passage p:canonical.all()) {
      JSONObject r=new JSONObject().put("id",p.id).put("surah",p.surah)
       .put("start",p.firstAyah).put("end",p.lastAyah).put("source_immutability","LOCKED")
       .put("display_label","SYNTHETIC "+p.id)
       .put("source",p.editorialAdaptation?JSONObject.NULL:"Ayah Themes")
       .put("source_title_exact",p.editorialAdaptation?JSONObject.NULL:"SYNTHETIC "+p.id)
       .put("display_label_origin",p.editorialAdaptation?AyahThemesCueIndex.EDITORIAL:AyahThemesCueIndex.ORIGINAL)
       .put("display_label_approved_by_user",p.editorialAdaptation);
      rows.put(r);
     }
     AyahThemesCueIndex index=AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA)
       .put("passages",rows).toString(),canonical,WordGeometryRepository.shared(a));
     AyahThemesReviewDisplay.apply(a,mushaf(a),index,518);
    } catch(Exception e){throw new AssertionError(e);}
   });
   String after=js(scenario,"JSON.stringify({mask:mask,holes:protectedWordBoxes(),layers:Array.from(document.querySelectorAll('.masklayer')).map(n=>n.outerHTML)})");
   assertEquals("Display cannot change a single mask/hole byte",before,after);
   assertTrue("New independent exact-word targets must be visible",
     Integer.parseInt(js(scenario,"String(document.querySelectorAll('.ayah-themes-title-targets rect').length)"))>0);
  }
 }
 @Test public void graphical114MapUsesRealQcfAndReturnsToExactUnit() throws Exception {
  Context context=ApplicationProvider.getApplicationContext();
  Intent intent=new Intent(context,IbnKathirMapActivity.class)
   .putExtra(IbnKathirMapActivity.EXTRA_SURAH,3).putExtra(IbnKathirMapActivity.EXTRA_AYAH,175);
  AtomicReference<MawdudiMapScreen> screen=new AtomicReference<>();
  AtomicReference<MawdudiPassageIndex.Passage> selected=new AtomicReference<>();
  try(ActivityScenario<IbnKathirMapActivity> scenario=ActivityScenario.launch(intent)) {
   HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"pagePreview","Historical audit host");
   scenario.onActivity(a->{
    try {
     MawdudiPassageIndex canonical=MawdudiPassageIndex.load(a);JSONArray rows=new JSONArray(),macros=new JSONArray();
     for(MawdudiPassageIndex.Passage p:canonical.all())rows.put(new JSONObject().put("id",p.id).put("surah",p.surah)
      .put("start",p.firstAyah).put("end",p.lastAyah).put("source_immutability","LOCKED")
      .put("display_label","SYNTHETIC original navigation heading "+p.id)
      .put("source",p.editorialAdaptation?JSONObject.NULL:"Ayah Themes")
      .put("source_title_exact",p.editorialAdaptation?JSONObject.NULL:"SYNTHETIC original navigation heading "+p.id)
      .put("display_label_origin",p.editorialAdaptation?AyahThemesCueIndex.EDITORIAL:AyahThemesCueIndex.ORIGINAL)
      .put("display_label_approved_by_user",p.editorialAdaptation));
     for(int n=1;n<=114;n++) {
      java.util.List<MawdudiPassageIndex.Passage> ps=canonical.forSurah(n);int last=ps.get(ps.size()-1).lastAyah;
      int[][] bounds=n==2?new int[][]{{1,20},{21,29},{30,39},{40,120},{121,141},{142,152},{153,251},{252,260},{261,283},{284,286}}:n==3?new int[][]{{1,32},{33,63},{64,120},{121,200}}:n==4?new int[][]{{1,35},{36,42},{43,43},{44,57},{58,72},{73,100},{101,103},{104,104},{105,135},{136,175},{176,176}}:n==5?new int[][]{{1,10},{11,26},{27,32},{33,40},{41,50},{51,69},{70,86},{87,108},{109,119},{120,120}}:n==6?new int[][]{{1,12},{13,24},{25,32},{33,73},{74,90},{91,108},{109,154},{155,160},{161,165}}:n==7?new int[][]{{1,10},{11,25},{26,53},{54,58},{59,171},{172,174},{175,179},{180,198},{199,206}}:n==8?new int[][]{{1,41},{42,54},{55,59},{60,66},{67,71},{72,75}}:n==9?new int[][]{{1,12},{13,37},{38,72},{73,90},{91,110},{111,118},{119,127},{128,129}}:new int[][]{{1,last}};
      for(int[] b:bounds)macros.put(new JSONObject().put("surah",n).put("start",b[0]).put("end",b[1])
       .put("label","SYNTHETIC macro heading").put("status",bounds.length==1?"WHOLE_SURAH_ONLY_NO_NEW_SPLIT_INVENTED":n==5&&b[0]==120?"SOURCE_CONCLUSION_FROM_INTRODUCTION":(n==2||n>=4)?"SOURCE_BOUNDARY_CONFIRMED_IN_INTRODUCTION":"SOURCE_BOUNDARY_CONFIRMED_FROM_PDF"));
     }
     AyahThemesCueIndex titles=AyahThemesCueIndex.parse(new JSONObject().put("schema",AyahThemesCueIndex.SCHEMA).put("passages",rows).toString(),canonical,WordGeometryRepository.shared(a));
     MawdudiMapData macro=MawdudiMapData.parse(new JSONObject().put("schema","MAWDUDI_SOURCE_SAFE_MACROS_V1").put("macros",macros).toString(),canonical);
     ((MushafView)field(a,"pagePreview")).destroySafely();
     MawdudiMapScreen map=new MawdudiMapScreen(a,GeometryRepository.get(a),canonical,titles,macro,3,175,null);
     java.lang.reflect.Field host=IbnKathirMapActivity.class.getDeclaredField("map114");
     host.setAccessible(true);host.set(a,map);
     map.display();
     // Unit U4 of Al-Imran, with no passage graph, commentary or synthetic QCF.
     java.lang.reflect.Method open=MawdudiMapScreen.class.getDeclaredMethod("openMacro",int.class);
     open.setAccessible(true);open.invoke(map,3);
     screen.set(map);
    }catch(Exception ex){throw new AssertionError(ex);}
   });
   int first=GeometryRepository.get(context).pageForVerse(new com.quransafeguard.hifz.core.VerseRef(3,121));
   int last=MawdudiMapModel.lastPageForVerse(new com.quransafeguard.hifz.core.VerseRef(3,200),
       GeometryRepository.get(context));
   assertTrue("This fixture contains several physical pages",last>first+2);
   awaitAtlasFullPage(scenario,screen,first);
   scenario.onActivity(a->{
    assertEquals("Unit 4 must be selected",3,((Integer)field(screen.get(),"activeMacro")).intValue());
    assertEquals("Atlas displays the selected unit graph, not the root overview","Graph",
      ((android.widget.HorizontalScrollView)field(screen.get(),"axis")).getChildAt(0).getClass().getSimpleName());
    assertTrue("Unit goes straight to physical QCF, without an intermediate mosaic",
      (Boolean)field(screen.get(),"passageOpen"));
    android.widget.TextView unitTitle=(android.widget.TextView)field(screen.get(),"passageTitle");
    android.widget.TextView sourceTitle=(android.widget.TextView)field(screen.get(),"selectedPassageTitle");
    assertTrue("Unit heading must be larger",unitTitle.getTextSize()>sourceTitle.getTextSize());
    assertTrue("Unit heading must be bold",
      (unitTitle.getTypeface().getStyle()&android.graphics.Typeface.BOLD)!=0);
    assertTrue("Selected source heading must be italic",
      (sourceTitle.getTypeface().getStyle()&android.graphics.Typeface.ITALIC)!=0);
    assertTrue("No empty source title",sourceTitle.length()>0);
    assertEquals("Direct QCF starts at the first unit page",first,((Integer)field(screen.get(),"page")).intValue());
   });
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-atlas-direct-qcf-unit-and-passages.png");
   scenario.onActivity(a->{
    try {
     java.lang.reflect.Method openPage=MawdudiMapScreen.class.getDeclaredMethod("onThumbnailPage",int.class);
     openPage.setAccessible(true);openPage.invoke(screen.get(),first+1);
    }catch(Exception e){throw new AssertionError(e);}
   });
   awaitAtlasFullPage(scenario,screen,first+1);
   swipeAtlasPage(scenario,screen,true);
   awaitAtlasFullPage(scenario,screen,first+2);
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-atlas-unit-swiped-original-qcf-page.png");
   scenario.onActivity(a->screen.get().openReader());
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   AtomicReference<StudyReaderActivity> reader=new AtomicReference<>();
   for(int attempt=0;attempt<44;attempt++){
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
     for(android.app.Activity a:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
          .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
      if(a instanceof StudyReaderActivity&&Boolean.TRUE.equals(field(mushaf((StudyReaderActivity)a),"pageShown")))
       reader.set((StudyReaderActivity)a);
    });
    if(reader.get()!=null)break;
    android.os.SystemClock.sleep(250);
   }
   assertNotNull("Reader must open the authentic QCF page",reader.get());
   // Jalalayn stays in the SAME Activity, with the authentic QCF Mushaf visible.
   // Test both phone bottom host and BOOX side host without mutating Tafsir assets.
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    try{
     StudyReaderActivity active=reader.get();
     assertNotNull("Atlas passage focus must select a Tafsir verse",field(active,"selected"));
     java.lang.reflect.Method open=StudyReaderActivity.class.getDeclaredMethod("openTafsir");
     open.setAccessible(true);open.invoke(active);
     boolean wide=(Boolean)field(active,"largeScreen");
     android.view.ViewGroup panel=(android.view.ViewGroup)field(active,
         wide?"sideTafsir":"bottomTafsir");
     assertNotNull("Inline Jalalayn host exists",panel);
     assertEquals("Inline Jalalayn must be visible",android.view.View.VISIBLE,panel.getVisibility());
     assertTrue("Jalalayn has a live embedded text panel",panel.getChildCount()>0);
     assertEquals("Mushaf must stay visible",android.view.View.VISIBLE,
         ((MushafView)field(active,"mushaf")).getVisibility());
     Object tafsir=field(active,"mapPreviewJalalayn");
     assertNotNull("Jalalayn controller must be live",tafsir);
     assertNull("Lecture must not open the old fullscreen Tafsir dialog",field(tafsir,"dialog"));
     assertTrue("Inline Tafsir is active",(Boolean)tafsir.getClass()
         .getDeclaredMethod("isShowing").invoke(tafsir));
    }catch(Exception error){throw new AssertionError(error);}
   });
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-atlas-inline-jalalayn-qcf-visible.png");
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    StudyReaderActivity active=reader.get();
    int before=(Integer)field(active,"page");
    Object selectedBefore=field(active,"selected");
    boolean wide=(Boolean)field(active,"largeScreen");
    android.view.ViewGroup panel=(android.view.ViewGroup)field(active,
        wide?"sideTafsir":"bottomTafsir");
    active.onBackPressed(); // Close Tafsir only; do not finish Lecture.
    assertEquals("Panel must actually close, not hide a full-screen dialog",
        android.view.View.GONE,panel.getVisibility());
    assertEquals("Closing Jalalayn preserves exact QCF page",before,
        ((Integer)field(active,"page")).intValue());
    assertEquals("Closing Jalalayn preserves the active verse",selectedBefore,
        field(active,"selected"));
    assertEquals("Closing Jalalayn leaves QCF visible",android.view.View.VISIBLE,
        ((MushafView)field(active,"mushaf")).getVisibility());
   });
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
    assertEquals(first+2,((Integer)field(reader.get(),"page")).intValue());
    assertTrue("Atlas reading must be unit-scoped",(Boolean)field(reader.get(),"mapUnitPreview"));
    assertNotNull("Atlas has an explicit previous-passage arrow",field(reader.get(),"atlasPassageUp"));
    assertNotNull("Atlas has an explicit next-passage arrow",field(reader.get(),"atlasPassageDown"));
    assertTrue("Atlas focus resolves a real passage",((Integer)field(reader.get(),"atlasPassagePosition"))>=0);
    assertNotNull("Page-dependent Ayah Themes footer",(android.widget.TextView)field(reader.get(),"atlasCueFooter"));
    assertEquals(121,reader.get().getIntent().getIntExtra("map114FocusStart",-1));
    assertEquals(200,reader.get().getIntent().getIntExtra("map114FocusEnd",-1));
    try{
     java.lang.reflect.Method setPage=StudyReaderActivity.class.getDeclaredMethod("setPage",int.class);
     setPage.setAccessible(true);
     setPage.invoke(reader.get(),last);
     setPage.invoke(reader.get(),last+1);
    }catch(Exception e){throw new AssertionError(e);}
    assertEquals("Reader must stop at the precise unit's last physical page",last,
      ((Integer)field(reader.get(),"page")).intValue());
   });
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(
       android.view.KeyEvent.KEYCODE_BACK);
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{
    assertEquals("Back returns to the very same QCF page",last,
      ((Integer)field(screen.get(),"page")).intValue());
    assertEquals(3,((Integer)field(screen.get(),"activeMacro")).intValue());
   });
   awaitAtlasFullPage(scenario,screen,last);
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-atlas-unit-reading-return-exact-page.png");
   // Unit rail must receive actual Android dispatched swipes after layout.
   scenario.onActivity(a->{
    try{
     MawdudiMapScreen map=screen.get();
     java.lang.reflect.Method choose=MawdudiMapScreen.class.getDeclaredMethod("choose",int.class,int.class);
     java.lang.reflect.Method setLevel=MawdudiMapScreen.class.getDeclaredMethod("setLevel",boolean.class);
     choose.setAccessible(true);setLevel.setAccessible(true);
     choose.invoke(map,4,1);
     setLevel.invoke(map,true);
     assertEquals("An-Nisa has all eleven unchanged sourced units",11,
       ((MawdudiMapData)field(map,"macros")).forSurah(4).size());
    }catch(Exception ex){throw new AssertionError(ex);}
   });
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
   scenario.onActivity(a->{
    try{
     MawdudiMapScreen map=screen.get();
     assertEquals("Unit window starts at U1",0,((Integer)field(map,"unitWindowStart")).intValue());
     android.view.View rail=(android.view.View)field(map,"axis");
     int width=rail.getWidth();
     assertTrue("Unit rail is laid out before gesture",width>0);
     float y=Ui.dp(a,31);
     for(int n=1;n<=6;n++){
      android.view.MotionEvent down=android.view.MotionEvent.obtain(0,0,
          android.view.MotionEvent.ACTION_DOWN,width*.75f,y,0);
      android.view.MotionEvent up=android.view.MotionEvent.obtain(0,1,
          android.view.MotionEvent.ACTION_UP,width*.25f,y,0);
      rail.dispatchTouchEvent(down);rail.dispatchTouchEvent(up);
      down.recycle();up.recycle();
      assertEquals("Each swipe advances exactly one unit position",n,
          ((Integer)field(map,"unitWindowStart")).intValue());
     }
     android.view.MotionEvent down=android.view.MotionEvent.obtain(0,0,
         android.view.MotionEvent.ACTION_DOWN,width*.9f,y,0);
     android.view.MotionEvent up=android.view.MotionEvent.obtain(0,1,
         android.view.MotionEvent.ACTION_UP,width*.9f,y,0);
     rail.dispatchTouchEvent(down);rail.dispatchTouchEvent(up);
     down.recycle();up.recycle();
     assertEquals("Rightmost node now selects An-Nisa U11",10,
         ((Integer)field(map,"activeMacro")).intValue());
     assertEquals("An-Nisa U11 starts on the genuine 4:176 QCF page",
         GeometryRepository.get(a).pageForVerse(new com.quransafeguard.hifz.core.VerseRef(4,176)),
         ((Integer)field(map,"page")).intValue());
     assertTrue("All 23 passages of An-Nisa U1 remain in canonical corpus",
         MawdudiPassageIndex.load(a).forSurah(4).size()>=23);
    }catch(Exception ex){throw new AssertionError(ex);}
   });
   awaitAtlasFullPage(scenario,screen,
       GeometryRepository.get(context).pageForVerse(
           new com.quransafeguard.hifz.core.VerseRef(4,176)));
   HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("audit-atlas-an-nisa-u11-sliding-units.png");
   scenario.onActivity(a->screen.get().destroy());
  }
 }
 /** Unit-only page view: original SVG fully contained, never a fake page or passage text. */
 private static void awaitAtlasFullPage(ActivityScenario<IbnKathirMapActivity> scenario,
     AtomicReference<MawdudiMapScreen> screen,int page)throws Exception {
  String script="(function(){var tile=document.querySelector('.tile.selected');"+
    "var img=tile&&tile.querySelector('img');"+
    "var badge=document.querySelector('[aria-current=page]');"+
    "if(!img||!img.complete||img.naturalWidth<1||!badge||Number(badge.textContent)!="+page+")return false;"+
    "if(!img.getAttribute('src').startsWith('data:image/svg+xml;base64,'))return false;"+
    "var r=img.getBoundingClientRect(),host=document.querySelector('.paper').getBoundingClientRect();"+
    "if(r.width<60||r.height<95||r.left<host.left-1||r.right>host.right+1||"+
    "r.top<host.top-1||r.bottom>host.bottom+1)return false;"+
    "return document.documentElement.scrollHeight<=innerHeight+1;})()";
  for(int attempt=0;attempt<64;attempt++){
   CountDownLatch ready=new CountDownLatch(1);
   AtomicReference<String> result=new AtomicReference<>();
   scenario.onActivity(a->((android.webkit.WebView)field(screen.get(),"thumbnails"))
     .evaluateJavascript(script,v->{result.set(v);ready.countDown();}));
   assertTrue(ready.await(15,TimeUnit.SECONDS));
   if("true".equals(result.get())){
    // SVG resource decoded and fitted does not guarantee WebView has painted.
    // Give BOOX/phone compositor a real display frame before pixel evidence.
    android.os.SystemClock.sleep(350);
    return;
   }
   android.os.SystemClock.sleep(250);
  }
  fail("The full original QCF page is not fitted within the Atlas viewport");
 }
 @Test public void titleTargetPreservesPassiveEndpointAndArmedActiveWeakMark()throws Exception {
  Context c=ApplicationProvider.getApplicationContext();JSONObject backup=HifzBackup.export(c);
  try {
   c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE).edit().clear().commit();
   HifzPrefs prefs=new HifzPrefs(c);GeometryRepository g=GeometryRepository.get(c);
   com.quransafeguard.hifz.core.VerseRef verse=new com.quransafeguard.hifz.core.VerseRef(1,1);
   assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new com.quransafeguard.hifz.core.VerseRange(verse,new com.quransafeguard.hifz.core.VerseRef(1,7))),g));
   prefs.setMurajaahCursor(verse); // Bootstrap default 2:1 lies outside this Fatiha-only fixture.
   for(String mode:new String[]{HifzSessionActivity.MURAJAAH,HifzSessionActivity.MURAJAAH_ACTIVE}) {
    try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode))) {
     HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Review title gesture "+mode);
     CountDownLatch done=new CountDownLatch(1);
     scenario.onActivity(a->{
      try {
       assertNotNull("Review fixture must have a plan: "+mode+" / "+((android.widget.TextView)field(a,"program")).getText(),field(a,"murajaahPlan"));
       java.lang.reflect.Field armed=a.getClass().getDeclaredField("weakMarkMode");armed.setAccessible(true);armed.setBoolean(a,HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode));
       String script;
       try(java.io.InputStream in=a.getAssets().open("ayah_themes_display/review_titles.js")) {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);script=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
       }
       // Real production installer/native bridge over exact QCF first-word geometry.
       WordGeometryRepository.WordBox word=null;
       for(WordGeometryRepository.WordBox w:WordGeometryRepository.shared(a).wordsForPage(1))if(w.surah==1&&w.ayah==1&&w.word==1){word=w;break;}
       assertNotNull(word);
       JSONArray cues=new JSONArray().put(new JSONObject().put("id","s001a001b007").put("surah",1).put("start",1).put("boxes",new JSONArray().put(word.boxJson())));
       ((MushafView)field(a,"mushaf")).evaluateJavascript(script+";window.AyahThemesReviewDisplay.install("+cues+");document.querySelector('.ayah-themes-title-targets rect').dispatchEvent(new MouseEvent('click',{bubbles:true}));",v->done.countDown());
      }catch(Exception e){throw new AssertionError(e);}
     });
     assertTrue(done.await(15,TimeUnit.SECONDS));
     androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
     scenario.onActivity(a->{
      if(HifzSessionActivity.MURAJAAH.equals(mode))assertEquals(verse,field(a,"murajaahActualEnd"));
      else {assertEquals(false,field(a,"weakMarkMode"));assertTrue(new HifzPrefs(a).murajaahWeakVerses().contains(verse));}
     });
    }
   }
  }finally{HifzBackup.restore(c,backup);}
 }
 /** Removing a real SVG, its exact verse highlight, or its visible viewport
  * must fail this check. The Atlas no longer renders its hidden MushafView. */
 private static void awaitAtlasThumbnails(ActivityScenario<IbnKathirMapActivity> scenario,
     AtomicReference<MawdudiMapScreen> screen,MawdudiPassageIndex.Passage passage,int page)throws Exception {
  String script="(function(){var tile=document.querySelector('.tile.selected');"+
   "var img=tile&&tile.querySelector('img');if(!img||!img.complete||!img.naturalWidth)return false;"+
   "var badge=document.querySelector('[aria-current=page]');if(!badge||Number(badge.textContent)!="+page+")return false;"+
   "if(document.body.textContent.includes('QCF')||document.body.textContent.includes(' / '))return false;"+
   "if(badge.getBoundingClientRect().width<52||getComputedStyle(badge).borderRadius==='0px')return false;"+
   "var r=img.getBoundingClientRect();if(r.width<=0||r.height<=0||r.top<0||r.bottom>innerHeight+1)return false;"+
   "var paper=document.querySelector('.paper').getBoundingClientRect();"+
   "if(r.left<paper.left-1||r.right>paper.right+1||r.top<paper.top-1||r.bottom>paper.bottom+1)return false;"+
   "if(Math.abs(r.width/r.height-img.naturalWidth/img.naturalHeight)>.02)return false;"+
   "if(document.documentElement.scrollHeight>innerHeight+1)return false;"+
   "var src=img.getAttribute('src');if(!src.startsWith('data:image/svg+xml;base64,'))return false;"+
   "var svg=new DOMParser().parseFromString(atob(src.split(',')[1]),'image/svg+xml');"+
   "if(svg.querySelector('parsererror'))return false;"+
   "var style=Array.from(svg.querySelectorAll('style')).map(s=>s.textContent).join('');"+
   "return Array.from(svg.querySelectorAll('path.ayahPolygon')).some(p=>"+
   "Number(p.getAttribute('surah'))==="+passage.surah+"&&Number(p.getAttribute('ayah'))>="+passage.firstAyah+
   "&&Number(p.getAttribute('ayah'))<="+passage.lastAyah+
   "&&style.includes(\"path.ayahPolygon[surah='\"+p.getAttribute('surah')+\"'][ayah='\"+p.getAttribute('ayah')+\"']\")"+
   "&&style.includes('fill-opacity:0.23!important'));})()";
  for(int attempt=0;attempt<44;attempt++) {
   CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
   scenario.onActivity(a->{
    android.webkit.WebView view=(android.webkit.WebView)field(screen.get(),"thumbnails");
    assertTrue("Real QCF thumbnails must be visible",view.isShown());
    android.view.ViewGroup icons=(android.view.ViewGroup)field(screen.get(),"icons");
    assertEquals("Keep only commentary and reading actions",2,icons.getChildCount());
    float density=a.getResources().getDisplayMetrics().density;
    for(int i=0;i<icons.getChildCount();i++) {
     android.view.View target=icons.getChildAt(i);
     assertTrue("Atlas touch targets must be at least 48dp",target.getWidth()+1>=48*density&&target.getHeight()+1>=48*density);
    }
    view.evaluateJavascript(script,v->{result.set(v);done.countDown();});
   });
   assertTrue("Atlas JavaScript timed out",done.await(15,TimeUnit.SECONDS));
   if("true".equals(result.get())) {
    CountDownLatch painted=new CountDownLatch(1);
    scenario.onActivity(a->{
     android.webkit.WebView view=(android.webkit.WebView)field(screen.get(),"thumbnails");
     view.postVisualStateCallback(1,new android.webkit.WebView.VisualStateCallback(){
      @Override public void onComplete(long request){view.postOnAnimation(()->view.postOnAnimation(painted::countDown));}
     });
    });
    assertTrue("Atlas compositor must paint before screenshots",painted.await(15,TimeUnit.SECONDS));
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();return;
   }
   android.os.SystemClock.sleep(250);
  }
  fail("Real highlighted QCF thumbnail not rendered in the Atlas viewport on page "+page);
 }
 private static void swipeAtlasPage(ActivityScenario<IbnKathirMapActivity> scenario,
     AtomicReference<MawdudiMapScreen> screen,boolean next)throws Exception {
  AtomicReference<int[]> bounds=new AtomicReference<>();
  scenario.onActivity(a->{android.view.View v=(android.view.View)field(screen.get(),"thumbnails");int[] xy=new int[2];v.getLocationOnScreen(xy);bounds.set(new int[]{xy[0],xy[1],v.getWidth(),v.getHeight()});});
  int[] b=bounds.get();float from=b[0]+b[2]*(next?.25f:.75f),to=b[0]+b[2]*(next?.75f:.25f),y=b[1]+b[3]*.4f;
  android.app.Instrumentation inst=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
  long down=android.os.SystemClock.uptimeMillis();
  for(int i=0;i<=10;i++) {
   int action=i==0?android.view.MotionEvent.ACTION_DOWN:i==10?android.view.MotionEvent.ACTION_UP:android.view.MotionEvent.ACTION_MOVE;
   android.view.MotionEvent event=android.view.MotionEvent.obtain(down,android.os.SystemClock.uptimeMillis(),action,from+(to-from)*i/10,y,0);
   inst.sendPointerSync(event);event.recycle();android.os.SystemClock.sleep(15);
  }
  inst.waitForIdleSync();
 }
 private static Object field(Object o,String name) {
  try{java.lang.reflect.Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}
 }
 private static MushafView mushaf(StudyReaderActivity a) {
  try{java.lang.reflect.Field f=StudyReaderActivity.class.getDeclaredField("mushaf");f.setAccessible(true);return (MushafView)f.get(a);}
  catch(Exception e){throw new AssertionError(e);}
 }
 private static String js(ActivityScenario<StudyReaderActivity> s,String expression)throws Exception {
  CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
  s.onActivity(a->mushaf(a).evaluateJavascript(expression,v->{result.set(v);done.countDown();}));
  assertTrue("WebView JavaScript result timed out",done.await(15,TimeUnit.SECONDS));
  return new JSONArray("["+result.get()+"]").getString(0);
 }
}
