package com.quransafeguard.hifz.preview;

/** Read-only presentation gate. Never validates a session or writes a receipt. */
final class RevisionAtlasAccess {
    private RevisionAtlasAccess() {}
    static boolean allowed(boolean active, boolean completed, boolean validatedReceipt,
                           boolean documented, boolean onPage, boolean covered) {
        return documented && covered && (active ? completed && validatedReceipt : !completed && onPage);
    }
}
