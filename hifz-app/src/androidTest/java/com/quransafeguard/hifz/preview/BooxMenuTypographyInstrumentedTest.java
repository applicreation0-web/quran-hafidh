package com.quransafeguard.hifz.preview;

import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import static org.junit.Assert.*;

/** Test the real Android menu TextViews, not an SVG mockup or source-only string. */
public final class BooxMenuTypographyInstrumentedTest {

    @Test public void settingsRowHasTwoDistinctReadableLinesAndLiveValue() {
        android.content.Context context = ApplicationProvider.getApplicationContext();
        LinearLayout row = Ui.settingRow(context, "Programme", "3 Sabqi · 4 Itqān", v -> {});
        assertTrue(row.getMinimumHeight() >= Ui.dp(context, 56));
        assertTrue(row.getChildAt(0) instanceof LinearLayout);
        LinearLayout copy = (LinearLayout) row.getChildAt(0);
        assertTrue(copy.getChildAt(0) instanceof TextView);
        assertTrue(copy.getChildAt(1) instanceof TextView);
        TextView title = (TextView) copy.getChildAt(0);
        TextView subtitle = (TextView) copy.getChildAt(1);
        assertEquals("Programme", title.getText().toString());
        assertEquals("3 Sabqi · 4 Itqān", subtitle.getText().toString());
        assertEquals(Typeface.BOLD, title.getTypeface().getStyle());
        assertEquals(Typeface.NORMAL, subtitle.getTypeface().getStyle());
        assertEquals(Ui.MUTED, subtitle.getCurrentTextColor());
        assertEquals(2, subtitle.getMaxLines());
        assertSame(subtitle, Ui.settingValue(row));
        Ui.settingValue(row).setText("4 Sabqi · 3 Itqān");
        assertEquals("4 Sabqi · 3 Itqān", subtitle.getText().toString());
    }
}
