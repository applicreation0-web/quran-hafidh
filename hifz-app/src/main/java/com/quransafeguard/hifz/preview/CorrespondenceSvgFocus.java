package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Display-only veil derived exclusively from the shipped QCF ayahPolygon paths.
 * Glyph groups, transforms and canonical page coordinates are never rewritten.
 * This class has no word/centre marking API: an ayah polygon is not word geometry.
 */
final class CorrespondenceSvgFocus {
 private static final Pattern PATH=Pattern.compile("<path\\b[^>]*>");
 private static final Pattern ATTRIBUTE=Pattern.compile("([A-Za-z_][A-Za-z0-9_:.-]*)\\s*=\\s*([\"'])(.*?)\\2");
 private CorrespondenceSvgFocus() {}
 static String untouched(String svg) { return svg; }
 static String apply(String svg,int surah,int first,int last,int normalNeighbor) {
  if(surah<1||surah>114||first<1||last<first||last>286||normalNeighbor<0||normalNeighbor>286)
   throw new IllegalArgumentException("Invalid focus range");
  if(svg==null||!svg.contains("viewBox=\"0 0 345 550\"")||!svg.contains("</svg>"))
   throw new IllegalArgumentException("Authentic QCF coordinate space required");
  // Reapplying a display veil to a previously transformed page could stack fades.
  if(svg.contains("id=\"corr-focus-mask\""))throw new IllegalArgumentException("Focus requires original SVG");
  List<String> gray=new ArrayList<>(),normal=new ArrayList<>();
  Matcher paths=PATH.matcher(svg);
  while(paths.find()) {
   String css=null,s=null,a=null,d=null;Matcher attrs=ATTRIBUTE.matcher(paths.group());
   while(attrs.find()) {
    switch(attrs.group(1)) {
     case "class":css=attrs.group(3);break;
     case "surah":s=attrs.group(3);break;
     case "ayah":a=attrs.group(3);break;
     case "d":d=attrs.group(3);break;
     default:break;
    }
   }
   if(css==null||!java.util.Arrays.asList(css.split("\\s+")).contains("ayahPolygon")||!Integer.toString(surah).equals(s)||a==null||d==null)continue;
   int ayah;try{ayah=Integer.parseInt(a);}catch(NumberFormatException invalid){continue;}
   // Restrict reused geometry to SVG path commands/numbers. Never copy markup.
   if(!d.matches("[MmZzLlHhVvCcSsQqTtAa0-9eE+.,\\s-]+"))continue;
   if(ayah>=first&&ayah<=last)gray.add(d);
   else if(ayah==normalNeighbor)normal.add(d);
  }
  // Missing focus geometry must leave the authentic page intact, not dim everything.
  if(gray.isEmpty()&&normal.isEmpty())return svg;
  StringBuilder overlay=new StringBuilder("<defs><mask id=\"corr-focus-mask\" maskUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"345\" height=\"550\"><rect width=\"345\" height=\"550\" fill=\"white\"/>");
  for(String d:gray)append(overlay,d,"black",null);
  for(String d:normal)append(overlay,d,"black",null);
  overlay.append("</mask></defs><rect width=\"345\" height=\"550\" fill=\"#faf8f2\" opacity=\"0.84\" mask=\"url(#corr-focus-mask)\" pointer-events=\"none\"/>");
  for(String d:gray)append(overlay,d,"#999","0.23");
  return svg.replace("</svg>",overlay+"</svg>");
 }
 private static void append(StringBuilder out,String d,String fill,String opacity) {
  out.append("<path d=\"").append(d).append("\" fill=\"").append(fill).append('"');
  if(opacity!=null)out.append(" fill-opacity=\"").append(opacity).append('"');
  out.append(" pointer-events=\"none\"/>");
 }
}
