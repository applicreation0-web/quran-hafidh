package com.quransafeguard.hifz.preview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Local JVM replay with production import/export and in-memory preferences. */
public final class HaafidhV3BackupCompatibility {
    private static final String[] SECTIONS = {"values", "dashboard", "j10", "legacy_gates", "free_mem"};
    private static final String[] STORES = {"MAIN", "LEDGER", "J10", "GATES", "FREE_MEM"};

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Expected a local V3 backup path");
        JSONObject input = new JSONObject(Files.readString(Path.of(args[0])));
        assertEquals(3, input.getInt("version"));
        assertEquals(6, input.getJSONObject("values").getJSONObject("schema").getInt("v"));
        PrivateBackupReplay.MemoryContext context = PrivateBackupReplay.MemoryContext.create();
        assertEquals(input.getJSONObject("values").length(), HifzBackup.restore(context, input));
        assertSameBackup(input, HifzBackup.export(context));
        System.out.println("PASS V3 schema 6: all five stores match the input");

        HifzBackup.restore(context, input);
        assertSameBackup(input, HifzBackup.export(context));
        System.out.println("PASS repeated import: all five stores match");

        JSONObject malformed = new JSONObject(input.toString());
        malformed.getJSONObject("values").remove("schema");
        try {
            HifzBackup.restore(context, malformed);
            fail("A backup without a schema must be rejected");
        } catch (JSONException expected) {
            assertSameBackup(input, HifzBackup.export(context));
        }
        System.out.println("PASS rejected schema: all five stores match the previous state");

        // Distinct values in the destination make a partial write observable at every boundary.
        PrivateBackupReplay.MemoryContext populated = PrivateBackupReplay.MemoryContext.create();
        HifzBackup.restore(populated, input);
        for (InMemoryPrefs store : populated.stores.values()) {
            store.edit().putString("local_replay_probe", "existing").commit();
        }
        JSONObject previous = HifzBackup.export(populated);
        for (String stage : STORES) {
            HifzBackup.failAfterStoreForTest = stage;
            try {
                HifzBackup.restore(populated, input);
                fail("Expected interruption after " + stage);
            } catch (AssertionError interrupted) {
                assertTrue(("Simulated process death after " + stage).equals(interrupted.getMessage()));
            } finally {
                HifzBackup.failAfterStoreForTest = null;
            }
            PrivateBackupReplay.MemoryContext reopened = PrivateBackupReplay.MemoryContext.create();
            reopened.stores.putAll(populated.stores);
            HifzBackup.recoverIfNecessary(reopened);
            assertSameBackup(previous, HifzBackup.export(reopened));
        }
        HifzBackup.restore(populated, input);
        assertSameBackup(input, HifzBackup.export(populated));
        System.out.println("PASS five interruptions: rollback verified followed by successful retry");
    }

    private static void assertSameBackup(JSONObject expected, JSONObject actual) {
        assertEquals(expected.getString("magic"), actual.getString("magic"));
        assertEquals(expected.getInt("version"), actual.getInt("version"));
        for (String section : SECTIONS) {
            JSONObject left = expected.getJSONObject(section);
            JSONObject right = actual.getJSONObject(section);
            assertTrue("Store keys changed in " + section, left.keySet().equals(right.keySet()));
            for (String key : left.keySet()) {
                JSONObject previous = left.getJSONObject(key);
                JSONObject current = right.getJSONObject(key);
                boolean same = "set".equals(previous.getString("t")) && "set".equals(current.getString("t"))
                    ? orderedSet(previous).equals(orderedSet(current))
                    : previous.similar(current);
                assertTrue("Store value changed in " + section, same);
            }
        }
    }

    private static ArrayList<String> orderedSet(JSONObject entry) {
        JSONArray values = entry.getJSONArray("v");
        ArrayList<String> ordered = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) ordered.add(values.getString(i));
        Collections.sort(ordered);
        return ordered;
    }
}
