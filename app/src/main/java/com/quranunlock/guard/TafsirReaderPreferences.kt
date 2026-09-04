package com.applicreation0.quransafeguard

import android.content.Context

object TafsirReaderPreferences {
    private const val PREFS = "tafsir_reader_preferences"
    private const val EDITION_KEY = "selected_tafsir_edition"

    fun selectedEdition(context: Context): TafsirEditionId =
        TafsirEditionId.fromStableId(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(EDITION_KEY, TafsirEditionId.JALALAYN.stableId)
        )

    fun setSelectedEdition(context: Context, editionId: TafsirEditionId) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(EDITION_KEY, editionId.stableId)
            .apply()
    }
}
