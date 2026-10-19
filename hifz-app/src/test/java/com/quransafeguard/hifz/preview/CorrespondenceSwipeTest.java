package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

public class CorrespondenceSwipeTest {
 @Test public void singleFingerHorizontalSwipeKeepsReaderDirection(){
  CorrespondenceSwipe swipe=new CorrespondenceSwipe();
  swipe.begin(10,10,1f);assertEquals(1,swipe.finish(80,14,1f,56));
  swipe.begin(80,10,1f);assertEquals(-1,swipe.finish(10,14,1f,56));
 }
 @Test public void verticalAndShortDragsNeverTurnPage(){
  CorrespondenceSwipe swipe=new CorrespondenceSwipe();
  swipe.begin(0,0,1f);assertEquals(0,swipe.finish(70,100,1f,56));
  swipe.begin(0,0,1f);assertEquals(0,swipe.finish(50,0,1f,56));
 }
 @Test public void zoomedSingleFingerPanNeverTurnsPage(){
  CorrespondenceSwipe swipe=new CorrespondenceSwipe();
  swipe.begin(0,0,2f);assertEquals(0,swipe.finish(90,0,2f,56));
  swipe.begin(0,0,2f);assertEquals(0,swipe.finish(90,0,1f,56));
 }
 @Test public void pinchReturningToOneCannotBecomeLastFingerSwipe(){
  CorrespondenceSwipe swipe=new CorrespondenceSwipe();
  swipe.begin(0,0,1f);swipe.multiplePointers();
  assertEquals(0,swipe.finish(100,0,1f,56));
  assertEquals(0,swipe.finish(200,0,1f,56));
  swipe.begin(0,0,1f);assertEquals(1,swipe.finish(100,0,1f,56));
 }
 @Test public void cancelClearsTrackingAndOnlyNewDownCanStartAgain(){
  CorrespondenceSwipe swipe=new CorrespondenceSwipe();
  swipe.begin(0,0,1f);swipe.cancel();assertEquals(0,swipe.finish(100,0,1f,56));
  assertEquals(0,swipe.finish(200,0,1f,56));
  swipe.begin(0,0,1f);assertEquals(1,swipe.finish(100,0,1f,56));
 }
 @Test public void missingDownCannotTurnPage(){
  assertEquals(0,new CorrespondenceSwipe().finish(100,0,1f,56));
 }
}
