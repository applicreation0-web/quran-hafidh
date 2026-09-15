from pathlib import Path

prefs_path = Path("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java")
prefs = prefs_path.read_text()
marker = "    void resolveV6Quarantine(\n"
if "public enum ProgressState" in prefs:
    raise SystemExit("Progression API already present; refusing duplicate patch")
if marker not in prefs:
    raise SystemExit("HifzPrefs insertion marker missing")

block = '''    public enum ProgressState {
        NONE,
        LEARNED,
        STABILIZED,
        ACQUIRED
    }

    public enum ProgressAction {
        LEARNING,
        STABILIZATION,
        CONSOLIDATION,
        REVIEW
    }

    public enum ProgressEvent {
        LEARNING_COMPLETED,
        STABILIZATION_COMPLETED,
        CONSOLIDATION_COMPLETED
    }

    ProgressState progressStateV6(String lineId) {
        if (lineId == null || lineId.isEmpty()) throw new IllegalArgumentException("lineId required");
        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");
            if (quarantine.contains(lineId) || legacyPartial.contains(lineId)) {
                throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
            }
            return progressStateFromSets(
                lineId,
                v6LineIdSet("v6LearnedLineIds"),
                v6LineIdSet("v6StabilizedLineIds"),
                v6LineIdSet("v6AcquiredCreditLineIds"));
        }
    }

    ProgressAction nextActionV6(String lineId) {
        ProgressState state = progressStateV6(lineId);
        switch (state) {
            case NONE: return ProgressAction.LEARNING;
            case LEARNED: return ProgressAction.STABILIZATION;
            case STABILIZED: return ProgressAction.CONSOLIDATION;
            case ACQUIRED: return ProgressAction.REVIEW;
            default: throw new IllegalStateException("Unsupported schema6 progression state: " + state);
        }
    }

    void transitionV6Lines(List<String> lineIds, ProgressEvent event) {
        if (lineIds == null || lineIds.isEmpty()) throw new IllegalArgumentException("lineIds required");
        if (event == null) throw new IllegalArgumentException("event required");

        LinkedHashSet<String> batch = new LinkedHashSet<>();
        for (String lineId : lineIds) {
            if (lineId == null || lineId.isEmpty()) throw new IllegalArgumentException("lineId required");
            batch.add(lineId);
        }
        if (batch.isEmpty()) throw new IllegalArgumentException("lineIds required");

        synchronized (V6_STATE_LOCK) {
            requireSchema6ProgressionState();
            LinkedHashSet<String> learned = v6LineIdSet("v6LearnedLineIds");
            LinkedHashSet<String> stabilized = v6LineIdSet("v6StabilizedLineIds");
            LinkedHashSet<String> acquired = v6LineIdSet("v6AcquiredCreditLineIds");
            LinkedHashSet<String> quarantine = v6LineIdSet("v6QuarantineLineIds");
            LinkedHashSet<String> legacyPartial = v6LineIdSet("v6LegacyPartialAcquiredLineIds");

            int targetOrdinal = progressionTargetOrdinal(event);
            LinkedHashMap<String, ProgressState> before = new LinkedHashMap<>();
            boolean changed = false;

            for (String lineId : batch) {
                if (quarantine.contains(lineId) || legacyPartial.contains(lineId)) {
                    throw new IllegalStateException("Unresolved schema6 progression state for line " + lineId);
                }
                ProgressState current = progressStateFromSets(lineId, learned, stabilized, acquired);
                before.put(lineId, current);
                int currentOrdinal = progressionOrdinal(current);
                if (currentOrdinal < targetOrdinal - 1) {
                    throw new IllegalStateException(
                        "Cannot skip schema6 progression stage for line " + lineId
                            + ": state=" + current + " event=" + event);
                }
                if (currentOrdinal == targetOrdinal - 1) changed = true;
            }

            if (!changed) return;

            for (String lineId : batch) {
                ProgressState current = before.get(lineId);
                if (progressionOrdinal(current) != targetOrdinal - 1) continue;
                learned.remove(lineId);
                stabilized.remove(lineId);
                acquired.remove(lineId);
                switch (event) {
                    case LEARNING_COMPLETED:
                        learned.add(lineId);
                        break;
                    case STABILIZATION_COMPLETED:
                        stabilized.add(lineId);
                        break;
                    case CONSOLIDATION_COMPLETED:
                        acquired.add(lineId);
                        break;
                    default:
                        throw new IllegalStateException("Unsupported schema6 progression event: " + event);
                }
            }

            SharedPreferences.Editor editor = p.edit()
                .putString("v6LearnedLineIds", lineIdsJson(learned))
                .putString("v6StabilizedLineIds", lineIdsJson(stabilized))
                .putString("v6AcquiredCreditLineIds", lineIdsJson(acquired));
            if (!editor.commit()) {
                throw new IllegalStateException("Unable to persist schema6 progression transition");
            }
        }
    }

    private void requireSchema6ProgressionState() {
        if (p.getInt("schema", -1) != 6) {
            throw new IllegalStateException("Schema 6 required for progression transitions");
        }
        requiredV6String("v6LearnedLineIds");
        requiredV6String("v6StabilizedLineIds");
        requiredV6String("v6AcquiredCreditLineIds");
        requiredV6String("v6QuarantineLineIds");
        requiredV6String("v6LegacyPartialAcquiredLineIds");
    }

    private static ProgressState progressStateFromSets(
            String lineId,
            LinkedHashSet<String> learned,
            LinkedHashSet<String> stabilized,
            LinkedHashSet<String> acquired) {
        boolean isLearned = learned.contains(lineId);
        boolean isStabilized = stabilized.contains(lineId);
        boolean isAcquired = acquired.contains(lineId);
        int memberships = (isLearned ? 1 : 0) + (isStabilized ? 1 : 0) + (isAcquired ? 1 : 0);
        if (memberships > 1) {
            throw new IllegalStateException("Overlapping schema6 progression states for line " + lineId);
        }
        if (isAcquired) return ProgressState.ACQUIRED;
        if (isStabilized) return ProgressState.STABILIZED;
        if (isLearned) return ProgressState.LEARNED;
        return ProgressState.NONE;
    }

    private static int progressionOrdinal(ProgressState state) {
        switch (state) {
            case NONE: return 0;
            case LEARNED: return 1;
            case STABILIZED: return 2;
            case ACQUIRED: return 3;
            default: throw new IllegalStateException("Unsupported schema6 progression state: " + state);
        }
    }

    private static int progressionTargetOrdinal(ProgressEvent event) {
        switch (event) {
            case LEARNING_COMPLETED: return 1;
            case STABILIZATION_COMPLETED: return 2;
            case CONSOLIDATION_COMPLETED: return 3;
            default: throw new IllegalStateException("Unsupported schema6 progression event: " + event);
        }
    }

'''

prefs_path.write_text(prefs.replace(marker, block + marker, 1))

workflow_path = Path(".github/workflows/hifz-075-tdd.yml")
workflow = workflow_path.read_text()
old = "com.quransafeguard.hifz.preview.StabilizationHalfPageInstrumentedTest\n            --stacktrace"
new = "com.quransafeguard.hifz.preview.StabilizationHalfPageInstrumentedTest,com.quransafeguard.hifz.preview.HifzV6StateTransitionsInstrumentedTest\n            --stacktrace"
if new in workflow:
    raise SystemExit("Transition instrumented test already wired")
if old not in workflow:
    raise SystemExit("TDD workflow class-list marker missing")
workflow_path.write_text(workflow.replace(old, new, 1))
