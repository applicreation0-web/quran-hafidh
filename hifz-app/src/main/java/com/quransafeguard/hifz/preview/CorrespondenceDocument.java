package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure display plan. Context pages never alter the canonical relation ranges. */
final class CorrespondenceDocument {
 private CorrespondenceDocument() {}
 /** Current verse-only renderer capability, checked before access affordances or dialogs.
  * Generic editorial approval is necessary but cannot certify this renderer. */
 static boolean displayable(CorrespondenceModel.Relation relation,CorrespondenceModel.Geometry geometry){
  if(relation==null || geometry==null || !relation.displayable() || relation.presentation==null)return false;
  // Alternance requires distinct grammatical roles/pairs. This reader currently paints
  // all verse polygons alike: never advertise an approved four-role structure as two gray blocks.
  if(relation.type==CorrespondenceModel.Type.ALTERNANCE)return false;
  try {
   for(CorrespondenceModel.VerseRange original:relation.originals){
    CorrespondenceModel.VerseRange checked=new CorrespondenceModel.VerseRange(original.surah,original.start,original.end,geometry);
    if(checked.firstPage!=original.firstPage || checked.lastPage!=original.lastPage)return false;
   }
   for(CorrespondenceModel.Anchor anchor:relation.anchors){
    CorrespondenceModel.VerseRange checked=new CorrespondenceModel.VerseRange(anchor.range.surah,anchor.range.start,anchor.range.end,geometry);
    if(checked.firstPage!=anchor.range.firstPage || checked.lastPage!=anchor.range.lastPage)return false;
   }
   if(relation.mode==CorrespondenceModel.Mode.SERIES){
    for(int occurrence=0;occurrence<relation.anchors.size();occurrence++)
     if(!focus(relation,0,occurrence,geometry).supported)return false;
   } else {
    int panes=relation.mode==CorrespondenceModel.Mode.TWO_PAGES?2:1;
    for(int pane=0;pane<panes;pane++)if(!focus(relation,pane,0,geometry).supported)return false;
   }
   return true;
  } catch(IllegalArgumentException invalidGeometry){return false;}
 }
 static final class Focus {
  final int surah,first,last,normalNeighbor,previousNeighbor,firstPage,lastPage,initialPage;
  final boolean supported;
  java.util.List<CorrespondenceModel.SegmentBox> segmentBoxes=java.util.Collections.emptyList();
  /** Disjoint source-attested verse ranges: never manufacture gray focus over the gaps. */
  java.util.List<CorrespondenceModel.VerseRange> focalRanges=java.util.Collections.emptyList();
  Focus(int s,int a,int b,int n,int previous,int fp,int lp,int ip,boolean supported){
   surah=s;first=a;last=b;normalNeighbor=n;previousNeighbor=previous;
   firstPage=fp;lastPage=lp;initialPage=ip;this.supported=supported;
  }
 }
 static Focus focus(CorrespondenceModel.Relation relation,int pane,int occurrence,CorrespondenceModel.Geometry geometry){
  if(relation.type==CorrespondenceModel.Type.CENTRE){
   CorrespondenceModel.Anchor a=relation.anchors.get(0);
   java.util.List<CorrespondenceModel.SegmentBox> boxes=CorrespondenceModel.exactSegmentBoxes(geometry,a);
   boolean supported=relation.displayable()&&relation.mode==CorrespondenceModel.Mode.INTERNAL&&
    relation.anchors.size()==1&&!boxes.isEmpty();
   int initial=boxes.isEmpty()?a.range.firstPage:boxes.get(0).page;
   Focus plan=new Focus(a.range.surah,a.range.start,a.range.end,0,0,a.range.firstPage,a.range.lastPage,initial,supported);
   if(supported)plan.segmentBoxes=boxes;return plan;
  }
  CorrespondenceModel.Anchor selected=relation.anchors.get(relation.mode==CorrespondenceModel.Mode.SERIES?occurrence:0);
  int original=relation.mode==CorrespondenceModel.Mode.SERIES?selected.originalIndex:pane;
  CorrespondenceModel.VerseRange range=relation.originals.get(original);
  List<CorrespondenceModel.Anchor> anchors=new ArrayList<>();
  if(relation.mode==CorrespondenceModel.Mode.SERIES)anchors.add(selected);
  else for(CorrespondenceModel.Anchor a:relation.anchors)if(a.originalIndex==original)anchors.add(a);
  anchors.sort(Comparator.comparingInt(a->a.range.start));
  int first=anchors.get(0).range.start,last=first,initial=anchors.get(0).range.firstPage;
  boolean supported=relation.type!=CorrespondenceModel.Type.CENTRE;
  for(CorrespondenceModel.Anchor a:anchors){
   // This renderer has verse geometry only. Never masquerade as a word renderer,
   // or fill gaps between several separate anchors as if they were attested.
   supported &= !a.isWordSegment() && a.range.surah==range.surah;
   last=Math.max(last,a.range.end);
  }
  int neighbor=0,previous=0,fp=range.firstPage,lp=range.lastPage;
  if(relation.mode==CorrespondenceModel.Mode.SERIES){
   // Refrain remembering: both actual adjacent ayat stay legible, not only the next.
   // Both may occur on separate QCF pages; their pages are display context, never anchors.
   previous=first>1?first-1:0;
   neighbor=last<geometry.ayahCount(range.surah)?last+1:0;
   if(neighbor==0)neighbor=previous;
   for(int n:new int[]{previous,neighbor})if(n>0){
    fp=Math.min(fp,geometry.firstPage(range.surah,n));
    lp=Math.max(lp,geometry.lastPage(range.surah,n));
   }
  }
  Focus plan=new Focus(range.surah,first,last,neighbor,previous,fp,lp,initial,supported);
  if(anchors.size()>1){
   java.util.List<CorrespondenceModel.VerseRange> discrete=new ArrayList<>();
   for(CorrespondenceModel.Anchor a:anchors)discrete.add(a.range);
   plan.focalRanges=java.util.Collections.unmodifiableList(discrete);
  }
  return plan;
 }
 static final class Render {
  final String svg;final boolean focusVerified;
  Render(String svg,boolean verified){this.svg=svg;focusVerified=verified;}
 }
 static Render render(String svg,Focus focus,int page){
  if(!focus.supported)return new Render(CorrespondenceSvgFocus.untouched(svg),false);
  String rendered;
  if(!focus.segmentBoxes.isEmpty()){
   java.util.List<double[]> boxes=new java.util.ArrayList<>();
   for(CorrespondenceModel.SegmentBox b:focus.segmentBoxes)if(b.page==page)boxes.add(new double[]{b.x0,b.y0,b.x1,b.y1});
   rendered=CorrespondenceSvgFocus.applyWords(svg,boxes);
  }else if(!focus.focalRanges.isEmpty()){
   rendered=CorrespondenceSvgFocus.applyDiscrete(svg,focus.surah,focus.focalRanges);
  }else rendered=CorrespondenceSvgFocus.apply(svg,focus.surah,focus.first,focus.last,focus.normalNeighbor,focus.previousNeighbor);
  return new Render(rendered,!rendered.equals(svg));
 }
 static String html(String svg){
  // SVG bytes and every glyph remain inline and local, with no script/bridge.
  return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
   +"<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src 'unsafe-inline'; img-src data:\">"
   +"<style>*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;background:#faf8f2;}"
   +"body{display:flex;align-items:center;justify-content:center;padding:4px;overflow:hidden;}"
   +"svg{display:block;width:100%;height:100%;max-width:100%;max-height:100%;background:transparent;overflow:hidden;}"
   +"</style></head><body>"
   +svg+"</body></html>";
 }
}
