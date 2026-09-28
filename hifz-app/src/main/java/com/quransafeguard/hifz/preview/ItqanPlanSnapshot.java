package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * P3's persisted "finish the page?" decision for the Itqān sub-block currently at rep 0. Tied to
 * the exact sub-block it was computed for — the parent unit's bounds plus its own block index,
 * the same identity HifzSessionActivity.renderItqan already persists via itqanUnitStart/
 * itqanUnitEnd/itqanBlockIndex — so a stale decision left over from a previous block, or from a
 * block that never got this far, can never leak into rendering a different one.
 *
 * Reps/assistance/finalReveals/elapsed stay the existing counters' own job; this only remembers
 * UNDECIDED/KEEP/EXTEND and, for EXTEND, exactly which lines were accepted. Immutable: every
 * transition returns a new instance rather than mutating in place, matching how the rest of this
 * package treats persisted session state.
 */
final class ItqanPlanSnapshot {
    enum Decision { UNDECIDED, KEEP, EXTEND }

    final VerseRef unitStart;
    final VerseRef unitEnd;
    final int blockIndex;
    final Decision decision;
    final List<String> bonusLineIds;

    private ItqanPlanSnapshot(VerseRef unitStart, VerseRef unitEnd, int blockIndex,
                              Decision decision, List<String> bonusLineIds) {
        this.unitStart = unitStart;
        this.unitEnd = unitEnd;
        this.blockIndex = blockIndex;
        this.decision = decision;
        this.bonusLineIds = Collections.unmodifiableList(new ArrayList<>(bonusLineIds));
    }

    static ItqanPlanSnapshot undecided(VerseRef unitStart, VerseRef unitEnd, int blockIndex) {
        return new ItqanPlanSnapshot(unitStart, unitEnd, blockIndex, Decision.UNDECIDED, Collections.emptyList());
    }

    ItqanPlanSnapshot keep() {
        return new ItqanPlanSnapshot(unitStart, unitEnd, blockIndex, Decision.KEEP, Collections.emptyList());
    }

    ItqanPlanSnapshot extend(List<String> bonusLineIds) {
        if (bonusLineIds == null || bonusLineIds.isEmpty() || bonusLineIds.size() > 2)
            throw new IllegalArgumentException("bonus must be exactly 1 or 2 lines");
        return new ItqanPlanSnapshot(unitStart, unitEnd, blockIndex, Decision.EXTEND, bonusLineIds);
    }

    /** A snapshot only ever answers for the exact sub-block it was computed for — a different
     *  parent unit or a different block index within the same parent is never a match. */
    boolean matches(VerseRef otherUnitStart, VerseRef otherUnitEnd, int otherBlockIndex) {
        return blockIndex == otherBlockIndex
            && unitStart.equals(otherUnitStart)
            && unitEnd.equals(otherUnitEnd);
    }

    JSONObject toJson() {
        try {
            JSONArray bonus = new JSONArray();
            for (String id : bonusLineIds) bonus.put(id);
            return new JSONObject()
                .put("unitStart", unitStart.toString())
                .put("unitEnd", unitEnd.toString())
                .put("blockIndex", blockIndex)
                .put("decision", decision.name())
                .put("bonusLineIds", bonus);
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** @throws JSONException on any malformed/incomplete blob — the caller decides whether that's
     *  a hard failure or a silently-recoverable "treat as no pending decision" (HifzPrefs does the
     *  latter). */
    static ItqanPlanSnapshot fromJson(JSONObject json) throws JSONException {
        VerseRef unitStart = GeometryRepository.parseVerse(json.getString("unitStart"));
        VerseRef unitEnd = GeometryRepository.parseVerse(json.getString("unitEnd"));
        int blockIndex = json.getInt("blockIndex");
        Decision decision = Decision.valueOf(json.getString("decision"));
        JSONArray bonusArray = json.getJSONArray("bonusLineIds");
        List<String> bonusLineIds = new ArrayList<>();
        for (int i = 0; i < bonusArray.length(); i++) bonusLineIds.add(bonusArray.getString(i));
        switch (decision) {
            case UNDECIDED: return undecided(unitStart, unitEnd, blockIndex);
            case KEEP: return undecided(unitStart, unitEnd, blockIndex).keep();
            case EXTEND: return undecided(unitStart, unitEnd, blockIndex).extend(bonusLineIds);
            default: throw new JSONException("unknown decision: " + decision);
        }
    }
}
