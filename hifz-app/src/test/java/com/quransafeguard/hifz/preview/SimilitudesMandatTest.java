package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
public final class SimilitudesMandatTest {
 @Test public void sharedConclusionIsNotCalledAReplacementWord(){
  for(AtlasDiscriminantCases.Entry e:AtlasDiscriminantCases.CATALOGUE)if(e.auditId==168){
   assertEquals("Conclusion commune",AtlasDiscriminantActivity.displayRelation(e));return;
  }
  fail("Documented pair missing");
 }
}
