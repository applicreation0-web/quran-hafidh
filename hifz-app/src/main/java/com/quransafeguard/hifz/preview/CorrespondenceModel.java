package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, read-only correspondence contracts. No corpus, classification inference,
 * renderer or persistence is supplied here. Approval flags must come from the
 * independently reviewed editorial/visual/QA pipeline, never from these classes.
 */
public final class CorrespondenceModel {
    private CorrespondenceModel() { }

    public enum Type {
        REPETITION, RESSEMBLANCE, OPPOSITION, PROGRESSION,
        ALTERNANCE, INVERSION, CENTRE, DEBUT_FIN_INCLUSIO
    }
    public enum Mode { TWO_PAGES, SERIES, INTERNAL }
    public enum Scope { INTRA, INTER }

    /** Adapter to the authentic geometry index. Last page includes ayah continuations.
     * Segment approval requires actual word/segment geometry, not an ayah polygon. */
    public interface Geometry {
        int ayahCount(int surah);
        int firstPage(int surah, int ayah);
        int lastPage(int surah, int ayah);
        boolean hasSegmentGeometry(int surah, int ayah, int firstWord, int lastWord);
    }

    public static final class VerseRange {
        public final int surah, start, end, firstPage, lastPage;
        public VerseRange(int surah, int start, int end, Geometry geometry) {
            require(geometry!=null && surah>=1 && surah<=114 && start>=1 && end>=start,
                "verse bounds/geometry");
            require(end<=geometry.ayahCount(surah),"ayah outside surah");
            int first=0, last=0, previousFirst=0, previousLast=0;
            for(int ayah=start;ayah<=end;ayah++) {
                int f=geometry.firstPage(surah,ayah), l=geometry.lastPage(surah,ayah);
                require(f>=1 && l>=f && l<=604 && f>=previousFirst && l>=previousLast,
                    "missing or inconsistent QCF page geometry");
                if(ayah==start) first=f;
                last=l; previousFirst=f; previousLast=l;
            }
            this.surah=surah; this.start=start; this.end=end;
            this.firstPage=first; this.lastPage=last;
        }
        public boolean contains(int surah, int ayah) {
            return this.surah==surah && start<=ayah && ayah<=end;
        }
        private boolean contains(VerseRange r) {
            return r!=null && contains(r.surah,r.start) && contains(r.surah,r.end);
        }
    }

    /** Explicit source-attested focal bounds; originals are retained separately.
     * firstWord=lastWord=0 requests only verse focus, never word hatching. */
    public static final class Anchor {
        public final int originalIndex, firstWord, lastWord;
        public final VerseRange range;
        public final boolean sourceAttested;
        public final String sourceLocator;
        public Anchor(int originalIndex, VerseRange range, boolean sourceAttested,
                      String sourceLocator, int firstWord, int lastWord) {
            require(originalIndex>=0 && range!=null,"anchor origin/range");
            require(sourceLocator!=null && (!sourceAttested || !sourceLocator.trim().isEmpty()),
                "attested anchor needs a source locator");
            require((firstWord==0 && lastWord==0) ||
                (firstWord>=1 && lastWord>=firstWord && range.start==range.end),
                "word segment must be complete and within one ayah");
            this.originalIndex=originalIndex; this.range=range;
            this.sourceAttested=sourceAttested; this.sourceLocator=sourceLocator;
            this.firstWord=firstWord; this.lastWord=lastWord;
        }
        public boolean isWordSegment() { return firstWord>0; }
    }

    public static final class Gates {
        public final boolean source, classification, anchors, visual, qa;
        public Gates(boolean source,boolean classification,boolean anchors,boolean visual,boolean qa) {
            this.source=source; this.classification=classification; this.anchors=anchors;
            this.visual=visual; this.qa=qa;
        }
        public boolean allApproved() { return source && classification && anchors && visual && qa; }
    }

    /** Supplied by the reviewed editorial pipeline; never inferred from a type or verse. */
    public static final class SourceReference {
        public final String author, work, edition, locator;
        public SourceReference(String author,String work,String edition,String locator) {
            this.author=nonBlank(author); this.work=nonBlank(work);
            this.edition=nonBlank(edition); this.locator=nonBlank(locator);
        }
    }
    public static final class Presentation {
        public final String shortTitle, factualLink;
        public final List<SourceReference> sources;
        public Presentation(String shortTitle,String factualLink,List<SourceReference> sources) {
            this.shortTitle=nonBlank(shortTitle); this.factualLink=nonBlank(factualLink);
            require(sources!=null && !sources.isEmpty(),"documented presentation sources required");
            for(SourceReference source:sources) require(source!=null,"null presentation source");
            this.sources=copy(sources);
        }
    }
    private static String nonBlank(String value) {
        require(value!=null && !value.trim().isEmpty(),"documented non-empty metadata required");
        return value.trim();
    }

    public static final class Relation {
        public final String id;
        /** Null denotes unresolved classification, not a ninth public type. */
        public final Type type;
        public final Mode mode;
        public final Scope scope;
        /** Order is significant, including for directed progression. */
        public final List<VerseRange> originals;
        public final List<Anchor> anchors;
        public final Gates gates;
        /** Null means not prepared for the current UI; the generic model stays reusable. */
        public final Presentation presentation;
        private final boolean completeAnchors, attestedCentralSegment;
        private final Geometry geometry;
        public Relation(String id,Type type,Mode mode,List<VerseRange> originals,
                        List<Anchor> anchors,Gates gates,Geometry geometry) {
            this(id,type,mode,originals,anchors,gates,geometry,null);
        }
        public Relation(String id,Type type,Mode mode,List<VerseRange> originals,
                        List<Anchor> anchors,Gates gates,Geometry geometry,Presentation presentation) {
            this.presentation=presentation;
            require(id!=null && !id.trim().isEmpty() && mode!=null && gates!=null && geometry!=null,
                "relation identity/mode/gates/geometry");
            require(originals!=null && anchors!=null,"relation ranges/anchors");
            int count=originals.size();
            require((mode==Mode.TWO_PAGES && count==2) || (mode==Mode.INTERNAL && count==1) ||
                (mode==Mode.SERIES && count>=4),"mode/reference cardinality");
            this.originals=copy(originals); this.anchors=copy(anchors);
            Set<Integer> surahs=new LinkedHashSet<>();
            for(VerseRange original:this.originals) {
                require(original!=null,"null original range");
                VerseRange validated=new VerseRange(original.surah,original.start,original.end,geometry);
                require(validated.firstPage==original.firstPage && validated.lastPage==original.lastPage,
                    "original geometry adapter mismatch");
                surahs.add(original.surah);
            }
            boolean valid=!this.anchors.isEmpty(), segment=false; boolean[] covered=new boolean[count];
            for(Anchor anchor:this.anchors) {
                require(anchor!=null && anchor.originalIndex<count,"anchor original index");
                require(this.originals.get(anchor.originalIndex).contains(anchor.range),
                    "anchor outside original bounds");
                VerseRange validated=new VerseRange(anchor.range.surah,anchor.range.start,anchor.range.end,geometry);
                require(validated.firstPage==anchor.range.firstPage && validated.lastPage==anchor.range.lastPage,
                    "anchor geometry adapter mismatch");
                covered[anchor.originalIndex]=true;
                valid &= anchor.sourceAttested;
                if(anchor.isWordSegment()) {
                    boolean exact=geometry.hasSegmentGeometry(anchor.range.surah,
                        anchor.range.start,anchor.firstWord,anchor.lastWord);
                    valid &= exact;
                    segment |= anchor.sourceAttested && exact;
                }
            }
            for(boolean coverage:covered) valid &= coverage;
            this.completeAnchors=valid; this.attestedCentralSegment=segment;
            this.id=id; this.type=type; this.mode=mode; this.gates=gates; this.geometry=geometry;
            this.scope=surahs.size()==1 ? Scope.INTRA : Scope.INTER;
        }
        public boolean displayable() {
            return type!=null && gates.allApproved() && completeAnchors &&
                (type!=Type.CENTRE || (mode==Mode.INTERNAL && attestedCentralSegment));
        }
    }

    /** Direct verse-to-approved-relations lookup. Never traverses another relation. */
    public static final class Index {
        private final Map<String,List<Relation>> byVerse;
        public Index(List<Relation> relations) {
            require(relations!=null,"relations");
            Map<String,List<Relation>> build=new LinkedHashMap<>(); Set<String> ids=new LinkedHashSet<>();
            for(Relation relation:relations) {
                require(relation!=null && ids.add(relation.id),"null/duplicate relation id");
                if(!relation.displayable()) continue;
                Set<String> seen=new LinkedHashSet<>();
                for(Anchor anchor:relation.anchors) for(int ayah=anchor.range.start;ayah<=anchor.range.end;ayah++) {
                    String key=key(anchor.range.surah,ayah);
                    if(!seen.add(key)) continue;
                    List<Relation> bucket=build.get(key);
                    if(bucket==null) { bucket=new ArrayList<>(); build.put(key,bucket); }
                    bucket.add(relation);
                }
            }
            Map<String,List<Relation>> frozen=new LinkedHashMap<>();
            for(Map.Entry<String,List<Relation>> e:build.entrySet()) frozen.put(e.getKey(),copy(e.getValue()));
            byVerse=Collections.unmodifiableMap(frozen);
        }
        public List<Relation> at(int surah,int ayah) {
            List<Relation> result=byVerse.get(key(surah,ayah));
            return result==null ? Collections.emptyList() : result;
        }
    }

    /** Opaque original reader/Atlas/Sabqi view state, never interpreted as learning credit. */
    public static final class ReaderSnapshot {
        public final int surah, ayah, page;
        public final int scrollX, scrollY;
        public final String unitId, passageId, mask, selection;
        public final double zoom;
        public ReaderSnapshot(int surah,int ayah,int page,String unitId,String passageId,
                              String mask,double zoom,String selection) {
            this(surah,ayah,page,unitId,passageId,mask,zoom,selection,0,0);
        }
        public ReaderSnapshot(int surah,int ayah,int page,String unitId,String passageId,
                              String mask,double zoom,String selection,int scrollX,int scrollY) {
            require(scrollX>=0 && scrollY>=0,"reader pan");
            this.scrollX=scrollX;this.scrollY=scrollY;
            require(surah>=1 && surah<=114 && ayah>=1,"reader verse");
            checkView(page,zoom,selection);
            require(unitId!=null && passageId!=null && mask!=null,"reader state");
            this.surah=surah; this.ayah=ayah; this.page=page; this.unitId=unitId;
            this.passageId=passageId; this.mask=mask; this.zoom=zoom; this.selection=selection;
        }
    }
    public static final class Pane {
        public final int page;
        public final double zoom;
        public final String selection;
        public final int scrollX,scrollY;
        private Pane(int page,double zoom,String selection) { this(page,zoom,selection,0,0); }
        private Pane(int page,double zoom,String selection,int scrollX,int scrollY) {
            checkView(page,zoom,selection);
            require(scrollX>=0 && scrollY>=0,"pane pan");
            this.page=page; this.zoom=zoom; this.selection=selection;
            this.scrollX=scrollX;this.scrollY=scrollY;
        }
    }
    public static final class Rail {
        public final int start,endExclusive,active;
        public final boolean hasPrevious,hasNext;
        private Rail(int count,int active) {
            int width=Math.min(5,count);
            start=Math.max(0,Math.min(active-width/2,count-width)); endExclusive=start+width;
            this.active=active; hasPrevious=start>0; hasNext=endExclusive<count;
        }
    }
    /** Fully immutable return point, including per-occurrence series positions. */
    public static final class Snapshot {
        public final List<Pane> panes;
        public final int selectedOccurrence,enlargedPane;
        public final ReaderSnapshot origin;
        private final Navigation owner;
        private final List<Pane> occurrencePanes;
        private Snapshot(Navigation nav) {
            owner=nav; panes=copy(nav.panes); occurrencePanes=copy(nav.occurrencePanes);
            selectedOccurrence=nav.selectedOccurrence; enlargedPane=nav.enlargedPane; origin=nav.origin;
        }
    }
    /** Pure navigation, retaining A/B independently and one pane for series/internal modes. */
    public static final class Navigation {
        public final Relation relation;
        private final ReaderSnapshot origin;
        private final List<Pane> panes=new ArrayList<>(), occurrencePanes=new ArrayList<>();
        private int selectedOccurrence=0, enlargedPane=-1;
        public Navigation(Relation relation,ReaderSnapshot origin) {
            require(relation!=null && relation.displayable() && origin!=null,"approved relation/origin");
            this.relation=relation; this.origin=origin;
            if(relation.mode==Mode.SERIES) {
                for(Anchor anchor:relation.anchors) occurrencePanes.add(new Pane(anchor.range.firstPage,1.0,""));
                panes.add(occurrencePanes.get(0));
            } else {
                int count=relation.mode==Mode.TWO_PAGES ? 2 : 1;
                for(int i=0;i<count;i++) {
                    int page=relation.originals.get(i).firstPage;
                    for(Anchor anchor:relation.anchors) if(anchor.originalIndex==i) { page=anchor.range.firstPage; break; }
                    panes.add(new Pane(page,1.0,""));
                }
            }
        }
        public Pane pane(int pane) { require(pane>=0 && pane<panes.size(),"pane index"); return panes.get(pane); }
        /** Display context is distinct from the immutable source relation bounds.
         * A series may show the next verse (or previous at the end of a surah),
         * including its authentic continuation page. This never creates an anchor. */
        public int normalNeighbor() {
            Anchor a=activeAnchor();
            return a.range.end<relation.geometry.ayahCount(a.range.surah) ? a.range.end+1 :
                (a.range.start>1 ? a.range.start-1 : 0);
        }
        public VerseRange viewport(int pane) {
            pane(pane);
            if(relation.mode!=Mode.SERIES)return relation.originals.get(pane);
            Anchor a=activeAnchor();VerseRange original=relation.originals.get(a.originalIndex);
            int neighbor=normalNeighbor();
            return new VerseRange(original.surah,neighbor==0?original.start:Math.min(original.start,neighbor),
                Math.max(original.end,neighbor),relation.geometry);
        }
        public void setPane(int pane,int page,double zoom,String selection) {
            setPane(pane,page,zoom,selection,0,0);
        }
        public void setPane(int pane,int page,double zoom,String selection,int scrollX,int scrollY) {
            VerseRange bounds=viewport(pane);
            require(page>=bounds.firstPage && page<=bounds.lastPage,"page outside display context");
            Pane next=new Pane(page,zoom,selection,scrollX,scrollY); panes.set(pane,next);
            if(relation.mode==Mode.SERIES) occurrencePanes.set(selectedOccurrence,next);
        }
        public void selectOccurrence(int occurrence) {
            require(occurrence>=0 && occurrence<relation.anchors.size(),"occurrence index");
            selectedOccurrence=occurrence;
            if(relation.mode==Mode.SERIES) panes.set(0,occurrencePanes.get(occurrence));
        }
        public Anchor activeAnchor() { return relation.anchors.get(selectedOccurrence); }
        public Rail rail() { return new Rail(relation.anchors.size(),selectedOccurrence); }
        public void enlarge(int pane) { pane(pane); enlargedPane=pane; }
        public int enlargedPane() { return enlargedPane; }
        public void showComparison() { enlargedPane=-1; }
        public Snapshot snapshot() { return new Snapshot(this); }
        public void restore(Snapshot snapshot) {
            require(snapshot!=null && snapshot.owner==this,"snapshot belongs to another navigation");
            panes.clear(); panes.addAll(snapshot.panes);
            occurrencePanes.clear(); occurrencePanes.addAll(snapshot.occurrencePanes);
            selectedOccurrence=snapshot.selectedOccurrence; enlargedPane=snapshot.enlargedPane;
        }
        public ReaderSnapshot returnToOrigin() { return origin; }
    }

    private static void checkView(int page,double zoom,String selection) {
        require(page>=1 && page<=604 && zoom>0 && !Double.isNaN(zoom) &&
            !Double.isInfinite(zoom) && selection!=null,"page/zoom/selection");
    }
    private static <T> List<T> copy(List<T> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }
    private static String key(int surah,int ayah) { return surah+":"+ayah; }
    private static void require(boolean condition,String message) {
        if(!condition) throw new IllegalArgumentException(message);
    }
}
