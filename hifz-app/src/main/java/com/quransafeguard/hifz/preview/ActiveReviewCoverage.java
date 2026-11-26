package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.EligibleCorpus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/** Evidence of user-confirmed traversal, never inferred from attendance or elapsed minutes. */
final class ActiveReviewCoverage {
    static final String KEY = "activeReviewCoverageV1";
    private static final int LIMIT = QuranCanon.TOTAL_VERSES + 1;
    private ActiveReviewCoverage() {}

    static BitSet through(List<VerseRef> traversal, VerseRef endpoint) {
        BitSet result=new BitSet(LIMIT);
        for(VerseRef verse:traversal){
            result.set(GeometryRepository.ordinal(verse));
            if(verse.equals(endpoint))return result;
        }
        throw new IllegalArgumentException("Endpoint outside active plan");
    }

    static String encode(BitSet bits){
        if(bits.isEmpty()||bits.get(0)||bits.length()>LIMIT)throw new IllegalArgumentException("Invalid coverage");
        StringBuilder out=new StringBuilder();
        for(int start=bits.nextSetBit(0);start>=0;){
            int end=bits.nextClearBit(start)-1;
            if(out.length()>0)out.append(',');
            out.append(start).append(':').append(end);
            start=bits.nextSetBit(end+1);
        }
        return out.toString();
    }

    static BitSet decode(String raw){
        if(raw==null||raw.isEmpty()||raw.length()>100000)throw new IllegalArgumentException("Invalid coverage proof");
        BitSet result=new BitSet(LIMIT);
        int previous=-1;
        for(String pair:raw.split(",",-1)){
            String[] parts=pair.split(":",-1);
            if(parts.length!=2||!parts[0].matches("[0-9]{1,4}")||!parts[1].matches("[0-9]{1,4}"))
                throw new IllegalArgumentException("Invalid coverage range");
            int start=Integer.parseInt(parts[0]),end=Integer.parseInt(parts[1]);
            if(start<1||start<=previous||end<start||end>=LIMIT)throw new IllegalArgumentException("Invalid coverage range");
            result.set(start,end+1);
            previous=end;
        }
        return result;
    }

    static List<VerseRef> restoreTraversal(String proof, VerseRef start, VerseRef end) {
        if (proof == null) return Collections.emptyList();
        BitSet covered = decode(proof);
        int first = GeometryRepository.ordinal(start);
        int last = GeometryRepository.ordinal(end);
        if (!covered.get(first) || !covered.get(last))
            throw new IllegalArgumentException("Confirmed review proof does not match its endpoints");
        List<VerseRef> traversal = new ArrayList<>();
        for (int ordinal = first; ; ordinal = ordinal == QuranCanon.TOTAL_VERSES ? 1 : ordinal + 1) {
            if (covered.get(ordinal)) traversal.add(QuranCanon.INSTANCE.fromOrdinal(ordinal));
            if (ordinal == last) break;
        }
        if (traversal.size() != covered.cardinality())
            throw new IllegalArgumentException("Confirmed review proof outside its original traversal");
        return traversal;
    }

    static void append(SharedPreferences.Editor edit,LocalDate date,ActiveReviewSlots.Slot slot,String proof){
        if(proof==null)return; // Historical callers retain their receipts, not invented coverage.
        decode(proof);
        edit.putString(KEY+"|"+date+"|"+slot.name(),proof);
    }

    static String summary(SharedPreferences prefs,ActiveReviewSlots slots,EligibleCorpus corpus,LocalDate today){
        BitSet acquired=new BitSet(LIMIT),covered=new BitSet(LIMIT);
        for(com.quransafeguard.hifz.core.VerseRange range:corpus.getRanges())
            acquired.set(GeometryRepository.ordinal(range.getStart()),GeometryRepository.ordinal(range.getEndInclusive())+1);
        int proofs=0,unknown=0;
        for(int offset=9;offset>=0;offset--){
            LocalDate day=today.minusDays(offset);
            for(ActiveReviewSlots.Slot slot:ActiveReviewSlots.Slot.values()){
                if(slots.receipt(day,slot)==null)continue;
                String raw=prefs.getString(KEY+"|"+day+"|"+slot.name(),null);
                if(raw==null){unknown++;continue;}
                covered.or(decode(raw));proofs++;
            }
        }
        covered.and(acquired);
        if(proofs==0)return "Couverture sur 10 jours : historique non mesuré";
        String result = "Couverture confirmée sur 10 jours : " + covered.cardinality()
            + "/" + acquired.cardinality() + " versets";
        if (unknown > 0) return result + " · " + unknown + " séance(s) ancienne(s) non mesurée(s)";
        if (!today.minusDays(9).isBefore(slots.programmeStart()) && !covered.equals(acquired))
            return result + " · rotation incomplète sur les dix derniers jours";
        return result;
    }
}
