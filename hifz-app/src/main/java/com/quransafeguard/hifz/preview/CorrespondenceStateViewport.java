package com.quransafeguard.hifz.preview;

/** Pending native viewport intent. Tokens reject stale posts and user gestures cancel intent.
 * Package-private and Android-free so callback ordering can be tested without claiming WebView QA. */
final class CorrespondenceStateViewport<T> {
 private long revision;
 private int generation,page;
 private T target;
 long request(T target,int generation,int page){
  if(target==null)throw new IllegalArgumentException("viewport target");
  this.target=target;this.generation=generation;this.page=page;return ++revision;
 }
 long revision(){return revision;}
 T pending(int generation,int page){return this.generation==generation&&this.page==page?target:null;}
 T pending(long token,int generation,int page){return token==revision?pending(generation,page):null;}
 void complete(long token){if(token==revision)target=null;}
 void cancel(){target=null;++revision;}
}
