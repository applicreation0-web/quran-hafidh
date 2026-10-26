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
    private ScrollView homeScroll;
    private ScrollView parcoursScroll;
    private TextView backlogHomeRow;
    private TextView backlogParcoursRow;
    private List<HifzBacklog.Due> backlogPending = java.util.Collections.emptyList();
    private boolean showingParcours;
    private final ExecutorService localLoader = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SemanticPassageRepository.preloadAsync(this);
        prefs = new HifzPrefs(this);
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
        parcoursScroll = scrollOf(path);
        LinearLayout pathHeader = Ui.row(this);
        pathHeader.setGravity(Gravity.CENTER_VERTICAL);
        pathHeader.addView(Ui.iconButton(this, "‹", "Retour", v -> showHome()));
        TextView pathTitle = Ui.bookText(this, "Parcours Hifz", 24, true);
        if (wideBoox()) pathTitle.setTextSize(27f);
        pathTitle.setGravity(Gravity.CENTER);
        Ui.weight(pathTitle, 1f);
        pathHeader.addView(pathTitle);
        pathHeader.addView(new View(this), new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        path.addView(pathHeader);
        path.addView(Ui.divider(this));
        backlogParcoursRow = backlogRow();
        path.addView(backlogParcoursRow);
        // The full weekly planner follows the six large pictograms. This direct
        // text link makes it reachable in one tap without shrinking those icons.
        TextView jumpToWeek = Ui.bookText(this, "PLANNING DE LA SEMAINE ↓", 16f, true);
        if (wideBoox()) jumpToWeek.setTextSize(20f);
        jumpToWeek.setPadding(Ui.dp(this, 5), Ui.dp(this, 12), Ui.dp(this, 5), Ui.dp(this, 12));
        jumpToWeek.setContentDescription("Aller au planning de la semaine");
        jumpToWeek.setClickable(true);
        jumpToWeek.setFocusable(true);
        jumpToWeek.setOnClickListener(v ->
            parcoursScroll.post(() -> parcoursScroll.scrollTo(0, dashboardTitle.getTop())));
        path.addView(jumpToWeek);
        // Hifz semantic grammar: paired by the real corpus and operation.
        // Sabqi: introduce one set, then cumulatively revisit it.
        path.addView(pathSectionTitle("SABQI"));
        LinearLayout learningModes = Ui.row(this);
        LinearLayout sabqi = pathTile(learningModes, "Apprentissage", "1 page/semaine",
            v -> openMode(HifzSessionActivity.SABQI));
        LinearLayout renforcement = pathTile(learningModes, "Renforcement", "Sabqi",
            v -> openMode(renforcementQuickAccessMode()));
        path.addView(learningModes);

        // Itqan: one bounded memorization unit, then its accumulated units.
        path.addView(pathSectionTitle("ITQĀN"));
        LinearLayout otherModes = Ui.row(this);
        itqanCard = pathTile(otherModes, "Stabilisation", stabilizationProgramCue(),
            v -> openMode(HifzSessionActivity.ITQAN));
        LinearLayout itqan = itqanCard;
        LinearLayout consolidation = pathTile(otherModes, "Consolidation", "Itqān",
            v -> openMode(consolidationQuickAccessMode()));
        consolidationCard = consolidation; // Same action and immutable original icon.
        path.addView(otherModes);

        // Same acquired corpus, two modalities: text masked vs text visible.
        path.addView(pathSectionTitle("RÉVISION"));
        LinearLayout reviewModes = Ui.row(this);
        LinearLayout activeRevision = pathTile(reviewModes, "Révision active", "Récitation masquée",
            v -> openMode(HifzSessionActivity.MURAJAAH_ACTIVE));
        LinearLayout passiveRevision = pathTile(reviewModes, "Révision passive", "Texte visible",
            v -> openMode(HifzSessionActivity.MURAJAAH));
        path.addView(reviewModes);
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

        dashboardTitle = Ui.bookText(this, "Cette semaine", 20, true);
        if (wideBoox()) dashboardTitle.setTextSize(24f);
        dashboardTitle.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), 0, Ui.dp(this, 3));
        dashboardTitle.setOnClickListener(v -> chooseWeeklyProgram());
        dashboardTitle.setContentDescription("Choisir le programme hebdomadaire");
        path.addView(dashboardTitle);
        dashboard = Ui.column(this);
        dashboard.setPadding(0, 0, 0, 0);
        weekStrip = Ui.row(this);
        weekDayPanel = Ui.column(this);
        weekDayPanel.setPadding(Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6), 0);
        path.addView(dashboard);

        showHome();

        localLoader.execute(() -> {
            try {
                GeometryRepository loaded = GeometryRepository.get(getApplicationContext());
                prefs.reconcileV6AcquiredBootstrap(loaded);
                prefs.repairStraddlingAcquiredVerses(loaded);
                prefs.releaseRevisionCursorsFromFatiha();
                prefs.currentAnchoringEntry(loaded);
                geometry = loaded;
                runOnUiThread(() -> {
                    setTodayEnabled(true);
                    setGeometryActionsEnabled(true);
                    ledger.capture(prefs);
                    refreshAll();
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
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
        TextView label = Ui.bookText(this, "AUJOURD’HUI", 17f, true);
        if (wideBoox()) label.setTextSize(22f);
        line.addView(label);
        TextView value = Ui.bookText(this, "…", 23f, true);
        if (wideBoox()) value.setTextSize(30f);
        value.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        value.setMaxLines(2);
        line.addView(value);
        todayObjective = Ui.text(this, "", 16f, false);
        if (wideBoox()) todayObjective.setTextSize(21f);
        todayObjective.setTextColor(Ui.MUTED);
        // Never truncate the authentic page/verse range on the BOOX screen.
        todayObjective.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 6));
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
        if (wideBoox()) heading.setTextSize(22f);
        heading.setPadding(Ui.dp(this, 4), Ui.dp(this, 9), 0, Ui.dp(this, 2));
        return heading;
    }

    /** Keep phone icons unchanged; allow large BOOX portrait panels to use their full width. */
    private int booxIconDp(int columns, int maxDp) {
        int widthDp = getResources().getConfiguration().screenWidthDp;
        if (widthDp <= 0) widthDp = 360;
        // The historical 112/124dp caps made the 10-inch BOOX glyphs look tiny.
        // Larger E-Ink layouts use 2x caps; column width still prevents clipping.
        // On phone-width screens the existing dimensions are preserved exactly.
        int responsiveCapDp = widthDp >= 600 ? maxDp * 2 : maxDp;
        return Math.max(62, Math.min(responsiveCapDp, (widthDp - 48) / columns - 14));
    }

    /** Four equal-size home destinations in a 2×2 BOOX grid, without cards or separators. */
    private LinearLayout primaryTile(LinearLayout parent, String role, String display,
                                    View.OnClickListener listener) {
        LinearLayout tile = Ui.column(this);
        tile.setPadding(Ui.dp(this, 3), Ui.dp(this, 12), Ui.dp(this, 3), Ui.dp(this, 12));
        tile.setGravity(Gravity.CENTER);
        int sideDp = booxIconDp(2, 112);
        tile.setMinimumHeight(Ui.dp(this, sideDp + 54));
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
        TextView title = Ui.bookText(this, display, 19f, true);
        if (wideBoox()) title.setTextSize(23f);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        title.setPadding(0, Ui.dp(this, 4), 0, 0);
        tile.addView(title);
        tile.setContentDescription(role);
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
        tile.setPadding(Ui.dp(this, 2), Ui.dp(this, 8), Ui.dp(this, 2), Ui.dp(this, 8));
        tile.setGravity(Gravity.CENTER);
        int sideDp = booxIconDp(2, 124);
        tile.setMinimumHeight(Ui.dp(this, sideDp + 84));
        String shortLabel = "Apprentissage".equals(label) ? "Apprendre"
            : "Stabilisation".equals(label) ? "Stabiliser"
            : "Révision".equals(label) ? "Réviser" : label;
        TextView name = Ui.bookText(this, shortLabel, 19f, true);
        if (wideBoox()) name.setTextSize(22f);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        int icon = largeIconFor(label);
        if (icon == 0) icon = Ui.iconFor(label, label);
        if (icon == 0) throw new IllegalArgumentException("Unmapped Hifz mode: "+label);
        // Intrinsic drawable size is only 24dp; explicit bounds keep it legible on BOOX.
        Drawable architectural = getDrawable(icon).mutate();
        architectural.setBounds(0, 0, Ui.dp(this, sideDp), Ui.dp(this, sideDp));
        name.setCompoundDrawables(null, architectural, null, null);
        name.setCompoundDrawablePadding(Ui.dp(this, 6));
        tile.addView(name);
        // The real target/verse range is on the selected day, not crammed into a 1/3-width cell.
        TextView cue = Ui.text(this, state, 15f, false);
        cue.setVisibility(View.GONE);
        tile.addView(cue);
        // A real short subtitle belongs under each large Hifz glyph, not in an invisible state label.
        TextView subtitle = Ui.menuSubline(this, pathSubtitle(label));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setMaxLines(2);
        subtitle.setPadding(0, Ui.dp(this, 3), 0, 0);
        tile.addView(subtitle);
        tile.setContentDescription(label+" · "+pathSubtitle(label)+" · "+state);
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(Ui.dp(this, 2), 0, Ui.dp(this, 2), 0);
        parent.addView(tile, lp);
        return tile;
    }

    /** Clear, stable vocabulary: the selected-day panel carries all actual verse limits. */
    private static String pathSubtitle(String label) {
        switch (label) {
            case "Apprentissage": return "Sabaq · nouvelle leçon";
            case "Renforcement": return "Sabqi · révision récente";
            case "Stabilisation": return "Fixation Itqān";
            case "Consolidation": return "Cumul Itqān";
            case "Révision active": return "Sans texte";
            case "Révision passive": return "Avec texte";
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
                : "VOIR LES SÉANCES  ›");
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
        boolean afterNas=prefs.itqanEntretienActive();
        String label=afterNas?"Maintien":"Consolidation";
        String subtitle=afterNas?"Itqān · Maintien de la maîtrise":"Itqān · Consolidation";
        if(consolidationCard.getChildAt(0) instanceof TextView)
            ((TextView)consolidationCard.getChildAt(0)).setText(label);
        if(consolidationCard.getChildCount()>2 && consolidationCard.getChildAt(2) instanceof TextView)
            ((TextView)consolidationCard.getChildAt(2)).setText(subtitle);
        consolidationCard.setContentDescription(label+" · "+subtitle);
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
        String summary=count+" séance"+(count>1?"s":"")+" à rattraper  ›";
        for(TextView label:new TextView[]{backlogHomeRow,backlogParcoursRow}) {
            if(label==null)continue;
            label.setVisibility(count==0?View.GONE:View.VISIBLE);
            label.setText(count==0?"":summary);
            label.setContentDescription(count+" leçons manquées, ouvrir les dates de rattrapage");
        }
    }

    private void showBacklog() {
        int overdue=HifzBacklog.lateCount(backlogPending,HifzClock.today());
        if(overdue==0)return;
        // Show the real dated queue, not future unearned verse ranges.
        int visible=Math.min(overdue,24);
        String[] rows=new String[visible];
        for(int i=0;i<visible;i++) {
            HifzBacklog.Due due=backlogPending.get(i);
            rows[i]=(i==0?"▸ ":"  ")+due.date+" · "+due.frenchName()+
                (i==0?" · à reprendre":" · en attente");
        }
        String note=overdue>visible?" (et "+(overdue-visible)+" autres)": "";
        new android.app.AlertDialog.Builder(this)
            .setTitle("Rattrapages · "+overdue+" séance"+(overdue>1?"s":"")+note)
            .setItems(rows,(dialog,index)->{
                if(index==0) {
                    HifzBacklog.Due first=backlogPending.get(0);
                    openMode(first.mode,first.date);
                } else {
                    new android.app.AlertDialog.Builder(this).setTitle("Ordre des leçons")
                        .setMessage("Terminer d’abord la séance du "+backlogPending.get(0).date+
                            ". Les plages suivantes seront déterminées par les curseurs validés.")
                        .setPositiveButton("Compris",null).show();
                }
            }).setNegativeButton("Fermer",null).show();
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
        itqanCard.setContentDescription("Stabilisation · " + cue.getText());
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
        HifzBacklog.Due oldest=HifzBacklog.first(backlogPending);
        if(oldest!=null && oldest.overdue(today)) {
            // The oldest outstanding lesson alone may advance a learning cursor.
            setLineEnabled(sabqiQuickAccess,HifzSessionActivity.SABQI.equals(oldest.mode));
            setLineEnabled(itqanQuickAccess,HifzSessionActivity.ITQAN.equals(oldest.mode));
            return;
        }
        CadenceAction action = WeeklyProgramPolicy.actionFor(HifzClock.today().getDayOfWeek(),prefs.weeklyProgramMode(),effectiveLearningDaysPerWeek());
        setLineEnabled(sabqiQuickAccess, action == CadenceAction.LEARNING);
        setLineEnabled(itqanQuickAccess, action == CadenceAction.STABILIZATION);
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
            // Also guard launcher/direct access: no credit for a future date while debt exists.
            List<HifzBacklog.Due> due=HifzBacklog.pending(prefs,ledger,
                HifzClock.today(),effectiveLearningDaysPerWeek());
            HifzBacklog.Due oldest=HifzBacklog.first(due);
            if(oldest==null || !oldest.mode.equals(mode)
                    || scheduledDate==null || !oldest.date.equals(scheduledDate)) {
                if(oldest==null) {
                    new android.app.AlertDialog.Builder(this).setTitle("Séances à jour")
                        .setMessage("Aucune nouvelle séance de cette catégorie n’est due aujourd’hui.")
                        .setPositiveButton("Fermer",null).show();
                } else {
                    new android.app.AlertDialog.Builder(this).setTitle("Rattrapage prioritaire")
                        .setMessage("Terminer d’abord : "+oldest.frenchName()+" du "+oldest.date+".")
                        .setPositiveButton("Ouvrir le report",(dialog,which)->openMode(oldest.mode,oldest.date))
                        .setNegativeButton("Fermer",null).show();
                }
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
        // The maintenance tile must lead to the hizb ×3, not a non-existent
        // post-An-Nās Consolidation finale. Existing pre-An-Nās OPEN finals
        // were checked above and always retain their original date/protocol.
        if(prefs.itqanEntretienActive())return HifzSessionActivity.ITQAN;
        CadenceAction action=WeeklyProgramPolicy.actionFor(today.getDayOfWeek(),prefs.weeklyProgramFor(today),effectiveLearningDaysPerWeek());
        return today.getDayOfWeek()==DayOfWeek.SUNDAY && !prefs.itqanEntretienActive()
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
                    && (prefs.itqanEntretienActive() || consolidationFinalResolved(date)));
        }
        CadenceAction action=WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),prefs.weeklyProgramFor(date),effectiveLearningDaysPerWeek());
        switch(action){
            case LEARNING:return modeComplete(date,HifzSessionActivity.SABQI);
            case STABILIZATION:return modeComplete(date,HifzSessionActivity.ITQAN);
            case REVISION:
                if(!date.equals(HifzClock.today()))return true;
                return learningFinalResolved(date)
                    && (prefs.itqanEntretienActive() || consolidationFinalResolved(date));
            default:return false;
        }
    }

    private ScheduledCadence nextDueCadence(LocalDate todayDate){
        LinkedHashSet<LocalDate> completed=new LinkedHashSet<>();
        LocalDate cursor=prefs.programStartDate();
        while(!cursor.isAfter(todayDate)){
            if(ledger.historyUnverifiable(cursor)||cadenceComplete(cursor))completed.add(cursor);
            cursor=cursor.plusDays(1);
        }
        return WeeklyProgramPolicy.nextDue(prefs,prefs.programStartDate(),todayDate,completed,effectiveLearningDaysPerWeek());
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
            if(!prefs.itqanEntretienActive() && !consolidationFinalResolved(date))
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
                if(!prefs.itqanEntretienActive() && !consolidationFinalResolved(today))
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
        TodaySession(String label,String mode,LocalDate scheduled,boolean done,boolean next){
            this.label=label;this.mode=mode;this.scheduled=scheduled;this.done=done;this.next=next;
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
                addToday(out,"Finale Renforcement Sabqi · matin",HifzSessionActivity.LEARNING_FINAL,
                    today,learningFinalResolved(today),nextMode);
            if(!prefs.itqanEntretienActive()
                    && (!prefs.stabilizationSnowballFinalUnits(today).isEmpty()
                        || todayStr.equals(prefs.lastRecentSabqiReviewDate())))
                addToday(out,"Finale Consolidation Itqān · soir",HifzSessionActivity.CONSOLIDATION_FINAL,
                    today,consolidationFinalResolved(today),nextMode);
            return out; // No active/passive reviews or ordinary Itqan on Sunday.
        }
        switch(action){
            case LEARNING:
                addToday(out,eveningLessonDetail!=null?eveningLessonDetail:"Apprentissage",HifzSessionActivity.SABQI,today,modeComplete(today,HifzSessionActivity.SABQI),nextMode);
                if(!prefs.learningConsolidationUnits(today).isEmpty())
                    addToday(out,"Renforcement · boule de neige du soir",HifzSessionActivity.LEARNING_CONSOLIDATION,today,!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,today),nextMode);
                break;
            case STABILIZATION:
                addToday(out,eveningLessonDetail!=null?eveningLessonDetail:"Stabilisation",HifzSessionActivity.ITQAN,today,modeComplete(today,HifzSessionActivity.ITQAN),nextMode);
                if(!prefs.stabilizedConsolidationUnits(today).isEmpty())
                    addToday(out,"Consolidation · boule de neige du soir",HifzSessionActivity.RECENT_SABQI_REVIEW,today,!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,today),nextMode);
                break;
            case REVISION:
                if(!prefs.learningSnowballFinalUnits(today).isEmpty()||todayStr.equals(prefs.lastLearningConsolidationDate()))
                    addToday(out,"Renforcement · révision finale ×3",HifzSessionActivity.LEARNING_FINAL,today,learningFinalResolved(today),nextMode);
                if(!prefs.itqanEntretienActive()
                        && (!prefs.stabilizationSnowballFinalUnits(today).isEmpty()
                            || todayStr.equals(prefs.lastRecentSabqiReviewDate())))
                    addToday(out,"Consolidation · révision finale ×3",HifzSessionActivity.CONSOLIDATION_FINAL,today,consolidationFinalResolved(today),nextMode);
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
                (s.done?" · Terminé":" · À faire")+
                ("Détails disponibles".equals(essential) || "Terminée".equals(essential)
                    ? "" : "\n"+essential);
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("Aujourd’hui · Choisir une séance")
            .setItems(items,(dialog,which)->{
                TodaySession s=sessions.get(which);
                openMode(s.mode,s.scheduled);
            })
            .show();
    }

    private void refreshToday() {
        GeometryRepository g=geometry;
        if(g==null){setToday("…",todayAction.isEnabled());return;}
        LocalDate current=HifzClock.today();
        // A past OPEN finale/renforcement outranks the new day's timetable.
        // Do not redirect the learner into Settings because an old due-day
        // cadence cannot be reconstructed after midnight.
        if(!unfinishedWeekendSessions(current).isEmpty()) {
            setToday("Reprendre les séances ouvertes · voir les finales",true);
            if(todayObjective!=null)
                todayObjective.setText("Sabqi / Itqān · reprendre les dates originales, sans perte d'acquis");
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
        // Never query a live Itqan block or mutate its cursor merely
        // to render a home navigation summary.
        LocalDate scheduledForCue = !isTodayAnchoredMode(mode) && due != null
            ? due.getScheduledDate() : current;
        String learningFraction = HifzSessionActivity.SABQI.equals(mode)
            ? WeeklyDashboardPlanner.learningSessionCue(scheduledForCue.getDayOfWeek(),
                prefs.weeklyProgramFor(scheduledForCue)) : "";
        setToday(prefix+todaySummary(mode)+learningFraction,true);
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
        boolean stabilization=(!prefs.itqanEntretienActive() || historicalItqanFinal)
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
        if (dashboardTitle != null) dashboardTitle.setText("Cette semaine  ⌄");
        List<WeeklyDashboardPlanner.Row> rows;
        try {
            rows = new WeeklyDashboardPlanner(prefs, geometry, ledger, speedStore).week(HifzClock.today());
            todayRow = rows.isEmpty() ? null : rows.get(0);
        } catch (RuntimeException error) {
            todayRow = null;
            weekRows = new ArrayList<>();
            TextView unavailable = Ui.bookText(this, "Semaine indisponible", 18f, false);
            dashboard.addView(unavailable);
            return;
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
        // CP6: receipts only; never interpret time spent as acquired mastery.
        // Unlike the seven-day forward rail, this balance covers Monday through today.
        WeeklyBilan.Summary bilan=WeeklyBilan.calculate(HifzClock.today(),
            prefs.programStartDate(),ledger.coverageStartDate(),
            date->WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),
                prefs.weeklyProgramFor(date),effectiveLearningDaysPerWeek()),
            (date,mode)->ledger.find(date,mode)!=null);
        String balancePreview = "BILAN · "+bilan.validated+"/"+bilan.planned+" contrôlées · "+bilan.dueToday+" à faire"
            +"\nRetards : "+bilan.lateLessons+" leçon(s) · "+bilan.missedReviews+" révision(s)"
            +(bilan.unverifiedDays>0 ? "\n"+bilan.unverifiedDays+" jour(s) non vérifiable(s)" : "");
        TextView weeklyBalance=Ui.text(this,balancePreview,16f,false);
        weeklyBalance.setPadding(Ui.dp(this,6),Ui.dp(this,9),Ui.dp(this,6),Ui.dp(this,10));
        weeklyBalance.setContentDescription(bilan.display());
        weeklyBalance.setClickable(true);
        weeklyBalance.setFocusable(true);
        weeklyBalance.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Bilan de la semaine").setMessage(bilan.display())
            .setPositiveButton("Fermer",null).show());
        dashboard.addView(Ui.divider(this));
        dashboard.addView(weeklyBalance);
    }

    private void renderSelectedWeekDay() {
        if (weekStrip == null || weekDayPanel == null) return;
        weekStrip.removeAllViews();
        weekDayPanel.removeAllViews();
        if (weekRows.isEmpty()) return;
        String[] initials = {"L", "M", "M", "J", "V", "S", "D"};
        WeeklyDashboardPlanner.Row chosen = weekRows.get(0);
        for (WeeklyDashboardPlanner.Row row : weekRows) {
            boolean active = row.date.equals(selectedWeekDate);
            if (active) chosen = row;
            TextView day = Ui.bookText(this,
                (active ? "● " : "")+initials[row.date.getDayOfWeek().getValue()-1]
                    +"\n"+row.date.getDayOfMonth(), 18f, active);
            day.setGravity(Gravity.CENTER);
            day.setMinimumHeight(Ui.dp(this, 56));
            day.setContentDescription(row.day+" · "+row.date);
            day.setClickable(true);
            day.setFocusable(true);
            day.setBackgroundColor(Ui.PAPER);
            day.setOnClickListener(v -> {
                selectedWeekDate = row.date;
                renderSelectedWeekDay();
            });
            weekStrip.addView(day,new LinearLayout.LayoutParams(0,Ui.dp(this, 56),1f));
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
        weekDayPanel.addView(eveningPreview);
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
        if(value.startsWith("Consolidation"))return postNas?"Maintien de la maîtrise · Itqān":"Consolidation · Itqān";
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
                || t.matches("×\\d+")) essentials.add(t);
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
