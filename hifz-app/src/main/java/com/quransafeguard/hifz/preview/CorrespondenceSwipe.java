package com.quransafeguard.hifz.preview;

/** Pure single-pointer folio gesture policy. Multi-touch belongs to WebView zoom. */
final class CorrespondenceSwipe {
 private float startX,startY;
 private boolean tracking,eligible;
 void begin(float x,float y,float zoom){
  startX=x;startY=y;tracking=true;eligible=zoom>0&&zoom<=1.05f;
 }
 // Pointer-up does not re-enable the remaining finger. Only the next DOWN may do so.
 void multiplePointers(){eligible=false;}
 void cancel(){tracking=false;eligible=false;}
 int finish(float x,float y,float zoom,float threshold){
  boolean singleSwipe=tracking&&eligible;cancel();
  float dx=x-startX,dy=y-startY;
  return singleSwipe&&zoom>0&&zoom<=1.05f&&Math.abs(dx)>threshold&&Math.abs(dx)>Math.abs(dy)*1.4f?(dx>0?1:-1):0;
 }
}
