package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.HifzSchedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure planning helpers for J10 capacity and oldest-first contiguous priority groups. */
final class J10ReviewPlanner {
    private J10ReviewPlanner() {}

    static int scheduledCapacityMinutes(LocalDate start, int days,
                                        int recentBlockCount, boolean consolidationActivated) {
        if (start == null || days <= 0) return 0;
        int total = 0;
        for (int offset = 0; offset < days; offset++) {
            DailyPlan plan = HifzSchedule.INSTANCE.planFor(
                start.plusDays(offset).getDayOfWeek(),
                Math.max(0, recentBlockCount),
                consolidationActivated);
            total += Math.max(0, plan.getMorning().getTargetMinutes());
            total += Math.max(0, plan.getEvening().getTargetMinutes());
        }
        return total;
    }

    static List<Integer> priorityLineIndexes(Map<Integer, LocalDate> lastReviewedByIndex,
                                             LocalDate today,
                                             J10ReviewPolicy.Sustainability sustainability,
                                             int maxLines) {
        if (lastReviewedByIndex == null || lastReviewedByIndex.isEmpty()
                || today == null || maxLines <= 0) return Collections.emptyList();

        LinkedHashMap<Integer, Integer> eligibleAge = new LinkedHashMap<>();
        for (Map.Entry<Integer, LocalDate> entry : lastReviewedByIndex.entrySet()) {
            Integer index = entry.getKey();
            LocalDate reviewed = entry.getValue();
            if (index == null || index < 0 || reviewed == null) continue;
            int age = J10ReviewPolicy.ageDays(reviewed, today);
            if (J10ReviewPolicy.shouldPreempt(age, sustainability)) eligibleAge.put(index, age);
        }
        if (eligibleAge.isEmpty()) return Collections.emptyList();

        List<Integer> eligible = new ArrayList<>(eligibleAge.keySet());
        eligible.sort(Comparator
            .comparingInt((Integer index) -> eligibleAge.get(index)).reversed()
            .thenComparingInt(Integer::intValue));
        int start = eligible.get(0);

        ArrayList<Integer> out = new ArrayList<>();
        for (int index = start; out.size() < maxLines; index++) {
            if (!eligibleAge.containsKey(index)) break;
            out.add(index);
        }
        return Collections.unmodifiableList(out);
    }
}
