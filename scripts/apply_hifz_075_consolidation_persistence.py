from pathlib import Path

path = Path("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
text = path.read_text()
anchor = '    public String lastSabqiDate() { return p.getString("lastSabqiDate", ""); }\n'
if text.count(anchor) != 1:
    raise SystemExit(f"expected one insertion anchor, found {text.count(anchor)}")
if "persistConsolidationSession(ConsolidationCycleEngine.Session session)" in text:
    raise SystemExit("Consolidation persistence is already present")

block = r'''    private static String consolidationStateKey(ConsolidationCycleEngine.Family family) {
        if (family == null) throw new IllegalArgumentException("Consolidation family required");
        switch (family) {
            case LEARNING:
                return "v6ConsolidationLearningState";
            case STABILIZATION:
                return "v6ConsolidationStabilizationState";
            default:
                throw new IllegalArgumentException("Unsupported Consolidation family: " + family);
        }
    }

    /** Persist one OPEN grouped Consolidation session without touching individual counters. */
    boolean persistConsolidationSession(ConsolidationCycleEngine.Session session) {
        if (session == null) throw new IllegalArgumentException("Consolidation session required");
        if (!session.open()) throw new IllegalStateException("Only an OPEN Consolidation session can be persisted");
        ConsolidationCycleEngine.Cycle cycle = session.cycle();
        if (cycle == null || cycle.family() == null) {
            throw new IllegalStateException("Consolidation cycle/family required");
        }
        if (session.sessionGroupSize() < 1 || session.sessionGroupSize() > 3
                || cycle.units().size() != session.sessionGroupSize()) {
            throw new IllegalStateException("Invalid frozen Consolidation session size");
        }
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("cycleId", cycle.cycleId());
            root.put("family", cycle.family().name());
            root.put("sessionId", session.sessionId());
            root.put("sessionGroupSize", session.sessionGroupSize());
            root.put("stage", session.stage());
            root.put("nextUnitIndex", session.nextUnitIndex());
            root.put("donePerStage", session.donePerStage());
            root.put("open", true);

            JSONArray units = new JSONArray();
            for (ConsolidationCycleEngine.Unit unit : cycle.units()) {
                JSONObject encoded = new JSONObject();
                encoded.put("id", unit.id());
                encoded.put("protocol", unit.protocol().name());
                units.put(encoded);
            }
            root.put("units", units);
            return p.edit()
                .putString(consolidationStateKey(cycle.family()), root.toString())
                .commit();
        } catch (Exception error) {
            throw new IllegalStateException("Unable to persist Consolidation session", error);
        }
    }

    /** Restore the exact frozen OPEN grouped session after process recreation; corruption fails closed. */
    ConsolidationCycleEngine.Session restoreConsolidationSession(
            ConsolidationCycleEngine engine, ConsolidationCycleEngine.Family family) {
        if (engine == null) throw new IllegalArgumentException("Consolidation engine required");
        String raw = p.getString(consolidationStateKey(family), null);
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(raw);
            if (root.getInt("version") != 1) {
                throw new IllegalStateException("Unsupported Consolidation persistence version");
            }
            if (!family.name().equals(root.getString("family"))) {
                throw new IllegalStateException("Consolidation family mismatch");
            }
            if (!root.getBoolean("open")) {
                throw new IllegalStateException("Persisted Consolidation session is not OPEN");
            }

            int groupSize = root.getInt("sessionGroupSize");
            JSONArray units = root.getJSONArray("units");
            if (groupSize < 1 || groupSize > 3 || units.length() != groupSize) {
                throw new IllegalStateException("Corrupt frozen Consolidation group size");
            }

            ConsolidationCycleEngine.Cycle cycle = null;
            for (int i = 0; i < units.length(); i++) {
                JSONObject encoded = units.getJSONObject(i);
                String id = encoded.getString("id");
                ConsolidationCycleEngine.Protocol protocol =
                    ConsolidationCycleEngine.Protocol.valueOf(encoded.getString("protocol"));
                ConsolidationCycleEngine.Unit unit = new ConsolidationCycleEngine.Unit(id, protocol);
                if (i == 0) {
                    cycle = engine.startCycle(root.getString("cycleId"), family, unit);
                } else {
                    cycle = engine.addUnit(cycle, unit);
                }
            }
            if (cycle == null) throw new IllegalStateException("Persisted Consolidation session has no units");

            return engine.restoreOpenSession(
                cycle,
                root.getString("sessionId"),
                groupSize,
                root.getInt("stage"),
                root.getInt("nextUnitIndex"),
                root.getInt("donePerStage"));
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Corrupt persisted Consolidation session", error);
        }
    }

'''

path.write_text(text.replace(anchor, block + anchor))
