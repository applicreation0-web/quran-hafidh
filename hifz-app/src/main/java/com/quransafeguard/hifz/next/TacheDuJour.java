package com.quransafeguard.hifz.next;

/**
 * Représente la tâche Hifz du jour.
 *
 * Le matin : une seule nouvelle unité.
 * Le soir : boule de neige acquisition ou consolidation.
 * Après : révision quotidienne.
 */
public class TacheDuJour {

    public enum Moment {
        MATIN,
        SOIR,
        REVISION
    }

    public enum Type {
        ACQUISITION,
        CONSOLIDATION,
        HIZB
    }

    private final Moment moment;
    private final Type type;
    private final String unite;
    private final int repetitions;

    public TacheDuJour(Moment moment, Type type, String unite, int repetitions) {
        this.moment = moment;
        this.type = type;
        this.unite = unite;
        this.repetitions = repetitions;
    }

    public Moment getMoment() {
        return moment;
    }

    public Type getType() {
        return type;
    }

    public String getUnite() {
        return unite;
    }

    public int getRepetitions() {
        return repetitions;
    }
}
