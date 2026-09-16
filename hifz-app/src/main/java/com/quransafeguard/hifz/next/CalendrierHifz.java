package com.quransafeguard.hifz.next;

import java.util.ArrayList;
import java.util.List;

/**
 * Calendrier simple du parcours Hifz.
 *
 * Matin : nouvelle leçon uniquement.
 * Soir : boule de neige puis révision quotidienne.
 */
public final class CalendrierHifz {

    public enum Type {
        ACQUISITION,
        CONSOLIDATION
    }

    public static final class Etape {
        public final Type type;
        public final String unite;
        public final List<String> chaineDuSoir;

        public Etape(Type type, String unite, List<String> chaineDuSoir) {
            this.type = type;
            this.unite = unite;
            this.chaineDuSoir = chaineDuSoir;
        }
    }

    private CalendrierHifz() {}

    /**
     * Semaine A : trois nouvelles unités par type.
     */
    public static List<Etape> semaineA() {
        List<Etape> resultat = new ArrayList<>();
        resultat.add(new Etape(Type.ACQUISITION, "A1", liste("A1")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C1", liste("C1")));
        resultat.add(new Etape(Type.ACQUISITION, "A2", liste("A1", "A2")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C2", liste("C1", "C2")));
        resultat.add(new Etape(Type.ACQUISITION, "A3", liste("A1", "A2", "A3")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C3", liste("C1", "C2", "C3")));
        return resultat;
    }

    /**
     * Semaine B : poursuite du cycle.
     */
    public static List<Etape> semaineB() {
        List<Etape> resultat = new ArrayList<>();
        resultat.add(new Etape(Type.ACQUISITION, "A4", liste("A1", "A2", "A3", "A4")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C4", liste("C1", "C2", "C3", "C4")));
        resultat.add(new Etape(Type.ACQUISITION, "A5", liste("A1", "A2", "A3", "A4", "A5")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C5", liste("C1", "C2", "C3", "C4", "C5")));
        resultat.add(new Etape(Type.ACQUISITION, "A6", liste("A1", "A2", "A3", "A4", "A5", "A6")));
        resultat.add(new Etape(Type.CONSOLIDATION, "C6", liste("C1", "C2", "C3", "C4", "C5", "C6")));
        return resultat;
    }

    private static List<String> liste(String... valeurs) {
        List<String> resultat = new ArrayList<>();
        for (String valeur : valeurs) resultat.add(valeur);
        return resultat;
    }
}
