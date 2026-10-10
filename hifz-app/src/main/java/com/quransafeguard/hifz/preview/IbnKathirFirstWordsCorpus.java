package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic 1.17.1 Ibn Kathir cue source: the first TWO exact Mushaf words of
 * each of the 1,903 verified commentary groups, or fewer only if the group ends.
 *
 * No editorial selection, invented Arabic, text parsing, title or commentary.
 * When a cue crosses a physical page, only same-page QCF boxes are provided;
 * the consuming masked mode must fail open if the expected two boxes are absent.
 */
final class IbnKathirFirstWordsCorpus {
    private static final int EXPECTED_QCF_WORDS = 77432;
    private static final int EXPECTED_PAGES = 604;

    private static final class Position {
        final int page, offset;
        Position(int page, int offset) { this.page=page; this.offset=offset; }
    }

    private IbnKathirFirstWordsCorpus() {}

    static IbnKathirApprovedRecallCorpus.Corpus load(WordGeometryRepository words) {
        if (words == null) return null;
        try {
            Map<Integer,List<WordGeometryRepository.WordBox>> pages = new HashMap<>();
            for (int page=1;page<=EXPECTED_PAGES;page++) {
                List<WordGeometryRepository.WordBox> row=words.wordsForPage(page);
                if (row.isEmpty()) return null;
                pages.put(page,row);
            }
            return build(pages,IbnKathirGroupIndex.shared());
        } catch (RuntimeException incomplete) {
            // Missing geometry must NEVER silently reactivate Al-Munir or create mask holes.
            return null;
        }
    }

    static IbnKathirApprovedRecallCorpus.Corpus build(
            Map<Integer,List<WordGeometryRepository.WordBox>> pages,
            IbnKathirGroupIndex index) {
        if (pages==null || index==null || index.count()!=1903 || pages.size()!=EXPECTED_PAGES)
            throw new IllegalStateException("Incomplete Ibn Kathir/QCF source");
        Map<String,Position> positions=new HashMap<>();
        Map<String,Integer> verseEndPages=new HashMap<>();
        for(int page=1;page<=EXPECTED_PAGES;page++) {
            List<WordGeometryRepository.WordBox> row=pages.get(page);
            if(row==null || row.isEmpty()) throw new IllegalStateException("Missing QCF page "+page);
            for(int offset=0;offset<row.size();offset++) {
                WordGeometryRepository.WordBox w=row.get(offset);
                if(positions.put(w.key,new Position(page,offset))!=null)
                    throw new IllegalStateException("Duplicate QCF word "+w.key);
                verseEndPages.put(w.surah+":"+w.ayah,page);
            }
        }
        if(positions.size()!=EXPECTED_QCF_WORDS)
            throw new IllegalStateException("Incomplete Quranic word coordinates "+positions.size());

        Map<Integer,List<SemanticPassageRepository.Cue>> byPage=new HashMap<>();
        Map<String,SemanticPassageRepository.Cue> byId=new HashMap<>();
        Map<String,JSONArray> boxesById=new HashMap<>();
        Set<String> seen=new HashSet<>();
        for(int surah=1;surah<=114;surah++) {
            for(IbnKathirGroupIndex.Group group:index.groupsForSurah(surah)) {
                String key=surah+":"+group.startAyah+":1";
                Position start=positions.get(key);
                if(start==null) throw new IllegalStateException("Missing group first word "+key);
                WordGeometryRepository.WordBox first=pages.get(start.page).get(start.offset);
                if(first.word!=1 || first.surah!=surah || first.ayah!=group.startAyah)
                    throw new IllegalStateException("Group-start QCF mismatch "+key);

                // Word 2 is the immediately following Quranic word, potentially the
                // first word of the next ayah. Never cross the group/surah boundary.
                WordGeometryRepository.WordBox next=null;
                int nextPage=start.page;
                List<WordGeometryRepository.WordBox> row=pages.get(start.page);
                if(start.offset+1<row.size()) next=row.get(start.offset+1);
                else if(start.page<EXPECTED_PAGES) {
                    nextPage=start.page+1;
                    List<WordGeometryRepository.WordBox> following=pages.get(nextPage);
                    if(following!=null && !following.isEmpty()) next=following.get(0);
                }
                boolean inside=next!=null && next.surah==surah
                    && next.ayah>=group.startAyah && next.ayah<=group.endAyah
                    && (next.ayah==first.ayah ? next.word==first.word+1
                        : next.ayah==first.ayah+1 && next.word==1);
                int count=inside?2:1;
                JSONArray physicalBoxes=new JSONArray().put(first.boxJson());
                if(inside && nextPage==start.page) physicalBoxes.put(next.boxJson());

                Integer lastPage=verseEndPages.get(surah+":"+group.endAyah);
                if(lastPage==null || lastPage<start.page)
                    throw new IllegalStateException("Group end missing in QCF "+group.id);
                int indexOnPage=byPage.containsKey(start.page)?byPage.get(start.page).size()+1:1;
                SemanticPassageRepository.Cue cue=new SemanticPassageRepository.Cue(
                    group.id,start.page,indexOnPage,
                    "Ibn Kathīr · "+group.navigationRange(),"",count,
                    new VerseRef(surah,group.startAyah),new VerseRef(surah,group.endAyah),
                    start.page,lastPage,0,0,0,1,count,true,Collections.emptyList());
                if(!seen.add(group.id) || byId.put(group.id,cue)!=null)
                    throw new IllegalStateException("Duplicated group "+group.id);
                byPage.computeIfAbsent(start.page,p->new ArrayList<>()).add(cue);
                boxesById.put(group.id,physicalBoxes);
            }
        }
        if(seen.size()!=1903 || byPage.size()!=EXPECTED_PAGES)
            throw new IllegalStateException("Incomplete 1903 groups / 604 page starts");
        return new IbnKathirApprovedRecallCorpus.Corpus(byPage,byId,boxesById);
    }
}
