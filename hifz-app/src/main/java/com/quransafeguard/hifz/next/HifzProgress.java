package com.quransafeguard.hifz.next;

/** Simplified progression state for the rebuilt Hifz engine. */
public final class HifzProgress {
    public final HifzMode mode;
    public final HifzPosition start;
    public final HifzPosition current;

    public HifzProgress(HifzMode mode, HifzPosition start, HifzPosition current) {
        this.mode = mode;
        this.start = start;
        this.current = current;
    }

    /** A progression step may not cross a sourate boundary automatically. */
    public boolean crossesSurah(HifzPosition next) {
        return current != null && next != null && current.surah != next.surah;
    }
}
