package com.quransafeguard.hifz.preview;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Versioned primitive-only checkpoint. Contains no approvals, relation corpus or learning credit.
 * Restore requires a freshly resolved, still approved relation and the exact reader return point.
 * Page geometry is bound here; the Android entry point additionally revalidates render geometry. */
public final class CorrespondenceState {
 private static final int VERSION=1,MAX_BYTES=1024*1024,MAX_PANES=4096;
 private final byte[] contract;
 public final ReaderSnapshot origin;
 public final List<Pane> panes,occurrences,comparison;
 public final int selectedOccurrence,enlargedPane,windowStart;
 private CorrespondenceState(byte[] contract,ReaderSnapshot origin,List<Pane> panes,List<Pane> occurrences,
                             List<Pane> comparison,int selected,int enlarged,int window){
  this.contract=contract.clone();this.origin=origin;this.panes=freeze(panes);
  this.occurrences=freeze(occurrences);this.comparison=comparison==null?null:freeze(comparison);
  selectedOccurrence=selected;enlargedPane=enlarged;windowStart=window;
 }
 public static CorrespondenceState capture(Navigation navigation,int windowStart,List<Pane> comparison){
  Snapshot s=navigation.snapshot();
  CorrespondenceState state=new CorrespondenceState(identity(navigation.relation),s.origin,s.panes,
   s.occurrencePanes,comparison,s.selectedOccurrence,s.enlargedPane,windowStart);
  state.restore(navigation.relation,s.origin); // Validate before a checkpoint can escape.
  return state;
 }
 /** Builds a fresh navigation; invalid data can never partially mutate a live reader. */
 public Navigation restore(Relation relation,ReaderSnapshot expectedOrigin){
  require(relation!=null&&relation.displayable()&&expectedOrigin!=null,"approved relation/origin required");
  require(Arrays.equals(contract,identity(relation)),"checkpoint relation/geometry changed");
  require(Arrays.equals(originBytes(origin),originBytes(expectedOrigin)),"checkpoint origin changed");
  Navigation nav=new Navigation(relation,expectedOrigin);
  int count=relation.mode==Mode.TWO_PAGES?2:1;
  require(panes.size()==count&&selectedOccurrence>=0&&selectedOccurrence<relation.anchors.size(),"checkpoint cardinality/occurrence");
  require(enlargedPane>=-1&&enlargedPane<count,"checkpoint enlargement");
  require(windowStart>=0&&windowStart<relation.anchors.size(),"checkpoint rail window");
  require(relation.mode==Mode.SERIES||windowStart==0,"checkpoint non-series rail");
  if(relation.mode==Mode.SERIES){
   require(occurrences.size()==relation.anchors.size(),"checkpoint occurrence panes");
   for(int i=0;i<occurrences.size();i++){nav.selectOccurrence(i);apply(nav,0,occurrences.get(i));}
  } else require(occurrences.isEmpty(),"checkpoint unexpected occurrences");
  nav.selectOccurrence(selectedOccurrence);
  if(relation.mode==Mode.SERIES)require(same(nav.pane(0),panes.get(0)),"checkpoint active pane mismatch");
  for(int p=0;p<count;p++)apply(nav,p,panes.get(p));
  require((enlargedPane>=0)==(comparison!=null),"checkpoint comparison missing/unexpected");
  if(comparison!=null){
   require(count==2&&comparison.size()==count,"checkpoint comparison cardinality");
   Navigation validate=new Navigation(relation,expectedOrigin);
   for(int p=0;p<count;p++)apply(validate,p,comparison.get(p));
   nav.enlarge(enlargedPane);
  }
  return nav;
 }
 public byte[] encode(){
  return output(out->{out.writeInt(VERSION);out.writeInt(contract.length);out.write(contract);
   writeOrigin(out,origin);writePanes(out,panes);writePanes(out,occurrences);
   out.writeBoolean(comparison!=null);if(comparison!=null)writePanes(out,comparison);
   out.writeInt(selectedOccurrence);out.writeInt(enlargedPane);out.writeInt(windowStart);});
 }
 public static CorrespondenceState decode(byte[] data){
  require(data!=null&&data.length<=MAX_BYTES,"checkpoint size");
  try{
   DataInputStream in=new DataInputStream(new ByteArrayInputStream(data));
   require(in.readInt()==VERSION,"checkpoint version");require(in.readInt()==32,"checkpoint digest");
   byte[] contract=new byte[32];in.readFully(contract);ReaderSnapshot origin=readOrigin(in);
   List<Pane> panes=readPanes(in),occurrences=readPanes(in),comparison=in.readBoolean()?readPanes(in):null;
   CorrespondenceState result=new CorrespondenceState(contract,origin,panes,occurrences,comparison,in.readInt(),in.readInt(),in.readInt());
   require(in.available()==0,"checkpoint trailing data");return result;
  }catch(IOException e){throw new IllegalArgumentException("invalid checkpoint",e);}
 }
 static boolean sameGeometry(Relation relation,Geometry geometry){
  return Arrays.equals(identity(relation),identity(relation,geometry));
 }
 private static byte[] identity(Relation r){return identity(r,r.checkpointGeometry());}
 private static byte[] identity(Relation r,Geometry g){
  require(r!=null&&r.displayable(),"approved relation required");
  byte[] value=output(out->{
   out.writeUTF(r.id);out.writeUTF(r.type.name());out.writeUTF(r.mode.name());
   out.writeInt(r.originals.size());
   for(VerseRange range:r.originals){writeRange(out,range);out.writeInt(g.ayahCount(range.surah));
    for(int a=range.start;a<=range.end;a++){out.writeInt(g.firstPage(range.surah,a));out.writeInt(g.lastPage(range.surah,a));}}
   out.writeInt(r.anchors.size());
   for(Anchor a:r.anchors){out.writeInt(a.originalIndex);writeRange(out,a.range);out.writeUTF(a.sourceLocator);
    out.writeBoolean(a.sourceAttested);out.writeInt(a.firstWord);out.writeInt(a.lastWord);
    int neighbor=a.range.end<g.ayahCount(a.range.surah)?a.range.end+1:(a.range.start>1?a.range.start-1:0);
    out.writeInt(neighbor);if(neighbor>0){out.writeInt(g.firstPage(a.range.surah,neighbor));out.writeInt(g.lastPage(a.range.surah,neighbor));}}
   out.writeBoolean(r.presentation!=null);
   if(r.presentation!=null){out.writeUTF(r.presentation.shortTitle);out.writeUTF(r.presentation.factualLink);out.writeInt(r.presentation.sources.size());
    for(SourceReference source:r.presentation.sources){out.writeUTF(source.author);out.writeUTF(source.work);out.writeUTF(source.edition);out.writeUTF(source.locator);}}
  });
  try{return MessageDigest.getInstance("SHA-256").digest(value);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 private static void writeRange(DataOutputStream out,VerseRange r)throws IOException{
  out.writeInt(r.surah);out.writeInt(r.start);out.writeInt(r.end);out.writeInt(r.firstPage);out.writeInt(r.lastPage);
 }
 private static boolean same(Pane a,Pane b){return a.page==b.page&&a.zoom==b.zoom&&a.scrollX==b.scrollX&&a.scrollY==b.scrollY&&a.selection.equals(b.selection);}
 private static void apply(Navigation n,int p,Pane s){n.setPane(p,s.page,s.zoom,s.selection,s.scrollX,s.scrollY);}
 private static byte[] originBytes(ReaderSnapshot s){return output(out->writeOrigin(out,s));}
 private static void writeOrigin(DataOutputStream out,ReaderSnapshot s)throws IOException{
  out.writeInt(s.surah);out.writeInt(s.ayah);out.writeInt(s.page);out.writeUTF(s.unitId);out.writeUTF(s.passageId);
  out.writeUTF(s.mask);out.writeDouble(s.zoom);out.writeUTF(s.selection);out.writeInt(s.scrollX);out.writeInt(s.scrollY);
 }
 private static ReaderSnapshot readOrigin(DataInputStream in)throws IOException{
  return new ReaderSnapshot(in.readInt(),in.readInt(),in.readInt(),in.readUTF(),in.readUTF(),in.readUTF(),in.readDouble(),in.readUTF(),in.readInt(),in.readInt());
 }
 private static void writePanes(DataOutputStream out,List<Pane> panes)throws IOException{
  out.writeInt(panes.size());for(Pane p:panes){out.writeInt(p.page);out.writeDouble(p.zoom);out.writeUTF(p.selection);out.writeInt(p.scrollX);out.writeInt(p.scrollY);}
 }
 private static List<Pane> readPanes(DataInputStream in)throws IOException{
  int count=in.readInt();require(count>=0&&count<=MAX_PANES,"checkpoint pane count");List<Pane> panes=new ArrayList<>();
  for(int i=0;i<count;i++)panes.add(new Pane(in.readInt(),in.readDouble(),in.readUTF(),in.readInt(),in.readInt()));return panes;
 }
 private interface Writer{void write(DataOutputStream out)throws IOException;}
 private static byte[] output(Writer writer){
  try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);writer.write(out);out.flush();
   require(bytes.size()<=MAX_BYTES,"checkpoint too large");return bytes.toByteArray();
  }catch(IOException e){throw new IllegalArgumentException("checkpoint encoding",e);}
 }
 private static List<Pane> freeze(List<Pane> p){return Collections.unmodifiableList(new ArrayList<>(p));}
 private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
