package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.*;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Private source-original PDF only. No text generation and no external network. */
public final class AtlasSourcePdfActivity extends Activity {
 public static final String EXTRA_SURAH="atlasV2Surah",EXTRA_AYAH="atlasV2Ayah";
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final AtomicInteger generation=new AtomicInteger();
 private PdfRenderer renderer;
 private ParcelFileDescriptor descriptor;
 private ImageView image;
 private TextView heading,foot;
 private Bitmap shown;
 private int first,last,page;
 private float x0,y0;
 private volatile boolean destroyed;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.PAPER);
  LinearLayout header=Ui.row(this);header.setGravity(Gravity.CENTER_VERTICAL);
  header.addView(Ui.iconButton(this,"","Retour à Atlas",v->finish()),
     new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
  heading=Ui.bookText(this,"Commentaire original · ouverture…",14f,true);
  heading.setGravity(Gravity.CENTER);
  header.addView(heading,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
  root.addView(header);
  image=new ImageView(this);image.setBackgroundColor(Ui.PAPER);
  image.setScaleType(ImageView.ScaleType.FIT_CENTER);
  image.setContentDescription("Page originale du commentaire de Maududi");
  root.addView(image,new LinearLayout.LayoutParams(-1,0,1));
  foot=Ui.text(this,"PDF original Maududi · hors ligne · balayer pour feuilleter",12f,false);
  foot.setGravity(Gravity.CENTER);
  root.addView(foot,new LinearLayout.LayoutParams(-1,Ui.dp(this,46)));
  image.setOnTouchListener((v,e)->{
   switch(e.getActionMasked()){
    case MotionEvent.ACTION_DOWN:x0=e.getX();y0=e.getY();return true;
    case MotionEvent.ACTION_UP:
     float dx=e.getX()-x0,dy=e.getY()-y0;
     if(Math.abs(dx)>Ui.dp(this,40)&&Math.abs(dx)>Math.abs(dy)*1.3f){turn(dx<0?1:-1);return true;}
     v.performClick();return true;
    default:return true;
   }
  });
  setContentView(root);
  final int s=getIntent().getIntExtra(EXTRA_SURAH,-1);
  final int a=getIntent().getIntExtra(EXTRA_AYAH,-1);
  if(s<1||s>114||a<1){fail("Référence Atlas invalide");return;}
  io.execute(()->{
   try{
    MawdudiPassageIndex pi=MawdudiPassageIndex.load(this);
    AtlasPrivateV2.Unit unit=AtlasPrivateV2.load(this,pi).at(s,a);
    first=unit.pdfStart;last=unit.pdfEnd;
    page=state==null?first:Math.max(first,Math.min(last,state.getInt("sourcePdfPage",first)));
    File source=verifiedPdf();
    descriptor=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);
    renderer=new PdfRenderer(descriptor);
    if(renderer.getPageCount()!=3117)throw new IllegalStateException("PDF edition mismatch");
    runOnUiThread(()->{
     if(destroyed)return;
     heading.setText("Maududi · unité "+unit.ordinal+" · "+s+":"+unit.start+"–"+unit.end);
     render(page);
    });
   }catch(Exception e){runOnUiThread(()->fail("Commentaire original indisponible"));}
  });
 }
 private File verifiedPdf()throws Exception{
  File dir=new File(getFilesDir(),"atlas_v2_original");
  if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("Private dir missing");
  File target=new File(dir,"maududi_original_verified.pdf");
  if(target.isFile()&&AtlasPrivateV2.PDF_SHA256.equals(shaFile(target)))return target;
  File tmp=new File(dir,"maududi_import.tmp");
  MessageDigest d=MessageDigest.getInstance("SHA-256");
  try(InputStream in=getAssets().open(AtlasPrivateV2.PDF_ASSET);FileOutputStream out=new FileOutputStream(tmp)){
   byte[] b=new byte[32768];int n;long len=0;
   while((n=in.read(b))!=-1){
    len+=n;if(len>30000000L)throw new IllegalStateException("PDF too large");
    out.write(b,0,n);d.update(b,0,n);
   }
   out.getFD().sync();
  }catch(Exception e){tmp.delete();throw e;}
  if(!AtlasPrivateV2.PDF_SHA256.equals(hex(d.digest()))){
   tmp.delete();throw new IllegalStateException("Wrong Maududi PDF");
  }
  if(target.exists()&&!target.delete())throw new IllegalStateException("PDF replacement failed");
  if(!tmp.renameTo(target))throw new IllegalStateException("PDF private activation failed");
  return target;
 }
 private static String hex(byte[] b){
  StringBuilder result=new StringBuilder();
  for(byte a:b)result.append(String.format(java.util.Locale.ROOT,"%02x",a&255));
  return result.toString();
 }
 private static String shaFile(File file)throws Exception{
  MessageDigest d=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(file)){
   byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)d.update(b,0,n);
  }
  return hex(d.digest());
 }
 private void render(int desired){
  if(destroyed)return;
  int selected=Math.max(first,Math.min(last,desired));page=selected;
  int token=generation.incrementAndGet();
  foot.setText("Maududi · PDF "+selected+" / 3117 · unité pages "+first+"–"+last);
  int w=Math.max(350,Math.min(1600,getResources().getDisplayMetrics().widthPixels-18));
  int h=Math.max(450,Math.min(2050,getResources().getDisplayMetrics().heightPixels-Ui.dp(this,105)));
  io.execute(()->{
   if(renderer==null||destroyed||token!=generation.get())return;
   Bitmap bitmap=null;
   try(PdfRenderer.Page current=renderer.openPage(selected-1)){
    float scale=Math.min((float)w/current.getWidth(),(float)h/current.getHeight());
    int width=Math.max(1,Math.round(current.getWidth()*scale));
    int height=Math.max(1,Math.round(current.getHeight()*scale));
    bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
    bitmap.eraseColor(android.graphics.Color.WHITE);
    current.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
    Bitmap ready=bitmap;
    runOnUiThread(()->{
     if(destroyed||token!=generation.get()){ready.recycle();return;}
     Bitmap previous=shown;shown=ready;image.setImageBitmap(ready);
     if(previous!=null&&!previous.isRecycled())previous.recycle();
    });
   }catch(Exception e){
    if(bitmap!=null&&!bitmap.isRecycled())bitmap.recycle();
    runOnUiThread(()->fail("Impossible d'afficher cette page"));
   }
  });
 }
 private void turn(int direction){
  if(renderer==null)return;
  int desired=Math.max(first,Math.min(last,page+direction));
  if(desired!=page)render(desired);
 }
 private void fail(String message){
  if(destroyed)return;
  foot.setText(message);Toast.makeText(this,message,Toast.LENGTH_LONG).show();
 }
 @Override protected void onSaveInstanceState(Bundle state){
  state.putInt("sourcePdfPage",page);super.onSaveInstanceState(state);
 }
 @Override protected void onDestroy(){
  destroyed=true;generation.incrementAndGet();image.setImageDrawable(null);
  if(shown!=null&&!shown.isRecycled())shown.recycle();shown=null;
  io.execute(()->{
   try{if(renderer!=null)renderer.close();}catch(Exception ignored){}
   try{if(descriptor!=null)descriptor.close();}catch(Exception ignored){}
  });
  io.shutdown();super.onDestroy();
 }
}