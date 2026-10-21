package com.quransafeguard.hifz.preview;

/** Fixed editorial scope: only the 80 historically documented dossiers are discoverable.
 * The 24 awaiting proof and six discarded dossiers stay in the immutable private archive.
 * A literary type or a source reference never upgrades a dossier's status. */
final class CorrespondenceEligibility {
 private CorrespondenceEligibility(){}
 static boolean retainedForDiscovery(String status,boolean excluded){
  return "RETENUE_DOCUMENTÉE".equals(status)&&!excluded;
 }
}
