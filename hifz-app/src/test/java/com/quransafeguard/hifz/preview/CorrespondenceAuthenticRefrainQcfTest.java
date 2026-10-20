package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.brotli.dec.BrotliInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Shipped and commit-pinned KFQC SVGs only. This is visual GEOMETRY QA,
 * not scholarly approval for any private R047/R048 catalog entries. */
public final class CorrespondenceAuthenticRefrainQcfTest {
 private static GeometryRepository geometry;
 private static final Map<Integer,String> pages=new HashMap<>();
 private static final int[] QAMAR={17,22,32,40};
 private static final int[] RAHMAN={13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77};
 private static Path source(String name) {
  for(String prefix:new String[]{"","../","quran-hafidh/"}){
   Path path=Paths.get(prefix+name);
   if(Files.isRegularFile(path))return path;
  }
  throw new IllegalStateException("Authentic QCF file absent: "+name);
 }
 @BeforeClass public static void load()throws Exception {
  String json=new String(Files.readAllBytes(source("app/src/main/assets/reader109/geometry.json")),StandardCharsets.UTF_8);
  geometry=GeometryRepository.fromJson(json);
 }
 private static int page(int surah,int ayah){
  return geometry.pageForVerse(new VerseRef(surah,ayah));
 }
 private static String qcf(int page)throws Exception {
  if(pages.containsKey(page))return pages.get(page);
  Path file=source(String.format(java.util.Locale.ROOT,
   "app/src/main/assets/mushaf/hafs/kfqc/svg-br/%03d.svg.br",page));
  ByteArrayOutputStream output=new ByteArrayOutputStream();
  try(InputStream raw=Files.newInputStream(file);BrotliInputStream decompressed=new BrotliInputStream(raw)){
   byte[] buffer=new byte[8192];int n;
   while((n=decompressed.read(buffer))!=-1){
    if(output.size()+n>850_000)throw new IllegalStateException("Oversized authentic QCF page "+page);
    output.write(buffer,0,n);
   }
  }
  String svg=new String(output.toByteArray(),StandardCharsets.UTF_8);
  CorrespondenceSvgFocus.coordinateSpace(svg);
  pages.put(page,svg);return svg;
 }
 private static void check(int surah,int[] occurrences)throws Exception {
  for(int verse:occurrences){
   int before=verse-1,after=verse+1;
   int actualPage=page(surah,verse);
   String original=qcf(actualPage);
   String selected=CorrespondenceSvgFocus.apply(original,surah,verse,verse,after,before);
   assertNotEquals("QCF polygon for refrain is absent: "+surah+":"+verse,original,selected);
   assertTrue("Selected refrain should have exact gray path: "+surah+":"+verse,
    selected.contains("fill=\"#999\""));
   assertTrue("Authentic SVG glyphs/geometry must remain unchanged: "+surah+":"+verse,
    selected.startsWith(original.substring(0,original.lastIndexOf("</svg>"))));
   assertEquals("Canonical refrain page must not move: "+surah+":"+verse,actualPage,page(surah,verse));
   for(int neighbor:new int[]{before,after}) {
    int neighborPage=page(surah,neighbor);
    String source=qcf(neighborPage);
    String view=CorrespondenceSvgFocus.apply(source,surah,verse,verse,after,before);
    assertNotEquals("QCF polygon missing for adjacent verse "+surah+":"+neighbor,source,view);
    assertTrue("Normal neighbor must be revealed: "+surah+":"+neighbor,
     view.contains("fill=\"black\""));
    assertTrue("No replacement of QCF glyphs: "+surah+":"+neighbor,
     view.startsWith(source.substring(0,source.lastIndexOf("</svg>"))));
   }
  }
 }
 @Test public void qamarAllFourRefrainPolygonsAndBothNeighborsAreAuthentic()throws Exception {
  check(54,QAMAR);
 }
 @Test public void rahmanAllThirtyOneRefrainPolygonsAndBothNeighborsAreAuthentic()throws Exception {
  check(55,RAHMAN);
 }
}
