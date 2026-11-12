package com.quransafeguard.hifz.preview;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledCadence;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Quran Hifz personal home. No Safeguard/blocking API is linked here. */
public final class MainActivity extends android.app.Activity {
    private HifzPrefs prefs;
    private PostFinalRecallStore postFinalStore;
    private HifzSpeedStore speedStore;
    private DashboardLedger ledger;
    private volatile GeometryRepository geometry;
    private WeeklyDashboardPlanner.Row todayRow;
    private boolean consolidationNeedsAttention;
    private boolean learningConsolidationNeedsAttention;
    private TextView today;
    private TextView todayObjective;
    private TextView recentSabqiAdvisory;
    private LinearLayout todayAction;
    private LinearLayout dashboard;
    private TextView dashboardTitle;
    private LinearLayout weekStrip;
    private LinearLayout weekDayPanel;
    private List<WeeklyDashboardPlanner.Row> weekRows = new ArrayList<>();
    private LocalDate selectedWeekDate;
    private LinearLayout sabqiQuickAccess;
    private LinearLayout itqanQuickAccess;
    private final List<View> geometryActions = new ArrayList<>();
    private LinearLayout itqanCard;
    private LinearLayout consolidationCard;
    private TextView todayHeading;
    private ScrollView homeScroll;
    private ViewGroup parcoursScroll;
    private TextView backlogHomeRow;
    private TextView backlogParcoursRow;
    private List<HifzBacklog.Due> backlogPending = java.util.Collections.emptyList();
    private boolean showingParcours;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SemanticPassageRepository.preloadAsync(this);
        prefs = new HifzPrefs(this);
        postFinalStore = new PostFinalRecallStore(this);
        speedStore = new HifzSpeedStore(this);
        ledger = new DashboardLedger(this);

        // 1.17.5 CP1: BOOX-first home, one dominant session and four equally sized destinations.
        // Navigation-only refactor: all cadence, Hifz credits and geometry guards are unchanged.
        LinearLayout root = page();
        // BOOX portrait home: fill the viewport without a centered 720dp tablet inset.
        homeScroll = scrollOf(root);
        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        // Home brand mark must match the actual installed Android launcher icon.
        // Do not substitute the unrelated house/home navigation pictogram.
        ImageView appEmblem = new ImageView(this);
        appEmblem.setImageResource(R.drawable.ic_quran_hifz_header);
        appEmblem.setScaleType(ImageView.ScaleType.FIT_CENTER);
        appEmblem.setContentDescription("Quran Haafidh");
        header.addView(appEmblem, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)));
        TextView title = Ui.bookText(this, "Quran Haafidh", 24, true);
        if (wideBoox()) title.setTextSize(29f);
        title.setGravity(Gravity.CENTER);
        Ui.weight(title, 1f);
        header.addView(title);
        header.addView(Ui.iconButton(this, "", "Paramètres", v -> startActivity(new Intent(this, SettingsActivity.class))));
        root.addView(header);
        root.addView(Ui.divider(this));

        todayAction = todayLine();
        today = Ui.settingValue(todayAction);
        root.addView(todayAction);
        backlogHomeRow = backlogRow();
        root.addView(backlogHomeRow);

        recentSabqiAdvisory = Ui.text(this, "", 17f, false);
        recentSabqiAdvisory.setTextColor(Ui.MUTED);
        recentSabqiAdvisory.setVisibility(View.GONE);
        root.addView(recentSabqiAdvisory);

        TextView destinationsLabel = Ui.bookText(this, "ACCÈS DIRECTS", 17f, true);
        if (wideBoox()) destinationsLabel.setTextSize(21f);
        destinationsLabel.setPadding(0, Ui.dp(this, 18), 0, Ui.dp(this, 8));
        root.addView(destinationsLabel);
        // Two rows with one visual grammar: practice + follow-up, reading + understanding.
        // No separator: Progression is an equal navigation destination, not a footer.
        LinearLayout practiceRow = Ui.row(this);
        LinearLayout parcours = primaryTile(practiceRow, "Parcours Hifz", "Parcours", v -> showParcours());
        LinearLayout progress = primaryTile(practiceRow, "Progression", "Progression",
            v -> startActivity(new Intent(this, ProgressMapActivity.class)));
        root.addView(practiceRow);

        LinearLayout readingRow = Ui.row(this);
        // Eight dp of breathing room groups the two related pairs without drawing a rule.
        readingRow.setPadding(0, Ui.dp(this, 8), 0, 0);
        LinearLayout study = primaryTile(readingRow, "Lecture", "Lecture",
            v -> startActivity(new Intent(this, StudyReaderActivity.class)));
        LinearLayout semanticMap = primaryTile(readingRow, "Atlas", "Atlas",
            v -> startActivity(new Intent(this, AtlasMapActivity.class)));
        root.addView(readingRow);

        // Mémorisation ponctuelle remains an optional advanced tool, not a fifth icon.

        geometryActions.add(study);
        geometryActions.add(semanticMap);
        geometryActions.add(progress);

        // --- Parcours Hifz: the five paths, one line each, then this week.
        LinearLayout path = page();
        parcoursScroll = wideBoox() ? fixedBooxParcours(path) : scrollOf(path);
        LinearLayout pathHeader = Ui.row(this);
        pathHeader.setGravity(Gravity.CENTER_VERTICAL);
        pathHeader.addView(Ui.iconButton(this, "‹", "Retour", v -> showHome()));
        TextView pathTitle = Ui.bookText(this, "Parcours Hifz", 24, true);
        if (wideBoox()) pathTitle.setTextSize(22f);
        pathTitle.setGravity(Gravity.CENTER);
        Ui.weight(pathTitle, 1f);
        pathHeader.addView(pathTitle);
        pathHeader.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        path.addView(pathHeader);
        path.addView(Ui.divider(this));
        backlogParcoursRow = backlogRow();
        if(!wideBoox())path.addView(backlogParcoursRow);
        // The full weekly planner follows the six large pictograms. This direct
        // text link makes it reachable in one tap without shrinking those icons.
        TextView jumpToWeek = Ui.bookText(this, "PLANNING DE LA SEMAINE ↓", 16f, true);
        if (wideBoox()) jumpToWeek.setTextSize(20f);
        jumpToWeek.setPadding(Ui.dp(this, 5), Ui.dp(this, wideBoox() ? 6 : 12), Ui.dp(this, 5), Ui.dp(this, wideBoox() ? 6 : 12));
        jumpToWeek.setContentDescription("Aller au planning de la semaine");
        jumpToWeek.setClickable(true);
        jumpToWeek.setFocusable(true);
        jumpToWeek.setOnClickListener(v ->
            parcoursScroll.post(() -> parcoursScroll.scrollTo(0, dashboardTitle.getTop())));
        if(!wideBoox())path.addView(jumpToWeek);
        // Hifz semantic grammar: paired by the real corpus and operation.
        // Sabqi: introduce one set, then cumulatively revisit it.
        path.addView(pathSectionTitle("SABQI"));
        LinearLayout learningModes = Ui.row(this);
        LinearLayout sabqi = pathTile(learningModes, "Apprentissage", "1 page/semaine",
            v -> openMode(HifzSessionActivity.SABQI));
        LinearLayout renforcement = pathTile(learningModes, "Renforcement", "Sabqi",
            v -> openMode(renforcementQuickAccessMode()));
        if(wideBoox())path.addView(learningModes,new LinearLayout.LayoutParams(-1,0,1f));
        else path.addView(learningModes);

        // Itqan: one bounded memorization unit, then its accumulated units.
        path.addView(pathSectionTitle("ITQĀN"));
        LinearLayout otherModes = Ui.row(this);
        itqanCard = pathTile(otherModes, "Stabilisation", stabilizationProgramCue(),
            v -> openMode(HifzSessionActivity.ITQAN));
        // BOOX: understated future-regime hint directly below the original
        // pictogram. Nothing added to the six-icon hierarchy; no tinted card.
        TextView maintienCue = Ui.text(this, "(Maintien)", 14f, false);
        if(wideBoox())maintienCue.setTextSize(14f);
        maintienCue.setGravity(Gravity.CENTER);
        maintienCue.setTextColor(Ui.MUTED);
        maintienCue.setPadding(0,Ui.dp(this,3),0,0);
        itqanCard.addView(maintienCue);
        LinearLayout itqan = itqanCard;
        LinearLayout consolidation = pathTile(otherModes, "Consolidation", "Itqān",
            v -> openMode(consolidationQuickAccessMode()));
        consolidationCard = consolidation; // Same action and immutable original icon.
        if(wideBoox())path.addView(otherModes,new LinearLayout.LayoutParams(-1,0,1f));
        else path.addView(otherModes);

        // Same acquired corpus, two modalities: text masked vs text visible.
        path.addView(pathSectionTitle("RÉVISION"));
        LinearLayout reviewModes = Ui.row(this);
        LinearLayout activeRevision = pathTile(reviewModes, "Révision active", "Récitation masquée",
            v -> openMode(HifzSessionActivity.MURAJAAH_ACTIVE));
        LinearLayout passiveRevision = pathTile(reviewModes, "Révision passive", "Texte visible",
            v -> openMode(HifzSessionActivity.MURAJAAH));
        if(wideBoox())path.addView(reviewModes,new LinearLayout.LayoutParams(-1,0,1f));
        else path.addView(reviewModes);
        sabqiQuickAccess = sabqi;
        itqanQuickAccess = itqan;
        geometryActions.add(parcours);
        geometryActions.add(sabqi);
        geometryActions.add(itqan);
        geometryActions.add(activeRevision);
        geometryActions.add(passiveRevision);
        geometryActions.add(renforcement);
        geometryActions.add(consolidation);
        setGeometryActionsEnabled(false);

        dashboardTitle = Ui.bookText(this, "7 prochains jours", 20, true);
        if (wideBoox()) dashboardTitle.setTextSize(18f);
        dashboardTitle.setPadding(Ui.dp(this, 4), Ui.dp(this, wideBoox()?5:14), 0, Ui.dp(this, 3));
        dashboardTitle.setOnClickListener(v -> {
            if(weekRows.isEmpty()) refreshDashboard();
            if(wideBoox())showBooxDayDetails();
            else parcoursScroll.post(() -> ((ScrollView)parcoursScroll).smoothScrollTo(0,dashboardTitle.getTop()));
        });
        dashboardTitle.setContentDescription("Voir le calendrier sur sept jours");
        path.addView(dashboardTitle);
        dashboard = Ui.column(this);
        dashboard.setPadding(0, 0, 0, 0);
        weekStrip = Ui.row(this);
        weekDayPanel = Ui.column(this);
        weekDayPanel.setPadding(Ui.dp(this, 6), Ui.dp(this, wideBoox()?2:14), Ui.dp(this, 6), 0);
        path.addView(dashboard);

        showHome();

        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                prefs.reconcileV6AcquiredBootstrap(loaded);
                prefs.repairStraddlingAcquiredVerses(loaded);
                prefs.releaseRevisionCursorsFromFatiha();
                prefs.currentAnchoringEntry(loaded);
                // Rebase only old 1.17.7 pending post-final plans whose previous
                // pages came from global Acquis. No Hifz credits are changed.
                try {
                    postFinalStore.repairLegacyPendingFamilyPlans(
                        loaded,prefs.progressionSnapshotV6().acquired);
                } catch(RuntimeException incompleteLegacyProof) {
                    // Optional recall repair may fail closed; it must never
                    // disable the core Hifz dashboard or alter acquired credits.
                    android.util.Log.e("QuranHifz","Legacy post-final recall needs proof",
                        incompleteLegacyProof);
                }
                // Historical missing cumulative readings are not recreated at
                // startup. Only separately prepared future readings are resumed.
                geometry = loaded;
                runOnUiThread(() -> {
                    // shutdownNow cannot remove a completion already queued on
                    // the Android main thread. A closed home must not refresh
                    // from stale preferences (e.g. after restore or teardown).
                    if (isFinishing() || isDestroyed()) return;
                    setTodayEnabled(true);
                    setGeometryActionsEnabled(true);
                    ledger.capture(prefs);
                    refreshAll();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    setToday("Parcours indisponible", false);
                    setGeometryActionsEnabled(false);
                });
            }
        });
    }

    private void setGeometryActionsEnabled(boolean enabled) {
        for (View action : geometryActions) setLineEnabled(action, enabled);
    }

    /** A plain line greys out when it cannot be opened (no card to dim). */
    private static void setLineEnabled(View line, boolean enabled) {
        if (line == null) return;
        line.setEnabled(enabled);
        // Disabled items retain readable high contrast on E-Ink.
        // Their touch target is disabled, without fading the text into the paper.
        line.setAlpha(1f);
    }

    private LinearLayout page() {
        LinearLayout root = Ui.column(this);
        int side = Ui.dp(this, 16);
        root.setPadding(side, Ui.dp(this, 4), side, Ui.dp(this, 16));
        return root;
    }

    /**
     * True fixed BOOX display, NOT a ScrollView whose content overflows invisibly.
     * Three tile rows each get an equal share of remaining height. All secondary
     * information remains accessible from the seven-day strip via dialogs.
     */
    private FrameLayout fixedBooxParcours(LinearLayout root) {
        FrameLayout viewport=new FrameLayout(this);
        viewport.setBackgroundColor(Ui.PAPER);
        viewport.addView(root,new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
        Ui.respectSystemBars(this,viewport,0,0,0,0);
        return viewport;
    }

    /** A single true full-width portrait column; fillViewport expands height, never constrains scroll. */
    private ScrollView scrollOf(LinearLayout root) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.PAPER);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(root, new ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.respectSystemBars(this, scroll, 0, 0, 0, 0);
        return scroll;
    }

    /** One dominant BOOX-safe action, preserving the existing openToday() routing. */
    private LinearLayout todayLine() {
        LinearLayout line = Ui.column(this);
        line.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        // Typographic reading surface, not a bordered card.
        todayHeading = Ui.bookText(this, "AUJOURD’HUI", 17f, true);
        if (wideBoox()) todayHeading.setTextSize(22f);
        line.addView(todayHeading);
        TextView value = Ui.bookText(this, "…", 23f, true);
        if (wideBoox()) value.setTextSize(30f);
        value.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        value.setMaxLines(2);
        line.addView(value);
        todayObjective = Ui.text(this, "", 16f, false);
        if (wideBoox()) todayObjective.setTextSize(21f);
        todayObjective.setTextColor(Ui.MUTED);
        // Never truncate the authentic page/verse range on the BOOX screen.
        todayObjective.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, wideBoox() ? 3 : 6));
        todayObjective.setMaxLines(3);
        todayObjective.setEllipsize(android.text.TextUtils.TruncateAt.END);
        line.addView(todayObjective);
        TextView action = Ui.bookText(this, "CONTINUER  ›", 20f, true);
        if (wideBoox()) action.setTextSize(24f);
        action.setGravity(Gravity.END);
        line.addView(action);
        line.setMinimumHeight(Ui.dp(this, 120));
        line.setContentDescription("Ouvrir la séance du jour");
        line.setClickable(true);
        line.setFocusable(true);
        line.setOnClickListener(v -> openToday());
        setLineEnabled(line, false);
        return line;
    }


    /** Full-size BOOX pictograms. Small action icons and their tint contract remain in Ui. */
    private static int largeIconFor(String role) {
        if ("Accueil".equals(role)) return R.drawable.ic_haafidh_accueil;
        if ("Parcours".equals(role) || "Parcours Hifz".equals(role)) return R.drawable.ic_haafidh_parcours;
        if ("Lecture".equals(role)) return R.drawable.ic_haafidh_lecture;
        if ("Atlas".equals(role)) return R.drawable.ic_haafidh_atlas;
        if ("Progression".equals(role)) return R.drawable.ic_haafidh_progression;
        if ("Apprentissage".equals(role)) return R.drawable.ic_haafidh_apprentissage;
        if ("Renforcement".equals(role)) return R.drawable.ic_haafidh_renforcement;
        if ("Stabilisation".equals(role)) return R.drawable.ic_haafidh_stabilisation;
        if ("Consolidation".equals(role)) return R.drawable.ic_haafidh_consolidation;
        if ("Révision active".equals(role)) return R.drawable.ic_haafidh_revision_active;
        if ("Révision passive".equals(role)) return R.drawable.ic_haafidh_revision_passive;
        return 0;
    }

    /** BOOX typography responds to physical tablet proportions, not to phone font sizes. */
    private boolean wideBoox() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /** BOOX-only text hierarchy for Hifz group labels; leaves modes and icons untouched. */
    private TextView pathSectionTitle(String caption) {
        TextView heading = Ui.bookText(this, caption, 16f, true);
        if (wideBoox()) heading.setTextSize(18f);
        heading.setPadding(Ui.dp(this, 4), Ui.dp(this, wideBoox() ? 1 : 9), 0, Ui.dp(this, 2));
        return heading;
    }

    /** Size from available BOOX display height, not tablet width: one-screen Hifz. */
    private int booxPathIconDp() {
        int heightDp=getResources().getConfiguration().screenHeightDp;
        if(heightDp<=0)heightDp=800;
        return Math.max(48,Math.min(96,(heightDp-270)/5));
    }

    /** Keep the pictograms compact enough for the calendar and session controls on BOOX. */
    private int booxIconDp(int columns, int maxDp) {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        if (widthDp <= 0) widthDp = 360;
        int responsiveCapDp = widthDp >= 600 ? maxDp + 36 : maxDp;
        return Math.max(62, Math.min(responsiveCapDp, (widthDp - 48) / columns - 14));
    }

    /** Four equal-size home destinations in a 2×2 BOOX grid, without cards or separators. */
    private LinearLayout primaryTile(LinearLayout parent, String role, String display,
                                    View.OnClickListener listener) {
        LinearLayout tile = Ui.column(this);
        tile.setPadding(Ui.dp(this, 3), Ui.dp(this, 12), Ui.dp(this, 3), Ui.dp(this, 12));
        tile.setGravity(Gravity.CENTER);
        int sideDp = booxIconDp(2, 112);
        tile.setMinimumHeight(Ui.dp(this, sideDp + 22));
        int drawable = largeIconFor(role);
        if (drawable == 0) drawable = Ui.iconFor(role, role);
        if (drawable != 0) {
            ImageView icon = new ImageView(this);
            icon.setImageResource(drawable);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            // One accessible label belongs to the complete tile, not the decorative image.
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            icon.setPadding(0, 0, 0, 0);
            tile.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, sideDp), Ui.dp(this, sideDp)));
        }
        // No labels beneath the four large home pictograms, per the BOOX
        // memory-palace interface contract. Keep the full spoken destination.
        tile.setContentDescription(display);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        parent.addView(tile, lp);
        return tile;
    }

    /** Borderless Hifz ribbon with frozen pictograms; child 1 remains live cue state. */
    private LinearLayout pathTile(LinearLayout parent, String label, String state,
                                 View.OnClickListener listener) {
        LinearLayout tile = Ui.column(this);
        tile.setPadding(Ui.dp(this, 2), Ui.dp(this, wideBoox() ? 4 : 8), Ui.dp(this, 2), Ui.dp(this, wideBoox() ? 4 : 8));
        tile.setGravity(Gravity.CENTER);
        int sideDp = wideBoox()?booxPathIconDp():booxIconDp(2, 124);
        if(wideBoox()){
            // Monochrome E-Ink tile: stroke instead of shadow or coloured fill.
            android.graphics.drawable.GradientDrawable tileBg=
                new android.graphics.drawable.GradientDrawable();
            tileBg.setColor(Ui.SURFACE);
            tileBg.setCornerRadius(Ui.dp(this,10));
            tileBg.setStroke(Ui.dp(this,1),Ui.LINE);
            tile.setBackground(tileBg);
            tile.setMinimumHeight(0); // weighted row owns exact viewport allocation
        } else tile.setMinimumHeight(Ui.dp(this,sideDp+52));
        String shortLabel = "Apprentissage".equals(label) ? "Apprendre"
            : "Stabilisation".equals(label) ? "Stabiliser"
            : "Révision".equals(label) ? "Réviser" : label;
        TextView name = Ui.bookText(this, shortLabel, 19f, true);
        if (wideBoox()) name.setTextSize(19f);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        int icon = largeIconFor(label);
        if (icon == 0) icon = Ui.iconFor(label, label);
        if (icon == 0) throw new IllegalArgumentException("Unmapped Hifz mode: "+label);
        // Intrinsic drawable size is only 24dp; explicit bounds keep it legible on BOOX.
        Drawable architectural = getDrawable(icon).mutate();
        architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp));
        tile.addView(name);
        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(architectural);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams iconLayout = wideBoox()
            ?new LinearLayout.LayoutParams(Ui.dp(this,sideDp),0,1f)
            :new LinearLayout.LayoutParams(Ui.dp(this,sideDp),Ui.dp(this,sideDp));
        iconLayout.topMargin=Ui.dp(this,wideBoox()?1:6);
        tile.addView(iconView,iconLayout);
        // The real target/verse range is on the selected day, not crammed into a 1/3-width cell.
        TextView cue = Ui.text(this, state, 15f, false);
        cue.setVisibility(View.GONE);
        tile.addView(cue);
        // No visible caption beneath Hifz pictograms; the title is ABOVE
        // the icon and a semantic subtitle remains available to accessibility.
        tile.setContentDescription(label+" · "+pathSubtitle(label)+" · "+state);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Ui.dp(this,wideBoox()?4:2),Ui.dp(this,wideBoox()?2:0),
            Ui.dp(this,wideBoox()?4:2),Ui.dp(this,wideBoox()?2:0));
        parent.addView(tile,lp);
        return tile;
    }

    /** Clear, stable vocabulary: the selected-day panel carries all actual verse limits. */
    private static String pathSubtitle(String label) {
        switch (label) {
            case "Apprentissage": return "Nouvelle leçon";
            case "Renforcement": return "Révision cumulative Sabqi";
            case "Stabilisation": return "Répétitions approfondies";
            case "Consolidation": return "Révision cumulative Itqān";
            case "Révision active": return "Texte masqué";
            case "Révision passive": return "Texte visible";
            default: return "";
        }
    }

    /** Today's dominant action displays the canonical next due session. */
    private void setToday(String text, boolean enabled) {
        today.setText(text);
        // The single hero CTA always describes its actual destination.
        if (todayObjective != null) {
            // One read-only preview of actual schedule objectives, never a credit or cursor advance.
            todayObjective.setText(text.startsWith("Report ")
                ? "PRIORITÉ · séance datée à rattraper"
                : "Sélectionnez la séance pour les détails");
            todayObjective.setContentDescription(todayRow==null ? "Programme détaillé dans Parcours" : scheduleDetail(todayRow.morning));
        }
        if (todayAction != null && todayAction.getChildCount() >= 4
                && todayAction.getChildAt(3) instanceof TextView) {
            TextView action = (TextView) todayAction.getChildAt(3);
            action.setText(text.startsWith("Consolidation · état") ? "VÉRIFIER  ›"
                : text.contains("à reprendre") ? "REPRENDRE  ›"
                : text.contains("Programme à jour") ? "VOIR LES SÉANCES  ›" : "CONTINUER  ›");
        }
        setTodayEnabled(enabled);
    }

    private void setTodayEnabled(boolean enabled) {
        setLineEnabled(todayAction, enabled);
    }

    private void showHome() {
        showingParcours = false;
        setContentView(homeScroll);
    }

    private void showParcours() {
        showingParcours = true;
        setContentView(parcoursScroll);
    }

    @Override public void onBackPressed() {
        if (showingParcours) { showHome(); return; }
        super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        prefs = new HifzPrefs(this);
        speedStore = new HifzSpeedStore(this);
        if (ledger == null) ledger = new DashboardLedger(this);
        ledger.capture(prefs);
        TextView itqanCue = Ui.settingValue(itqanCard);
        if (itqanCue != null) itqanCue.setText(stabilizationProgramCue());
        if (today != null && geometry != null) refreshAll();
    }

    /** Only change text; retain the original fixed pictogram and click destination. */
    private void refreshItqanCardVocabulary() {
        if(consolidationCard==null || prefs==null) return;
        boolean afterNas=prefs.itqanPostNasMaintenance();
        // Child order: title, pictogram, invisible cue, subtitle.
        if(itqanCard!=null){
            if(itqanCard.getChildAt(0) instanceof TextView)
                ((TextView)itqanCard.getChildAt(0)).setText(afterNas?"Maintien":"Stabiliser");
            if(itqanCard.getChildCount()>3 && itqanCard.getChildAt(3) instanceof TextView){
                TextView prochain=(TextView)itqanCard.getChildAt(3);
                prochain.setText("(Maintien)");
                prochain.setVisibility(afterNas?View.GONE:View.VISIBLE);
            }
            itqanCard.setContentDescription(afterNas
                ?"Maintien · deuxième régime Itqān"
                :"Stabiliser · Premier passage ; ensuite Maintien après An-Nās");
        }
        if(consolidationCard.getChildAt(0) instanceof TextView)
            ((TextView)consolidationCard.getChildAt(0)).setText("Consolidation");
        String subtitle=afterNas?"Maintiens de la semaine · masqués":"Révision cumulative Itqān";
        if(consolidationCard.getChildCount()>3 && consolidationCard.getChildAt(3) instanceof TextView)
            ((TextView)consolidationCard.getChildAt(3)).setText(subtitle);
        consolidationCard.setContentDescription("Consolidation · "+subtitle);
    }

    private void refreshAll() { refreshItqanCardVocabulary(); refreshBacklog(); refreshQuickAccessCadenceGating(); refreshDashboard(); refreshToday(); refreshRecentSabqiAdvisory(); refreshStabilizationPosition(); }

    /** BOOX-only compact text cue; never another oversized competing pictogram. */
    private TextView backlogRow() {
        TextView view=Ui.bookText(this,"",17f,true);
        view.setTextColor(Ui.INK);
        view.setPadding(Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12));
        view.setMinimumHeight(Ui.dp(this,52));
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_hifz_catchup_clock,0,0,0);
        view.setCompoundDrawablePadding(Ui.dp(this,8));
        view.setVisibility(View.GONE);
        view.setOnClickListener(v->showBacklog());
        return view;
    }

    private void refreshBacklog() {
        if(geometry==null)return;
        backlogPending=HifzBacklog.pending(prefs,ledger,HifzClock.today(),effectiveLearningDaysPerWeek());
        int count=HifzBacklog.lateCount(backlogPending,HifzClock.today());
        String summary=count+" séance"+(count>1?"s":"")+" non effectuée"+(count>1?"s":"")+" · contenu décalé  ›";
        for(TextView label:new TextView[]{backlogHomeRow,backlogParcoursRow}) {
            if(label==null)continue;
            // The home has no missed-session counter: the next lesson simply shifts.
            if(label==backlogHomeRow) {
                label.setVisibility(View.GONE);
                label.setText("");
                label.setContentDescription("");
                continue;
            }
            label.setVisibility(count==0?View.GONE:View.VISIBLE);
            label.setText(count==0?"":summary);
            label.setContentDescription(count+" créneaux non effectués ; les leçons avancent au prochain jour de leur discipline");
        }
    }

    private void showBacklog() {
        int overdue=HifzBacklog.lateCount(backlogPending,HifzClock.today());
        if(overdue==0)return;
        // Historical misses are attendance facts, not actionable extra sessions.
        // A missed family remains at its physical cursor until its next own weekday.
        int visible=Math.min(overdue,24);
        StringBuilder details=new StringBuilder(
            "Aucune séance supplémentaire n’est imposée. Les lignes non travaillées "
            +"restent au prochain créneau Sabqi ou Itqān, sans modifier l’autre discipline.\n\n");
        for(int i=0;i<visible;i++) {
            HifzBacklog.Due missed=backlogPending.get(i);
            details.append(missed.date).append(" · ").append(missed.frenchName())
                .append(" · non effectuée\n");
        }
        if(overdue>visible)details.append("… et ").append(overdue-visible).append(" autres");
        new android.app.AlertDialog.Builder(this)
            .setTitle("Créneaux manqués · "+overdue)
            .setMessage(details.toString())
            .setPositiveButton("Fermer",null).show();
    }

    /** Describes the selected weekly plan, never a stale fixed 22-line budget. */
    private String stabilizationProgramCue() {
        return prefs.itqanEntretienActive() ? "1 hizb · répétitions"
            : "2 pages/semaine";
    }

    /** Option 7B: where the Stabilisation rotation stands, e.g. "Hizb 52 · 3/61 séances". */
    private void refreshStabilizationPosition() {
        TextView cue = Ui.settingValue(itqanCard);
        if (cue == null || geometry == null) return;
        // The Parcours menu contains navigation labels, not engine diagnostics.
        // Position, cursor, repetitions and block geometry stay inside Itqan.
        cue.setText(stabilizationProgramCue());
        // Keep the full live programme accessible without overcrowding the visible subtitle.
        itqanCard.setContentDescription(
            (prefs.itqanPostNasMaintenance()?"Maintien":"Stabilisation")
                +" · "+cue.getText()
                +(prefs.itqanPostNasMaintenance()?"":" · Ensuite : Maintien après An-Nās"));
    }

    /**
     * Apprentissage/Stabilisation quick-access must respect the weekday-pinned cadence (Settings'
     * configurable Apprentissage-days-per-week split, Mon/Wed/Fri by default) — opening tomorrow's
     * Apprentissage today would let a learner get ahead of the weekly snowball attribution it's
     * built on. Révision/Renforcement/Consolidation stay free since they need to be
     * testable/catchable-up any day.
     */
    /** P4: the Roadmap's own automatic ratio for today, when active, otherwise the user's manual
     *  learningDaysPerWeek setting — see HifzPrefs.currentRoadmapDecision for exactly when the
     *  automatic ratio applies (Phase.CURRENT only, for now). */
    private int effectiveLearningDaysPerWeek() {
        RoadmapPolicy.Decision decision = geometry != null ? prefs.currentRoadmapDecision(geometry) : null;
        return decision != null ? decision.learningDays : prefs.learningDaysPerWeek();
    }

    private void refreshQuickAccessCadenceGating() {
        if (geometry == null) return;
        LocalDate today=HifzClock.today();
        // Only today's unvalidated own-family slot is actionable. A skipped
        // Tuesday never steals Wednesday Sabqi or adds a Thursday catch-up.
        HifzBacklog.Due slot=HifzBacklog.onDate(backlogPending,today);
        setLineEnabled(sabqiQuickAccess,slot!=null && HifzSessionActivity.SABQI.equals(slot.mode));
        setLineEnabled(itqanQuickAccess,slot!=null && HifzSessionActivity.ITQAN.equals(slot.mode));
    }

    /** The Révision card opens this compact selector rather than jumping straight to a mode:
        Révision active and Révision passive can now be done in any order. */
    private void showRevisionSelector() {
        RevisionSelector.show(this, prefs, geometry, speedStore, new RevisionSelector.Choice() {
            @Override public void openActiveRevision() { openMode(HifzSessionActivity.MURAJAAH_ACTIVE); }
            @Override public void openPassiveRevision() { openMode(HifzSessionActivity.MURAJAAH); }
        });
    }

    private void openMode(String mode) { openMode(mode,HifzClock.today()); }

    private void openMode(String mode,LocalDate scheduledDate) {
        if (HifzClock.today().getDayOfWeek()==DayOfWeek.SUNDAY
                && (HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode)
                    || HifzSessionActivity.MURAJAAH.equals(mode))) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Dimanche sans révision")
                .setMessage("Aucune Révision active ou Révision passive n’est programmée le dimanche. Consultez les séances Itqān et les finales du jour.")
                .setPositiveButton("Fermer",null).show();
            return;
        }
        if (HifzSessionActivity.SABQI.equals(mode) || HifzSessionActivity.ITQAN.equals(mode)) {
            // Date/weekday and durable completion receipts gate real work;
            // old misses stay recorded but do not authorize retroactive credits.
            LocalDate today=HifzClock.today();
            List<HifzBacklog.Due> slots=HifzBacklog.pending(prefs,ledger,
                today,effectiveLearningDaysPerWeek());
            HifzBacklog.Due slot=HifzBacklog.onDate(slots,today);
            if(slot==null || !slot.mode.equals(mode)
                    || !today.equals(scheduledDate)) {
                new android.app.AlertDialog.Builder(this).setTitle("Séance non prévue")
                    .setMessage("La prochaine leçon est proposée au créneau normal "
                        +"Sabqi ou Itqān. Aucune ancienne séance ne sera créditée.")
                    .setPositiveButton("Fermer",null).show();
                return;
            }
        }
        Intent intent=new Intent(this,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        if(scheduledDate!=null)intent.putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,scheduledDate.toString());
        startActivity(intent);
    }

    private boolean modeComplete(LocalDate date,String mode){
        return ledger.find(date,mode)!=null;
    }

    /**
     * Reported directly: these two direct-access tiles always opened the non-graduating evening
     * snowball review (LEARNING_CONSOLIDATION/RECENT_SABQI_REVIEW), even on the Sunday the real
     * finale (LEARNING_FINAL/CONSOLIDATION_FINAL — the only thing that ever graduates material to
     * Acquis and unlocks it for Révision passive) is due. A learner using these shortcuts instead of
     * "Aujourd'hui" could complete every review faithfully and still never graduate a single line,
     * with no error or warning — confirmed on a live export where recentSabqi/promotedRanges had
     * accumulated unpromoted material since day one. Mirror nextMode's own Sunday-due check so
     * these shortcuts reach the same finale "Aujourd'hui" would have sequenced them into.
     */
    private String renforcementQuickAccessMode(){
        LocalDate today=HifzClock.today();
        if(prefs.unfinishedConsolidationSessionDate(
                ConsolidationCycleEngine.Family.LEARNING,"learning-")!=null)
            return HifzSessionActivity.LEARNING_CONSOLIDATION;
        if(prefs.unfinishedConsolidationSessionDate(
                ConsolidationCycleEngine.Family.LEARNING,"learning-final-")!=null)
            return HifzSessionActivity.LEARNING_FINAL;
        CadenceAction action=WeeklyProgramPolicy.actionFor(today.getDayOfWeek(),prefs.weeklyProgramFor(today),effectiveLearningDaysPerWeek());
        return today.getDayOfWeek()==DayOfWeek.SUNDAY && !learningFinalResolved(today)
            ? HifzSessionActivity.LEARNING_FINAL : HifzSessionActivity.LEARNING_CONSOLIDATION;
    }

    /** See renforcementQuickAccessMode: the Consolidation-track counterpart. */
    private String consolidationQuickAccessMode(){
        LocalDate today=HifzClock.today();
        if(prefs.unfinishedConsolidationSessionDate(
                ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-")!=null)
            return HifzSessionActivity.CONSOLIDATION_FINAL;
        // A previously OPEN ×7 evening outranks starting a NEW Sunday finale.
        // Direct access cannot silently bypass the evening's earned receipt.
        // HifzSessionActivity restores that evening's original frozen date.
        if(prefs.unfinishedConsolidationSessionDate(
                ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-")!=null)
            return HifzSessionActivity.RECENT_SABQI_REVIEW;
        // The maintenance tile must lead to the hizb ×3, not a non-existent
        // post-An-Nās Consolidation finale. Existing pre-An-Nās OPEN finals
        // were checked above and always retain their original date/protocol.
        if(prefs.itqanPostNasMaintenance())return HifzSessionActivity.RECENT_SABQI_REVIEW;
        CadenceAction action=WeeklyProgramPolicy.actionFor(today.getDayOfWeek(),prefs.weeklyProgramFor(today),effectiveLearningDaysPerWeek());
        return today.getDayOfWeek()==DayOfWeek.SUNDAY && !prefs.itqanPostNasMaintenance()
                && !consolidationFinalResolved(today)
            ? HifzSessionActivity.CONSOLIDATION_FINAL : HifzSessionActivity.RECENT_SABQI_REVIEW;
    }

    /**
     * A past Sunday's weekly snowball final review can never be caught up later — by the time it
     * would be revisited, the accumulator has already rolled to a new week — so once a Sunday is
     * no longer today it is treated as satisfied rather than stalling every later cadence day.
     */
    /** Recommended Sunday contains finals only; no seventh ordinary lesson. */
    private boolean recommendedSunday(LocalDate date) {
        return date.getDayOfWeek()==DayOfWeek.SUNDAY
            && prefs.weeklyProgramFor(date)!=HifzPrefs.WeeklyProgram.CLASSIC;
    }

    private boolean cadenceComplete(LocalDate date){
        if(recommendedSunday(date)){
            // Sunday is reserved for finals; a missed ordinary Sunday Itqan must
            // not keep Monday's six-day calendar in backlog forever.
            return !date.equals(HifzClock.today())
                || (learningFinalResolved(date)
                    && (prefs.itqanPostNasMaintenance() || consolidationFinalResolved(date)));
        }
        CadenceAction action=WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),prefs.weeklyProgramFor(date),effectiveLearningDaysPerWeek());
        switch(action){
            case LEARNING:return modeComplete(date,HifzSessionActivity.SABQI);
            case STABILIZATION:return modeComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:
                if(!date.equals(HifzClock.today()))return true;
                return learningFinalResolved(date)
                    && (prefs.itqanPostNasMaintenance() || consolidationFinalResolved(date));
            default:return false;
        }
    }

    private ScheduledCadence nextDueCadence(LocalDate todayDate){
        // Follow the real calendar, NOT the oldest historical missed date.
        // Each family carries its physical cursor forward into its next eligible
        // slot; today's receipt is dated today. Sunday finals can therefore
        // legitimately contain fewer than 15 Sabqi or 22 Itqān weekly lines.
        if(todayDate.isBefore(prefs.programStartDate()) || cadenceComplete(todayDate))return null;
        return WeeklyProgramPolicy.nextDue(prefs,todayDate,todayDate,
            java.util.Collections.emptySet(),effectiveLearningDaysPerWeek());
    }

    /** Done, or nothing to review this week (rare, e.g. a brand-new install's first week). */
    private boolean learningFinalResolved(LocalDate date){
        return date.toString().equals(prefs.lastLearningConsolidationDate())
            || prefs.learningSnowballFinalUnits(date).isEmpty();
    }

    private boolean consolidationFinalResolved(LocalDate date){
        return date.toString().equals(prefs.lastRecentSabqiReviewDate())
            || prefs.stabilizationSnowballFinalUnits(date).isEmpty();
    }

    /** Evening Renforcement and Consolidation never re-schedule the morning revisions. */
    private String eveningLearningMode(LocalDate today){
        return prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,today)
            ?HifzSessionActivity.LEARNING_CONSOLIDATION:null;
    }

    private String eveningStabilizationMode(LocalDate today){
        return prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,today)
            ?HifzSessionActivity.RECENT_SABQI_REVIEW:null;
    }

    private String nextMode(ScheduledCadence due){
        consolidationNeedsAttention=false;
        learningConsolidationNeedsAttention=false;
        try{
            return computeNextMode(due);
        }catch(RuntimeException error){
            consolidationNeedsAttention=true;
            learningConsolidationNeedsAttention=true;
            android.util.Log.e("QuranHifz","Unable to evaluate next Hifz mode",error);
            return null;
        }
    }

    private String computeNextMode(ScheduledCadence due){
        LocalDate today=HifzClock.today();
        // Review still appears when all dated lessons are complete for the day.
        if(due==null) {
            if(today.getDayOfWeek()==DayOfWeek.SUNDAY)return null;
            if(!today.toString().equals(prefs.lastActiveMurajaahDate()))
                return HifzSessionActivity.MURAJAAH_ACTIVE;
            if(!today.toString().equals(prefs.lastMurajaahDate()))
                return HifzSessionActivity.MURAJAAH;
            return null;
        }
        LocalDate date=due.getScheduledDate();
        boolean isToday=date.equals(today);
        // Both independent reviews belong to 05:30, ahead of today's 20:00 work.
        // An older dated lesson stays prioritaire: isToday=false for carry-over.
        if(isToday && today.getDayOfWeek()!=DayOfWeek.SUNDAY) {
            if(!date.toString().equals(prefs.lastActiveMurajaahDate()))
                return HifzSessionActivity.MURAJAAH_ACTIVE;
            if(!date.toString().equals(prefs.lastMurajaahDate()))
                return HifzSessionActivity.MURAJAAH;
        }
        if(recommendedSunday(date)){
            // Older Sunday finals are resumed via their original OPEN snapshots.
            if(!isToday)return null;
            if(!learningFinalResolved(date))return HifzSessionActivity.LEARNING_FINAL;
            if(!prefs.itqanPostNasMaintenance() && !consolidationFinalResolved(date))
                return HifzSessionActivity.CONSOLIDATION_FINAL;
            return null;
        }
        switch(due.getAction()){
            case LEARNING:
                if(!modeComplete(date,HifzSessionActivity.SABQI))return HifzSessionActivity.SABQI;
                return isToday?eveningLearningMode(today):null;
            case STABILIZATION:
                if(!modeComplete(date,HifzSessionActivity.ITQAN))return HifzSessionActivity.ITQAN;
                return isToday?eveningStabilizationMode(today):null;
            case REVISION:
                if(!isToday)return null;
                if(!learningFinalResolved(today))return HifzSessionActivity.LEARNING_FINAL;
                if(!prefs.itqanPostNasMaintenance() && !consolidationFinalResolved(today))
                    return HifzSessionActivity.CONSOLIDATION_FINAL;
                return null; // No active/passive review on Sunday in EITHER programme.
            default:return null;
        }
    }

    private static boolean isTodayAnchoredMode(String mode){
        return HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ||HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode)
            ||HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode)
            ||HifzSessionActivity.LEARNING_FINAL.equals(mode)
            ||HifzSessionActivity.MURAJAAH.equals(mode)
            ||HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode);
    }

    private void openToday() {
        LocalDate current=HifzClock.today();
        // Frozen prior-day work is always accessible, even if a cadence check fails.
        // This allows the 25 October Sunday finals to be resumed on Monday 26 without
        // overwriting their original dated receipts or forcing an unrelated lesson.
        List<TodaySession> unfinished=unfinishedWeekendSessions(current);
        if(!unfinished.isEmpty()) {
            try {
                for(TodaySession row:todaySessions(current,null,null))
                    addToday(unfinished,row.label,row.mode,row.scheduled,row.done,null);
            } catch(RuntimeException unavailable) {
                android.util.Log.w("QuranHifz","Today's preview unavailable during frozen recovery",unavailable);
            }
            showTodayWindow(unfinished);
            return;
        }
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention||learningConsolidationNeedsAttention){
            startActivity(new Intent(this,SettingsActivity.class));
            return;
        }
        // A fully validated day must remain inspectable: the old disabled “Programme à jour”
        // prevented the learner from seeing the real session list and times afterwards.
        if(mode==null){
            List<TodaySession> completed;
            try { completed=todaySessions(current,null,null); }
            catch(RuntimeException error){ completed=new ArrayList<>(); }
            if(completed.isEmpty()){
                new android.app.AlertDialog.Builder(this).setTitle("Aujourd’hui")
                    .setMessage("Aucune séance programmée.").setPositiveButton("Fermer",null).show();
            }else showTodayWindow(completed);
            return;
        }
        LocalDate scheduled=isTodayAnchoredMode(mode)?current:due.getScheduledDate();
        List<TodaySession> sessions;
        try{ sessions=todaySessions(current,mode,scheduled); }
        catch(RuntimeException error){ sessions=new ArrayList<>(); }
        // Always show the destination before navigating, even for a single session.
        if(sessions.isEmpty()){
            new android.app.AlertDialog.Builder(this).setTitle("Aujourd’hui")
                .setMessage("Aucune séance disponible.").setPositiveButton("Fermer",null).show();
            return;
        }
        showTodayWindow(sessions);
    }

    /** Incomplete dated sessions remain open across Sunday/Monday boundaries.
     * This is navigation only: no new credit, replay, migration or week replan.
     * Finale Itqan is shown after the regime switch ONLY if already frozen OPEN. */
    private List<TodaySession> unfinishedWeekendSessions(LocalDate today) {
        ArrayList<TodaySession> out=new ArrayList<>();
        String learningEvening=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING,"learning-");
        if(learningEvening!=null) {
            LocalDate dated=LocalDate.parse(learningEvening);
            if(!dated.isAfter(today))
                addToday(out,"Reprendre Renforcement Sabqi du "+dated+" · soir",
                    HifzSessionActivity.LEARNING_CONSOLIDATION,dated,false,null);
        }
        // A frozen Itqan evening has its own slot and original calendar date,
        // just like Renforcement Sabqi. Without this entry, "Aujourd'hui"
        // silently hides it after midnight even though tapping the separate
        // Consolidation tile can still reopen it. Keep the original date and
        // do NOT replan, auto-close or award repetitions.
        String itqanEvening=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-");
        if(itqanEvening!=null) {
            LocalDate dated=LocalDate.parse(itqanEvening);
            if(!dated.isAfter(today))
                addToday(out,"Reprendre Consolidation Itqān du "+dated+" · soir",
                    HifzSessionActivity.RECENT_SABQI_REVIEW,dated,false,null);
        }
        String sabqiFinal=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING,"learning-final-");
        if(sabqiFinal!=null) {
            LocalDate dated=LocalDate.parse(sabqiFinal);
            if(!dated.isAfter(today))
                addToday(out,"Reprendre finale Sabqi du "+dated+" · matin",
                    HifzSessionActivity.LEARNING_FINAL,dated,false,null);
        }
        String itqanFinal=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-");
        if(itqanFinal!=null) {
            LocalDate dated=LocalDate.parse(itqanFinal);
            if(!dated.isAfter(today))
                addToday(out,"Reprendre finale Itqān du "+dated+" · soir",
                    HifzSessionActivity.CONSOLIDATION_FINAL,dated,false,null);
        }
        // Non-crediting cumulative recall survives day changes and process death.
        // It never replaces an unfinished original Sunday finale.
        // A detached legacy test/navigation shell has no Android Context;
        // the historical finals must not depend on this optional read-only queue.
        if(postFinalStore!=null)for(String family:new String[]{
                PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
            PostFinalRecallStore.Review recall=postFinalStore.nextReadyFor(family,
                prefs.lastLearningConsolidationDate(),prefs.lastRecentSabqiReviewDate());
            if(recall==null||LocalDate.parse(recall.date).isAfter(today))continue;
            out.add(new TodaySession("Lecture cumulative "
                +(PostFinalRecallStore.SABQI.equals(family)?"Sabqi":"Itqān")
                +" · "+(recall.position+1)+"/"+recall.total+" · masquée",
                HifzSessionActivity.POST_FINAL_RECALL,LocalDate.parse(recall.date),
                false,false,family));
        }
        // The learning final may have been BLOCKED before it could open. Give
        // access to the actual preceding Sunday rather than silently dropping
        // the ungraduated lines on Monday. Do not create a new Itqan final here.
        LocalDate sunday=prefs.pendingLearningFinalDate(today);
        if(sunday!=null && sunday.isBefore(today) && !sunday.isBefore(prefs.programStartDate())
                && !sunday.toString().equals(prefs.lastLearningConsolidationDate())
                && !prefs.learningSnowballFinalUnits(sunday).isEmpty()) {
            addToday(out,"Finale Sabqi du "+sunday+" · matin",
                HifzSessionActivity.LEARNING_FINAL,sunday,false,null);
        }
        return out;
    }

    /** One session expected today, as the "Aujourd’hui" window lists it. */
    static final class TodaySession {
        final String label; final String mode; final LocalDate scheduled; final boolean done; final boolean next;
        final String postFinalFamily;
        TodaySession(String label,String mode,LocalDate scheduled,boolean done,boolean next){
            this(label,mode,scheduled,done,next,null);
        }
        TodaySession(String label,String mode,LocalDate scheduled,boolean done,boolean next,String postFinalFamily){
            this.label=label;this.mode=mode;this.scheduled=scheduled;this.done=done;this.next=next;
            this.postFinalFamily=postFinalFamily;
        }
    }

    /**
     * User decision: "Aujourd’hui" usually holds several expected sessions (morning, evening
     * snowball, Révision active, Révision passive), so it opens a window listing all of them, in the
     * order the day sequences them, each with its state; the next one due is marked. Only
     * today's own cadence is listed (plus an overdue morning, if "Aujourd’hui" points to one).
     */
    private List<TodaySession> todaySessions(LocalDate today,String nextMode,LocalDate nextScheduled){
        ArrayList<TodaySession> out=new ArrayList<>();
        String todayStr=today.toString();
        // Both review modes share the dynamic duration; only corpora/masks remain distinct.
        final int revisionMinutes=MaintenanceCoveragePolicy.minutes(
            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
        String eveningLessonDetail=todayRow!=null?stripDone(compactSession(todayRow.evening)):null;
        // Preserve the exact 5-line/Itqan range from the EVENING row, not revisions.
        if(eveningLessonDetail!=null && eveningLessonDetail.contains(" + "))
            eveningLessonDetail=eveningLessonDetail.substring(0,eveningLessonDetail.indexOf(" + "));
        if(nextScheduled!=null&&nextScheduled.isBefore(today)&&!isTodayAnchoredMode(nextMode)){
            out.add(new TodaySession("Report "+nextScheduled+" · "+modeTitle(nextMode),nextMode,nextScheduled,false,true));
        }
        CadenceAction action=WeeklyProgramPolicy.actionFor(today.getDayOfWeek(),prefs.weeklyProgramFor(today),effectiveLearningDaysPerWeek());
        if(today.getDayOfWeek()!=DayOfWeek.SUNDAY) {
            addToday(out,"Révision active · matin · "+revisionMinutes+" min",
                HifzSessionActivity.MURAJAAH_ACTIVE,today,
                todayStr.equals(prefs.lastActiveMurajaahDate()),nextMode);
            addToday(out,"Révision passive · matin · "+revisionMinutes+" min"+revisionPassiveDetail(),
                HifzSessionActivity.MURAJAAH,today,
                todayStr.equals(prefs.lastMurajaahDate()),nextMode);
        }
        if(recommendedSunday(today)){
            // No seventh ordinary Itqan lesson on Sunday.
            if(!prefs.learningSnowballFinalUnits(today).isEmpty()||todayStr.equals(prefs.lastLearningConsolidationDate()))
                addToday(out,"Finale Sabqi · matin",HifzSessionActivity.LEARNING_FINAL,
                    today,learningFinalResolved(today),nextMode);
            if(!prefs.itqanPostNasMaintenance()
                    && (!prefs.stabilizationSnowballFinalUnits(today).isEmpty()
                        || todayStr.equals(prefs.lastRecentSabqiReviewDate())))
                addToday(out,"Finale Itqān · soir",HifzSessionActivity.CONSOLIDATION_FINAL,
                    today,consolidationFinalResolved(today),nextMode);
            return out; // No active/passive reviews or ordinary Itqan on Sunday.
        }
        switch(action){
            case LEARNING:
                addToday(out,eveningLessonDetail!=null?eveningLessonDetail:"Apprentissage",HifzSessionActivity.SABQI,today,modeComplete(today,HifzSessionActivity.SABQI),nextMode);
                EveningReviewDisplay.Status learningEvening=EveningReviewDisplay.forDay(
                    modeComplete(today,HifzSessionActivity.SABQI),
                    modeComplete(today,HifzSessionActivity.LEARNING_CONSOLIDATION),
                    prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,today));
                if(learningEvening!=EveningReviewDisplay.Status.NO_NEW_UNITS)
                    addToday(out,EveningReviewDisplay.learningTodayLabel(learningEvening),
                        HifzSessionActivity.LEARNING_CONSOLIDATION,today,
                        learningEvening==EveningReviewDisplay.Status.COMPLETED,nextMode);
                break;
            case STABILIZATION:
                addToday(out,eveningLessonDetail!=null?eveningLessonDetail:"Stabilisation",HifzSessionActivity.ITQAN,today,modeComplete(today,HifzSessionActivity.ITQAN),nextMode);
                EveningReviewDisplay.Status stabilizationEvening=EveningReviewDisplay.forDay(
                    modeComplete(today,HifzSessionActivity.ITQAN),
                    modeComplete(today,HifzSessionActivity.RECENT_SABQI_REVIEW),
                    prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,today));
                if(stabilizationEvening!=EveningReviewDisplay.Status.NO_NEW_UNITS)
                    addToday(out,EveningReviewDisplay.stabilizationTodayLabel(stabilizationEvening),
                        HifzSessionActivity.RECENT_SABQI_REVIEW,today,
                        stabilizationEvening==EveningReviewDisplay.Status.COMPLETED,nextMode);
                break;
            case REVISION:
                if(!prefs.learningSnowballFinalUnits(today).isEmpty()||todayStr.equals(prefs.lastLearningConsolidationDate()))
                    addToday(out,"Finale Sabqi · révision par page ×3",HifzSessionActivity.LEARNING_FINAL,today,learningFinalResolved(today),nextMode);
                if(!prefs.itqanPostNasMaintenance()
                        && (!prefs.stabilizationSnowballFinalUnits(today).isEmpty()
                            || todayStr.equals(prefs.lastRecentSabqiReviewDate())))
                    addToday(out,"Finale Itqān · révision par page ×3",HifzSessionActivity.CONSOLIDATION_FINAL,today,consolidationFinalResolved(today),nextMode);
                break;
            default:break;
        }
        return out;
    }

    private void addToday(List<TodaySession> out,String label,String mode,LocalDate scheduled,boolean done,String nextMode){
        for(TodaySession existing:out)if(existing.mode.equals(mode))return;
        out.add(new TodaySession(label,mode,scheduled,done,!done&&mode.equals(nextMode)));
    }

    private String revisionPassiveDetail(){
        if(todayRow==null||todayRow.morning==null)return "";
        String raw=todayRow.morning;
        int start=raw.indexOf("Révision passive · ");
        if(start<0)return "";
        int afterMinutes=raw.indexOf(" · ",start+"Révision passive · ".length());
        return afterMinutes<0?"":raw.substring(afterMinutes);
    }

    private static String stripDone(String label){
        return label!=null&&label.startsWith("✓ ")?label.substring(2):label;
    }

    private static String modeTitle(String mode){
        if(HifzSessionActivity.SABQI.equals(mode))return "Apprentissage";
        if(HifzSessionActivity.ITQAN.equals(mode))return "Stabilisation";
        return "séance";
    }

    private void showTodayWindow(List<TodaySession> sessions){
        String[] items=new String[sessions.size()];
        for(int i=0;i<items.length;i++){
            TodaySession s=sessions.get(i);
            String slot=s.label.contains("· matin")?"MATIN · ":
                s.label.contains("· soir")?"SOIR · ":"";
            String title=readableScheduleTitle(s.label,prefs.itqanEntretienActive());
            String essential=readableScheduleEssentials(s.label);
            // Completed finales are identified independently from the Itqān
            // line geometry. Never present a Sabqi finale as Itqān repetition.
            items[i]=(s.done?"✓ ":s.next?"▸ ":"  ")+slot+title+
                (s.label.contains("en attente")?" · En attente":
                    (s.done?" · Terminé":" · À faire"))+
                ("Détails disponibles".equals(essential) || "Terminée".equals(essential)
                    ? "" : "\n"+essential);
        }
        android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<String>(
            this,android.R.layout.simple_list_item_1,items) {
            @Override public View getView(int position,View convertView,android.view.ViewGroup parent) {
                TextView line=(TextView)super.getView(position,convertView,parent);
                TodaySession session=sessions.get(position);
                int icon=HifzSessionActivity.LEARNING_FINAL.equals(session.mode)
                    ?R.drawable.ic_hifz_final_sabqi
                    :HifzSessionActivity.CONSOLIDATION_FINAL.equals(session.mode)
                    ?R.drawable.ic_hifz_final_itqan:0;
                if(icon!=0) {
                    Drawable picture=getDrawable(icon).mutate();
                    picture.setBounds(0,0,Ui.dp(MainActivity.this,40),Ui.dp(MainActivity.this,40));
                    line.setCompoundDrawablesRelative(picture,null,null,null);
                    line.setCompoundDrawablePadding(Ui.dp(MainActivity.this,12));
                } else line.setCompoundDrawablesRelative(null,null,null,null);
                line.setPadding(Ui.dp(MainActivity.this,16),Ui.dp(MainActivity.this,12),Ui.dp(MainActivity.this,12),Ui.dp(MainActivity.this,12));
                line.setMinHeight(Ui.dp(MainActivity.this,64));
                return line;
            }
        };
        new android.app.AlertDialog.Builder(this)
            .setTitle("Aujourd’hui · Choisir une séance")
            .setAdapter(adapter,(dialog,which)->{
                TodaySession session=sessions.get(which);
                if(session.label.contains("en attente") && !session.done) {
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Séance en attente")
                        .setMessage("Termine d’abord la séance du jour avant la révision cumulative du soir.")
                        .setPositiveButton("Fermer",null).show();
                    return;
                }
                if(HifzSessionActivity.POST_FINAL_RECALL.equals(session.mode)
                        && session.postFinalFamily!=null)
                    startActivity(new Intent(this,HifzSessionActivity.class)
                        .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.POST_FINAL_RECALL)
                        .putExtra(HifzSessionActivity.EXTRA_POST_FINAL_FAMILY,session.postFinalFamily));
                else openMode(session.mode,session.scheduled);
            }).show();
    }

    private void refreshToday() {
        GeometryRepository g=geometry;
        if(g==null){setToday("…",todayAction.isEnabled());return;}
        LocalDate current=HifzClock.today();
        if(todayHeading!=null) {
            String day=current.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM",java.util.Locale.FRANCE));
            todayHeading.setText("AUJOURD’HUI · "+day.toUpperCase(java.util.Locale.FRANCE));
        }
        // A past OPEN finale/renforcement outranks the new day's timetable.
        // Do not redirect the learner into Settings because an old due-day
        // cadence cannot be reconstructed after midnight.
        List<TodaySession> prior=unfinishedWeekendSessions(current);
        if(!prior.isEmpty()) {
            setToday(prior.size()==1?"Séance à reprendre":"Séances à reprendre",true);
            if(todayObjective!=null) {
                java.util.LinkedHashSet<String> names=new java.util.LinkedHashSet<>();
                for(TodaySession item:prior) {
                    if(HifzSessionActivity.LEARNING_FINAL.equals(item.mode))names.add("Finale Sabqi");
                    else if(HifzSessionActivity.CONSOLIDATION_FINAL.equals(item.mode))names.add("Finale Itqān");
                    else if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(item.mode))names.add("Consolidation Itqān");
                    else if(HifzSessionActivity.POST_FINAL_RECALL.equals(item.mode))
                        names.add("Lecture cumulative "+(PostFinalRecallStore.ITQAN.equals(item.postFinalFamily)
                            ?"Itqān":"Sabqi")+" masquée");
                    else names.add("Renforcement Sabqi");
                }
                todayObjective.setText(android.text.TextUtils.join(" · ",names)+" · dates conservées");
            }
            return;
        }
        if(current.isBefore(prefs.programStartDate())){
            setToday("Parcours non démarré",false);return;
        }
        ScheduledCadence due=nextDueCadence(current);
        String mode=nextMode(due);
        if(consolidationNeedsAttention||learningConsolidationNeedsAttention){
            setToday("Consolidation · état à vérifier",true);
            return;
        }
        if(mode==null){setToday("Programme à jour · voir les séances",true);return;}
        String prefix=!isTodayAnchoredMode(mode)&&due!=null&&due.getOverdue()?"Report "+due.getScheduledDate()+" · ":"";
        // A delayed lesson keeps its physical cursor. No fictitious 1/3
        // marker on Home: attendance dates are not learned-unit ordinals.
        setToday(prefix+todaySummary(mode),true);
        if(todayObjective!=null){
            String description=homePreviewForMode(mode,todayRow,prefs.itqanEntretienActive());
            todayObjective.setText(description);
            todayObjective.setContentDescription("Séance prévue : "+description);
        }
    }

    private String todaySummary(String mode) {
        if(HifzSessionActivity.SABQI.equals(mode))return "Apprentissage";
        if(HifzSessionActivity.ITQAN.equals(mode))return prefs.itqanEntretienActive()
            ? "Itqān · Maintien de la maîtrise" : "Itqān · Consolidation";
        if(HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode))return "Revue apprentissage";
        if(HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode))return "Révision active";
        if(HifzSessionActivity.MURAJAAH.equals(mode))return "Révision passive";
        if(HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode)
            ||HifzSessionActivity.LEARNING_FINAL.equals(mode))return "Renforcement";
        if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ||HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode))return "Consolidation";
        return "Séance du jour";
    }

    static String anchoringTodayDetail(HifzPrefs prefs,GeometryRepository geometry){
        AnchoringQueue.Entry entry=prefs.inProgressAnchoringEntry();
        if(entry==null)entry=prefs.currentAnchoringEntry(geometry);
        if(entry==null)return "Stabilisation · aucune unité à stabiliser";
        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
        ItqanRegimeStore.UnitPlan plan=prefs.itqanUnitPlan(entry);
        int reps=ItqanMaintenancePolicy.totalReps(plan.regime,plan.protocol);
        List<String> owned=prefs.ordinaryItqanLineIds(start,end,geometry);
        if(plan.regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE){
            java.util.LinkedHashSet<Integer> pages=new java.util.LinkedHashSet<>();
            for(GeometryRepository.LineMeta line:geometry.linesForExactIds(owned))pages.add(line.page);
            return "Stabilisation · "+shortRange(start,end)+" · "+pages.size()+" pages · ×"+reps+" répétitions";
        }
        List<StabilizationHalfPagePolicy.Unit> planned=StabilizationHalfPagePolicy.planPage(
            geometry.linesForExactIds(owned));
        int blocks=Math.max(1,planned.size());
        if(planned.isEmpty())return "Stabilisation · "+shortRange(start,end)+" · ×"+reps;
        if(blocks<=1)return "Stabilisation · "+QuranSurahNames.block(geometry,planned.get(0).lineIds,shortRange(start,end))+" · ×"+reps;
        int block=Math.max(0,Math.min(prefs.itqanBlockIndex(),blocks-1));
        return "Stabilisation · bloc "+(block+1)+"/"+blocks+" · "+QuranSurahNames.block(geometry,planned.get(block).lineIds,shortRange(start,end))+" · ×"+reps;
    }

    private void refreshRecentSabqiAdvisory() {
        if(recentSabqiAdvisory==null || geometry==null)return;
        // An overdue Sunday must NOT be counted as earned. Previously worked but
        // unpromoted physical blocks remain in extended history until a verified finale.
        boolean learning=!prefs.learningSnowballFinalUnits(HifzClock.today()).isEmpty();
        boolean historicalItqanFinal=prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-")!=null;
        boolean stabilization=(!prefs.itqanPostNasMaintenance() || historicalItqanFinal)
            && !prefs.stabilizationSnowballFinalUnits(HifzClock.today()).isEmpty();
        if(!learning && !stabilization) {
            recentSabqiAdvisory.setText("");
            recentSabqiAdvisory.setVisibility(View.GONE);
            return;
        }
        boolean sunday=HifzClock.today().getDayOfWeek()==DayOfWeek.SUNDAY;
        recentSabqiAdvisory.setText("À consolider · "+
            (learning&&stabilization?"Sabqi et Itqān":learning?"Sabqi":"Itqān")+
            (sunday?" · finales du jour":" · prochaines finales dimanche")+
            " · aucun crédit anticipé");
        recentSabqiAdvisory.setVisibility(View.VISIBLE);
    }

    /**
     * The selected plan must be visible AND directly selectable on the week.
     * Never switch the learner's Hifz cadence silently or rewrite past dates.
     * HifzPrefs records an effective-dated transition without changing credits.
     */
    private void chooseWeeklyProgram() {
        HifzPrefs.WeeklyProgram[] options={
            HifzPrefs.WeeklyProgram.MANUAL_SEVEN,HifzPrefs.WeeklyProgram.RECOMMENDED
        };
        // "Manuel" selects only a NUMBER of days via Settings, not weekday positions.
        String[] labels={
            "Manuel · nombre de jours",
            "Standard · 3 Sabqi / 3 Itqān"
        };
        int current=prefs.weeklyProgramMode()==options[1]?1:0;
        new android.app.AlertDialog.Builder(this)
            .setTitle("Programme hebdomadaire")
            .setSingleChoiceItems(labels,current,(dialog,which)->{
                dialog.dismiss();
                if(options[which]==prefs.weeklyProgramMode())return;
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Modifier le programme ?")
                    .setMessage("À partir d’aujourd’hui. Acquis et séances passées conservés. Manuel : régler le nombre de jours dans Paramètres ; six jours ordinaires, finales le dimanche.")
                    .setNegativeButton("Annuler",null)
                    .setPositiveButton("Confirmer",(yes,button)->{
                        if(prefs.setWeeklyProgramMode(options[which])) refreshAll();
                        else Ui.showFatal(this,"Le programme n’a pas été enregistré.");
                    }).show();
            }).setNegativeButton("Fermer",null).show();
    }

    /** Seven-day interactive rail, with one readable day instead of seven stacked lists. */
    private void refreshDashboard() {
        if (dashboard == null || geometry == null) return;
        dashboard.removeAllViews();
        if (dashboardTitle != null) dashboardTitle.setText(
            wideBoox()?"CYCLE HIFZ · 7 JOURS  ›":"7 prochains jours  ⌄");
        List<WeeklyDashboardPlanner.Row> rows;
        try {
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());
            todayRow = rows.isEmpty() ? null : rows.get(0);
        } catch (RuntimeException error) {
            todayRow = null;
            weekRows = new ArrayList<>();
            // The real weekly projection can fail for one historical OPEN finale
            // or an unusually disjoint revision cursor. Never hide all seven dates.
            // The fallback is purely navigational; it credits and schedules nothing.
            android.util.Log.e("QuranHifz","Read-only weekly projection failed",error);
            rows = new ArrayList<>();
            for(LocalDate date:WeeklyDashboardPlanner.window(HifzClock.today())){
                String day=WeeklyDashboardPlanner.day(date,HifzClock.today());
                rows.add(new WeeklyDashboardPlanner.Row(date,day,
                    "Matin · détails à vérifier","Soir · détails à vérifier",
                    "Projection à vérifier"));
            }
            todayRow = rows.get(0);
        }
        weekRows = rows;
        LocalDate todayDate = HifzClock.today();
        boolean found = false;
        for (WeeklyDashboardPlanner.Row row : rows)
            if (row.date.equals(selectedWeekDate)) found = true;
        if (!found) selectedWeekDate = todayDate;
        TextView hours = Ui.text(this, "Matin "+prefs.suggestedMorningTime()+
            "  ·  Soir "+prefs.suggestedEveningTime(), 17f, false);
        if (wideBoox()) hours.setTextSize(20f);
        hours.setTextColor(Ui.MUTED);
        dashboard.addView(hours);
        dashboard.addView(weekStrip);
        dashboard.addView(weekDayPanel);
        renderSelectedWeekDay();
        if(wideBoox()){
            // Full recap/bilan is one tap away rather than taking the space of
            // the seven-day frieze and forcing a vertical swipe on E-Ink.
            TextView details=Ui.text(this,"Bilan · Lectures cumulatives ›",14f,true);
            details.setTextColor(Ui.INK);
            details.setGravity(Gravity.CENTER);
            details.setPadding(0,Ui.dp(this,2),0,0);
            details.setContentDescription("Ouvrir le bilan et les lectures cumulatives");
            details.setClickable(true);
            details.setFocusable(true);
            details.setOnClickListener(v->showBooxExtras());
            dashboard.addView(details);
            return;
        }
        // CP6: receipts only; never interpret time spent as acquired mastery.
        // Unlike the seven-day forward rail, this balance covers Monday through today.
        WeeklyBilan.Summary bilan=WeeklyBilan.calculate(HifzClock.today(),
            prefs.programStartDate(),ledger.coverageStartDate(),
            date->WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
                prefs.weeklyProgramFor(date),effectiveLearningDaysPerWeek()),
            (date,mode)->ledger.find(date,mode)!=null);
        String balancePreview = "BILAN · "+bilan.validated+"/"+bilan.planned+" contrôlées · "+bilan.dueToday+" à faire"
            +(bilan.unverifiedDays>0 ? "\n"+bilan.unverifiedDays+" jour(s) non vérifiable(s)" : "");
        TextView weeklyBalance=Ui.text(this,balancePreview,16f,false);
        weeklyBalance.setPadding(Ui.dp(this,6),Ui.dp(this,9),Ui.dp(this,6),Ui.dp(this,10));
        weeklyBalance.setContentDescription(bilan.display());
        weeklyBalance.setClickable(true);
        weeklyBalance.setFocusable(true);
        weeklyBalance.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Bilan de la semaine").setMessage(bilan.display())
            .setPositiveButton("Fermer",null).show());
        // A blank 0/N accounting line is noise, not an actionable control.
        // Existing verified totals and diagnostic days remain inspectable.
        if (bilan.validated > 0 || bilan.dueToday > 0 || bilan.unverifiedDays > 0) {
            dashboard.addView(Ui.divider(this));
            dashboard.addView(weeklyBalance);
        }
        // Do not add an empty post-final recap. When a genuine history, pending
        // reading or unverified legacy family exists, preserve the full action.
        if (hasPostFinalRecapContent()) {
            TextView recallRecap=Ui.bookText(this,postFinalRecapPreview(),16f,true);
            if(wideBoox())recallRecap.setTextSize(19f);
            recallRecap.setPadding(Ui.dp(this,6),Ui.dp(this,14),
                Ui.dp(this,6),Ui.dp(this,14));
            recallRecap.setContentDescription("Récapitulatif des lectures cumulatives");
            recallRecap.setClickable(true);
            recallRecap.setFocusable(true);
            recallRecap.setOnClickListener(v->showPostFinalRecap());
            dashboard.addView(recallRecap);
        }
    }

    /** All details still available on BOOX, but not stacked below the viewport. */
    private void showBooxExtras(){
        new android.app.AlertDialog.Builder(this).setTitle("Parcours Hifz · détails")
            .setItems(new String[]{"Bilan de la semaine","Lectures cumulatives"},(d,choice)->{
                if(choice==1){showPostFinalRecap();return;}
                WeeklyBilan.Summary bilan=WeeklyBilan.calculate(HifzClock.today(),
                    prefs.programStartDate(),ledger.coverageStartDate(),
                    date->WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
                        prefs.weeklyProgramFor(date),effectiveLearningDaysPerWeek()),
                    (date,mode)->ledger.find(date,mode)!=null);
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Bilan de la semaine").setMessage(bilan.display())
                    .setPositiveButton("Fermer",null).show();
            }).setNegativeButton("Fermer",null).show();
    }

    /** Tap the selected timeline date; preserve every planning detail verbatim. */
    private void showBooxDayDetails(){
        WeeklyDashboardPlanner.Row selected=null;
        for(WeeklyDashboardPlanner.Row row:weekRows)
            if(row.date.equals(selectedWeekDate)){selected=row;break;}
        if(selected==null)return;
        final WeeklyDashboardPlanner.Row day=selected;
        new android.app.AlertDialog.Builder(this)
            .setTitle(day.day+" "+day.date.getDayOfMonth()+" · "+compactState(day.state))
            .setItems(new String[]{"Matin · détail","Soir · détail"},(dialog,index)->
                showScheduleDetail(index==0?"Matin":"Soir",index==0?day.morning:day.evening))
            .setNegativeButton("Fermer",null).show();
    }

    /** Read-only presentation gate. Never hide a legacy-warning or a saved recap. */
    private boolean hasPostFinalRecapContent() {
        try {
            return postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.SABQI)
                || postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.ITQAN)
                || postFinalRecap(PostFinalRecallStore.SABQI) != null
                || postFinalRecap(PostFinalRecallStore.ITQAN) != null;
        } catch (RuntimeException unavailable) {
            // Keep the diagnostic action visible on corrupted/ambiguous history.
            return true;
        }
    }

    private PostFinalRecallStore.Recap postFinalRecap(String family){
        return postFinalStore.recapFor(family,
            prefs.lastLearningConsolidationDate(),prefs.lastRecentSabqiReviewDate());
    }

    private static String postFinalRecapLine(String label,PostFinalRecallStore.Recap recap){
        if(recap==null)return label+" · aucune lecture en attente";
        if(recap.finished)return label+" · "+recap.date+" · terminée · "+recap.totalPages+" pages";
        return label+" · "+recap.date+" · "+recap.completedPages+"/"+recap.totalPages
            +" pages lues · "+(recap.totalPages-recap.completedPages)+" restantes";
    }

    private String postFinalRecapPreview(){
        try {
            PostFinalRecallStore.Recap sabqi=postFinalRecap(PostFinalRecallStore.SABQI);
            PostFinalRecallStore.Recap itqan=postFinalRecap(PostFinalRecallStore.ITQAN);
            return "LECTURES CUMULATIVES · RÉCAPITULATIF ›"
                +"\n"+(postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.SABQI)
                    ?"Sabqi · ancienne lecture 1.17.7 à vérifier"
                    :postFinalRecapLine("Sabqi",sabqi))
                +"\n"+(postFinalStore.hasBlockedLegacyFamily(PostFinalRecallStore.ITQAN)
                    ?"Itqān · ancienne lecture 1.17.7 à vérifier"
                    :postFinalRecapLine("Itqān",itqan));
        }catch(RuntimeException unavailable){
            android.util.Log.w("QuranHifz","Post-final recap unavailable",unavailable);
            return "LECTURES CUMULATIVES · RÉCAPITULATIF ›"
                +"\nLecture des états à vérifier (acquis conservés)";
        }
    }

    /** Read-only recap: only a saved READY review may be launched. */
    private void showPostFinalRecap(){
        final PostFinalRecallStore.Recap sabqi,itqan;
        try {
            sabqi=postFinalRecap(PostFinalRecallStore.SABQI);
            itqan=postFinalRecap(PostFinalRecallStore.ITQAN);
        }catch(RuntimeException unavailable){
            new android.app.AlertDialog.Builder(this)
                .setTitle("Lectures cumulatives")
                .setMessage("État des lectures à vérifier ; aucune finale n’est modifiée.")
                .setPositiveButton("Fermer",null).show();
            return;
        }
        java.util.ArrayList<String> options=new java.util.ArrayList<>();
        java.util.ArrayList<String> families=new java.util.ArrayList<>();
        for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
            PostFinalRecallStore.Recap recap=PostFinalRecallStore.SABQI.equals(family)?sabqi:itqan;
            if(recap==null||recap.finished)continue;
            PostFinalRecallStore.Review ready=postFinalStore.nextReadyFor(family,
                prefs.lastLearningConsolidationDate(),prefs.lastRecentSabqiReviewDate());
            if(ready==null)continue;
            String title=PostFinalRecallStore.SABQI.equals(family)?"Sabqi":"Itqān";
            options.add(title+" · "+recap.date+" · "+recap.completedPages+"/"+recap.totalPages
                +" pages · reprendre la lecture masquée");
            families.add(family);
        }
        String detail=postFinalRecapLine("Sabqi",sabqi)+"\n"
            +postFinalRecapLine("Itqān",itqan)
            +"\n\nChaque lecture : jusqu’aux 20 pages précédemment acquises"
            +" + celles de la finale validée ; une récitation masquée par page,"
            +" sans crédit supplémentaire.";
        if(options.isEmpty()){
            new android.app.AlertDialog.Builder(this)
                .setTitle("Lectures cumulatives · récapitulatif")
                .setMessage(detail).setPositiveButton("Fermer",null).show();
        }else{
            new android.app.AlertDialog.Builder(this)
                .setTitle("Lectures cumulatives · récapitulatif")
                .setItems(options.toArray(new String[0]),(dialog,index)->{
                    startActivity(new Intent(this,HifzSessionActivity.class)
                        .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.POST_FINAL_RECALL)
                        .putExtra(HifzSessionActivity.EXTRA_POST_FINAL_FAMILY,families.get(index)));
                })
                .setNegativeButton("Fermer",null).show();
        }
    }

    private void renderSelectedWeekDay() {
        if (weekStrip == null || weekDayPanel == null) return;
        weekStrip.removeAllViews();
        weekDayPanel.removeAllViews();
        if (weekRows.isEmpty()) return;
        // Atlas is the GRAPHIC vocabulary only: one rail, seven real day targets, circular
        // nodes and an outlined selection. No Atlas editorial titles are borrowed.
        // This read-only view never changes Hifz receipts, dates, or the weekly programme.
        String[] initials = {"L", "M", "M", "J", "V", "S", "D"};
        WeeklyDashboardPlanner.Row chosen = weekRows.get(0);
        FrameLayout timeline = new FrameLayout(this);
        View axis = new View(this);
        axis.setBackgroundColor(Ui.INK);
        FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(-1, Ui.dp(this, 1));
        lineParams.setMargins(Ui.dp(this, 25), Ui.dp(this, 17), Ui.dp(this, 25), 0);
        timeline.addView(axis, lineParams);
        LinearLayout stations = Ui.row(this);
        stations.setGravity(Gravity.TOP);
        int stationHeight=Ui.dp(this,wideBoox()?75:80);
        for (WeeklyDashboardPlanner.Row row : weekRows) {
            boolean active = row.date.equals(selectedWeekDate);
            if (active) chosen = row;
            boolean future = row.date.isAfter(HifzClock.today());
            // Validation is a real signed-off ledger state, never a forecast for future days.
            boolean done = !future && "Validé".equals(row.state);
            String status = future ? "Prévu" : done ? "Validé"
                : row.state == null ? "À vérifier" : compactState(row.state);
            LinearLayout station = Ui.column(this);
            station.setPadding(0,0,0,0);
            station.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            station.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            // Itqān-cadence glyph: rotation means "scheduled type", not "done".
            boolean itqanDay=row.date.getDayOfWeek()==DayOfWeek.TUESDAY
                ||row.date.getDayOfWeek()==DayOfWeek.THURSDAY
                ||row.date.getDayOfWeek()==DayOfWeek.SATURDAY;
            TextView node = Ui.text(this, done?"✓":wideBoox()&&itqanDay?"↻":"", 15f, true);
            node.setGravity(Gravity.CENTER);
            node.setTextColor(done ? Ui.PAPER : Ui.INK);
            android.graphics.drawable.GradientDrawable circle =
                new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(done ? Ui.INK : Ui.PAPER);
            circle.setStroke(Ui.dp(this,1),Ui.INK);
            node.setBackground(circle);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                Ui.dp(this,26), Ui.dp(this,26));
            // Atlas selected station: two concentric rings. Inner fill/check
            // belongs only to a real Hifz completion; outer ring means
            // "consulted date", NEVER "validated". No E-Ink animation.
            FrameLayout marker = new FrameLayout(this);
            if (active) {
                android.graphics.drawable.GradientDrawable selection =
                    new android.graphics.drawable.GradientDrawable();
                selection.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                selection.setColor(Ui.PAPER);
                selection.setStroke(Ui.dp(this,2),Ui.INK);
                marker.setBackground(selection);
            }
            FrameLayout.LayoutParams inner = new FrameLayout.LayoutParams(
                Ui.dp(this,26),Ui.dp(this,26),Gravity.CENTER);
            marker.addView(node,inner);
            station.addView(marker,new LinearLayout.LayoutParams(
                Ui.dp(this,34),Ui.dp(this,34)));
            TextView day = Ui.bookText(this,initials[row.date.getDayOfWeek().getValue()-1],wideBoox()?17f:13f,active);
            day.setGravity(Gravity.CENTER);
            station.addView(day);
            TextView date = Ui.text(this, Integer.toString(row.date.getDayOfMonth()),wideBoox()?16f:12f,active);
            date.setGravity(Gravity.CENTER);
            station.addView(date);
            if (row.date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                TextView finale = Ui.text(this,"Finale",wideBoox()?15f:10f,false);
                finale.setGravity(Gravity.CENTER);
                station.addView(finale);
            }
            station.setMinimumHeight(stationHeight);
            station.setClickable(true);
            station.setFocusable(true);
            station.setContentDescription(row.day+" "+row.date+" · "+status+
                (active?" · sélectionné":"")+" · ouvrir le détail matin et soir");
            station.setOnClickListener(v -> {
                selectedWeekDate = row.date;
                renderSelectedWeekDay();
                if(wideBoox())showBooxDayDetails();
            });
            stations.addView(station,new LinearLayout.LayoutParams(
                0,stationHeight,1f));
        }
        timeline.addView(stations,new FrameLayout.LayoutParams(-1,stationHeight));
        weekStrip.addView(timeline,new LinearLayout.LayoutParams(-1,stationHeight));
        if(wideBoox()){
            final WeeklyDashboardPlanner.Row selected=chosen;
            // Read-only compact strip; the full morning/evening descriptions
            // open as dialogs, preserving actions without a long scrolling panel.
            TextView summary=Ui.bookText(this,
                chosen.day+" "+chosen.date.getDayOfMonth()+" · "+
                compactState(chosen.state),17f,true);
            summary.setGravity(Gravity.CENTER_VERTICAL);
            summary.setSingleLine(true);
            summary.setEllipsize(android.text.TextUtils.TruncateAt.END);
            weekDayPanel.addView(summary);
            TextView morning=Ui.text(this,
                "MATIN  "+readableScheduleTitle(chosen.morning,prefs.itqanEntretienActive())+"  ›",
                15f,false);
            morning.setSingleLine(true);
            morning.setEllipsize(android.text.TextUtils.TruncateAt.END);
            morning.setContentDescription(scheduleDetail(chosen.morning));
            morning.setOnClickListener(v->showScheduleDetail("Matin",selected.morning));
            weekDayPanel.addView(morning);
            TextView evening=Ui.text(this,
                "SOIR  "+readableScheduleTitle(chosen.evening,prefs.itqanEntretienActive())+"  ›",
                15f,false);
            evening.setSingleLine(true);
            evening.setEllipsize(android.text.TextUtils.TruncateAt.END);
            evening.setContentDescription(scheduleDetail(chosen.evening));
            evening.setOnClickListener(v->showScheduleDetail("Soir",selected.evening));
            weekDayPanel.addView(evening);
            return;
        }
        TextView heading = Ui.bookText(this,
            chosen.day+" "+chosen.date.getDayOfMonth()+" · "+compactState(chosen.state), 20f, true);
        weekDayPanel.addView(heading);
        TextView morning = Ui.text(this, "MATIN", 16f, true);
        morning.setPadding(0, Ui.dp(this, 7), 0, 0);
        morning.setText("MATIN · détails ›");
        morning.setContentDescription("Matin · afficher les informations complètes");
        morning.setClickable(true);
        morning.setFocusable(true);
        final String fullMorning = chosen.morning;
        morning.setOnClickListener(v -> showScheduleDetail("Matin", fullMorning));
        weekDayPanel.addView(morning);
        TextView morningTitle=Ui.bookText(this,readableScheduleTitle(chosen.morning,prefs.itqanEntretienActive()),19f,true);
        morningTitle.setPadding(0,Ui.dp(this,5),0,0);
        weekDayPanel.addView(morningTitle);
        TextView morningPreview = Ui.text(this, readableScheduleEssentials(chosen.morning), 16f, false);
        if (wideBoox()) morningPreview.setTextSize(20f);
        morningPreview.setMaxLines(2);
        morningPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        morningPreview.setContentDescription(scheduleDetail(chosen.morning));
        morningPreview.setClickable(true);
        morningPreview.setFocusable(true);
        morningPreview.setOnClickListener(v -> showScheduleDetail("Matin", fullMorning));
        if (hasUsefulSchedulePreview(morningPreview.getText().toString()))
            weekDayPanel.addView(morningPreview);
        String completedFinale=completedFinaleFromPreview(chosen.morning);
        if(!completedFinale.isEmpty()){
            TextView finalStatus=Ui.bookText(this,completedFinale,16f,true);
            finalStatus.setPadding(0,Ui.dp(this,7),0,Ui.dp(this,4));
            finalStatus.setOnClickListener(v -> showScheduleDetail("Matin", fullMorning));
            weekDayPanel.addView(finalStatus);
        }
        TextView evening = Ui.text(this, "SOIR · détails ›", 16f, true);
        evening.setPadding(0, Ui.dp(this, 8), 0, 0);
        evening.setContentDescription("Soir · afficher les informations complètes");
        evening.setClickable(true);
        evening.setFocusable(true);
        final String fullEvening = chosen.evening;
        evening.setOnClickListener(v -> showScheduleDetail("Soir", fullEvening));
        weekDayPanel.addView(evening);
        TextView eveningTitle=Ui.bookText(this,readableScheduleTitle(chosen.evening,prefs.itqanEntretienActive()),19f,true);
        eveningTitle.setPadding(0,Ui.dp(this,5),0,0);
        weekDayPanel.addView(eveningTitle);
        TextView eveningPreview = Ui.text(this, readableScheduleEssentials(chosen.evening), 16f, false);
        if (wideBoox()) eveningPreview.setTextSize(20f);
        eveningPreview.setMaxLines(2);
        eveningPreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        eveningPreview.setContentDescription(scheduleDetail(chosen.evening));
        eveningPreview.setClickable(true);
        eveningPreview.setFocusable(true);
        eveningPreview.setOnClickListener(v -> showScheduleDetail("Soir", fullEvening));
        if (hasUsefulSchedulePreview(eveningPreview.getText().toString()))
            weekDayPanel.addView(eveningPreview);
    }

    /** Suppress filler text only; the MATIN/SOIR detail actions and finales remain. */
    private static boolean hasUsefulSchedulePreview(String value) {
        return value != null && !value.trim().isEmpty()
            && !"Détails disponibles".equals(value)
            && !"Terminée".equals(value)
            && !"Aucune séance".equals(value);
    }

    private void addCell(LinearLayout row, String value, float weight, boolean bold, boolean singleLine) {
        TextView cell = Ui.text(this, value, 10.8f, bold);
        cell.setPadding(Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2));
        cell.setSingleLine(singleLine);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        row.addView(cell);
    }

    private static String compactState(String state) {
        if (state == null || state.trim().isEmpty()) return "—";
        if (state.contains("Parcours non démarré")) return "—";
        if (state.startsWith("En cours")) return "En cours";
        return state;
    }

    private static String compactSession(String value) {
        if (value == null || value.trim().isEmpty()) return "—";
        return value.replace("Sourate ", "S.").replace("v.", "");
    }

    /** Render only a compact preview: the full unmodified forecast stays one tap away.
     * No schedule, credit, line or verse boundary is changed by this formatter. */
    static String compactSchedulePreview(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        String value = raw.replace(" bloc(s)", " bloc");
        // Two independent reviews each keep their own minutes; range stays on the next line.
        if (value.contains("Révision active · ") && value.contains("Révision passive · ")) {
            value = value.replace("Révision active · ", "Active ")
                .replace("Révision passive · ", "Passive ");
            int range = value.indexOf(" min · ", value.indexOf("Passive "));
            if (range >= 0) value = value.substring(0, range + 4)
                + "\n" + value.substring(range + " min · ".length());
        }
        return value;
    }

    /** Phone-first hierarchical rendering; original Mushaf bounds remain in details. */
    static String readableScheduleTitle(String raw, boolean postNas) {
        if(raw==null||raw.trim().isEmpty()||"—".equals(raw.trim()))return "Aucune séance";
        String value=stripDone(raw);
        if(value.startsWith("Stabilisation"))return postNas?"Itqān · Maintien de la maîtrise":"Itqān · Consolidation";
        if(value.startsWith("Apprentissage"))return "Apprentissage · Sabaq";
        if(value.contains("Finale Renforcement Sabqi"))return "Finale · Sabqi";
        if(value.contains("Finale Consolidation Itqān"))return "Finale · Itqān";
        if(value.startsWith("Renforcement"))return "Renforcement · Sabqi";
        if(value.startsWith("Consolidation"))return "Consolidation · Itqān";
        if(value.startsWith("Révision active"))return "Révision active";
        if(value.startsWith("Révision passive"))return "Révision passive";
        return value.split(" · ",2)[0];
    }

    /** Avoid a wall of blocks/pages/Arabic coordinates in the main phone rail. */
    static String readableScheduleEssentials(String raw) {
        if(raw==null||raw.trim().isEmpty())return "Aucune séance";
        String[] tokens=stripDone(raw).split(" · ");
        java.util.ArrayList<String> essentials=new java.util.ArrayList<>();
        for(String part:tokens){
            String t=part.trim();
            if(t.matches("séance \\d+/\\d+") || t.matches("\\d+ lignes?")
                || t.matches("\\d+ pages?") || t.matches("\\d+ min")
                || t.matches("×\\d+") || t.equals("à prévoir selon les acquis")
                || t.equals("aucun passage admissible")
                || t.equals("par page ×3")) essentials.add(t);
        }
        if(essentials.isEmpty())return raw.startsWith("✓")?"Terminée":"Détails disponibles";
        return String.join(" · ",essentials);
    }

    /** Older planning strings sometimes combine a lesson and an unrelated finale. */
    static String completedFinaleFromPreview(String raw) {
        if(raw==null)return "";
        int at=raw.indexOf("✓ Finale ");
        if(at<0)return "";
        String tail=raw.substring(at).trim();
        int next=tail.indexOf(" · ");
        return next<0?tail:tail.substring(0,next);
    }

    static String homePreviewForMode(String mode,WeeklyDashboardPlanner.Row row,boolean postNas){
        if(row==null)return "Séances à consulter";
        String morning=row.morning==null?"":row.morning;
        String evening=row.evening==null?"":row.evening;
        if(HifzSessionActivity.ITQAN.equals(mode)){
            String scheduled=morning.contains("Stabilisation")?morning:
                evening.contains("Stabilisation")?evening:"";
            if(!scheduled.isEmpty())return readableScheduleEssentials(scheduled);
            return postNas?"1 hizb · 2 visibles + 1 masquée":"Voir le programme Itqān";
        }
        if(HifzSessionActivity.SABQI.equals(mode))
            return readableScheduleEssentials(evening.contains("Apprentissage")?evening:morning);
        if(HifzSessionActivity.LEARNING_FINAL.equals(mode))
            return "Finale Sabqi · 1 visible + 2 masquées";
        if(HifzSessionActivity.CONSOLIDATION_FINAL.equals(mode))
            return "Finale Itqān · 1 visible + 2 masquées";
        if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)
            ||HifzSessionActivity.LEARNING_CONSOLIDATION.equals(mode))
            return readableScheduleEssentials(evening);
        if(HifzSessionActivity.MURAJAAH_ACTIVE.equals(mode)
            ||HifzSessionActivity.MURAJAAH.equals(mode))return "Révision du matin";
        return "Consulter les séances prévues";
    }

    private void showScheduleDetail(String title, String raw) {
        new android.app.AlertDialog.Builder(this)
            .setTitle(title).setMessage(scheduleDetail(raw))
            .setPositiveButton("Fermer", null).show();
    }

    /** Show *real* verse boundaries from WeeklyDashboardPlanner instead of discarding
     * them into a generic label. Projections remain read-only and may change on validation.
     * The delimiter becomes a line break to avoid dense walls of text on E-Ink. */
    private static String scheduleDetail(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "—";
        return raw.replace(" · ", "\n");
    }

    private static String shortRange(VerseRef a, VerseRef b) {
        return QuranSurahNames.range(a, b);
    }

    @Override protected void onDestroy() {
        localLoader.shutdownNow();
        super.onDestroy();
    }
}
