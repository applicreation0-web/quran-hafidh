package com.quransafeguard.hifz.preview;

import android.content.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Offline trusted-package boundary. An absent reviewed pin keeps every production entry disabled. */
final class CorrespondenceRuntime {
 static final String ASSET="correspondences/approved_v1.json";
 // No production subset has passed rights, source, render and native QA. Never derive this from an Intent/asset.
 private static final String APPROVED_SHA256="";
 final CorrespondenceCatalog catalog;
 final CorrespondenceModel.Geometry geometry;
 private CorrespondenceRuntime(CorrespondenceCatalog c,CorrespondenceModel.Geometry g){catalog=c;geometry=g;}
 // One immutable package snapshot per process: repeated Atlas/reader/session entries
 // must not repeatedly decode geometry or observe different catalogue instances.
 private static volatile CorrespondenceRuntime cached;
 static CorrespondenceRuntime load(Context context){
  CorrespondenceRuntime current=cached;
  if(current!=null)return current;
  synchronized(CorrespondenceRuntime.class){
   if(cached==null)cached=read(context);
   return cached;
  }
 }
 private static CorrespondenceRuntime read(Context context){
  if(APPROVED_SHA256.isEmpty())return new CorrespondenceRuntime(CorrespondenceCatalog.empty(),null);
  try {
   CorrespondenceModel.Geometry geometry=new CorrespondenceGeometry(GeometryRepository.get(context),WordGeometryRepository.shared(context));
   byte[] bytes;
   try(InputStream input=context.getAssets().open(ASSET);ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buffer=new byte[8192];int n;
    while((n=input.read(buffer))!=-1){if(out.size()+n>1048576)throw new IllegalArgumentException("catalogue size");out.write(buffer,0,n);}
    bytes=out.toByteArray();
   }
   return new CorrespondenceRuntime(CorrespondenceCatalog.parse(bytes,APPROVED_SHA256,geometry),geometry);
  } catch(Exception unavailable){
   android.util.Log.w("Correspondences","Approved offline catalogue unavailable; contextual access disabled");
   return new CorrespondenceRuntime(CorrespondenceCatalog.empty(),null);
  }
 }
 CorrespondenceModel.Relation find(String id){for(CorrespondenceModel.Relation r:catalog.relations)if(r.id.equals(id))return r;return null;}
}
