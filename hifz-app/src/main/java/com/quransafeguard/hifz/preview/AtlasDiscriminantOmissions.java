package com.quransafeguard.hifz.preview;

/**
 * Display-only corrections of nine already admitted al-Abbad comparisons.
 * The approved 279-case catalogue and original word anchors remain unchanged.
 * Five additions/omissions must NOT underline common words as substitutes.
 * Two grouped pivots underline complete canonical source expressions; two more
 * have independently placed second QCF strips (not a false word substitution).
 * Reviewed in research/mutash_pivot_review_v2_20261031.json:
 * 120,199 (two separate risks); 124,147 (complete word groups);
 * 142,145,149 (word absent on right);
 * 161,185 (phrase absent on left).
 * No generated Quran, no French gloss, no changes to Hifz engines or credits.
 */
final class AtlasDiscriminantOmissions {
    private AtlasDiscriminantOmissions() {}

    static int[] actualDifference(AtlasDiscriminantCases.Entry entry, boolean first) {
        switch (entry.auditId) {
            case 199: // 9:94 ثم تردون / 9:105 وستردون; second strip shows the added والمؤمنون.
                return first ? new int[]{21,22} : new int[]{8};
            case 124: // 6:5: فسوف يأتيهم, 26:6: فسيأتيهم (one complete phrase each).
                return first ? new int[]{6,7} : new int[]{3};
            case 147: // 22:5: من مضغة, 40:67: يخرجكم.
                return first ? new int[]{20,21} : new int[]{13};
            case 142: // 11:60 has الدنيا; 11:99 does not (لعنة is shared).
            case 145: // 39:72 has قيل; 40:76 does not (ادخلوا is shared).
            case 149: // 22:62 has هو; 31:30 does not (الباطل is shared).
                return first ? entry.firstWordIndices.clone() : new int[0];
            case 161: // 3:77 alone adds ولا ينظر إليهم before يوم القيامة.
                return first ? new int[0] : new int[]{18,19,20};
            case 185: // 25:59 alone adds وما بينهما after والأرض.
                return first ? new int[0] : new int[]{5,6};
            default:
                return (first ? entry.firstWordIndices : entry.secondWordIndices).clone();
        }
    }

    /**
     * A genuine QCF crop needs a source location even when the contrast side
     * has no extra word. Retain its existing common-word anchor for the crop,
     * but never pass that anchor to the underline painter.
     */
    static int[] cropAnchor(AtlasDiscriminantCases.Entry entry, boolean first) {
        // On the side with a real inserted phrase, include all of its words in
        // the local QCF crop; on the absent side only, use the original common
        // word as a location anchor without highlighting it.
        if(entry.auditId==124 || entry.auditId==147 || entry.auditId==199)
            return actualDifference(entry,first);
        if(!first && (entry.auditId==161 || entry.auditId==185))
            return actualDifference(entry,false);
        return (first ? entry.firstWordIndices : entry.secondWordIndices).clone();
    }

    static boolean explicitlyAbsent(AtlasDiscriminantCases.Entry entry, boolean first) {
        return actualDifference(entry, first).length == 0;
    }

    static boolean hasSecondary(int auditId){return auditId==120 || auditId==199;}

    static int[] secondaryDifference(AtlasDiscriminantCases.Entry entry,boolean first){
        if(entry.auditId==120)return first?new int[]{11}:new int[]{10};
        if(entry.auditId==199)return first?new int[0]:new int[]{7};
        return new int[0];
    }

    static int[] secondaryCropAnchor(AtlasDiscriminantCases.Entry entry,boolean first){
        if(entry.auditId==199)return first?new int[]{20}:new int[]{7};
        return secondaryDifference(entry,first);
    }

    static String relation(AtlasDiscriminantCases.Entry entry) {
        if(entry.auditId==120)return "Deux bifurcations";
        if(entry.auditId==199)return "Ajout puis bifurcation";
        if(entry.auditId==124 || entry.auditId==147)
            return "Bifurcation : groupe de mots";
        return explicitlyAbsent(entry, true) || explicitlyAbsent(entry, false)
            ? "Ajout / omission" : entry.kind;
    }
}
