package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Atlas BOOX DISPLAY ONLY: keep all 1,419 reviewed source titles byte-for-byte.
 * Render personal names with readable Arabic transliteration + the Arabic name.
 * No verse ranges, source assets, passage identities or acquired Hifz data change.
 */
final class AtlasDisplayTitleFormatter {
 private AtlasDisplayTitleFormatter(){}
 private static final class Name {
  final Pattern match;
  final String label;
  Name(String label,String arabic,String... variants) {
   this.label=label+" ("+arabic+")";
   StringBuilder names=new StringBuilder();
   for(String v:variants){if(names.length()>0)names.append("|");names.append(Pattern.quote(v));}
   String pair="(?:\\s*\\((?:"+names+")\\))?";
   String alreadyArabic="(?:\\s*\\("+Pattern.quote(arabic)+"\\))*";
   // A literal Arabic name is not a type of punctuation to erase.
   match=Pattern.compile("(?<![\\p{L}\\p{N}])(?:"+names+")"+pair+alreadyArabic+
        "(?![\\p{L}\\p{N}])",Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
  }
 }
 private static final List<Name> PERSONAL_NAMES=new ArrayList<>();
 static {
  add("Ādam","آدم","Ādam","Adam");
  add("Mūsā","موسى","Mūsā","Musa","Moses");
  add("ʿĪsā","عيسى","ʿĪsā","Isa","Jesus");
  add("Maryam","مريم","Maryam","Maryem","Mary");
  add("Ibrāhīm","إبراهيم","Ibrāhīm","Ibrahim","Abraham");
  add("Ismāʿīl","إسماعيل","Ismāʿīl","Isma'il","Isma\u0060il","Ismail","Ishmael");
  add("Isḥāq","إسحاق","Isḥāq","Ishaq","Isaac");
  add("Yaʿqūb","يعقوب","Yaʿqūb","Yaqoub","Yaqub","Jacob");
  add("Yūsuf","يوسف","Yūsuf","Yusuf","Joseph");
  add("Nūḥ","نوح","Nūḥ","Nuh","Noah");
  add("Hārūn","هارون","Hārūn","Harun","Haroon","Aaron");
  add("Yaḥyā","يحيى","Yaḥyā","Yahya","John");
  add("Zakariyyā","زكريا","Zakariyyā","Zakariya","Zakaria","Zechariah","Zachariah");
  add("Sulaymān","سليمان","Sulaymān","Sulaiman","Solomon");
  add("Dāwūd","داود","Dāwūd","Dawud","David");
  add("Lūṭ","لوط","Lūṭ","Lut","Lot");
  add("Yūnus","يونس","Yūnus","Yunus","Jonah");
  add("Ayyūb","أيوب","Ayyūb","Ayyub","Job");
  add("Ilyās","إلياس","Ilyās","Ilyas","Elias");
  add("Ṣāliḥ","صالح","Ṣāliḥ","Salih","Saleh");
  add("Shuʿayb","شعيب","Shuʿayb","Shuaib");
  add("Idrīs","إدريس","Idrīs","Idris");
  add("Dhū al-Kifl","ذو الكفل","Dhū al-Kifl","Dhul Kifl","Zulkifl","Zul-kifl");
  add("Muḥammad","محمد","Muḥammad","Muhammad","Mohammed");
  // Title of a ruler, not a prophet's given name; preserve Quranic naming.
  add("Firʿawn","فرعون","Firʿawn","Fir'on","Firaun","Pharaoh");
 }
 private static void add(String latin,String arabic,String... variants){
  PERSONAL_NAMES.add(new Name(latin,arabic,variants));
 }
 static String display(String original){
  if(original==null||original.isEmpty())return original==null?"":original;
  // Some original Ayah Themes titles contain literal "\\t" text separators.
  // Suppress these visible artefacts only when rendering; preserve source bytes.
  String text=original.replace("\\t"," ").replace("\\n"," ").replace("\\r"," ");
  text=text.replace("&nbsp;"," ").replace("&quot;","\"").replace("&apos;","\u0027")
      .replace("&#39;","\u0027").replace("&#x27;","\u0027").replace("&lt;","<")
      .replace("&gt;",">").replace("&amp;","&").replace("\\\"","\"");
  for(Name n:PERSONAL_NAMES){
   Matcher m=n.match.matcher(text);
   text=m.replaceAll(Matcher.quoteReplacement(n.label));
  }
  return text;
 }
 /** Only Ayah Themes subtitles suppress numeric ranges; canonical source is untouched. */
 static String displayPassage(String original){
  String text=display(original);
  return text.replaceFirst("^\\s*\\d{1,3}:\\d{1,3}(?:\\s*[–-]\\s*\\d{1,3})?\\s*[·—–:]\\s+","");
 }
}
