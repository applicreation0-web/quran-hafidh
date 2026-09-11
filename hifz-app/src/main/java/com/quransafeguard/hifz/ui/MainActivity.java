package com.quransafeguard.hifz.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/** Temporary shell for the local-first recode. The native Mushaf reader replaces this next. */
public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView title = new TextView(this);
        title.setText("Quran Hifz");
        title.setTextSize(28f);
        title.setGravity(Gravity.CENTER);
        setContentView(title);
    }
}
