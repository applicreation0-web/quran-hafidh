package com.quransafeguard.hifz.preview;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.function.Consumer;

/** Canonical location picker. The caller decides whether the selected range is eligible for study. */
final class QuranRangePicker {
    private QuranRangePicker() {}

    static AlertDialog show(Activity activity, Consumer<VerseRange> onAdd) {
        LinearLayout form = Ui.column(activity);
        int pad = Ui.dp(activity, 16);
        form.setPadding(pad, 0, pad, 0);
        String[] names = new String[114];
        for (int index = 0; index < names.length; index++)
            names[index] = (index + 1) + " · " + QuranSurahNames.name(index + 1);
        ArrayAdapter<String> titles = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, names);
        titles.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Position first = new Position(activity, form, titles, R.string.setup_range_first,
            R.id.setup_range_first_surah, R.id.setup_range_first_ayah);
        Position last = new Position(activity, form, titles, R.string.setup_range_last,
            R.id.setup_range_last_surah, R.id.setup_range_last_ayah);
        TextView error = Ui.text(activity, activity.getString(R.string.setup_range_order_error), 16f, false);
        error.setVisibility(View.GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        form.addView(error);
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(R.string.setup_pick_range)
            .setView(scroll).setNegativeButton(R.string.ui_close,null)
            .setPositiveButton(R.string.setup_add_range,null).show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            VerseRef start = first.selection();
            VerseRef end = last.selection();
            if (start.compareTo(end) > 0) {
                error.setVisibility(View.VISIBLE);
                return;
            }
            onAdd.accept(new VerseRange(start,end));
            dialog.dismiss();
        });
        return dialog;
    }

    static void appendTo(android.widget.EditText input, VerseRange range) {
        String prior = input.getText().toString().trim();
        String location = range.getStart() + "-" + range.getEndInclusive();
        input.setText(prior.isEmpty() ? location : prior + ", " + location);
    }

    private static final class Position {
        private final Spinner surah;
        private final NumberPicker ayah;

        Position(Activity activity, LinearLayout form, ArrayAdapter<String> names, int label,
                int surahId, int ayahId) {
            TextView heading = Ui.text(activity, activity.getString(label), 16f, true);
            heading.setLabelFor(surahId);
            form.addView(heading);
            LinearLayout controls = Ui.row(activity);
            surah = new Spinner(activity);
            surah.setId(surahId);
            surah.setAdapter(names);
            surah.setMinimumHeight(Ui.dp(activity, 48));
            controls.addView(surah, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            ayah = new NumberPicker(activity);
            ayah.setId(ayahId);
            ayah.setContentDescription(activity.getString(R.string.setup_range_ayah));
            ayah.setMinValue(1);
            ayah.setMaxValue(QuranCanon.INSTANCE.ayahCount(1));
            ayah.setWrapSelectorWheel(false);
            controls.addView(ayah);
            form.addView(controls);
            surah.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long itemId) {
                    ayah.setMaxValue(QuranCanon.INSTANCE.ayahCount(index + 1));
                }

                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        VerseRef selection() {
            ayah.clearFocus();
            return new VerseRef(surah.getSelectedItemPosition() + 1, ayah.getValue());
        }
    }
}
