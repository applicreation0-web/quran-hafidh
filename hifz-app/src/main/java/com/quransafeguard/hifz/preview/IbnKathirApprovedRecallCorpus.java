package com.quransafeguard.hifz.preview;

import android.content.Context;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fail-closed J5 adapter to the EXACT existing SemanticPassageRepository.Cue API.
 *
 * The expected reviewed-manifest SHA is deliberately UNSET. The real application
 * keeps the v1.17.1 Hifz/Quiz behaviour until a complete, independently reviewed
 * QCF recitation corpus is pinned in a separate change and audited in a debug APK.
 *
 * Group != amorce: one group may contain zero, one, or many actual recall starts.
 */
final class IbnKathirApprovedRecallCorpus {
    static final String ASSET_PATH = "ibn-kathir/approved-recall-v1.json";
    static final String EXPECTED_APPROVED_MANIFEST_SHA256 = ""; // NO GO; never auto-approve drafts
    static final String SCHEMA = "IK_QCF_RECITATION_APPROVED_V1";

    static final class Corpus {
        final Map<Integer,List<SemanticPassageRepository.Cue>> byPage;
        final Map<String,SemanticPassageRepository.Cue> byId;
        final Map<String,JSONArray> wordBoxes;
        Corpus(Map<Integer,List<SemanticPassageRepository.Cue>> byPage,
                Map<String,SemanticPassageRepository.Cue> byId,Map<String,JSONArray> wordBoxes) {
            this.byPage=byPage;
            this.byId=byId;
            this.wordBoxes=wordBoxes;
        }
        JSONArray boxes(SemanticPassageRepository.Cue cue) {
            JSONArray data=cue==null?null:wordBoxes.get(cue.passageId);
            return data==null?new JSONArray():data;
        }
    }

    private static final class Pos {
        final int page,offset;
        Pos(int page,int offset){this.page=page;this.offset=offset;}
    }

    private IbnKathirApprovedRecallCorpus(){}

    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Ibn Kathir approved-recall NO GO: "+message);
    }

    static boolean activationPinned(){
        return EXPECTED_APPROVED_MANIFEST_SHA256.matches("[0-9a-f]{64}")
           && !EXPECTED_APPROVED_MANIFEST_SHA256.equals(
                "0000000000000000000000000000000000000000000000000000000000000000");
    }

    /**
     * This method changes nothing in 1.17.1 while the audit SHA is not pinned.
     * No runtime setting, previous preference or unreviewed J2 JSON can enable it.
     */
    static Corpus tryLoad(Context context,WordGeometryRepository wordGeometry) {
        if(!activationPinned())return null;
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream input=context.getAssets().open(ASSET_PATH)){
                byte[] buffer=new byte[8192];
                int n;
                while((n=input.read(buffer))!=-1){
                    out.write(buffer,0,n);
                    require(out.size()<2_000_000,"unexpectedly large approval payload");
                }
            }
            byte[] raw=out.toByteArray();
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(raw);
            StringBuilder sha=new StringBuilder(64);
            for(byte v:hash)sha.append(String.format(java.util.Locale.ROOT,"%02x",v&255));
            require(EXPECTED_APPROVED_MANIFEST_SHA256.equals(sha.toString()),
                "editorially reviewed manifest digest changed");
            return parseForTest(new String(raw,StandardCharsets.UTF_8),
                collectWordGeometry(wordGeometry),IbnKathirGroupIndex.shared());
        } catch(Throwable incomplete){
            // Preserve exact old Hifz mode semantics, never activate a partial corpus.
            return null;
        }
    }

    static Map<Integer,List<WordGeometryRepository.WordBox>> collectWordGeometry(
            WordGeometryRepository words) {
        Map<Integer,List<WordGeometryRepository.WordBox>> all=new HashMap<>();
        for(int page=1;page<=604;page++) {
            List<WordGeometryRepository.WordBox> row=words.wordsForPage(page);
            require(!row.isEmpty(),"missing physical page "+page);
            all.put(page,row);
        }
        return all;
    }

    /**
     * The JVM-only positive case uses synthetic approval flags to test geometry;
     * it does not make those synthetic flags editorially genuine. Production
     * activation *additionally* requires the SHA explicitly pinned above.
     */
    static Corpus parseForTest(String raw,
            Map<Integer,List<WordGeometryRepository.WordBox>> pages,
            IbnKathirGroupIndex index) throws JSONException {
        require(raw!=null&&pages!=null&&index!=null&&index.count()==1903,
            "missing pinned source index/geometry");
        JSONObject root=new JSONObject(raw);
        require(SCHEMA.equals(root.optString("schema"))
            && IbnKathirGroupIndex.SOURCE_COMMIT.equals(root.optString("source_commit"))
            && IbnKathirAmorceBridge.BOUNDARY_SHA256.equals(root.optString("boundary_sha256"))
            && IbnKathirAmorceBridge.HAFS_SHA256.equals(root.optString("hafs_sha256"))
            && root.optBoolean("full_editorial_review_complete",false)
            && root.optBoolean("ready_for_runtime",false),
            "source, complete-review, or runtime sign-off missing");

        Map<String,Pos> coordinate=new HashMap<>();
        Map<Integer,List<WordGeometryRepository.WordBox>> checkedPages=new HashMap<>();
        for(int page=1;page<=604;page++) {
            List<WordGeometryRepository.WordBox> row=pages.get(page);
            require(row!=null&&!row.isEmpty(),"incomplete QCF page "+page);
            checkedPages.put(page,row);
            for(int i=0;i<row.size();i++) {
                String key=row.get(i).key;
                require(coordinate.put(key,new Pos(page,i))==null,
                    "duplicated QCF word "+key);
            }
        }
        require(coordinate.size()==77432,"incomplete 77432 QCF word keys");

        JSONArray groups=root.getJSONArray("groups");
        require(groups.length()==1903,"every documentary group requires a decision");
        Map<String,IbnKathirGroupIndex.Group> source=new HashMap<>();
        for(int s=1;s<=114;s++)
            for(IbnKathirGroupIndex.Group g:index.groupsForSurah(s))
                require(source.put(g.id,g)==null,"duplicate documentary group");
        Set<String> seenGroups=new HashSet<>();
        Set<String> seenKeys=new HashSet<>();
        Map<Integer,List<SemanticPassageRepository.Cue>> pageCues=new HashMap<>();
        Map<String,SemanticPassageRepository.Cue> idCues=new HashMap<>();
        Map<String,JSONArray> cueBoxes=new HashMap<>();

        for(int i=0;i<groups.length();i++) {
            JSONObject groupRow=groups.getJSONObject(i);
            String groupId=groupRow.optString("id","");
            IbnKathirGroupIndex.Group group=source.get(groupId);
            require(group!=null&&seenGroups.add(groupId),"unknown/repeated documentary group "+groupId);
            require(groupRow.optBoolean("human_recitation_review_approved",false),
                "group has no independent recitation decision "+groupId);
            JSONArray cues=groupRow.getJSONArray("cues");
            require(cues.length()<=80,"implausible cue count; investigate "+groupId);
            for(int j=0;j<cues.length();j++) {
                JSONObject candidate=cues.getJSONObject(j);
                require(candidate.optBoolean("semantic_recitation_start_approved",false)
                    && candidate.optBoolean("word_span_qcf_verified",false)
                    && candidate.optBoolean("minimality_checked",false),
                    "cue has no verified recitation-start approval");
                String startKey=candidate.optString("first_word_key","");
                String[] bits=startKey.split(":");
                require(bits.length==3,"bad Quran reference");
                int s,a,w;
                try{s=Integer.parseInt(bits[0]);a=Integer.parseInt(bits[1]);w=Integer.parseInt(bits[2]);}
                catch(NumberFormatException invalid){throw new IllegalStateException("bad word key "+startKey);}
                require(s==group.surah&&a>=group.startAyah&&a<=group.endAyah&&w>=1
                    && seenKeys.add(startKey),"cue does not belong to source group or repeats "+startKey);
                int n=candidate.optInt("word_count",0);
                require(n>=1&&n<=80,"bad approved recitation key length");
                Pos position=coordinate.get(startKey);
                require(position!=null,"word missing from canonical Mushaf "+startKey);
                List<WordGeometryRepository.WordBox> pageWords=checkedPages.get(position.page);
                require(position.offset+n<=pageWords.size(),"cue crosses QCF page "+startKey);
                JSONArray boxes=new JSONArray();
                for(int k=0;k<n;k++) {
                    WordGeometryRepository.WordBox wb=pageWords.get(position.offset+k);
                    // Crossing an ayah boundary is okay only when still in the same
                    // verified Ibn Kathir documentary group and on the same page.
                    require(wb.surah==s&&wb.ayah>=a&&wb.ayah<=group.endAyah,
                        "cue crosses documentary group "+startKey);
                    boxes.put(wb.boxJson());
                }
                String id=groupId+"@"+startKey;
                SemanticPassageRepository.Cue cue=new SemanticPassageRepository.Cue(
                    id,position.page,pageCues.containsKey(position.page)
                        ? pageCues.get(position.page).size()+1:1,
                    "Départ de récitation · "+group.navigationRange(),
                    "",n,new VerseRef(s,a),new VerseRef(s,group.endAyah),
                    position.page,position.page,0,0,0,w,w+n-1,true,
                    Collections.emptyList());
                pageCues.computeIfAbsent(position.page,unused->new ArrayList<>()).add(cue);
                require(idCues.put(id,cue)==null,"duplicate cue id "+id);
                cueBoxes.put(id,boxes);
            }
        }
        require(seenGroups.equals(source.keySet()),"some source groups never reviewed");
        // An anchored Itqan page with NO cue would be fully blacked out.
        // This is a safety gate, not a quota per group or a mnemonic selection rule.
        // Individual unit ranges must still undergo post-debug runtime audit.
        require(pageCues.size()==604,"masked recall would have pages with zero approved starts");
        for(List<SemanticPassageRepository.Cue> list:pageCues.values())
            list.sort((a,b)->Integer.compare(a.firstWordPosition,b.firstWordPosition));
        return new Corpus(pageCues,idCues,cueBoxes);
    }
}
