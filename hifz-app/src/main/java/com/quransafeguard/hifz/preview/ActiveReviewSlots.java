package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Dated active-review checkpoints and receipts stored with their common acquired-corpus cursor. */
final class ActiveReviewSlots {
    static final int LEGACY_MINUTES = 30;
    static final long DURATION_MS = LEGACY_MINUTES * 60_000L;
    private static final String DRAFT = "activeReviewDraftV1";
    private static final String RECEIPT = "activeReviewReceiptV1";
    private static final String RECEIPT_MINUTES = "activeReviewReceiptMinutesV1";
    private static final String CURSOR = "activeMurajaahCursor";
    private static final String REVISION = "activeReviewCursorRevisionV1";
    private static final String PROGRAMME_START = "activeReviewProgrammeStartV1";
    private static final Object LOCK = new Object();

    enum Slot {
        MORNING("MURAJAAH_ACTIVE_AM", "matin"),
        EVENING("MURAJAAH_ACTIVE_PM", "soir");

        final String mode;
        final String label;
        Slot(String mode, String label) {
            this.mode = mode;
            this.label = label;
        }
        static Slot fromMode(String mode) {
            for (Slot slot : values()) if (slot.mode.equals(mode)) return slot;
            return null;
        }
        String title() { return "Révision active · " + label; }
    }

    static final class Draft {
        final VerseRef start, end;
        final int page, targetMinutes;
        final long elapsedMs, revision;
        final boolean paused;
        Draft(VerseRef start, VerseRef end, int page, long elapsedMs, long revision, boolean paused, int targetMinutes) {
            this.targetMinutes = targetMinutes;
            this.start = start;
            this.end = end;
            this.page = page;
            this.elapsedMs = elapsedMs;
            this.revision = revision;
            this.paused = paused;
        }
        long durationMs() { return targetMinutes * 60_000L; }
    }

    private final SharedPreferences prefs;

    ActiveReviewSlots(SharedPreferences prefs) { this.prefs = prefs; }

    void ensureProgramme(LocalDate today) {
        synchronized (LOCK) {
            if (!prefs.contains(PROGRAMME_START)
                    && !prefs.edit().putString(PROGRAMME_START, today.toString()).commit())
                throw new IllegalStateException("Impossible d’enregistrer les créneaux de révision.");
        }
    }

    LocalDate programmeStart() {
        return LocalDate.parse(prefs.getString(PROGRAMME_START, HifzClock.today().toString()));
    }

    private static String scope(LocalDate date, Slot slot) { return "|" + date + "|" + slot.name(); }

    Draft draft(LocalDate date, Slot slot) {
        String raw = prefs.getString(DRAFT + scope(date, slot), null);
        if (raw == null) return null;
        try {
            JSONObject doc = new JSONObject(raw);
            int page = Math.toIntExact(integral(doc.get("page")));
            long elapsed = integral(doc.get("elapsedMs"));
            long revision = integral(doc.get("revision"));
            int minutes = doc.has("targetMinutes")
                ? Math.toIntExact(integral(doc.get("targetMinutes"))) : LEGACY_MINUTES;
            if (page < 1 || page > 604 || elapsed < 0L || elapsed > minutes * 60_000L
                    || revision < 0L || minutes < LEGACY_MINUTES || minutes % 15 != 0)
                throw new IllegalArgumentException("Invalid active-review checkpoint");
            String end = doc.getString("end");
            return new Draft(GeometryRepository.parseVerse(doc.getString("start")),
                end.isEmpty() ? null : GeometryRepository.parseVerse(end), page, elapsed, revision,
                doc.getBoolean("paused"), minutes);
        } catch (JSONException | RuntimeException invalid) {
            throw new IllegalStateException("État du créneau de révision à vérifier.", invalid);
        }
    }

    private static long integral(Object value) {
        if (!(value instanceof Integer) && !(value instanceof Long))
            throw new IllegalArgumentException("Invalid checkpoint number");
        return ((Number) value).longValue();
    }

    private long revision() { return prefs.getLong(REVISION, 0L); }

    boolean isCurrent(Draft draft, VerseRef cursor) {
        return draft != null && draft.revision == revision() && draft.start.equals(cursor);
    }

    /**
     * Two slots are independent receipts, not permission to revise the same
     * uncredited passage twice. An unfinished slot reserves today's active
     * cursor until it has a genuine receipt. A skipped slot reserves nothing.
     */
    boolean canOpen(LocalDate date, Slot slot) {
        if (date == null || slot == null) return false;
        synchronized (LOCK) {
            Slot other = slot == Slot.MORNING ? Slot.EVENING : Slot.MORNING;
            if (receipt(date, other) != null || draft(date, other) == null) return true;
            // Old versions could persist both unfinished slots. Allow the
            // morning proof to finish, never both, without discarding either.
            return slot == Slot.MORNING && draft(date, slot) != null;
        }
    }

    /**
     * Missing dates never earn receipts or extra daily minutes. Rehome a real,
     * unfinished checkpoint to the ACTUAL day and slot being resumed. The same
     * frozen target, elapsed time, physical cursor and corrective page journal
     * travel atomically. Yesterday stays visibly unvalidated.
     */
    boolean carryForward(LocalDate today, Slot destination) {
        if (today == null || destination == null) return false;
        synchronized (LOCK) {
            if (receipt(today, destination) != null || draft(today, destination) != null
                    || !canOpen(today, destination)) return false;
            LocalDate oldest = null;
            Slot source = null;
            for (Map.Entry<String, ?> item : prefs.getAll().entrySet()) {
                String key = item.getKey();
                if (!key.startsWith(DRAFT + "|")) continue;
                String[] parts = key.split("\\|", -1);
                if (parts.length != 3) throw new IllegalStateException("Invalid active draft key");
                LocalDate date = LocalDate.parse(parts[1]);
                Slot slot = Slot.valueOf(parts[2]);
                if (!date.isBefore(today) || receipt(date, slot) != null) continue;
                Draft pending = draft(date, slot);
                if (pending == null || !isCurrent(pending, currentCursor(pending.start))) continue;
                if (oldest == null || date.isBefore(oldest)
                        || date.equals(oldest) && slot == Slot.MORNING) {
                    oldest = date;
                    source = slot;
                }
            }
            return oldest != null && carryForward(oldest, source, today, destination);
        }
    }

    /** Explicit source used when a live foreground session crosses midnight. */
    boolean carryForward(LocalDate oldDate, Slot oldSlot, LocalDate today, Slot newSlot) {
        if (oldDate == null || oldSlot == null || today == null || newSlot == null
                || !oldDate.isBefore(today)) return false;
        synchronized (LOCK) {
            Draft saved = draft(oldDate, oldSlot);
            if (saved == null || receipt(oldDate, oldSlot) != null
                    || receipt(today, newSlot) != null || draft(today, newSlot) != null
                    || !canOpen(today, newSlot)
                    || !isCurrent(saved, currentCursor(saved.start))) return false;
            String origin = scope(oldDate, oldSlot);
            String destination = scope(today, newSlot);
            String rawDraft = prefs.getString(DRAFT + origin, null);
            String oldRevealKey = ActiveReviewCorrectionStore.KEY_REVEALED + origin;
            String oldStateKey = ActiveReviewCorrectionStore.KEY_STATE + origin;
            String newRevealKey = ActiveReviewCorrectionStore.KEY_REVEALED + destination;
            String newStateKey = ActiveReviewCorrectionStore.KEY_STATE + destination;
            if (rawDraft == null || prefs.contains(newRevealKey) || prefs.contains(newStateKey))
                return false;
            // Read and validate ALL source bytes before any edit; on failure
            // leave the old draft and both page journals intact.
            ActiveReviewCorrectionStore journal = correction(oldDate, oldSlot);
            String revealed = prefs.getString(oldRevealKey, null);
            String pending = prefs.getString(oldStateKey, null);
            try {
                JSONObject movedReveal = null;
                JSONObject movedState = null;
                if (revealed != null) {
                    if (journal.revealed(oldDate.toString(), saved.start).isEmpty())
                        throw new IllegalStateException("Invalid active reveal journal");
                    movedReveal = new JSONObject(revealed);
                    movedReveal.put("date", today.toString());
                }
                if (pending != null) {
                    ActiveReviewCorrectionStore.State verified = journal.pending();
                    if (verified == null || !verified.date.equals(oldDate.toString())
                            || !verified.start.equals(saved.start)
                            || !verified.end.equals(saved.end == null ? saved.start : saved.end)
                            || verified.activeElapsedMs > saved.durationMs())
                        throw new IllegalStateException("Invalid active correction journal");
                    movedState = new JSONObject(pending);
                    movedState.put("date", today.toString());
                }
                SharedPreferences.Editor edit = prefs.edit()
                    .putString(DRAFT + destination, rawDraft).remove(DRAFT + origin);
                if (movedReveal != null)
                    edit.putString(newRevealKey, movedReveal.toString()).remove(oldRevealKey);
                if (movedState != null)
                    edit.putString(newStateKey, movedState.toString()).remove(oldStateKey);
                return edit.commit();
            } catch (JSONException invalid) {
                throw new IllegalStateException("Unable to carry active correction safely", invalid);
            }
        }
    }

    private boolean earlierCurrentDraftExists(LocalDate today) {
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(DRAFT + "|")) continue;
            String[] parts = key.split("\\|", -1);
            if (parts.length != 3) throw new IllegalStateException("Invalid active draft key");
            LocalDate date = LocalDate.parse(parts[1]);
            Slot slot = Slot.valueOf(parts[2]);
            if (!date.isBefore(today) || receipt(date, slot) != null) continue;
            Draft old = draft(date, slot);
            if (old != null && isCurrent(old, currentCursor(old.start))) return true;
        }
        return false;
    }

    boolean checkpoint(LocalDate date, Slot slot, VerseRef start, VerseRef end, int page, long elapsed) {
        return checkpoint(date, slot, start, end, page, elapsed, false, LEGACY_MINUTES);
    }

    boolean checkpoint(LocalDate date, Slot slot, VerseRef start, VerseRef end, int page, long elapsed,
            boolean paused) {
        return checkpoint(date, slot, start, end, page, elapsed, paused, LEGACY_MINUTES);
    }

    boolean checkpoint(LocalDate date, Slot slot, VerseRef start, VerseRef end, int page, long elapsed,
            boolean paused, int proposedMinutes) {
        synchronized (LOCK) {
            if (receipt(date, slot) != null || !canOpen(date, slot)) return false;
            Draft saved = draft(date, slot);
            if (saved == null && (earlierCurrentDraftExists(date)
                    || !start.equals(currentCursor(start)))) return false;
            if (saved != null && (!saved.start.equals(start)
                    || !isCurrent(saved, currentCursor(start)))) return false;
            long checkpointRevision = saved == null ? revision() : saved.revision;
            long savedElapsed = saved == null ? 0L : saved.elapsedMs;
            return save(date, slot, start, end, page, Math.max(elapsed, savedElapsed),
                checkpointRevision, paused, saved == null ? proposedMinutes : saved.targetMinutes);
        }
    }

    private JSONObject encode(VerseRef start, VerseRef end, int page, long elapsed, long revision,
            boolean paused, int minutes)
            throws JSONException {
        return new JSONObject().put("start", start.toString())
            .put("end", end == null ? "" : end.toString()).put("page", page)
            .put("elapsedMs", Math.min(minutes * 60_000L, elapsed)).put("revision", revision)
            .put("targetMinutes", minutes)
            .put("paused", paused);
    }

    private boolean save(LocalDate date, Slot slot, VerseRef start, VerseRef end, int page,
            long elapsed, long revision, boolean paused, int minutes) {
        if (minutes < LEGACY_MINUTES || minutes % 15 != 0) return false;
        if (start == null || page < 1 || page > 604 || elapsed < 0L) return false;
        try {
            JSONObject doc = encode(start, end, page, elapsed, revision, paused, minutes);
            return prefs.edit().putString(DRAFT + scope(date, slot), doc.toString()).commit();
        } catch (JSONException invalid) {
            throw new IllegalStateException("Impossible d’enregistrer le créneau de révision.", invalid);
        }
    }

    ActiveReviewCorrectionStore correction(LocalDate date, Slot slot) {
        return new ActiveReviewCorrectionStore(prefs, scope(date, slot));
    }

    LocalDate dateToResume(LocalDate today, Slot slot) {
        LocalDate date = today;
        String suffix = "|" + slot.name();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(DRAFT + "|") || !key.endsWith(suffix)) continue;
            LocalDate candidate = LocalDate.parse(
                key.substring(DRAFT.length() + 1, key.length() - suffix.length()));
            if (candidate.isBefore(date)) {
                ActiveReviewCorrectionStore correction = correction(candidate, slot);
                Draft saved = draft(candidate, slot);
                if (correction.pending() != null
                        || saved != null && !correction.revealed(candidate.toString(), saved.start).isEmpty())
                    date = candidate;
            }
        }
        return date;
    }

    boolean restoreExpiredRevealCorrection(LocalDate date, Slot slot) {
        if (!date.isBefore(HifzClock.today())) return false;
        synchronized (LOCK) {
            Draft saved = draft(date, slot);
            if (saved == null || receipt(date, slot) != null) return false;
            ActiveReviewCorrectionStore correction = correction(date, slot);
            if (correction.pending() != null) return true;
            List<Integer> pages = ActiveReviewCorrectionPlan.select(
                correction.revealed(date.toString(), saved.start));
            return !pages.isEmpty() && correction.begin(date.toString(), saved.start,
                saved.end == null ? saved.start : saved.end, saved.elapsedMs, pages, saved.page);
        }
    }

    String receipt(LocalDate date, Slot slot) {
        return prefs.getString(RECEIPT + scope(date, slot), null);
    }

    int targetMinutes(LocalDate date, Slot slot, int proposedMinutes) {
        if (receipt(date, slot) != null)
            return prefs.getInt(RECEIPT_MINUTES + scope(date, slot), LEGACY_MINUTES);
        Draft saved = draft(date, slot);
        return saved == null ? proposedMinutes : saved.targetMinutes;
    }

    List<DashboardLedger.Record> receipts() {
        List<DashboardLedger.Record> records = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(RECEIPT + "|")) continue;
            String[] parts = key.split("\\|", -1);
            if (parts.length != 3 || !(entry.getValue() instanceof String))
                throw new IllegalStateException("Reçu de révision active invalide.");
            records.add(new DashboardLedger.Record(LocalDate.parse(parts[1]),
                Slot.valueOf(parts[2]).mode, (String) entry.getValue()));
        }
        return records;
    }

    boolean complete(LocalDate date, Slot slot, VerseRef start, VerseRef next, String label) {
        synchronized (LOCK) {
            if (!date.equals(HifzClock.today())) return false;
            Draft saved = draft(date, slot);
            if (saved == null || !saved.start.equals(start) || saved.end == null || next == null
                    || saved.elapsedMs != saved.durationMs() || receipt(date, slot) != null
                    || !canOpen(date, slot) || !isCurrent(saved, currentCursor(start))
                    || label == null || label.isEmpty()) return false;

            ActiveReviewCorrectionStore correction = correction(date, slot);
            ActiveReviewCorrectionStore.State replay = correction.pending();
            if (replay != null) {
                if (!"DONE".equals(replay.stage) || !date.toString().equals(replay.date)
                        || !start.equals(replay.start) || !saved.end.equals(replay.end)
                        || replay.activeElapsedMs != saved.durationMs()) return false;
            } else if (!correction.revealed(date.toString(), start).isEmpty()) {
                return false;
            }

            SharedPreferences.Editor editor = correction.clear(prefs.edit())
                .putString(RECEIPT + scope(date, slot), label)
                .putInt(RECEIPT_MINUTES + scope(date, slot), saved.targetMinutes)
                .remove(DRAFT + scope(date, slot));
            editor.putString(CURSOR, next.toString()).putLong(REVISION, revision() + 1L);
            return editor.commit();
        }
    }

    private VerseRef currentCursor(VerseRef start) {
        return GeometryRepository.parseVerse(prefs.getString(CURSOR, start.toString()));
    }

    boolean finishCorrectionWithoutReceipt(LocalDate date, Slot slot) {
        synchronized (LOCK) {
            LocalDate today = HifzClock.today();
            if (!date.isBefore(today)) return false;
            Draft saved = draft(date, slot);
            ActiveReviewCorrectionStore correction = correction(date, slot);
            ActiveReviewCorrectionStore.State replay = correction.pending();
            if (saved == null || replay == null || !"DONE".equals(replay.stage)
                    || !date.toString().equals(replay.date) || !saved.start.equals(replay.start)
                    || !replay.end.equals(saved.end == null ? saved.start : saved.end)
                    || replay.activeElapsedMs != saved.elapsedMs || receipt(date, slot) != null) return false;
            return correction.clear(prefs.edit()).remove(DRAFT + scope(date, slot)).commit();
        }
    }
}
