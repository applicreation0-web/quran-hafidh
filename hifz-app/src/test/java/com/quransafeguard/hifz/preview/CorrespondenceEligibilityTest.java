package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Explicitly prevent the 24 pending and six rejected historical dossiers
 * from surfacing in the graphical page/type explorer or direct search. */
public final class CorrespondenceEligibilityTest {
 @Test public void onlyTheHistoricalEightyAreDiscoverable(){
  assertTrue(CorrespondenceEligibility.retainedForDiscovery("RETENUE_DOCUMENTÉE",false));
  assertFalse(CorrespondenceEligibility.retainedForDiscovery("RETENUE_DOCUMENTÉE",true));
  assertFalse(CorrespondenceEligibility.retainedForDiscovery("EN_ATTENTE_PREUVE",false));
  assertFalse(CorrespondenceEligibility.retainedForDiscovery("ÉCARTÉE_JUSTIFIÉE",false));
  assertFalse(CorrespondenceEligibility.retainedForDiscovery("RETENUE",false));
  assertFalse(CorrespondenceEligibility.retainedForDiscovery(null,false));
 }
}
