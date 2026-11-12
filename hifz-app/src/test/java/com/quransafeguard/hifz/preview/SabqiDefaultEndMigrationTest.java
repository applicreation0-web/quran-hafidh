package com.quransafeguard.hifz.preview;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Default-only route extension: never rewrite an earned lesson or custom range. */
public final class SabqiDefaultEndMigrationTest {
    @Test public void legacyBaqaraDefaultExtendsToFathWithoutRewritingProgress() {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("schema", 6);
        disk.disk.put("sabqiStart", "2:77");
        disk.disk.put("sabqiEnd", "2:286");
        disk.disk.put("sabqiLineCursor", 263); // 19:10; real export 12 Nov 2026
        disk.disk.put("sabqiRep", 13);
        disk.disk.put("sabqiAssisted", 1);
        disk.disk.put("sabqiPhysicalLessonSnapshotV1",
            "{\"version\":1,\"date\":\"2026-11-11\",\"sessionId\":\"frozen\",\"reps\":13}");
        disk.disk.put("v6LearnedLineIds", "[\"19:5\",\"19:6\"]");
        disk.disk.put("v6StabilizedLineIds", "[\"527:1\"]");
        disk.disk.put("v6AcquiredCreditLineIds", "[\"527:0\"]");
        disk.disk.put("lastSabqiDate", "2026-11-11");
        disk.disk.put("itqanCursor", "53:33");
        disk.disk.put("itqanRep", 0);
        Map<String, Object> previous = new LinkedHashMap<>(disk.disk);

        HifzPrefs prefs = new HifzPrefs(disk);
        prefs.migrateLegacySabqiEndDefault();

        assertEquals("48:29", disk.getString("sabqiEnd", ""));
        assertTrue(disk.getBoolean("sabqiEndFathDefaultMigrationV1", false));
        for (Map.Entry<String, Object> entry : previous.entrySet()) {
            if ("sabqiEnd".equals(entry.getKey())) continue;
            assertEquals("Unrelated preference modified: " + entry.getKey(),
                entry.getValue(), disk.disk.get(entry.getKey()));
        }

        Map<String, Object> once = new LinkedHashMap<>(disk.disk);
        new HifzPrefs(disk).migrateLegacySabqiEndDefault(); // process restart
        assertEquals(once, disk.disk);
    }

    @Test public void customEndpointIsLeftUntouched() {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("sabqiStart", "2:77");
        disk.disk.put("sabqiEnd", "3:200");
        disk.disk.put("sabqiLineCursor", 263);
        new HifzPrefs(disk).migrateLegacySabqiEndDefault();
        assertEquals("3:200", disk.getString("sabqiEnd", ""));
        assertEquals(263, disk.getInt("sabqiLineCursor", -1));
        assertTrue(disk.getBoolean("sabqiEndFathDefaultMigrationV1", false));
    }

    @Test public void manuallyReturningToBaqaraAfterMigrationIsRespected() {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("sabqiEnd", "2:286");
        HifzPrefs prefs = new HifzPrefs(disk);
        prefs.migrateLegacySabqiEndDefault();
        assertEquals("48:29", disk.getString("sabqiEnd", ""));

        disk.edit().putString("sabqiEnd", "2:286").commit();
        new HifzPrefs(disk).migrateLegacySabqiEndDefault();
        assertEquals("2:286", disk.getString("sabqiEnd", ""));
    }

    @Test public void alreadyExtendedEndIsNotRewritten() {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("sabqiEnd", "48:29");
        new HifzPrefs(disk).migrateLegacySabqiEndDefault();
        assertEquals(SabqiRoute.ROUTE_END.toString(), disk.getString("sabqiEnd", ""));
    }
}
