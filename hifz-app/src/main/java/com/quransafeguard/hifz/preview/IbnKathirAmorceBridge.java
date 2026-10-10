package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * J3 staging seam between source-verified Ibn Kathir documentary IDs and the
 * existing Hifz/reader semantic consumers. It deliberately accepts NO active
 * anchors: a lexical draft is not a mnemonic, and rights are not cleared.
 *
 * It cannot mutate Hifz state, expose an unfinished cue as live, synthesize
 * Arabic, or override SemanticPassageRepository before editorial acceptance.
 */
final class IbnKathirAmorceBridge {
    static final String SOURCE_COMMIT = IbnKathirGroupIndex.SOURCE_COMMIT;
    static final String BOUNDARY_SHA256 =
        "deffec6e775965d7b1f717a63fc4d0d60c1933540aacefc13e1333d30c49be62";
    static final String HAFS_SHA256 =
        "9b9eb07ff5cff144bf964400924e593d97115e4b937db3c61783f782a5168075";

    static final class StagedGate {
        final int documentaryGroups;
        final int pagesWithAtLeastOneStart;
        final int approved;
        final int pending;
        final boolean runtimeReady;
        StagedGate(int groups, int pages, int approved, int pending) {
            this.documentaryGroups = groups;
            this.pagesWithAtLeastOneStart = pages;
            this.approved = approved;
            this.pending = pending;
            this.runtimeReady = false;
        }
    }

    private IbnKathirAmorceBridge() {}

    private static void demand(boolean valid, String detail) {
        if (!valid) throw new IllegalStateException("J3 Ibn Kathir handoff NO GO: " + detail);
    }

    /**
     * Reject all unapproved or mutated payloads. A future, reviewed approval
     * manifest MUST use a new schema and independently verified loader; this
     * J2 review file must NEVER be cast into an active cue collection.
     */
    static StagedGate inspectReviewOnly(String json, IbnKathirGroupIndex index,
                                        GeometryRepository geometry) throws JSONException {
        demand(json != null && index != null && geometry != null, "missing source/geometry");
        JSONObject root = new JSONObject(json);
        demand("IK_J2_CONTRADICTORY_REVIEW_V1".equals(root.optString("schema")),
            "not the pinned editorial review schema");
        demand(!root.optBoolean("runtime_ready", true), "runtime flag activated");
        JSONObject provenance = root.getJSONObject("provenance");
        demand(BOUNDARY_SHA256.equals(provenance.optString("ibn_kathir_boundaries_sha256")),
            "group boundary SHA changed");
        demand(HAFS_SHA256.equals(provenance.optString("quran_ws_hafs_sha256")),
            "Hafs SHA changed");
        JSONObject counts = root.getJSONObject("counts");
        demand(counts.optInt("groups", -1) == IbnKathirGroupIndex.EXPECTED_GROUPS
               && counts.optInt("approved_keys", -1) == 0
               && counts.optInt("groups_pending_editorial_decision", -1)
                    == IbnKathirGroupIndex.EXPECTED_GROUPS, "unverified approval counts");
        JSONArray entries = root.getJSONArray("entries");
        demand(index.count() == IbnKathirGroupIndex.EXPECTED_GROUPS
               && entries.length() == IbnKathirGroupIndex.EXPECTED_GROUPS,
               "incomplete/oversized documentary index");

        Map<String,IbnKathirGroupIndex.Group> source = new HashMap<>();
        for (int surah=1;surah<=114;surah++) {
            for (IbnKathirGroupIndex.Group group : index.groupsForSurah(surah)) {
                demand(source.put(group.id, group) == null, "duplicated frozen group ID");
            }
        }
        demand(source.size() == IbnKathirGroupIndex.EXPECTED_GROUPS,
            "source index incomplete");
        Set<String> seen = new HashSet<>();
        Set<Integer> pagesWithBeginnings = new HashSet<>();
        for (int i=0;i<entries.length();i++) {
            JSONObject row = entries.getJSONObject(i);
            String id = row.optString("id", "");
            IbnKathirGroupIndex.Group group = source.get(id);
            demand(group != null && seen.add(id), "unknown or duplicated group: "+id);
            int surah = row.optInt("surah", -1);
            int first = row.optInt("start_ayah", -1);
            int last = row.optInt("end_ayah", -1);
            demand(surah == group.surah && first == group.startAyah
                   && last == group.endAyah, id+": changed digital boundary");
            VerseRef begin = new VerseRef(surah,first);
            VerseRef end = new VerseRef(surah,last);
            int firstPage = row.optInt("start_page", -1);
            int lastPage = row.optInt("end_page", -1);
            demand(firstPage == geometry.pageForVerse(begin)
                   && lastPage == geometry.pageForVerse(end),
                   id+": QCF page differs from real Mushaf");
            demand((surah+":"+first+":1").equals(row.optString("first_word_key")),
                   id+": QCF first-word key differs");
            pagesWithBeginnings.add(firstPage);
            JSONArray approved = row.optJSONArray("approved_keys");
            JSONArray extra = row.optJSONArray("extra_cues_within_group");
            demand("UNREVIEWED".equals(row.optString("human_disposition"))
                   && approved != null && approved.length() == 0
                   && extra != null && extra.length() == 0
                   && !row.optBoolean("runtime_enabled",true)
                   && !row.optBoolean("word_by_word_qcf_verified_for_selected_cue",true)
                   && !row.optBoolean("mnemonic_context_minimality_approved",true)
                   && !row.optBoolean("text_reuse_rights_confirmed",true),
                   id+": draft cannot be a live mnemonic");
            // A reference may need no key, one or several; never infer a quota.
        }
        demand(seen.equals(source.keySet()), "missing documentary group");
        demand(pagesWithBeginnings.size() == 604,
               "a physical Mushaf page has no verified group start");
        return new StagedGate(seen.size(),pagesWithBeginnings.size(),0,seen.size());
    }
}
