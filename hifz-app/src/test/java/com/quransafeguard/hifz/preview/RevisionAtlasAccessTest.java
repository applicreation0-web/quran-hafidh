package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RevisionAtlasAccessTest {
    @Test public void passiveRequiresExactPageAndCoveredDocumentedVerse() {
        assertTrue(RevisionAtlasAccess.allowed(false,false,false,true,true,true));
        assertFalse(RevisionAtlasAccess.allowed(false,false,false,false,true,true));
        assertFalse(RevisionAtlasAccess.allowed(false,false,false,true,false,true));
        assertFalse(RevisionAtlasAccess.allowed(false,false,false,true,true,false));
    }
    @Test public void revealEndpointAndRenderFailureNeverUnlockActiveRecall() {
        assertFalse(RevisionAtlasAccess.allowed(true,false,false,true,true,true));
        assertFalse(RevisionAtlasAccess.allowed(true,false,true,true,true,true));
        assertFalse(RevisionAtlasAccess.allowed(true,true,false,true,true,true));
    }
    @Test public void activeNeedsSuccessfulReceiptAndValidatedSource() {
        assertTrue(RevisionAtlasAccess.allowed(true,true,true,true,false,true));
        assertFalse(RevisionAtlasAccess.allowed(true,true,true,false,true,true));
        assertFalse(RevisionAtlasAccess.allowed(true,true,true,true,true,false));
    }
}
