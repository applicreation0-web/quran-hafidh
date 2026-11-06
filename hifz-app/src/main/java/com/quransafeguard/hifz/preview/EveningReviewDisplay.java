package com.quransafeguard.hifz.preview;

/**
 * Display-only state for a dated evening session. "Nothing new to review"
 * is not proof of completion. A completion checkmark requires a real,
 * date-specific receipt; no Hifz credits or calendars are mutated here.
 */
final class EveningReviewDisplay {
    enum Status { WAITING_FOR_LESSON, TO_DO, COMPLETED, NO_NEW_UNITS }

    static Status forDay(boolean lessonCompleted,boolean datedEveningReceipt,boolean newUnitsDue) {
        if(datedEveningReceipt)return Status.COMPLETED;
        if(!lessonCompleted)return Status.WAITING_FOR_LESSON;
        return newUnitsDue?Status.TO_DO:Status.NO_NEW_UNITS;
    }

    static boolean resolved(Status status) {
        return status==Status.COMPLETED||status==Status.NO_NEW_UNITS;
    }

    static String learningLabel(Status status) {
        switch(status) {
            case COMPLETED:return "✓ Renforcement";
            case WAITING_FOR_LESSON:return "Renforcement · en attente du Sabqi";
            case TO_DO:return "Renforcement · révision cumulative";
            case NO_NEW_UNITS:return "Renforcement · aucune nouvelle unité";
            default:throw new IllegalArgumentException("Unknown Renforcement state");
        }
    }

    static String stabilizationLabel(Status status,boolean postNasMaintenance) {
        switch(status) {
            case COMPLETED:return "✓ Consolidation";
            case WAITING_FOR_LESSON:return "Consolidation · en attente du Itqān";
            case TO_DO:return postNasMaintenance
                ?"Consolidation · maintiens de la semaine · ×1 masqué"
                :"Consolidation · révision cumulative";
            case NO_NEW_UNITS:return "Consolidation · aucune nouvelle unité";
            default:throw new IllegalArgumentException("Unknown Consolidation state");
        }
    }

    static String learningTodayLabel(Status status) {
        return status==Status.WAITING_FOR_LESSON||status==Status.NO_NEW_UNITS
            ?learningLabel(status):"Renforcement · révision cumulative du soir";
    }

    static String stabilizationTodayLabel(Status status) {
        return status==Status.WAITING_FOR_LESSON||status==Status.NO_NEW_UNITS
            ?stabilizationLabel(status,false):"Consolidation · révision cumulative du soir";
    }
}
