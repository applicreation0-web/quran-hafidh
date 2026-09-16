package com.quransafeguard.hifz.next;

/**
 * Gestion du rappel des boules de neige.
 *
 * Règle Quran Haafidh :
 * - Les boules de neige du soir sont créées du lundi au samedi.
 * - Chaque dimanche reprend les boules de neige envoyées exactement 14 jours auparavant.
 * - La reprise du dimanche est répétée 10 fois.
 */
public final class GestionnaireRappelDeuxSemaines {

    public static final int DECALAGE_JOURS = 14;
    public static final int REPETITIONS_DIMANCHE = 10;

    private GestionnaireRappelDeuxSemaines() {
    }

    public static boolean estRappelDimanche(int joursDepuisCreation) {
        return joursDepuisCreation == DECALAGE_JOURS;
    }
}
