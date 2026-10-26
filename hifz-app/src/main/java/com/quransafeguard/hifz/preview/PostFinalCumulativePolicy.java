package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Post-validation *non-crediting* cumulative recall: at most the last 20
 * previously-acquired pages, followed by the pages of the just-completed final.
 * Never expands an eligible physical line into unearned lines on the same page.
 * Input order of acquired lines reflects their persisted acquisition order.
 */
final class PostFinalCumulativePolicy {
    static final int MAX_PREVIOUS_PAGES = 20;
    private PostFinalCumulativePolicy() {}

    static final class Page {
        final int mushafPage;
        final List<String> lineIds;
        final boolean fromCurrentFinal;
        Page(int mushafPage,List<String> lineIds,boolean fromCurrentFinal){
            if(mushafPage<1||mushafPage>604||lineIds==null||lineIds.isEmpty())
                throw new IllegalArgumentException("Invalid post-final physical page");
            this.mushafPage=mushafPage;
            this.lineIds=Collections.unmodifiableList(new ArrayList<>(lineIds));
            this.fromCurrentFinal=fromCurrentFinal;
        }
    }

    static List<Page> plan(List<GeometryRepository.LineMeta> previouslyAcquired,
                           List<GeometryRepository.LineMeta> justValidated) {
        if(previouslyAcquired==null||justValidated==null)
            throw new IllegalArgumentException("Explicit before/after final snapshots required");
        LinkedHashMap<Integer,LinkedHashMap<String,GeometryRepository.LineMeta>> previous=new LinkedHashMap<>();
        LinkedHashMap<Integer,LinkedHashMap<String,GeometryRepository.LineMeta>> current=new LinkedHashMap<>();
        for(GeometryRepository.LineMeta line:justValidated) {
            if(line==null||line.id==null||line.id.isEmpty())throw new IllegalArgumentException("Invalid final line");
            current.computeIfAbsent(line.page,k->new LinkedHashMap<>()).put(line.id,line);
        }
        if(current.isEmpty())throw new IllegalArgumentException("At least one validated page required");
        for(GeometryRepository.LineMeta line:previouslyAcquired) {
            if(line==null||line.id==null||line.id.isEmpty())throw new IllegalArgumentException("Invalid acquired line");
            if(current.containsKey(line.page))continue; // current page must not appear twice
            previous.computeIfAbsent(line.page,k->new LinkedHashMap<>()).put(line.id,line);
        }
        ArrayList<Integer> priorOrder=new ArrayList<>(previous.keySet());
        int first=Math.max(0,priorOrder.size()-MAX_PREVIOUS_PAGES);
        ArrayList<Page> out=new ArrayList<>();
        for(int i=first;i<priorOrder.size();i++){
            int number=priorOrder.get(i);
            out.add(toPage(number,previous.get(number),false));
        }
        for(Map.Entry<Integer,LinkedHashMap<String,GeometryRepository.LineMeta>> e:current.entrySet())
            out.add(toPage(e.getKey(),e.getValue(),true));
        return Collections.unmodifiableList(out);
    }

    private static Page toPage(int number,Map<String,GeometryRepository.LineMeta> lines,boolean weekly) {
        ArrayList<GeometryRepository.LineMeta> sorted=new ArrayList<>(lines.values());
        sorted.sort(Comparator.comparingInt(v->v.globalIndex));
        ArrayList<String> ids=new ArrayList<>();
        Set<Integer> indices=new HashSet<>();
        for(GeometryRepository.LineMeta line:sorted){
            if(line.page!=number||!indices.add(line.globalIndex))
                throw new IllegalStateException("Repeated or cross-page physical line");
            ids.add(line.id);
        }
        return new Page(number,ids,weekly);
    }
}
