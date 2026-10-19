package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
/** Callback ordering model only; no Android/WebView or physical device execution. */
public final class CorrespondenceStateViewportTest {
 @Test public void checkpointReadsIntendedViewportUntilMatchingPostCompletes(){
  CorrespondenceStateViewport<String> v=new CorrespondenceStateViewport<>();
  long token=v.request("comparison",4,12);
  assertEquals("comparison",v.pending(4,12));
  assertEquals("comparison",v.pending(token,4,12));
  v.complete(token);assertNull(v.pending(4,12));
 }
 @Test public void oldDocumentAndOldPostsCannotApplyOrClearNewerIntent(){
  CorrespondenceStateViewport<String> v=new CorrespondenceStateViewport<>();
  long old=v.request("old",4,12),current=v.request("new",5,13);
  assertNull(v.pending(old,4,12));assertNull(v.pending(current,4,12));
  v.complete(old);assertEquals("new",v.pending(current,5,13));
  long newer=v.request("latest same page",5,13);
  assertNull(v.pending(current,5,13));v.complete(current);
  assertEquals("latest same page",v.pending(newer,5,13));
 }
 @Test public void gestureOrDisposalCancelsQueuedRestoration(){
  CorrespondenceStateViewport<String> v=new CorrespondenceStateViewport<>();
  long token=v.request("restore",4,12);v.cancel();
  assertNull(v.pending(token,4,12));assertNull(v.pending(4,12));
  long next=v.request("new",4,12);assertTrue(next>token);
  v.complete(token);assertEquals("new",v.pending(next,4,12));
 }
}
