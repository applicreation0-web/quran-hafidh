package com.quransafeguard.hifz.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.quransafeguard.hifz.R;
import com.quransafeguard.hifz.data.MushafRepository;
import com.quransafeguard.hifz.reader.MushafRenderer;

/** First native local-reader slice: page 1 -> page 2 -> page 1. */
public final class MainActivity extends Activity {
    private MushafRenderer renderer;
    private TextView pageLabel;
    private Button previous;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(android.graphics.Color.WHITE);

        renderer = new MushafRenderer(this);
        renderer.setId(R.id.mushaf_renderer);
        root.addView(renderer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        previous = new Button(this);
        previous.setId(R.id.previous_page);
        previous.setText("‹");
        previous.setContentDescription("Page précédente");
        previous.setOnClickListener(v -> renderer.previousPage());

        pageLabel = new TextView(this);
        pageLabel.setId(R.id.page_label);
        pageLabel.setGravity(Gravity.CENTER);
        pageLabel.setTextSize(18f);

        next = new Button(this);
        next.setId(R.id.next_page);
        next.setText("›");
        next.setContentDescription("Page suivante");
        next.setOnClickListener(v -> renderer.nextPage());

        int buttonWidth = dp(64);
        controls.addView(previous, new LinearLayout.LayoutParams(buttonWidth, dp(56)));
        controls.addView(pageLabel, new LinearLayout.LayoutParams(dp(120), dp(56)));
        controls.addView(next, new LinearLayout.LayoutParams(buttonWidth, dp(56)));
        root.addView(controls, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        renderer.setListener(new MushafRenderer.Listener() {
            @Override public void onPageChanged(int page) {
                pageLabel.setText("Page " + page + " / 604");
                previous.setEnabled(page > MushafRepository.FIRST_PAGE && renderer.isPageBundled(page - 1));
                next.setEnabled(page < MushafRepository.LAST_PAGE && renderer.isPageBundled(page + 1));
            }

            @Override public void onError(int page, Throwable error) {
                Toast.makeText(MainActivity.this,
                    "Page locale indisponible : " + page,
                    Toast.LENGTH_LONG).show();
            }
        });

        previous.setEnabled(false);
        next.setEnabled(false);
        pageLabel.setText("Chargement…");
        setContentView(root);
        renderer.showPage(MushafRepository.FIRST_PAGE);
    }

    @Override
    protected void onDestroy() {
        if (renderer != null) renderer.close();
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
