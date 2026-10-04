package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

final class ConsolidationAttendance {
    private ConsolidationAttendance() {}

    static List<LocalDate> add(Collection<LocalDate> existing, LocalDate date) {
        TreeSet<LocalDate> sorted = new TreeSet<>();
        if (existing != null) sorted.addAll(existing);
        if (date != null) sorted.add(date);
        return new ArrayList<>(sorted);
    }

    static List<LocalDate> between(Collection<LocalDate> existing,
                                   LocalDate fromInclusive,
                                   LocalDate throughInclusive) {
        ArrayList<LocalDate> out = new ArrayList<>();
        if (existing == null || fromInclusive == null || throughInclusive == null) return out;
        for (LocalDate date : new TreeSet<>(existing)) {
            if (date.compareTo(fromInclusive) >= 0 && date.compareTo(throughInclusive) <= 0) {
                out.add(date);
            }
        }
        return out;
    }
}
