package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Display-only veil derived exclusively from the shipped QCF ayahPolygon paths.
 * Glyph groups, transforms and canonical page coordinates are never rewritten.
 * Segment overlays require separately validated source word rectangles; ayah polygons never substitute.
 */
final class CorrespondenceSvgFocus {
 private static final Pattern PATH=Pattern.compile("<path\\b[^>]*>");
 private static final Pattern ATTRIBUTE=Pattern.compile("([A-Za-z_][A-Za-z0-9_:.-]*)\\s*=\\s*([\"'])(.*?)\\2");
 static final class CoordinateSpace {
  final double x,y;
  CoordinateSpace(double x,double y){this.x=x;this.y=y;}
  String frame(){return "x=\""+number(x)+"\" y=\""+number(y)+"\" width=\"345\" height=\"550\"";}
 }
 static CoordinateSpace coordinateSpace(String svg){
  if(svg==null||!svg.contains("</svg>"))throw new IllegalArgumentException("Authentic QCF coordinate space required");
  Matcher root=Pattern.compile("<svg\\b[^>]*>").matcher(svg);
  if(root.find()){
   Matcher attributes=ATTRIBUTE.matcher(root.group());
   while(attributes.find())if("viewBox".equals(attributes.group(1))){
    String[] values=attributes.group(3).trim().split("\\s+");
    if(values.length==4)try{
     double x=Double.parseDouble(values[0]),y=Double.parseDouble(values[1]);
     double w=Double.parseDouble(values[2]),h=Double.parseDouble(values[3]);
     if(Double.isFinite(x)&&Double.isFinite(y)&&w==345&&h==550)return new CoordinateSpace(x,y);
    }catch(NumberFormatException invalid){/* fail closed below */}
   }
  }
  throw new IllegalArgumentException("Authentic QCF coordinate space required");
 }
 private static String number(double n){return java.math.BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();}
 private static String offset(double n,double origin){return java.math.BigDecimal.valueOf(n).add(java.math.BigDecimal.valueOf(origin)).stripTrailingZeros().toPlainString();}
 private CorrespondenceSvgFocus() {}
 static String untouched(String svg) { return svg; }
 static String apply(String svg,int surah,int first,int last,int normalNeighbor) {
  if(surah<1||surah>114||first<1||last<first||last>286||normalNeighbor<0||normalNeighbor>286)
   throw new IllegalArgumentException("Invalid focus range");
  CoordinateSpace space=coordinateSpace(svg);
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
  StringBuilder overlay=new StringBuilder("<defs><mask id=\"corr-focus-mask\" maskUnits=\"userSpaceOnUse\" "+space.frame()+"><rect "+space.frame()+" fill=\"white\"/>");
  for(String d:gray)append(overlay,d,"black",null);
  for(String d:normal)append(overlay,d,"black",null);
  overlay.append("</mask></defs><rect "+space.frame()+" fill=\"#faf8f2\" opacity=\"0.84\" mask=\"url(#corr-focus-mask)\" pointer-events=\"none\"/>");
  for(String d:gray)append(overlay,d,"#999","0.23");
  return svg.replace("</svg>",overlay+"</svg>");
 }
 /** A display overlay confined to each exact word rectangle, never a bounding union. */
 static String applyWords(String svg,List<double[]> boxes){
  CoordinateSpace space=coordinateSpace(svg);
  if(svg.contains("id=\"corr-focus-mask\""))throw new IllegalArgumentException("Focus requires original SVG");
  if(boxes==null||boxes.isEmpty())return svg;
  StringBuilder rects=new StringBuilder();
  for(double[] b:boxes){
   if(b==null||b.length!=4)throw new IllegalArgumentException("Exact word rectangle required");
   for(double n:b)if(!Double.isFinite(n))throw new IllegalArgumentException("Nonfinite word rectangle");
   if(b[0]<0||b[1]<0||b[2]>345||b[3]>550||b[2]<=b[0]||b[3]<=b[1])
    throw new IllegalArgumentException("Word rectangle outside authentic page");
   rects.append("<rect x=\"").append(offset(b[0],space.x)).append("\" y=\"").append(offset(b[1],space.y)).append("\" width=\"")
    .append(b[2]-b[0]).append("\" height=\"").append(b[3]-b[1]).append("\"/>");
  }
  String overlay="<defs><mask id=\"corr-focus-mask\" maskUnits=\"userSpaceOnUse\" "+space.frame()+">"
   +"<rect "+space.frame()+" fill=\"white\"/><g fill=\"black\">"+rects+"</g></mask>"
   +"<pattern id=\"corr-centre-hatch\" patternUnits=\"userSpaceOnUse\" width=\"4\" height=\"4\" patternTransform=\"rotate(45)\">"
   +"<path d=\"M0 0V4\" stroke=\"#555\" stroke-width=\"0.7\" stroke-opacity=\"0.32\"/></pattern></defs>"
   +"<rect "+space.frame()+" fill=\"#faf8f2\" opacity=\"0.84\" mask=\"url(#corr-focus-mask)\" pointer-events=\"none\"/>"
   +"<g fill=\"url(#corr-centre-hatch)\" pointer-events=\"none\">"+rects+"</g>";
  return svg.replace("</svg>",overlay+"</svg>");
 }
 private static void append(StringBuilder out,String d,String fill,String opacity) {
  out.append("<path d=\"").append(d).append("\" fill=\"").append(fill).append('"');
  if(opacity!=null)out.append(" fill-opacity=\"").append(opacity).append('"');
  out.append(" pointer-events=\"none\"/>");
 }
}
