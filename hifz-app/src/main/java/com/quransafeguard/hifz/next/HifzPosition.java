package com.quransafeguard.hifz.next;

/**
 * Minimal persisted position for Hifz Next.
 * The sourate remains the hard boundary; verse remains flexible.
 */
public final class HifzPosition {
    public final int surah;
    public final int verse;
    public final int mushafLine;
    public final int page;

    public HifzPosition(int surah, int verse, int mushafLine, int page) {
        this.surah = surah;
        this.verse = verse;
        this.mushafLine = mushafLine;
        this.page = page;
    }

    public boolean sameSurah(HifzPosition other) {
        return other != null && surah == other.surah;
    }
}
