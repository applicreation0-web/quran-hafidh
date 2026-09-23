package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Pure immutable state machine for grouped Consolidation cycles and frozen session plans. */
final class ConsolidationCycleEngine {
    enum Family { LEARNING, STABILIZATION }
    /**
     * LEARNING37/LIGHT/FULL are the frozen quota tables from the original per-progression boule de
     * neige design; SNOWBALL backs the weekday-pinned weekly snowball's evening reviews — ×10 per
     * unit, uniformly regardless of family, group size or position. SNOWBALL_FINAL (×5) is no
     * longer used for new sessions — the value stays defined so a session already persisted under
     * it before that change can still be restored and closed out normally. SNOWBALL_EXTENDED (×3)
     * backs Sunday's extended review: the current + 7 previous weeks' accumulated blocks, read
     * ×3 each instead of the evening's ×10, replacing the old this-week-only ×10 Sunday final.
     */
    enum Protocol { LEARNING37, LIGHT, FULL, SNOWBALL, SNOWBALL_FINAL, SNOWBALL_EXTENDED }

    /**
     * LEARNING37/LIGHT/FULL cycles freeze at three physical units (their quota tables only define
     * positions 0..2). SNOWBALL/SNOWBALL_FINAL cycles allow up to seven: Settings lets Apprentissage
     * (or Stabilisation) run as many as HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK (6) days a week,
     * plus the weekly snowball's extra "continuous" unit, added once at least two of the week's own
     * blocks have accumulated. SNOWBALL_EXTENDED spans up to 8 weeks (up to 3 blocks each) plus one
     * combined pass, so it needs far more headroom than a single week ever could.
     */
    static int maxUnitsFor(Protocol protocol) {
        if (protocol == Protocol.SNOWBALL_EXTENDED) return 30;
        return (protocol == Protocol.SNOWBALL || protocol == Protocol.SNOWBALL_FINAL) ? 7 : 3;
    }

    static final class Unit {
        private final String id;
        private final Protocol protocol;

        Unit(String id, Protocol protocol) {
            if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("unit id required");
            if (protocol == null) throw new IllegalArgumentException("protocol required");
            this.id = id;
            this.protocol = protocol;
        }

        String id() { return id; }
        Protocol protocol() { return protocol; }
    }

    static final class Cycle {
        private final String cycleId;
        private final Family family;
        private final List<Unit> units;
        private final boolean openSession;

        private Cycle(String cycleId, Family family, List<Unit> units, boolean openSession) {
            this.cycleId = cycleId;
            this.family = family;
            this.units = Collections.unmodifiableList(new ArrayList<>(units));
            this.openSession = openSession;
        }

        String cycleId() { return cycleId; }
        Family family() { return family; }
        List<Unit> units() { return units; }
        boolean hasOpenSession() { return openSession; }
    }

    static final class Session {
        private final String sessionId;
        private final Cycle cycle;
        private final List<String> unitIds;
        private final List<Protocol> protocols;
        private final int sessionGroupSize;
        private final boolean open;
        private final int stage;
        private final int nextUnitIndex;
        private final int donePerStage;

        private Session(String sessionId, Cycle cycle, List<String> unitIds,
                        List<Protocol> protocols, int sessionGroupSize, boolean open,
                        int stage, int nextUnitIndex, int donePerStage) {
            this.sessionId = sessionId;
            this.cycle = cycle;
            this.unitIds = Collections.unmodifiableList(new ArrayList<>(unitIds));
            this.protocols = Collections.unmodifiableList(new ArrayList<>(protocols));
            this.sessionGroupSize = sessionGroupSize;
            this.open = open;
            this.stage = stage;
            this.nextUnitIndex = nextUnitIndex;
            this.donePerStage = donePerStage;
        }

        String sessionId() { return sessionId; }
        Cycle cycle() { return cycle; }
        List<String> unitIds() { return unitIds; }
        int sessionGroupSize() { return sessionGroupSize; }
        boolean open() { return open; }
        int stage() { return stage; }
        int nextUnitIndex() { return nextUnitIndex; }
        int donePerStage() { return donePerStage; }
        boolean readyToClose() { return stage >= 5; }

        int quotaAt(int position) {
            return ConsolidationCycleEngine.quota(protocolAt(position), sessionGroupSize, position);
        }

        int[] stageVectorAt(int position) {
            return ConsolidationCycleEngine.stageVector(protocolAt(position), sessionGroupSize, position);
        }

        private Protocol protocolAt(int position) {
            if (position < 0 || position >= protocols.size()) {
                throw new IllegalArgumentException("session unit position out of range");
            }
            return protocols.get(position);
        }
    }

    Cycle startCycle(String cycleId, Family family, Unit firstUnit) {
        if (cycleId == null || cycleId.trim().isEmpty()) throw new IllegalArgumentException("cycle id required");
        if (family == null) throw new IllegalArgumentException("family required");
        validateUnitForFamily(family, firstUnit);
        ArrayList<Unit> units = new ArrayList<>();
        units.add(firstUnit);
        return new Cycle(cycleId, family, units, false);
    }

    Cycle addUnit(Cycle cycle, Unit unit) {
        requireCycle(cycle);
        if (cycle.hasOpenSession()) throw new IllegalStateException("cannot add unit while session is OPEN");
        int cap = maxUnitsFor(unit.protocol());
        if (cycle.units().size() >= cap) throw new IllegalStateException("cycle cannot exceed " + cap + " units");
        validateUnitForFamily(cycle.family(), unit);
        for (Unit existing : cycle.units()) {
            if (existing.id().equals(unit.id())) throw new IllegalArgumentException("duplicate unit id");
        }
        ArrayList<Unit> units = new ArrayList<>(cycle.units());
        units.add(unit);
        return new Cycle(cycle.cycleId(), cycle.family(), units, false);
    }

    Session openSession(Cycle cycle, String sessionId) {
        requireCycle(cycle);
        if (cycle.hasOpenSession()) throw new IllegalStateException("cycle already has an OPEN session");
        if (sessionId == null || sessionId.trim().isEmpty()) throw new IllegalArgumentException("session id required");
        int groupSize = cycle.units().size();

        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Protocol> protocols = new ArrayList<>();
        for (Unit unit : cycle.units()) {
            ids.add(unit.id());
            protocols.add(unit.protocol());
        }
        Cycle opened = new Cycle(cycle.cycleId(), cycle.family(), cycle.units(), true);
        int[] normalized = normalizeProgress(protocols, groupSize, 0, 0, 0);
        return new Session(sessionId, opened, ids, protocols, groupSize, true,
            normalized[0], normalized[1], normalized[2]);
    }

    Session restoreOpenSession(Cycle cycle, String sessionId, int sessionGroupSize,
                               int stage, int nextUnitIndex, int donePerStage) {
        requireCycle(cycle);
        if (cycle.hasOpenSession()) throw new IllegalStateException("cycle already has an OPEN session");
        if (sessionId == null || sessionId.trim().isEmpty()) throw new IllegalArgumentException("session id required");
        if (sessionGroupSize != cycle.units().size()) {
            throw new IllegalStateException("restored group size must match frozen cycle snapshot");
        }

        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Protocol> protocols = new ArrayList<>();
        for (Unit unit : cycle.units()) {
            ids.add(unit.id());
            protocols.add(unit.protocol());
        }
        int[] normalized = normalizeProgress(protocols, sessionGroupSize, stage, nextUnitIndex, donePerStage);
        if (normalized[0] != stage || normalized[1] != nextUnitIndex || normalized[2] != donePerStage) {
            throw new IllegalStateException("restored progress must match frozen session snapshot exactly");
        }
        Cycle opened = new Cycle(cycle.cycleId(), cycle.family(), cycle.units(), true);
        return new Session(sessionId, opened, ids, protocols, sessionGroupSize, true,
            stage, nextUnitIndex, donePerStage);
    }

    Session recordRepetition(Session session) {
        if (session == null) throw new IllegalArgumentException("session required");
        if (!session.open()) throw new IllegalStateException("session is closed");
        if (session.readyToClose()) throw new IllegalStateException("session quotas already complete");

        int[] current = normalizeProgress(session.protocols, session.sessionGroupSize,
            session.stage, session.nextUnitIndex, session.donePerStage);
        int stage = current[0];
        int unitIndex = current[1];
        int done = current[2];
        if (stage >= 5) throw new IllegalStateException("session quotas already complete");

        int target = stageVector(session.protocols.get(unitIndex), session.sessionGroupSize, unitIndex)[stage];
        if (target <= 0) throw new IllegalStateException("normalized progress points to zero quota");
        done++;
        if (done >= target) {
            done = 0;
            unitIndex++;
            if (unitIndex >= session.sessionGroupSize) {
                unitIndex = 0;
                stage++;
            }
        }
        int[] next = normalizeProgress(session.protocols, session.sessionGroupSize, stage, unitIndex, done);
        return new Session(session.sessionId, session.cycle, session.unitIds, session.protocols,
            session.sessionGroupSize, true, next[0], next[1], next[2]);
    }

    Session closeSession(Session session) {
        if (session == null) throw new IllegalArgumentException("session required");
        if (!session.open()) throw new IllegalStateException("session already closed");
        Cycle current = session.cycle();
        Cycle closed = new Cycle(current.cycleId(), current.family(), current.units(), false);
        return new Session(session.sessionId(), closed, session.unitIds, session.protocols,
            session.sessionGroupSize(), false, session.stage, session.nextUnitIndex, session.donePerStage);
    }

    static int quota(Protocol protocol, int groupSize, int position) {
        int[] vector = stageVector(protocol, groupSize, position);
        int total = 0;
        for (int value : vector) total += value;
        return total;
    }

    static int[] stageVector(Protocol protocol, int groupSize, int position) {
        if (protocol == null) throw new IllegalArgumentException("protocol required");
        int cap = maxUnitsFor(protocol);
        if (groupSize < 1 || groupSize > cap) throw new IllegalArgumentException("group size must be 1.." + cap);
        if (position < 0 || position >= groupSize) throw new IllegalArgumentException("position out of range");

        switch (protocol) {
            case LEARNING37:
                if (groupSize == 1) return copy(15, 5, 5, 5, 7);
                if (groupSize == 2) return position == 0
                    ? copy(8, 2, 2, 3, 4)
                    : copy(7, 2, 3, 3, 3);
                return position == 0
                    ? copy(5, 2, 2, 2, 2)
                    : copy(5, 1, 2, 2, 2);
            case LIGHT:
                if (groupSize == 1) return copy(20, 0, 5, 5, 5);
                if (groupSize == 2) return position == 0
                    ? copy(10, 0, 2, 3, 3)
                    : copy(10, 0, 2, 2, 3);
                if (position < 2) return copy(7, 0, 1, 2, 2);
                return copy(6, 0, 1, 2, 2);
            case FULL:
                if (groupSize == 1) return copy(15, 5, 5, 5, 10);
                if (groupSize == 2) return copy(7, 2, 3, 3, 5);
                return position == 0
                    ? copy(5, 2, 2, 2, 3)
                    : copy(5, 1, 2, 2, 3);
            case SNOWBALL:
                return copy(10, 0, 0, 0, 0);
            case SNOWBALL_FINAL:
                return copy(5, 0, 0, 0, 0);
            case SNOWBALL_EXTENDED:
                return copy(3, 0, 0, 0, 0);
            default:
                throw new IllegalArgumentException("unsupported protocol");
        }
    }

    private static int[] normalizeProgress(List<Protocol> protocols, int groupSize,
                                           int stage, int unitIndex, int done) {
        if (protocols == null || protocols.isEmpty() || protocols.size() != groupSize || groupSize < 1
                || groupSize > maxUnitsFor(protocols.get(0))) {
            throw new IllegalStateException("invalid frozen consolidation snapshot");
        }
        if (stage < 0 || stage > 5 || unitIndex < 0 || unitIndex >= groupSize || done < 0) {
            throw new IllegalStateException("invalid consolidation progress");
        }
        int normalizedStage = stage;
        int normalizedUnit = unitIndex;
        int normalizedDone = done;
        while (normalizedStage < 5) {
            int target = stageVector(protocols.get(normalizedUnit), groupSize, normalizedUnit)[normalizedStage];
            if (target > 0) {
                if (normalizedDone >= target) throw new IllegalStateException("progress exceeds stage quota");
                return new int[]{normalizedStage, normalizedUnit, normalizedDone};
            }
            if (normalizedDone != 0) throw new IllegalStateException("zero-quota stage cannot have progress");
            normalizedUnit++;
            if (normalizedUnit >= groupSize) {
                normalizedUnit = 0;
                normalizedStage++;
            }
        }
        return new int[]{5, 0, 0};
    }

    private static void validateUnitForFamily(Family family, Unit unit) {
        if (unit == null) throw new IllegalArgumentException("unit required");
        if (unit.protocol() == Protocol.SNOWBALL || unit.protocol() == Protocol.SNOWBALL_FINAL
                || unit.protocol() == Protocol.SNOWBALL_EXTENDED) return;
        if (family == Family.LEARNING && unit.protocol() != Protocol.LEARNING37) {
            throw new IllegalArgumentException("learning cycle requires LEARNING37 protocol");
        }
        if (family == Family.STABILIZATION && unit.protocol() == Protocol.LEARNING37) {
            throw new IllegalArgumentException("stabilization cycle requires LIGHT or FULL protocol");
        }
    }

    private static void requireCycle(Cycle cycle) {
        if (cycle == null) throw new IllegalArgumentException("cycle required");
        if (cycle.units().isEmpty()) throw new IllegalStateException("cycle size must be non-empty");
        int cap = maxUnitsFor(cycle.units().get(0).protocol());
        if (cycle.units().size() > cap) {
            throw new IllegalStateException("cycle size must be 1.." + cap);
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Unit unit : cycle.units()) {
            validateUnitForFamily(cycle.family(), unit);
            if (!ids.add(unit.id())) throw new IllegalStateException("duplicate unit id in cycle");
        }
    }

    private static int[] copy(int a, int b, int c, int d, int e) {
        return new int[]{a, b, c, d, e};
    }
}
