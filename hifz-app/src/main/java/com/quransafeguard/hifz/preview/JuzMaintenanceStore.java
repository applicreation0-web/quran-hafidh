package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A dedicated, resumable Itqān rotation over whole acquired juz. */
final class JuzMaintenanceStore {
    private static final String DRAFT = "itqanJuzMaintenanceDraftV1";
    private static final String NEXT = "itqanJuzMaintenanceNextV1";
    private static final String RECEIPT = "itqanJuzMaintenanceReceiptV1|";
    static final String CUE = "1 juz visible, puis le même juz masqué. Pages dévoilées à corriger à la fin.";

    enum Phase {
        READ, RECALL, CORRECT_READ, CORRECT_RECALL;
        boolean masked() { return this == RECALL || this == CORRECT_RECALL; }
        boolean correction() { return this == CORRECT_READ || this == CORRECT_RECALL; }
    }

    static final class Plan {
        final int juz;
        final Map<Integer, List<String>> linesByPage;
        final List<Integer> pages;
        Plan(int juz, Map<Integer, List<String>> linesByPage) {
            this.juz = juz;
            this.linesByPage = linesByPage;
            this.pages = Collections.unmodifiableList(new ArrayList<>(linesByPage.keySet()));
        }
    }

    static final class Draft {
        final String raw;
        final int juz, index;
        final LocalDate opened;
        final Phase phase;
        final List<Integer> revealed;
        final boolean correctionAssisted;

        Draft(String raw, int juz, LocalDate opened, Phase phase, int index,
                List<Integer> revealed, boolean correctionAssisted) {
            this.raw = raw;
            this.juz = juz;
            this.opened = opened;
            this.phase = phase;
            this.index = index;
            this.revealed = Collections.unmodifiableList(revealed);
            this.correctionAssisted = correctionAssisted;
        }

        int page(Plan plan) { return (phase.correction() ? revealed : plan.pages).get(index); }
    }

    private final SharedPreferences prefs;

    JuzMaintenanceStore(SharedPreferences prefs) { this.prefs = prefs; }

    static Plan plan(int juz, GeometryRepository geometry, EligibleCorpus acquired) {
        int[] starts = JuzProgressSummary.boundaries();
        if (juz < 1 || juz > starts.length) throw new IllegalArgumentException("Invalid juz");
        QuranCanon canon = QuranCanon.INSTANCE;
        int start = starts[juz - 1];
        int end = juz == starts.length ? canon.ordinal(new VerseRef(114, 6)) : starts[juz] - 1;
        for (int ordinal = start; ordinal <= end; ordinal++) {
            if (!acquired.contains(canon.fromOrdinal(ordinal))) return null;
        }
        int first = geometry.firstLineIndex(canon.fromOrdinal(start));
        int last = geometry.lastLineIndex(canon.fromOrdinal(end));
        Map<Integer, List<String>> lines = new LinkedHashMap<>();
        for (int i = first; i <= last; i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            lines.computeIfAbsent(line.page, ignored -> new ArrayList<>()).add(line.id);
        }
        return new Plan(juz, lines);
    }

    Draft open(LocalDate today, GeometryRepository geometry, EligibleCorpus acquired) {
        synchronized (prefs) {
            if (!today.equals(HifzClock.today()) || !ItqanRegimeStore.postNasMaintenance(prefs)
                    || !afterLastReceipt(today)) return null;
            Draft saved = read();
            if (saved != null) return saved;
            int start = prefs.getInt(NEXT, 1);
            if (start < 1 || start > 30) throw new IllegalStateException("Invalid maintenance position");
            for (int offset = 0; offset < 30; offset++) {
                int juz = (start + offset - 1) % 30 + 1;
                if (plan(juz, geometry, acquired) != null) {
                    if (!save(juz, today, Phase.READ, 0, Collections.emptyList(), false))
                        throw new IllegalStateException("Unable to open maintenance");
                    return read();
                }
            }
            return null;
        }
    }

    Draft read() {
        String raw = prefs.getString(DRAFT, null);
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            List<Integer> revealed = new ArrayList<>();
            JSONArray values = json.getJSONArray("revealed");
            for (int i = 0; i < values.length(); i++) revealed.add(values.getInt(i));
            Draft draft = new Draft(raw, json.getInt("juz"), LocalDate.parse(json.getString("opened")),
                Phase.valueOf(json.getString("phase")), json.getInt("index"), revealed,
                json.getBoolean("correctionAssisted"));
            if (draft.index < 0 || new LinkedHashSet<>(revealed).size() != revealed.size()
                    || draft.correctionAssisted && draft.phase != Phase.CORRECT_RECALL)
                throw new IllegalStateException("Invalid maintenance checkpoint");
            return draft;
        } catch (JSONException invalid) {
            throw new IllegalStateException("Unreadable maintenance checkpoint", invalid);
        }
    }

    boolean reveal(Draft expected, Plan plan) {
        synchronized (prefs) {
            if (!current(expected, plan) || !expected.phase.masked()) return false;
            if (expected.phase == Phase.CORRECT_RECALL)
                return expected.correctionAssisted || save(expected.juz, expected.opened,
                    expected.phase, expected.index, expected.revealed, true);
            Set<Integer> pages = new LinkedHashSet<>(expected.revealed);
            return !pages.add(expected.page(plan)) || save(expected.juz, expected.opened,
                expected.phase, expected.index, new ArrayList<>(pages), false);
        }
    }

    boolean advance(Draft expected, Plan plan, LocalDate today) {
        synchronized (prefs) {
            if (!current(expected, plan) || !today.equals(HifzClock.today())
                    || today.isBefore(expected.opened) || prefs.contains(RECEIPT + today)
                    || !afterLastReceipt(today)) return false;
            int next = expected.index + 1;
            List<Integer> revealed = expected.revealed;
            Phase phase = expected.phase;
            if (phase == Phase.CORRECT_READ)
                return save(expected.juz, expected.opened, Phase.CORRECT_RECALL,
                    expected.index, revealed, false);
            if (expected.correctionAssisted)
                return save(expected.juz, expected.opened, Phase.CORRECT_READ,
                    expected.index, revealed, false);
            if (phase == Phase.CORRECT_RECALL && next < revealed.size())
                return save(expected.juz, expected.opened, Phase.CORRECT_READ, next, revealed, false);
            if (!phase.correction() && next < plan.pages.size())
                return save(expected.juz, expected.opened, phase, next, revealed, false);
            if (phase == Phase.READ)
                return save(expected.juz, expected.opened, Phase.RECALL, 0, revealed, false);
            if (phase == Phase.RECALL && !revealed.isEmpty())
                return save(expected.juz, expected.opened, Phase.CORRECT_READ, 0, revealed, false);

            String label = "Maintien · juz " + expected.juz + " · " + plan.pages.size() + " pages";
            SharedPreferences.Editor editor = prefs.edit().remove(DRAFT).putInt(NEXT, expected.juz % 30 + 1)
                .putString(RECEIPT + today, label)
                .putString("lastItqanDate", today.toString()).putString("lastItqanLabel", label)
                .putLong("itqanElapsedMs", 0L);
            List<String> lines = new ArrayList<>();
            for (int page : plan.pages) lines.addAll(plan.linesByPage.get(page));
            HifzPrefs.appendPostNasMaintainedUnit(prefs,editor,today,lines);
            return editor.commit();
        }
    }

    private boolean afterLastReceipt(LocalDate today) {
        String last = prefs.getString("lastItqanDate", "");
        return last.isEmpty() || today.isAfter(LocalDate.parse(last));
    }

    private boolean current(Draft expected, Plan plan) {
        return expected != null && plan != null && expected.juz == plan.juz && expected.raw.equals(prefs.getString(DRAFT, null))
            && plan.pages.containsAll(expected.revealed)
            && expected.index < (expected.phase.correction() ? expected.revealed : plan.pages).size();
    }

    private boolean save(int juz, LocalDate opened, Phase phase, int index,
            List<Integer> revealed, boolean assisted) {
        try {
            JSONObject value = new JSONObject().put("juz", juz).put("opened", opened.toString())
                .put("phase", phase.name()).put("index", index).put("revealed", new JSONArray(revealed))
                .put("correctionAssisted", assisted);
            return prefs.edit().putString(DRAFT, value.toString()).commit();
        } catch (JSONException invalid) {
            throw new IllegalStateException("Unable to save maintenance", invalid);
        }
    }
}
