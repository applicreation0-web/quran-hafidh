package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class DashboardLedgerPolicyTest {
    @Test public void identicalUpsertStillPurgesRecordsOlderThan120Days() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        ArrayList<DashboardLedger.Record> existing = new ArrayList<>();
        existing.add(new DashboardLedger.Record(today.minusDays(121), "OLD", "ancien"));
        existing.add(new DashboardLedger.Record(today, "MURAJAAH", "Entretien · 45 min"));

        List<DashboardLedger.Record> next = DashboardLedger.mergeRecords(
            existing, today, "MURAJAAH", "Entretien · 45 min", today);

        assertEquals(1, next.size());
        assertEquals(today, next.get(0).date);
        assertEquals("MURAJAAH", next.get(0).type);
    }

    @Test public void upsertReplacesSameDateAndTypeWithoutDuplicate() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        ArrayList<DashboardLedger.Record> existing = new ArrayList<>();
        existing.add(new DashboardLedger.Record(today, "RECENT_SABQI_REVIEW", "ancien label"));

        List<DashboardLedger.Record> next = DashboardLedger.mergeRecords(
            existing, today, "RECENT_SABQI_REVIEW", "30L/1800s", today);

        assertEquals(1, next.size());
        assertEquals("30L/1800s", next.get(0).label);
    }

    @Test public void incomingRecordOlderThanRetentionIsNotReinserted() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        List<DashboardLedger.Record> next = DashboardLedger.mergeRecords(
            new ArrayList<>(), today.minusDays(121), "MURAJAAH", "ancien", today);
        assertEquals(0, next.size());
    }
}
