package com.quransafeguard.hifz.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.storage.HifzProgressStore;

/** Rare initial configuration: exactly four verse bounds, as defined by the Hifz contract. */
public final class HifzSetupActivity extends android.app.Activity {
    private HifzProgressStore store;
    private EditText sabqiStart, sabqiEnd, itqanStart, itqanEnd;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        store = new HifzProgressStore(this);
        LinearLayout root = Ui.column(this);
        root.addView(Ui.text(this, "Parcours Hifz — configuration", 22, true));
        TextView help = Ui.text(this, "Indiquez sourate:verset pour les quatre bornes. Exemple : 2:1.", 14, false);
        root.addView(help);

        sabqiStart = field("Début Sabqi"); sabqiEnd = field("Fin Sabqi");
        itqanStart = field("Début Itqān"); itqanEnd = field("Fin Itqān");
        root.addView(label("Début Sabqi")); root.addView(sabqiStart);
        root.addView(label("Fin Sabqi")); root.addView(sabqiEnd);
        root.addView(label("Début Itqān")); root.addView(itqanStart);
        root.addView(label("Fin Itqān")); root.addView(itqanEnd);

        if (store.isConfigured()) {
            sabqiStart.setText(store.sabqiStart().toString()); sabqiEnd.setText(store.sabqiEnd().toString());
            itqanStart.setText(store.itqanStart().toString()); itqanEnd.setText(store.itqanEnd().toString());
        }
        root.addView(Ui.button(this, "Enregistrer", v -> save()));
        root.addView(Ui.button(this, "Annuler", v -> finish()));
        setContentView(root);
    }

    private TextView label(String value) { return Ui.text(this, value, 14, true); }
    private EditText field(String hint) {
        EditText e = new EditText(this); e.setHint(hint + " (ex. 2:1)");
        e.setSingleLine(true); e.setInputType(InputType.TYPE_CLASS_TEXT); e.setTextSize(18f); return e;
    }

    private void save() {
        try {
            VerseRef s1 = parse(sabqiStart.getText().toString());
            VerseRef s2 = parse(sabqiEnd.getText().toString());
            VerseRef i1 = parse(itqanStart.getText().toString());
            VerseRef i2 = parse(itqanEnd.getText().toString());
            store.configure(s1, s2, i1, i2);
            Toast.makeText(this, "Parcours Hifz enregistré.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Throwable error) {
            Toast.makeText(this, "Bornes invalides : " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static VerseRef parse(String value) {
        String[] parts = value.trim().split(":", -1);
        if (parts.length != 2) throw new IllegalArgumentException("format attendu sourate:verset");
        VerseRef ref = new VerseRef(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        QuranCanon.INSTANCE.requireValid(ref);
        return ref;
    }
}
