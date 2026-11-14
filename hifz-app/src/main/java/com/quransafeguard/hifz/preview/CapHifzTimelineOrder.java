package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Defends the timeline from serpentine-style reversal, swapped Juz order,
 * and the false implication that a forecast is already an earned Hifz credit.
 */
final class CapHifzTimelineOrder {
    private CapHifzTimelineOrder(){}

    static void verify(List<CapHifzForecast.Quarter> quarters) {
        if(quarters==null||quarters.isEmpty())
            throw new IllegalArgumentException("Nonempty forecast required");
        LocalDate lastDate=null;
        int lastSabqi=-1,lastTail=-1;
        boolean tailFinished=false,sabqiFinished=false;
        int fath=GeometryRepository.ordinal(SabqiRoute.ROUTE_END);
        int tailStart=GeometryRepository.ordinal(ItqanRotationPolicy.TAIL_START);
        int nas=GeometryRepository.ordinal(ItqanRotationPolicy.TAIL_END);
        for(CapHifzForecast.Quarter q:quarters) {
            if(q==null||q.date==null||lastDate!=null&&!q.date.isAfter(lastDate))
                throw new IllegalStateException("Non chronological Hifz milestone");
            lastDate=q.date;
            if(q.sabqi!=null){
                int current=GeometryRepository.ordinal(q.sabqi);
                if(current<lastSabqi||current>fath||q.sabqiComplete&&current!=fath)
                    throw new IllegalStateException("Sabqi reversed or exceeded Al-Fath");
                lastSabqi=current;
            } else if(q.sabqiComplete) {
                throw new IllegalStateException("Sabqi completion without a position");
            }
            if(sabqiFinished&&!q.sabqiComplete)
                throw new IllegalStateException("Sabqi completion must not go backwards");
            sabqiFinished=q.sabqiComplete;
            if(tailFinished&&!q.firstTailComplete)
                throw new IllegalStateException("An-Nas latch must never reverse");
            if(q.itqan!=null){
                int current=GeometryRepository.ordinal(q.itqan);
                if(q.firstTailComplete||tailFinished||current<tailStart||current>nas||current<lastTail)
                    throw new IllegalStateException("Itqan reversed, reappeared or crossed leg");
                lastTail=current;
            }
            tailFinished=q.firstTailComplete;
        }
    }

    /**
     * The first quarter with a real forecast position in this canonical Juz
     * or later. No interpolation between quarter dates, and no fictitious
     * Itqān return after the first arrival at An-Nās.
     */
    static LocalDate firstProjectedJuz(List<CapHifzForecast.Quarter> quarters,
            int juz, boolean sabqi) {
        verify(quarters);
        if (juz < 1 || juz > 30) throw new IllegalArgumentException("Juz 1..30");
        for (CapHifzForecast.Quarter row : quarters) {
            VerseRef ref = sabqi ? row.sabqi
                : row.firstTailComplete ? ItqanRotationPolicy.TAIL_END : row.itqan;
            if (ref != null && CapHifzForecast.juz(ref) >= juz) return row.date;
            if (!sabqi && row.firstTailComplete) break;
        }
        return null;
    }

    /** Date means the END of the first quarter when both ROUTES are planned complete. */
    static LocalDate firstFullCoverageQuarter(List<CapHifzForecast.Quarter> quarters) {
        verify(quarters);
        for(CapHifzForecast.Quarter q:quarters)
            if(q.sabqiComplete&&q.firstTailComplete)return q.date;
        return null;
    }

    /** An earned 30-Juz state needs EVERY real QCF physical line acquired. */
    static boolean allThirtyJuzAcquired(GeometryRepository geometry,Set<String> acquired) {
        if(geometry==null||acquired==null)throw new IllegalArgumentException("Real progress required");
        for(int i=0;i<geometry.lineCount();i++)
            if(!acquired.contains(geometry.line(i).id))return false;
        return true;
    }
}
