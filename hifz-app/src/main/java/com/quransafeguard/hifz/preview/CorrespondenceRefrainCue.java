package com.quransafeguard.hifz.preview;

/**
 * Compact recall cues for two precisely identified Qur'anic refrains.
 * Arabic keywords were transcribed from the ayat immediately preceding/following
 * each refrain (source: surah 55 and 54 Arabic text, alquran.cloud).
 * These are optional mnemonic labels, NEVER Qur'an headings or scholarly citations.
 * No user state, internet access, progress or new relationship is created.
 */
final class CorrespondenceRefrainCue {
 private CorrespondenceRefrainCue(){}
 static final int[] RAHMAN={13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77};
 private static final String[] BEFORE={
  "ريحان","جانّ","مشرقين","برزخ","لؤلؤ","جوار","يبقى","يسأله",
  "سنفرغ","معشر","شواظ","انشقت","يومئذ","مجرمون","حميم","جنتان",
  "أفنان","عينان","فاكهة","فرش","قاصرات","ياقوت","إحسان","جنتان",
  "مدهامتان","عينان","رمان","خيرات","حور","يطمثهن","رفرف"};
 private static final String[] AFTER={
  "إنسان","مشرقين","بحرين","لؤلؤ","جوار","فان","يسأله","سنفرغ",
  "معشر","شواظ","انشقت","يومئذ","مجرمون","جهنم","جنتان","أفنان",
  "عينان","فاكهة","فرش","قاصرات","ياقوت","إحسان","جنتان","مدهامتان",
  "عينان","رمان","خيرات","حور","يطمثهن","رفرف","تبارك"};
 private static final String[] KEY={
  "Création","Créatures","Horizons","Deux mers","Navigation","Finitude",
  "Permanence","Dépendance","Interpellation","Fuite","Jugement","Jugement",
  "Coupables","Châtiment","Jardins","Jardins","Sources","Fruits",
  "Repos","Compagnes","Compagnes","Rétribution","Autres jardins",
  "Jardins","Végétation","Fruits","Fruits","Compagnes","Compagnes","Repos","Conclusion"};
 private static final int[] QAMAR={17,22,32,40};
 private static final String[] QAMAR_CONTEXT={
  "Nūḥ → ʿĀd","ʿĀd → Thamūd","Thamūd → Lūṭ","Lūṭ → Pharaon"
 };
 static String forOccurrence(CorrespondenceModel.Relation relation,int selected){
  if(relation==null||relation.type!=CorrespondenceModel.Type.REPETITION||
      relation.mode!=CorrespondenceModel.Mode.SERIES||selected<0||selected>=relation.anchors.size())
   return "";
  if(exactSeries(relation,55,RAHMAN))
   return "Repère · "+BEFORE[selected]+" → "+AFTER[selected]+" · "+KEY[selected];
  if(exactSeries(relation,54,QAMAR))
   return "Repère · "+QAMAR_CONTEXT[selected];
  return "";
 }
 /** No label if a series mixes sourates, omits a refrain, or changes any exact reference. */
 private static boolean exactSeries(CorrespondenceModel.Relation relation,int surah,int[] expected){
  if(relation.anchors.size()!=expected.length)return false;
  for(int i=0;i<expected.length;i++){
   CorrespondenceModel.Anchor a=relation.anchors.get(i);
   if(a==null||!a.sourceAttested||a.range.surah!=surah||
       a.range.start!=expected[i]||a.range.end!=expected[i])return false;
  }
  return true;
 }
}
