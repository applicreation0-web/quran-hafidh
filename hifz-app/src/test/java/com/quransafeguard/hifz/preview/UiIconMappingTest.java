package com.quransafeguard.hifz.preview;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/** C22: every semantic label currently sent to compact actions has an explicit stable icon mapping. */
@RunWith(Parameterized.class)
public final class UiIconMappingTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"Retour", R.drawable.ic_ui_back},
            {"Plus tard", R.drawable.ic_ui_close},
            {"Fermer", R.drawable.ic_ui_close},
            {"Fermer le Tafsir", R.drawable.ic_ui_close},
            {"Page précédente", R.drawable.ic_ui_previous},
            {"Verset précédent", R.drawable.ic_ui_previous},
            {"Page suivante", R.drawable.ic_ui_next},
            {"Verset suivant", R.drawable.ic_ui_next},
            {"Lire / pause", R.drawable.ic_ui_play},
            {"Référence", R.drawable.ic_ui_info},
            {"Lecture", R.drawable.ic_ui_reading},
            {"Mémoriser", R.drawable.ic_ui_memorize},
            {"Progression", R.drawable.ic_ui_progress_map},
            {"Paramètres", R.drawable.ic_ui_settings},
            {"Audio", R.drawable.ic_ui_audio},
            {"Écouter", R.drawable.ic_ui_audio},
            {"Remettre à zéro", R.drawable.ic_ui_reset},
            {"Retirer une répétition", R.drawable.ic_ui_delete},
            {"Ajouter une répétition", R.drawable.ic_ui_add},
            {"Répétition", R.drawable.ic_ui_repeat},
            {"Répéter le verset", R.drawable.ic_ui_repeat},
            {"Révéler", R.drawable.ic_ui_reveal},
            {"À renforcer", R.drawable.ic_hifz_strengthen},
            {"Apprentissage", R.drawable.ic_hifz_new_lesson},
            {"Stabilisation", R.drawable.ic_hifz_anchor},
            {"Consolidation", R.drawable.ic_hifz_consolidation},
            {"Renforcement", R.drawable.ic_hifz_consolidation},
            {"Révision", R.drawable.ic_hifz_maintenance},
            {"Passage suivant du corpus", R.drawable.ic_ui_rotation},
            {"Leçon neuve", R.drawable.ic_hifz_new_lesson},
            {"Ancrage", R.drawable.ic_hifz_anchor},
            {"Entretien", R.drawable.ic_hifz_maintenance},
            {"Valider", R.drawable.ic_ui_validate},
            {"Valider jusqu’ici", R.drawable.ic_ui_validate},
            {"Revu", R.drawable.ic_ui_validate},
            {"Modifier la plage", R.drawable.ic_ui_edit},
            {"Supprimer la plage", R.drawable.ic_ui_delete},
            {"Sourate", R.drawable.ic_ui_surah_list},
            {"Hizb", R.drawable.ic_ui_hizb}
        });
    }

    private final String label;
    private final int expected;

    public UiIconMappingTest(String label, int expected) {
        this.label = label;
        this.expected = expected;
    }

    @Test public void mappedToExpectedDrawable() {
        assertEquals(expected, Ui.iconFor(label, ""));
    }
}
