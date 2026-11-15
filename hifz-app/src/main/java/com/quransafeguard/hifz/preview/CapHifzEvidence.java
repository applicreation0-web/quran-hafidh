package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/** Read-only view of dated lesson receipts, kept separate from Hifz credit and scheduling. */
final class CapHifzEvidence {
    enum Track { SABQI, ITQAN, MORNING, EVENING }

    static final class Count {
        int received;
        int due;

        String fraction() { return due == 0 ? "—" : received + "/" + due; }

        String rate() { return due == 0 ? "non déterminé" : fraction() + " · " + Math.round(100.0 * received / due) + " %"; }
    }

    static final class Lesson {
        final Track track;
        final LocalDate date;
        final Set<String> lines;

        Lesson(Track track, LocalDate date, Set<String> lines) {
            this.track = track;
            this.date = date;
            this.lines = lines;
        }
    }

    static List<Lesson> lessons(String json, Set<String> canonicalIds) {
        List<Lesson> result = new ArrayList<>();
        try {
            JSONArray rows = new JSONArray(json);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String family = row.getString("family");
                Track track = "LEARNING".equals(family) ? Track.SABQI
                    : "STABILIZATION".equals(family) ? Track.ITQAN : null;
                LocalDate date = LocalDate.parse(row.getString("date"));
                JSONArray ids = row.getJSONArray("lines");
                Set<String> lines = new HashSet<>();
                for (int j = 0; j < ids.length(); j++) lines.add(ids.getString(j));
                if (track == null || lines.isEmpty() || lines.size() != ids.length() || !canonicalIds.containsAll(lines))
                    throw new IllegalStateException("Invalid physical lesson receipt");
                result.add(new Lesson(track, date, lines));
            }
        } catch (Exception malformed) {
            throw new IllegalStateException("Historique des reçus indisponible.", malformed);
        }
        return result;
    }

    static Map<Track, Count> count(LocalDate from, LocalDate through, LocalDate today,
            LocalDate coverage, LocalDate activeFrom, WeeklyBilan.Calendar datedCalendar,
            Predicate<LocalDate> sabqiDue, Predicate<LocalDate> itqanDue,
            BiPredicate<Track, LocalDate> received) {
        Map<Track, Count> counts = new EnumMap<>(Track.class);
        for (Track track : Track.values()) counts.put(track, new Count());
        LocalDate day = from.isBefore(coverage) ? coverage : from;
        for (; !day.isAfter(through); day = day.plusDays(1)) {
            CadenceAction action = datedCalendar.action(day);
            if (action == CadenceAction.LEARNING && sabqiDue.test(day))
                add(counts.get(Track.SABQI), day, today, received.test(Track.SABQI, day));
            if (action == CadenceAction.STABILIZATION && itqanDue.test(day))
                add(counts.get(Track.ITQAN), day, today, received.test(Track.ITQAN, day));
            if (!day.isBefore(activeFrom)) {
                add(counts.get(Track.MORNING), day, today, received.test(Track.MORNING, day));
                add(counts.get(Track.EVENING), day, today, received.test(Track.EVENING, day));
            }
        }
        return counts;
    }

    private static void add(Count count, LocalDate day, LocalDate today, boolean received) {
        if (day.isBefore(today) || received) count.due++;
        if (received) count.received++;
    }

    static Map<String, LocalDate> firstDates(List<Lesson> lessons, Track track, LocalDate today) {
        Map<String, LocalDate> result = new HashMap<>();
        for (Lesson receipt : lessons) {
            if (receipt.track != track || receipt.date.isAfter(today)) continue;
            for (String id : receipt.lines) {
                result.merge(id, receipt.date, (first, next) -> first.isBefore(next) ? first : next);
            }
        }
        return result;
    }

    private CapHifzEvidence() {}
}
