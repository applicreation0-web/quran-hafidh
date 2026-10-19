package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;

/** Dedicated read-only activity. Parent session naturally pauses via its unchanged lifecycle. */
public final class CorrespondenceActivity extends Activity {
 private CorrespondenceScreen screen;
 private String relationId;
 private Bundle source;
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  FrameLayout background=new FrameLayout(this);background.setBackgroundColor(Ui.PAPER);setContentView(background);
  CorrespondenceRuntime runtime=CorrespondenceRuntime.load(this);
  if(runtime.geometry==null){finish();return;}
  try {
   relationId=state==null?getIntent().getStringExtra("relationId"):state.getString("relationId");
   source=state==null?getIntent().getBundleExtra("origin"):state.getBundle("origin");
   CorrespondenceAccess.Origin origin=CorrespondenceEntryPoints.decode(source,runtime.geometry);
   CorrespondenceModel.Relation relation=runtime.find(relationId);
   CorrespondenceAccess.Result permitted=new CorrespondenceAccess(runtime.catalog.index,runtime.geometry).forWorkedRange(origin);
   boolean direct=false;
   for(CorrespondenceModel.Relation r:permitted.visible)if(r==relation)direct=true;
   for(CorrespondenceModel.Relation r:permitted.others())if(r==relation)direct=true;
   if(!direct){finish();return;}
   if(state==null)screen=CorrespondenceScreen.open(this,relation,origin.snapshot,runtime.geometry,returned->finish());
   else screen=CorrespondenceScreen.restore(this,relation,origin.snapshot,runtime.geometry,state.getBundle("comparison"),returned->finish());
   if(screen==null)finish();
  }catch(RuntimeException invalidOrRevoked){finish();}
 }
 @Override protected void onSaveInstanceState(Bundle state){
  if(screen!=null&&screen.isShowing()){
   state.putString("relationId",relationId);state.putBundle("origin",source);state.putBundle("comparison",screen.saveCheckpoint());
  }
  super.onSaveInstanceState(state);
 }
 @Override public void onBackPressed(){if(screen!=null&&screen.isShowing())screen.onBackPressed();else super.onBackPressed();}
 @Override protected void onDestroy(){if(screen!=null){screen.detachForHostDestruction();screen=null;}super.onDestroy();}
}
