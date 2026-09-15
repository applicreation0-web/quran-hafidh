package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Pure immutable state machine for grouped Consolidation cycles and frozen session plans. */
final class ConsolidationCycleEngine {
    enum Family { LEARNING, STABILIZATION }
    enum Protocol { LEARNING37, LIGHT, FULL }

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

        private Session(String sessionId, Cycle cycle, List<String> unitIds,
                        List<Protocol> protocols, int sessionGroupSize, boolean open) {
            this.sessionId = sessionId;
            this.cycle = cycle;
            this.unitIds = Collections.unmodifiableList(new ArrayList<>(unitIds));
            this.protocols = Collections.unmodifiableList(new ArrayList<>(protocols));
            this.sessionGroupSize = sessionGroupSize;
            this.open = open;
        }

        String sessionId() { return sessionId; }
        Cycle cycle() { return cycle; }
        List<String> unitIds() { return unitIds; }
        int sessionGroupSize() { return sessionGroupSize; }
        boolean open() { return open; }

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
        if (cycle.units().size() >= 3) throw new IllegalStateException("cycle cannot exceed three units");
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
        if (groupSize < 1 || groupSize > 3) throw new IllegalStateException("cycle size must be 1..3");

        ArrayList<String> ids = new ArrayList<>();
        ArrayList<Protocol> protocols = new ArrayList<>();
        for (Unit unit : cycle.units()) {
            ids.add(unit.id());
            protocols.add(unit.protocol());
        }
        Cycle opened = new Cycle(cycle.cycleId(), cycle.family(), cycle.units(), true);
        return new Session(sessionId, opened, ids, protocols, groupSize, true);
    }

    Session closeSession(Session session) {
        if (session == null) throw new IllegalArgumentException("session required");
        if (!session.open()) throw new IllegalStateException("session already closed");
        Cycle current = session.cycle();
        Cycle closed = new Cycle(current.cycleId(), current.family(), current.units(), false);
        return new Session(session.sessionId(), closed, session.unitIds, session.protocols,
            session.sessionGroupSize(), false);
    }

    static int quota(Protocol protocol, int groupSize, int position) {
        int[] vector = stageVector(protocol, groupSize, position);
        int total = 0;
        for (int value : vector) total += value;
        return total;
    }

    static int[] stageVector(Protocol protocol, int groupSize, int position) {
        if (protocol == null) throw new IllegalArgumentException("protocol required");
        if (groupSize < 1 || groupSize > 3) throw new IllegalArgumentException("group size must be 1..3");
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
            default:
                throw new IllegalArgumentException("unsupported protocol");
        }
    }

    private static void validateUnitForFamily(Family family, Unit unit) {
        if (unit == null) throw new IllegalArgumentException("unit required");
        if (family == Family.LEARNING && unit.protocol() != Protocol.LEARNING37) {
            throw new IllegalArgumentException("learning cycle requires LEARNING37 protocol");
        }
        if (family == Family.STABILIZATION && unit.protocol() == Protocol.LEARNING37) {
            throw new IllegalArgumentException("stabilization cycle requires LIGHT or FULL protocol");
        }
    }

    private static void requireCycle(Cycle cycle) {
        if (cycle == null) throw new IllegalArgumentException("cycle required");
        if (cycle.units().isEmpty() || cycle.units().size() > 3) {
            throw new IllegalStateException("cycle size must be 1..3");
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
