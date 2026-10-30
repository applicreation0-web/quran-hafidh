package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/** Approved al-Abbad reference catalogue plus separately attributed user priority.
 * Rebuilt from individually recorded editorial decisions; Quran glyphs and Jalalayn
 * always come from the existing offline corpora. No generated religious commentary.
 */
final class AtlasDiscriminantCases {
    static final class Entry {
        final VerseRef first, second;
        final int firstPage, secondPage, auditId;
        final String kind, firstCue, secondCue, explanation, source, sourceUrl;
        /** Audited one-based Quran word positions; empty until individually checked. */
        final int[] firstWordIndices, secondWordIndices;
        Entry(int s1,int a1,int s2,int a2,String kind,String cue1,String cue2,
              String explanation,String source,String url,int[] words1,int[] words2,int page1,int page2,int id) {
            first=new VerseRef(s1,a1);second=new VerseRef(s2,a2);
            if(page1<1||page1>604||page2<1||page2>604)throw new IllegalArgumentException("Invalid QCF page");
            firstPage=page1;secondPage=page2;auditId=id;
            this.kind=kind;firstCue=cue1;secondCue=cue2;this.explanation=explanation;
            this.source=source;sourceUrl=url;
            firstWordIndices=words1.clone();secondWordIndices=words2.clone();
        }
    }
    private AtlasDiscriminantCases() {}
    // BEGIN GENERATED APPROVED CATALOGUE
    static final List<Entry> CATALOGUE = Collections.unmodifiableList(Arrays.asList(
        new Entry(2,18,2,171,"Variation de mot ou de particule","2:18","2:171","","al-Abbad · 1","https://quranpedia.app/books/b10901/s1.html",new int[]{6},new int[]{18},4,26,1),
        new Entry(2,23,10,38,"Variation de mot ou de particule","2:23","10:38","","al-Abbad · 2","https://quranpedia.app/books/b10901/s1.html",new int[]{14},new int[]{9},4,213,2),
        new Entry(2,49,7,141,"Variation de mot ou de particule","2:49","7:141","","al-Abbad · 3","https://quranpedia.app/books/b10901/s1.html",new int[]{2},new int[]{2},8,167,3),
        new Entry(7,141,14,6,"Variation de mot ou de particule","7:141","14:6","","al-Abbad · 4","https://quranpedia.app/books/b10901/s1.html",new int[]{9},new int[]{17},167,256,4),
        new Entry(2,136,3,84,"Variation de mot ou de particule","2:136","3:84","","al-Abbad · 5","https://quranpedia.app/books/b10901/s1.html",new int[]{1},new int[]{1},21,61,5),
        new Entry(2,170,31,21,"Variation de mot ou de particule","2:170","31:21","","al-Abbad · 6","https://quranpedia.app/books/b10901/s1.html",new int[]{12},new int[]{12},26,413,6),
        new Entry(2,170,5,104,"Variation de mot ou de particule","2:170","5:104","","al-Abbad · 7","https://quranpedia.app/books/b10901/s1.html",new int[]{19},new int[]{21},26,125,7),
        new Entry(6,145,16,115,"Variation de mot ou de particule","6:145","16:115","","al-Abbad · 8","https://quranpedia.app/books/b10901/s1.html",new int[]{37},new int[]{20},147,280,8),
        new Entry(2,231,65,2,"Variation de mot ou de particule","2:231","65:2","","al-Abbad · 9","https://quranpedia.app/books/b10901/s2.html",new int[]{9},new int[]{7},37,558,9),
        new Entry(3,137,6,11,"Variation de mot ou de particule","3:137","6:11","","al-Abbad · 10","https://quranpedia.app/books/b10901/s2.html",new int[]{9},new int[]{6},67,129,10),
        new Entry(3,167,48,11,"Variation de mot ou de particule","3:167","48:11","","al-Abbad · 11","https://quranpedia.app/books/b10901/s2.html",new int[]{25},new int[]{12},72,512,11),
        new Entry(3,182,22,10,"Variation de mot ou de particule","3:182","22:10","","al-Abbad · 12","https://quranpedia.app/books/b10901/s2.html",new int[]{4},new int[]{4},74,333,12),
        new Entry(4,87,4,122,"Variation de mot ou de particule","4:87","4:122","","al-Abbad · 13","https://quranpedia.app/books/b10901/s2.html",new int[]{17},new int[]{21},92,98,13),
        new Entry(4,135,5,8,"Variation de mot ou de particule","4:135","5:8","","al-Abbad · 14","https://quranpedia.app/books/b10901/s2.html",new int[]{6},new int[]{6},100,108,14),
        new Entry(4,137,4,168,"Variation de mot ou de particule","4:137","4:168","","al-Abbad · 15","https://quranpedia.app/books/b10901/s2.html",new int[]{20},new int[]{12},100,104,15),
        new Entry(6,21,10,17,"Variation de mot ou de particule","6:21","10:17","","al-Abbad · 16","https://quranpedia.app/books/b10901/s2.html",new int[]{14},new int[]{14},130,210,16),
        new Entry(6,148,16,35,"Variation de mot ou de particule","6:148","16:35","","al-Abbad · 17","https://quranpedia.app/books/b10901/s2.html",new int[]{8},new int[]{8},148,271,17),
        new Entry(7,64,10,73,"Variation de mot ou de particule","7:64","10:73","","al-Abbad · 18","https://quranpedia.app/books/b10901/s2.html",new int[]{2},new int[]{2},158,217,18),
        new Entry(7,73,11,64,"Variation de mot ou de particule","7:73","11:64","","al-Abbad · 19","https://quranpedia.app/books/b10901/s2.html",new int[]{34},new int[]{17},159,229,19),
        new Entry(7,83,27,57,"Variation de mot ou de particule","7:83","27:57","","al-Abbad · 20","https://quranpedia.app/books/b10901/s2.html",new int[]{5},new int[]{5},161,382,20),
        new Entry(20,71,26,49,"Variation de mot ou de particule","20:71","26:49","","al-Abbad · 21","https://quranpedia.app/books/b10901/s2.html",new int[]{13},new int[]{13},316,369,21),
        new Entry(8,22,8,55,"Variation de mot ou de particule","8:22","8:55","","al-Abbad · 22","https://quranpedia.app/books/b10901/s2.html",new int[]{6},new int[]{6},179,184,22),
        new Entry(9,32,61,8,"Variation de mot ou de particule","9:32","61:8","","al-Abbad · 23","https://quranpedia.app/books/b10901/s3.html",new int[]{2},new int[]{2},192,552,23),
        new Entry(10,18,25,55,"Variation de mot ou de particule","10:18","25:55","","al-Abbad · 24","https://quranpedia.app/books/b10901/s3.html",new int[]{7},new int[]{7},210,364,24),
        new Entry(10,33,40,6,"Variation de mot ou de particule","10:33","40:6","","al-Abbad · 25","https://quranpedia.app/books/b10901/s3.html",new int[]{7},new int[]{7},212,467,25),
        new Entry(10,55,10,66,"Variation de mot ou de particule","10:55","10:66","","al-Abbad · 26","https://quranpedia.app/books/b10901/s3.html",new int[]{4},new int[]{4},215,216,26),
        new Entry(10,104,27,91,"Variation de mot ou de particule","10:104","27:91","","al-Abbad · 27","https://quranpedia.app/books/b10901/s3.html",new int[]{26},new int[]{17},220,385,27),
        new Entry(11,22,16,109,"Variation de mot ou de particule","11:22","16:109","","al-Abbad · 28","https://quranpedia.app/books/b10901/s3.html",new int[]{7},new int[]{7},224,279,28),
        new Entry(11,28,11,63,"Variation de mot ou de particule","11:28","11:63","","al-Abbad · 29","https://quranpedia.app/books/b10901/s3.html",new int[]{11},new int[]{11},224,229,29),
        new Entry(11,40,23,27,"Variation de mot ou de particule","11:40","23:27","","al-Abbad · 30","https://quranpedia.app/books/b10901/s3.html",new int[]{20},new int[]{25},226,343,30),
        new Entry(12,2,43,3,"Variation de mot ou de particule","12:2","43:3","","al-Abbad · 31","https://quranpedia.app/books/b10901/s3.html",new int[]{2},new int[]{2},235,489,31),
        new Entry(15,11,43,7,"Variation de mot ou de particule","15:11","43:7","","al-Abbad · 32","https://quranpedia.app/books/b10901/s3.html",new int[]{4},new int[]{4},262,489,32),
        new Entry(15,45,52,17,"Variation de mot ou de particule","15:45","52:17","","al-Abbad · 33","https://quranpedia.app/books/b10901/s3.html",new int[]{5},new int[]{5},264,524,33),
        new Entry(52,17,77,41,"Variation de mot ou de particule","52:17","77:41","","al-Abbad · 34","https://quranpedia.app/books/b10901/s3.html",new int[]{4},new int[]{4},524,581,34),
        new Entry(16,61,35,45,"Variation de mot ou de particule","16:61","35:45","","al-Abbad · 35","https://quranpedia.app/books/b10901/s3.html",new int[]{5},new int[]{5},273,440,35),
        new Entry(18,46,19,76,"Variation de mot ou de particule","18:46","19:76","","al-Abbad · 36","https://quranpedia.app/books/b10901/s3.html",new int[]{13},new int[]{13},299,310,36),
        new Entry(18,71,18,74,"Variation de mot ou de particule","18:71","18:74","","al-Abbad · 37","https://quranpedia.app/books/b10901/s3.html",new int[]{15},new int[]{16},301,301,37),
        new Entry(20,40,28,13,"Variation de mot ou de particule","20:40","28:13","","al-Abbad · 38","https://quranpedia.app/books/b10901/s3.html",new int[]{10},new int[]{1},314,386,38),
        new Entry(21,2,26,5,"Variation de mot ou de particule","21:2","26:5","","al-Abbad · 39","https://quranpedia.app/books/b10901/s3.html",new int[]{6},new int[]{6},322,367,39),
        new Entry(22,28,22,36,"Variation de mot ou de particule","22:28","22:36","","al-Abbad · 40","https://quranpedia.app/books/b10901/s3.html",new int[]{19},new int[]{21},335,336,40),
        new Entry(23,83,27,68,"Variation de mot ou de particule","23:83","27:68","","al-Abbad · 41","https://quranpedia.app/books/b10901/s3.html",new int[]{3},new int[]{3},347,383,41),
        new Entry(24,10,24,20,"Variation de mot ou de particule","24:10","24:20","","al-Abbad · 42","https://quranpedia.app/books/b10901/s3.html",new int[]{8},new int[]{8},350,351,42),
        new Entry(27,12,28,32,"Variation de mot ou de particule","27:12","28:32","","al-Abbad · 43","https://quranpedia.app/books/b10901/s3.html",new int[]{15},new int[]{21},377,389,43),
        new Entry(27,53,41,18,"Variation de mot ou de particule","27:53","41:18","","al-Abbad · 44","https://quranpedia.app/books/b10901/s3.html",new int[]{1},new int[]{1},381,478,44),
        new Entry(30,30,30,43,"Variation de mot ou de particule","30:30","30:43","","al-Abbad · 45","https://quranpedia.app/books/b10901/s3.html",new int[]{4},new int[]{4},407,409,45),
        new Entry(39,21,57,20,"Variation de mot ou de particule","39:21","57:20","","al-Abbad · 47","https://quranpedia.app/books/b10901/s4.html",new int[]{24},new int[]{24},460,540,47),
        new Entry(41,52,46,10,"Variation de mot ou de particule","41:52","46:10","","al-Abbad · 48","https://quranpedia.app/books/b10901/s4.html",new int[]{8},new int[]{8},482,503,48),
        new Entry(2,60,7,160,"Variation de mot ou de particule","2:60","7:160","","al-Abbad · 49","https://quranpedia.app/books/b10901/s4.html",new int[]{9},new int[]{16},9,171,49),
        new Entry(2,62,5,69,"Variation de mot ou de particule","2:62","5:69","","al-Abbad · 50","https://quranpedia.app/books/b10901/s4.html",new int[]{6},new int[]{6},10,119,50),
        new Entry(5,69,22,17,"Variation de mot ou de particule","5:69","22:17","","al-Abbad · 51","https://quranpedia.app/books/b10901/s4.html",new int[]{6},new int[]{6},119,334,51),
        new Entry(2,95,62,7,"Variation de mot ou de particule","2:95","62:7","","al-Abbad · 52","https://quranpedia.app/books/b10901/s4.html",new int[]{2},new int[]{2},15,553,52),
        new Entry(3,164,62,2,"Variation de mot ou de particule","3:164","62:2","","al-Abbad · 53","https://quranpedia.app/books/b10901/s4.html",new int[]{11},new int[]{7},71,553,53),
        new Entry(3,40,3,47,"Variation de mot ou de particule","3:40","3:47","","al-Abbad · 54","https://quranpedia.app/books/b10901/s4.html",new int[]{15},new int[]{13},55,56,54),
        new Entry(3,47,19,8,"Variation de mot ou de particule","3:47","19:8","","al-Abbad · 55","https://quranpedia.app/books/b10901/s4.html",new int[]{6},new int[]{6},56,305,55),
        new Entry(3,151,8,12,"Variation de mot ou de particule","3:151","8:12","","al-Abbad · 56","https://quranpedia.app/books/b10901/s4.html",new int[]{1},new int[]{11},69,178,56),
        new Entry(4,1,7,189,"Variation de mot ou de particule","4:1","7:189","","al-Abbad · 57","https://quranpedia.app/books/b10901/s4.html",new int[]{10},new int[]{7},77,175,57),
        new Entry(7,189,39,6,"Variation de mot ou de particule","7:189","39:6","","al-Abbad · 58","https://quranpedia.app/books/b10901/s4.html",new int[]{7},new int[]{5},175,459,58),
        new Entry(5,2,48,29,"Variation de mot ou de particule","5:2","48:29","","al-Abbad · 61","https://quranpedia.app/books/b10901/s4.html",new int[]{22},new int[]{17},106,515,61),
        new Entry(5,44,5,45,"Variation de mot ou de particule","5:44","5:45","","al-Abbad · 63","https://quranpedia.app/books/b10901/s4.html",new int[]{41},new int[]{31},115,115,63),
        new Entry(5,45,5,47,"Variation de mot ou de particule","5:45","5:47","","al-Abbad · 64","https://quranpedia.app/books/b10901/s4.html",new int[]{31},new int[]{16},115,116,64),
        new Entry(5,62,5,63,"Variation de mot ou de particule","5:62","5:63","","al-Abbad · 65","https://quranpedia.app/books/b10901/s4.html",new int[]{13},new int[]{13},118,118,65),
        new Entry(6,17,10,107,"Variation de mot ou de particule","6:17","10:107","","al-Abbad · 66","https://quranpedia.app/books/b10901/s4.html",new int[]{11},new int[]{11},129,221,66),
        new Entry(6,37,10,20,"Variation de mot ou de particule","6:37","10:20","","al-Abbad · 67","https://quranpedia.app/books/b10901/s4.html",new int[]{3},new int[]{3},132,210,67),
        new Entry(10,20,29,50,"Variation de mot ou de particule","10:20","29:50","","al-Abbad · 68","https://quranpedia.app/books/b10901/s4.html",new int[]{5},new int[]{5},210,402,68),
        new Entry(6,102,40,62,"Variation de mot ou de particule","6:102","40:62","","al-Abbad · 69","https://quranpedia.app/books/b10901/s4.html",new int[]{4},new int[]{4},141,474,69),
        new Entry(6,112,6,137,"Variation de mot ou de particule","6:112","6:137","","al-Abbad · 70","https://quranpedia.app/books/b10901/s4.html",new int[]{18},new int[]{15},142,145,70),
        new Entry(6,130,39,71,"Variation de mot ou de particule","6:130","39:71","","al-Abbad · 71","https://quranpedia.app/books/b10901/s4.html",new int[]{8},new int[]{19},144,466,71),
        new Entry(6,151,17,31,"Variation de mot ou de particule","6:151","17:31","","al-Abbad · 72","https://quranpedia.app/books/b10901/s4.html",new int[]{17},new int[]{4},148,285,72),
        new Entry(6,151,6,152,"Variation de mot ou de particule","6:151","6:152","","al-Abbad · 73","https://quranpedia.app/books/b10901/s4.html",new int[]{42},new int[]{35},148,149,73),
        new Entry(6,152,6,153,"Variation de mot ou de particule","6:152","6:153","","al-Abbad · 74","https://quranpedia.app/books/b10901/s4.html",new int[]{35},new int[]{17},149,149,74),
        new Entry(6,160,27,89,"Variation de mot ou de particule","6:160","27:89","","al-Abbad · 75","https://quranpedia.app/books/b10901/s4.html",new int[]{5},new int[]{5},150,385,75),
        new Entry(15,32,38,75,"Variation de mot ou de particule","15:32","38:75","","al-Abbad · 76","https://quranpedia.app/books/b10901/s5.html",new int[]{4},new int[]{4},264,457,76),
        new Entry(7,44,11,18,"Variation de mot ou de particule","7:44","11:18","","al-Abbad · 77","https://quranpedia.app/books/b10901/s5.html",new int[]{24},new int[]{19},156,223,77),
        new Entry(7,57,25,48,"Variation de mot ou de particule","7:57","25:48","","al-Abbad · 78","https://quranpedia.app/books/b10901/s5.html",new int[]{3},new int[]{3},157,364,78),
        new Entry(7,59,11,26,"Variation de mot ou de particule","7:59","11:26","","al-Abbad · 79","https://quranpedia.app/books/b10901/s5.html",new int[]{20},new int[]{11},158,224,79),
        new Entry(7,82,27,56,"Variation de mot ou de particule","7:82","27:56","","al-Abbad · 80","https://quranpedia.app/books/b10901/s5.html",new int[]{8},new int[]{8},161,382,80),
        new Entry(27,56,29,24,"Variation de mot ou de particule","27:56","29:24","","al-Abbad · 81","https://quranpedia.app/books/b10901/s5.html",new int[]{8},new int[]{8},382,399,81),
        new Entry(7,103,10,75,"Variation de mot ou de particule","7:103","10:75","","al-Abbad · 82","https://quranpedia.app/books/b10901/s5.html",new int[]{6},new int[]{6},163,217,82),
        new Entry(7,188,10,49,"Variation de mot ou de particule","7:188","10:49","","al-Abbad · 83","https://quranpedia.app/books/b10901/s5.html",new int[]{5},new int[]{5},175,214,83),
        new Entry(10,48,32,28,"Variation de mot ou de particule","10:48","32:28","","al-Abbad · 84","https://quranpedia.app/books/b10901/s5.html",new int[]{4},new int[]{4},214,417,84),
        new Entry(10,47,10,54,"Variation de mot ou de particule","10:47","10:54","","al-Abbad · 85","https://quranpedia.app/books/b10901/s5.html",new int[]{7},new int[]{16},214,215,85),
        new Entry(10,61,34,3,"Variation de mot ou de particule","10:61","34:3","","al-Abbad · 86","https://quranpedia.app/books/b10901/s5.html",new int[]{29},new int[]{19},215,428,86),
        new Entry(11,29,11,51,"Variation de mot ou de particule","11:29","11:51","","al-Abbad · 87","https://quranpedia.app/books/b10901/s5.html",new int[]{5},new int[]{5},225,227,87),
        new Entry(11,66,11,82,"Variation de mot ou de particule","11:66","11:82","","al-Abbad · 88","https://quranpedia.app/books/b10901/s5.html",new int[]{4},new int[]{4},229,231,88),
        new Entry(13,2,31,10,"Variation de mot ou de particule","13:2","31:10","","al-Abbad · 89","https://quranpedia.app/books/b10901/s5.html",new int[]{3},new int[]{1},249,411,89),
        new Entry(13,2,31,29,"Variation de mot ou de particule","13:2","31:29","","al-Abbad · 90","https://quranpedia.app/books/b10901/s5.html",new int[]{17},new int[]{18},249,414,90),
        new Entry(16,78,23,78,"Variation de mot ou de particule","16:78","23:78","","al-Abbad · 91","https://quranpedia.app/books/b10901/s5.html",new int[]{14},new int[]{8},275,347,91),
        new Entry(23,78,67,23,"Variation de mot ou de particule","23:78","67:23","","al-Abbad · 92","https://quranpedia.app/books/b10901/s5.html",new int[]{3},new int[]{5},347,563,92),
        new Entry(17,9,18,2,"Variation de mot ou de particule","17:9","18:2","","al-Abbad · 93","https://quranpedia.app/books/b10901/s5.html",new int[]{16},new int[]{15},283,293,93),
        new Entry(17,68,17,69,"Variation de mot ou de particule","17:68","17:69","","al-Abbad · 94","https://quranpedia.app/books/b10901/s5.html",new int[]{15},new int[]{20},289,289,94),
        new Entry(17,75,17,86,"Variation de mot ou de particule","17:75","17:86","","al-Abbad · 95","https://quranpedia.app/books/b10901/s5.html",new int[]{11},new int[]{11},289,290,95),
        new Entry(17,96,29,52,"Variation de mot ou de particule","17:96","29:52","","al-Abbad · 96","https://quranpedia.app/books/b10901/s6.html",new int[]{4},new int[]{4},291,402,96),
        new Entry(18,57,32,22,"Variation de mot ou de particule","18:57","32:22","","al-Abbad · 97","https://quranpedia.app/books/b10901/s6.html",new int[]{7},new int[]{7},300,417,97),
        new Entry(19,37,43,65,"Variation de mot ou de particule","19:37","43:65","","al-Abbad · 98","https://quranpedia.app/books/b10901/s6.html",new int[]{7},new int[]{7},307,494,98),
        new Entry(20,22,27,12,"Variation de mot ou de particule","20:22","27:12","","al-Abbad · 99","https://quranpedia.app/books/b10901/s6.html",new int[]{1},new int[]{1},313,377,99),
        new Entry(20,40,28,12,"Variation de mot ou de particule","20:40","28:12","","al-Abbad · 100","https://quranpedia.app/books/b10901/s6.html",new int[]{8},new int[]{10},314,386,100),
        new Entry(20,53,43,10,"Variation de mot ou de particule","20:53","43:10","","al-Abbad · 101","https://quranpedia.app/books/b10901/s6.html",new int[]{6},new int[]{6},315,489,101),
        new Entry(21,92,23,52,"Variation de mot ou de particule","21:92","23:52","","al-Abbad · 102","https://quranpedia.app/books/b10901/s6.html",new int[]{8},new int[]{8},330,345,102),
        new Entry(22,5,41,39,"Variation de mot ou de particule","22:5","41:39","","al-Abbad · 103","https://quranpedia.app/books/b10901/s6.html",new int[]{58},new int[]{6},332,481,103),
        new Entry(29,8,31,14,"Variation de mot ou de particule","29:8","31:14","","al-Abbad · 104","https://quranpedia.app/books/b10901/s6.html",new int[]{4},new int[]{4},397,412,104),
        new Entry(31,14,46,15,"Variation de mot ou de particule","31:14","46:15","","al-Abbad · 105","https://quranpedia.app/books/b10901/s6.html",new int[]{4},new int[]{4},412,504,105),
        new Entry(29,8,31,15,"Variation de mot ou de particule","29:8","31:15","","al-Abbad · 106","https://quranpedia.app/books/b10901/s6.html",new int[]{7},new int[]{3},397,412,106),
        new Entry(37,27,37,50,"Variation de mot ou de particule","37:27","37:50","","al-Abbad · 107","https://quranpedia.app/books/b10901/s6.html",new int[]{1},new int[]{1},447,447,107),
        new Entry(39,48,45,33,"Variation de mot ou de particule","39:48","45:33","","al-Abbad · 108","https://quranpedia.app/books/b10901/s6.html",new int[]{5},new int[]{5},464,502,108),
        new Entry(59,13,59,14,"Variation de mot ou de particule","59:13","59:14","","al-Abbad · 109","https://quranpedia.app/books/b10901/s6.html",new int[]{12},new int[]{23},547,547,109),
        new Entry(71,24,71,28,"Variation de mot ou de particule","71:24","71:28","","al-Abbad · 110","https://quranpedia.app/books/b10901/s6.html",new int[]{8},new int[]{15},571,571,110),
        new Entry(2,61,3,21,"Variation de mot ou de particule","2:61","3:21","","al-Abbad · 111","https://quranpedia.app/books/b10901/s6.html",new int[]{54},new int[]{9},9,52,111),
        new Entry(3,21,3,112,"Variation de mot ou de particule","3:21","3:112","","al-Abbad · 112","https://quranpedia.app/books/b10901/s6.html",new int[]{7},new int[]{28},52,64,112),
        new Entry(2,116,10,68,"Variation de mot ou de particule","2:116","10:68","","al-Abbad · 113","https://quranpedia.app/books/b10901/s6.html",new int[]{1},new int[]{1},18,216,113),
        new Entry(2,145,13,37,"Variation de mot ou de particule","2:145","13:37","","al-Abbad · 114","https://quranpedia.app/books/b10901/s6.html",new int[]{23},new int[]{8},22,254,114),
        new Entry(13,37,2,120,"Variation de mot ou de particule","13:37","2:120","","al-Abbad · 115","https://quranpedia.app/books/b10901/s6.html",new int[]{9},new int[]{20},254,19,115),
        new Entry(2,147,3,60,"Variation de mot ou de particule","2:147","3:60","","al-Abbad · 116","https://quranpedia.app/books/b10901/s6.html",new int[]{5},new int[]{5},23,57,116),
        new Entry(2,218,8,72,"Variation de mot ou de particule","2:218","8:72","","al-Abbad · 117","https://quranpedia.app/books/b10901/s7.html",new int[]{4},new int[]{4},34,186,117),
        new Entry(3,126,8,10,"Variation de mot ou de particule","3:126","8:10","","al-Abbad · 118","https://quranpedia.app/books/b10901/s7.html",new int[]{8},new int[]{7},66,178,118),
        new Entry(4,124,16,97,"Variation de mot ou de particule","4:124","16:97","","al-Abbad · 120","https://quranpedia.app/books/b10901/s7.html",new int[]{2},new int[]{2},98,278,120),
        new Entry(16,97,40,40,"Variation de mot ou de particule","16:97","40:40","","al-Abbad · 121","https://quranpedia.app/books/b10901/s7.html",new int[]{1},new int[]{8},278,471,121),
        new Entry(5,92,64,12,"Variation de mot ou de particule","5:92","64:12","","al-Abbad · 122","https://quranpedia.app/books/b10901/s7.html",new int[]{5},new int[]{5},123,557,122),
        new Entry(64,12,24,54,"Variation de mot ou de particule","64:12","24:54","","al-Abbad · 123","https://quranpedia.app/books/b10901/s7.html",new int[]{6},new int[]{7},557,357,123),
        new Entry(6,5,26,6,"Variation de mot ou de particule","6:5","26:6","","al-Abbad · 124","https://quranpedia.app/books/b10901/s7.html",new int[]{6},new int[]{3},128,367,124),
        new Entry(6,32,7,169,"Variation de mot ou de particule","6:32","7:169","","al-Abbad · 125","https://quranpedia.app/books/b10901/s7.html",new int[]{7},new int[]{34},131,172,125),
        new Entry(6,50,11,31,"Variation de mot ou de particule","6:50","11:31","","al-Abbad · 126","https://quranpedia.app/books/b10901/s7.html",new int[]{13},new int[]{12},133,225,126),
        new Entry(6,135,11,93,"Variation de mot ou de particule","6:135","11:93","","al-Abbad · 127","https://quranpedia.app/books/b10901/s7.html",new int[]{8},new int[]{7},145,232,127),
        new Entry(11,93,39,39,"Variation de mot ou de particule","11:93","39:39","","al-Abbad · 128","https://quranpedia.app/books/b10901/s7.html",new int[]{7},new int[]{8},232,462,128),
        new Entry(7,22,20,121,"Variation de mot ou de particule","7:22","20:121","","al-Abbad · 129","https://quranpedia.app/books/b10901/s7.html",new int[]{6},new int[]{3},152,320,129),
        new Entry(7,34,10,49,"Variation de mot ou de particule","7:34","10:49","","al-Abbad · 130","https://quranpedia.app/books/b10901/s7.html",new int[]{7},new int[]{18},154,214,130),
        new Entry(7,62,7,68,"Variation de mot ou de particule","7:62","7:68","","al-Abbad · 131","https://quranpedia.app/books/b10901/s7.html",new int[]{4},new int[]{4},158,159,131),
        new Entry(7,178,18,17,"Variation de mot ou de particule","7:178","18:17","","al-Abbad · 132","https://quranpedia.app/books/b10901/s7.html",new int[]{5},new int[]{27},173,295,132),
        new Entry(18,17,17,97,"Variation de mot ou de particule","18:17","17:97","","al-Abbad · 133","https://quranpedia.app/books/b10901/s7.html",new int[]{32},new int[]{10},295,292,133),
        new Entry(8,4,8,74,"Variation de mot ou de particule","8:4","8:74","","al-Abbad · 134","https://quranpedia.app/books/b10901/s7.html",new int[]{6},new int[]{16},177,186,134),
        new Entry(8,13,59,4,"Variation de mot ou de particule","8:13","59:4","","al-Abbad · 135","https://quranpedia.app/books/b10901/s7.html",new int[]{7},new int[]{7},178,546,135),
        new Entry(8,28,64,15,"Variation de mot ou de particule","8:28","64:15","","al-Abbad · 136","https://quranpedia.app/books/b10901/s7.html",new int[]{6},new int[]{5},180,557,136),
        new Entry(9,55,9,85,"Variation de mot ou de particule","9:55","9:85","","al-Abbad · 139","https://quranpedia.app/books/b10901/s8.html",new int[]{1},new int[]{1},196,200,139),
        new Entry(10,12,39,8,"Variation de mot ou de particule","10:12","39:8","","al-Abbad · 140","https://quranpedia.app/books/b10901/s8.html",new int[]{4},new int[]{4},209,459,140),
        new Entry(10,108,27,92,"Variation de mot ou de particule","10:108","27:92","","al-Abbad · 141","https://quranpedia.app/books/b10901/s8.html",new int[]{16},new int[]{11},221,385,141),
        new Entry(11,60,11,99,"Variation de mot ou de particule","11:60","11:99","","al-Abbad · 142","https://quranpedia.app/books/b10901/s8.html",new int[]{4},new int[]{4},228,233,142),
        new Entry(12,109,16,43,"Variation de mot ou de particule","12:109","16:43","","al-Abbad · 143","https://quranpedia.app/books/b10901/s8.html",new int[]{9},new int[]{9},248,272,143),
        new Entry(16,29,39,72,"Variation de mot ou de particule","16:29","39:72","","al-Abbad · 144","https://quranpedia.app/books/b10901/s8.html",new int[]{6},new int[]{7},270,466,144),
        new Entry(39,72,40,76,"Variation de mot ou de particule","39:72","40:76","","al-Abbad · 145","https://quranpedia.app/books/b10901/s8.html",new int[]{1},new int[]{1},466,475,145),
        new Entry(16,43,21,7,"Variation de mot ou de particule","16:43","21:7","","al-Abbad · 146","https://quranpedia.app/books/b10901/s8.html",new int[]{3},new int[]{3},272,322,146),
        new Entry(22,5,40,67,"Variation de mot ou de particule","22:5","40:67","","al-Abbad · 147","https://quranpedia.app/books/b10901/s8.html",new int[]{21},new int[]{13},332,475,147),
        new Entry(22,22,32,20,"Variation de mot ou de particule","22:22","32:20","","al-Abbad · 148","https://quranpedia.app/books/b10901/s8.html",new int[]{6},new int[]{11},334,416,148),
        new Entry(22,62,31,30,"Variation de mot ou de particule","22:62","31:30","","al-Abbad · 149","https://quranpedia.app/books/b10901/s8.html",new int[]{11},new int[]{11},339,414,149),
        new Entry(23,9,70,34,"Variation de mot ou de particule","23:9","70:34","","al-Abbad · 150","https://quranpedia.app/books/b10901/s8.html",new int[]{4},new int[]{4},342,569,150),
        new Entry(23,96,41,34,"Variation de mot ou de particule","23:96","41:34","","al-Abbad · 151","https://quranpedia.app/books/b10901/s8.html",new int[]{5},new int[]{10},348,480,151),
        new Entry(24,58,24,59,"Variation de mot ou de particule","24:58","24:59","","al-Abbad · 152","https://quranpedia.app/books/b10901/s8.html",new int[]{46},new int[]{16},357,358,152),
        new Entry(28,60,42,36,"Variation de mot ou de particule","28:60","42:36","","al-Abbad · 153","https://quranpedia.app/books/b10901/s8.html",new int[]{8},new int[]{8},393,487,153),
        new Entry(28,78,39,49,"Variation de mot ou de particule","28:78","39:49","","al-Abbad · 154","https://quranpedia.app/books/b10901/s8.html",new int[]{6},new int[]{16},395,464,154),
        new Entry(29,22,42,31,"Variation de mot ou de particule","29:22","42:31","","al-Abbad · 155","https://quranpedia.app/books/b10901/s8.html",new int[]{6},new int[]{6},398,486,155),
        new Entry(40,22,64,6,"Variation de mot ou de particule","40:22","64:6","","al-Abbad · 156","https://quranpedia.app/books/b10901/s9.html",new int[]{2},new int[]{2},469,556,156),
        new Entry(2,36,7,24,"Variation de mot ou de particule","2:36","7:24","","al-Abbad · 157","https://quranpedia.app/books/b10901/s9.html",new int[]{8},new int[]{1},6,153,157),
        new Entry(2,59,7,162,"Variation de mot ou de particule","2:59","7:162","","al-Abbad · 158","https://quranpedia.app/books/b10901/s9.html",new int[]{4},new int[]{4},9,171,158),
        new Entry(2,80,3,24,"Variation de mot ou de particule","2:80","3:24","","al-Abbad · 159","https://quranpedia.app/books/b10901/s9.html",new int[]{7},new int[]{9},12,53,159),
        new Entry(2,126,14,35,"Variation de mot ou de particule","2:126","14:35","","al-Abbad · 160","https://quranpedia.app/books/b10901/s9.html",new int[]{7},new int[]{7},19,260,160),
        new Entry(2,174,3,77,"Variation de mot ou de particule","2:174","3:77","","al-Abbad · 161","https://quranpedia.app/books/b10901/s9.html",new int[]{23},new int[]{20},26,59,161),
        new Entry(2,193,8,39,"Variation de mot ou de particule","2:193","8:39","","al-Abbad · 163","https://quranpedia.app/books/b10901/s9.html",new int[]{8},new int[]{8},30,181,163),
        new Entry(2,210,6,158,"Variation de mot ou de particule","2:210","6:158","","al-Abbad · 164","https://quranpedia.app/books/b10901/s9.html",new int[]{5},new int[]{5},32,150,164),
        new Entry(6,158,16,33,"Variation de mot ou de particule","6:158","16:33","","al-Abbad · 165","https://quranpedia.app/books/b10901/s9.html",new int[]{9},new int[]{9},150,270,165),
        new Entry(2,232,65,2,"Variation de mot ou de particule","2:232","65:2","","al-Abbad · 166","https://quranpedia.app/books/b10901/s9.html",new int[]{15},new int[]{16},37,558,166),
        new Entry(2,262,2,274,"Variation de mot ou de particule","2:262","2:274","","al-Abbad · 167","https://quranpedia.app/books/b10901/s9.html",new int[]{4},new int[]{4},44,46,167),
        new Entry(2,274,2,277,"Variation de mot ou de particule","2:274","2:277","","al-Abbad · 168","https://quranpedia.app/books/b10901/s9.html",new int[]{8},new int[]{10},46,47,168),
        new Entry(3,11,8,52,"Variation de mot ou de particule","3:11","8:52","","al-Abbad · 169","https://quranpedia.app/books/b10901/s9.html",new int[]{7},new int[]{7},51,183,169),
        new Entry(8,52,8,54,"Variation de mot ou de particule","8:52","8:54","","al-Abbad · 170","https://quranpedia.app/books/b10901/s9.html",new int[]{7},new int[]{7},183,184,170),
        new Entry(46,31,71,4,"Variation de mot ou de particule","46:31","71:4","","al-Abbad · 171","https://quranpedia.app/books/b10901/s9.html",new int[]{11},new int[]{5},506,570,171),
        new Entry(3,51,19,36,"Variation de mot ou de particule","3:51","19:36","","al-Abbad · 172","https://quranpedia.app/books/b10901/s9.html",new int[]{1},new int[]{1},56,307,172),
        new Entry(19,36,43,64,"Variation de mot ou de particule","19:36","43:64","","al-Abbad · 173","https://quranpedia.app/books/b10901/s9.html",new int[]{1},new int[]{1},307,494,173),
        new Entry(3,133,57,21,"Variation de mot ou de particule","3:133","57:21","","al-Abbad · 174","https://quranpedia.app/books/b10901/s9.html",new int[]{8},new int[]{8},67,540,174),
        new Entry(35,4,35,25,"Variation de mot ou de particule","35:4","35:25","","al-Abbad · 175","https://quranpedia.app/books/b10901/s9.html",new int[]{4},new int[]{4},435,437,175),
        new Entry(4,43,5,6,"Variation de mot ou de particule","4:43","5:6","","al-Abbad · 177","https://quranpedia.app/books/b10901/s10.html",new int[]{44},new int[]{46},85,108,177),
        new Entry(4,171,5,77,"Variation de mot ou de particule","4:171","5:77","","al-Abbad · 178","https://quranpedia.app/books/b10901/s10.html",new int[]{7},new int[]{8},105,120,178),
        new Entry(6,29,23,37,"Variation de mot ou de particule","6:29","23:37","","al-Abbad · 179","https://quranpedia.app/books/b10901/s10.html",new int[]{7},new int[]{6},131,344,179),
        new Entry(6,93,46,20,"Variation de mot ou de particule","6:93","46:20","","al-Abbad · 180","https://quranpedia.app/books/b10901/s10.html",new int[]{35},new int[]{14},139,504,180),
        new Entry(6,117,68,7,"Variation de mot ou de particule","6:117","68:7","","al-Abbad · 181","https://quranpedia.app/books/b10901/s10.html",new int[]{5},new int[]{5},142,564,181),
        new Entry(68,7,53,30,"Variation de mot ou de particule","68:7","53:30","","al-Abbad · 182","https://quranpedia.app/books/b10901/s10.html",new int[]{11},new int[]{15},564,527,182),
        new Entry(6,165,7,167,"Variation de mot ou de particule","6:165","7:167","","al-Abbad · 183","https://quranpedia.app/books/b10901/s10.html",new int[]{17},new int[]{15},150,172,183),
        new Entry(7,45,11,19,"Variation de mot ou de particule","7:45","11:19","","al-Abbad · 184","https://quranpedia.app/books/b10901/s10.html",new int[]{10},new int[]{10},156,223,184),
        new Entry(7,54,25,59,"Variation de mot ou de particule","7:54","25:59","","al-Abbad · 185","https://quranpedia.app/books/b10901/s10.html",new int[]{7},new int[]{5},157,365,185),
        new Entry(7,59,11,25,"Variation de mot ou de particule","7:59","11:25","","al-Abbad · 186","https://quranpedia.app/books/b10901/s10.html",new int[]{6},new int[]{6},158,224,186),
        new Entry(11,27,23,24,"Variation de mot ou de particule","11:27","23:24","","al-Abbad · 189","https://quranpedia.app/books/b10901/s10.html",new int[]{8},new int[]{8},224,343,189),
        new Entry(7,80,29,28,"Variation de mot ou de particule","7:80","29:28","","al-Abbad · 190","https://quranpedia.app/books/b10901/s10.html",new int[]{5},new int[]{5},160,399,190),
        new Entry(7,81,27,55,"Variation de mot ou de particule","7:81","27:55","","al-Abbad · 191","https://quranpedia.app/books/b10901/s10.html",new int[]{11},new int[]{11},160,381,191),
        new Entry(27,55,29,29,"Variation de mot ou de particule","27:55","29:29","","al-Abbad · 192","https://quranpedia.app/books/b10901/s10.html",new int[]{4},new int[]{4},381,399,192),
        new Entry(7,85,11,85,"Variation de mot ou de particule","7:85","11:85","","al-Abbad · 193","https://quranpedia.app/books/b10901/s10.html",new int[]{27},new int[]{11},161,231,193),
        new Entry(7,101,10,74,"Variation de mot ou de particule","7:101","10:74","","al-Abbad · 194","https://quranpedia.app/books/b10901/s10.html",new int[]{16},new int[]{15},163,217,194),
        new Entry(7,125,26,50,"Variation de mot ou de particule","7:125","26:50","","al-Abbad · 195","https://quranpedia.app/books/b10901/s10.html",new int[]{2},new int[]{2},165,369,195),
        new Entry(7,200,41,36,"Variation de mot ou de particule","7:200","41:36","","al-Abbad · 196","https://quranpedia.app/books/b10901/s10.html",new int[]{9},new int[]{9},176,480,196),
        new Entry(8,31,10,15,"Variation de mot ou de particule","8:31","10:15","","al-Abbad · 197","https://quranpedia.app/books/b10901/s10.html",new int[]{5},new int[]{5},180,210,197),
        new Entry(9,87,9,93,"Variation de mot ou de particule","9:87","9:93","","al-Abbad · 198","https://quranpedia.app/books/b10901/s11.html",new int[]{6},new int[]{13},201,201,198),
        new Entry(9,94,9,105,"Variation de mot ou de particule","9:94","9:105","","al-Abbad · 199","https://quranpedia.app/books/b10901/s11.html",new int[]{21},new int[]{8},202,203,199),
        new Entry(10,31,34,24,"Variation de mot ou de particule","10:31","34:24","","al-Abbad · 200","https://quranpedia.app/books/b10901/s11.html",new int[]{5},new int[]{5},212,431,200),
        new Entry(11,77,29,33,"Variation de mot ou de particule","11:77","29:33","","al-Abbad · 202","https://quranpedia.app/books/b10901/s11.html",new int[]{2},new int[]{2},230,400,202),
        new Entry(11,81,15,65,"Variation de mot ou de particule","11:81","15:65","","al-Abbad · 203","https://quranpedia.app/books/b10901/s11.html",new int[]{14},new int[]{6},230,265,203),
        new Entry(12,22,28,14,"Variation de mot ou de particule","12:22","28:14","","al-Abbad · 204","https://quranpedia.app/books/b10901/s11.html",new int[]{4},new int[]{4},237,387,204),
        new Entry(13,26,17,30,"Variation de mot ou de particule","13:26","17:30","","al-Abbad · 205","https://quranpedia.app/books/b10901/s11.html",new int[]{1},new int[]{2},252,285,205),
        new Entry(17,30,28,82,"Variation de mot ou de particule","17:30","28:82","","al-Abbad · 206","https://quranpedia.app/books/b10901/s11.html",new int[]{7},new int[]{13},285,395,206),
        new Entry(28,82,29,62,"Variation de mot ou de particule","28:82","29:62","","al-Abbad · 207","https://quranpedia.app/books/b10901/s11.html",new int[]{16},new int[]{9},395,403,207),
        new Entry(29,62,30,37,"Variation de mot ou de particule","29:62","30:37","","al-Abbad · 208","https://quranpedia.app/books/b10901/s11.html",new int[]{6},new int[]{9},403,408,208),
        new Entry(30,37,34,36,"Variation de mot ou de particule","30:37","34:36","","al-Abbad · 209","https://quranpedia.app/books/b10901/s11.html",new int[]{4},new int[]{3},408,432,209),
        new Entry(34,36,34,39,"Variation de mot ou de particule","34:36","34:39","","al-Abbad · 210","https://quranpedia.app/books/b10901/s11.html",new int[]{8},new int[]{8},432,432,210),
        new Entry(34,39,39,52,"Variation de mot ou de particule","34:39","39:52","","al-Abbad · 211","https://quranpedia.app/books/b10901/s11.html",new int[]{8},new int[]{9},432,464,211),
        new Entry(15,88,20,131,"Variation de mot ou de particule","15:88","20:131","","al-Abbad · 213","https://quranpedia.app/books/b10901/s11.html",new int[]{10},new int[]{10},266,321,213),
        new Entry(16,55,29,66,"Variation de mot ou de particule","16:55","29:66","","al-Abbad · 214","https://quranpedia.app/books/b10901/s11.html",new int[]{4},new int[]{4},273,404,214),
        new Entry(16,58,43,17,"Variation de mot ou de particule","16:58","43:17","","al-Abbad · 215","https://quranpedia.app/books/b10901/s11.html",new int[]{4},new int[]{4},273,490,215),
        new Entry(16,66,23,21,"Variation de mot ou de particule","16:66","23:21","","al-Abbad · 216","https://quranpedia.app/books/b10901/s11.html",new int[]{9},new int[]{9},274,343,216),
        new Entry(17,56,34,22,"Variation de mot ou de particule","17:56","34:22","","al-Abbad · 217","https://quranpedia.app/books/b10901/s11.html",new int[]{6},new int[]{6},287,430,217),
        new Entry(18,72,18,75,"Variation de mot ou de particule","18:72","18:75","","al-Abbad · 218","https://quranpedia.app/books/b10901/s11.html",new int[]{4},new int[]{4},301,302,218),
        new Entry(19,60,25,70,"Variation de mot ou de particule","19:60","25:70","","al-Abbad · 219","https://quranpedia.app/books/b10901/s11.html",new int[]{6},new int[]{6},309,366,219),
        new Entry(20,128,32,26,"Variation de mot ou de particule","20:128","32:26","","al-Abbad · 220","https://quranpedia.app/books/b10901/s11.html",new int[]{6},new int[]{6},321,417,220),
        new Entry(25,53,35,12,"Variation de mot ou de particule","25:53","35:12","","al-Abbad · 222","https://quranpedia.app/books/b10901/s11.html",new int[]{8},new int[]{7},364,436,222),
        new Entry(26,70,37,85,"Variation de mot ou de particule","26:70","37:85","","al-Abbad · 223","https://quranpedia.app/books/b10901/s11.html",new int[]{5},new int[]{5},370,449,223),
        new Entry(27,10,28,31,"Variation de mot ou de particule","27:10","28:31","","al-Abbad · 224","https://quranpedia.app/books/b10901/s12.html",new int[]{13},new int[]{14},377,389,224),
        new Entry(27,80,30,52,"Variation de mot ou de particule","27:80","30:52","","al-Abbad · 225","https://quranpedia.app/books/b10901/s12.html",new int[]{1},new int[]{1},384,410,225),
        new Entry(30,9,35,44,"Variation de mot ou de particule","30:9","35:44","","al-Abbad · 226","https://quranpedia.app/books/b10901/s12.html",new int[]{12},new int[]{12},405,439,226),
        new Entry(35,44,40,21,"Variation de mot ou de particule","35:44","40:21","","al-Abbad · 227","https://quranpedia.app/books/b10901/s12.html",new int[]{10},new int[]{10},439,469,227),
        new Entry(40,21,40,82,"Variation de mot ou de particule","40:21","40:82","","al-Abbad · 228","https://quranpedia.app/books/b10901/s12.html",new int[]{1},new int[]{1},469,476,228),
        new Entry(39,71,39,73,"Variation de mot ou de particule","39:71","39:73","","al-Abbad · 229","https://quranpedia.app/books/b10901/s12.html",new int[]{10},new int[]{11},466,466,229),
        new Entry(41,8,84,25,"Variation de mot ou de particule","41:8","84:25","","al-Abbad · 230","https://quranpedia.app/books/b10901/s12.html",new int[]{1},new int[]{1},477,590,230),
        new Entry(84,25,95,6,"Variation de mot ou de particule","84:25","95:6","","al-Abbad · 231","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},590,597,231),
        new Entry(57,1,59,1,"Variation de mot ou de particule","57:1","59:1","","al-Abbad · 232","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},537,545,232),
        new Entry(2,14,2,76,"Même amorce, bifurcation","2:14","2:76","","al-Abbad · 233","https://quranpedia.app/books/b10901/s12.html",new int[]{8},new int[]{8},3,11,233),
        new Entry(2,16,2,175,"Même amorce, bifurcation","2:16","2:175","","al-Abbad · 234","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},3,26,234),
        new Entry(2,146,6,20,"Même amorce, bifurcation","2:146","6:20","","al-Abbad · 235","https://quranpedia.app/books/b10901/s12.html",new int[]{8},new int[]{8},23,130,235),
        new Entry(2,214,3,142,"Même amorce, bifurcation","2:214","3:142","","al-Abbad · 236","https://quranpedia.app/books/b10901/s12.html",new int[]{7},new int[]{7},33,68,236),
        new Entry(3,10,3,116,"Même amorce, bifurcation","3:10","3:116","","al-Abbad · 237","https://quranpedia.app/books/b10901/s12.html",new int[]{14},new int[]{14},51,65,237),
        new Entry(3,185,21,35,"Même amorce, bifurcation","3:185","21:35","","al-Abbad · 238","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},74,324,238),
        new Entry(21,35,29,57,"Même amorce, bifurcation","21:35","29:57","","al-Abbad · 239","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},324,403,239),
        new Entry(4,37,57,24,"Même amorce, bifurcation","4:37","57:24","","al-Abbad · 240","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},84,540,240),
        new Entry(4,44,4,51,"Même amorce, bifurcation","4:44","4:51","","al-Abbad · 241","https://quranpedia.app/books/b10901/s12.html",new int[]{9},new int[]{9},85,86,241),
        new Entry(4,48,4,116,"Même amorce, bifurcation","4:48","4:116","","al-Abbad · 242","https://quranpedia.app/books/b10901/s12.html",new int[]{18},new int[]{18},86,97,242),
        new Entry(4,57,4,122,"Même amorce, bifurcation","4:57","4:122","","al-Abbad · 243","https://quranpedia.app/books/b10901/s12.html",new int[]{14},new int[]{14},87,98,243),
        new Entry(4,61,5,104,"Même amorce, bifurcation","4:61","5:104","","al-Abbad · 244","https://quranpedia.app/books/b10901/s12.html",new int[]{11},new int[]{11},88,125,244),
        new Entry(4,133,6,133,"Même amorce, bifurcation","4:133","6:133","","al-Abbad · 245","https://quranpedia.app/books/b10901/s12.html",new int[]{4},new int[]{8},99,145,245),
        new Entry(5,17,5,72,"Même amorce, bifurcation","5:17","5:72","","al-Abbad · 246","https://quranpedia.app/books/b10901/s12.html",new int[]{11},new int[]{11},110,120,246),
        new Entry(6,10,13,32,"Même amorce, bifurcation","6:10","13:32","","al-Abbad · 247","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},129,253,247),
        new Entry(6,18,6,61,"Même amorce, bifurcation","6:18","6:61","","al-Abbad · 248","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},129,135,248),
        new Entry(6,22,10,28,"Même amorce, bifurcation","6:22","10:28","","al-Abbad · 249","https://quranpedia.app/books/b10901/s12.html",new int[]{8},new int[]{8},130,212,249),
        new Entry(6,92,6,155,"Même amorce, bifurcation","6:92","6:155","","al-Abbad · 250","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},139,149,250),
        new Entry(6,109,16,38,"Même amorce, bifurcation","6:109","16:38","","al-Abbad · 251","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},141,271,251),
        new Entry(16,38,24,53,"Même amorce, bifurcation","16:38","24:53","","al-Abbad · 252","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},271,356,252),
        new Entry(24,53,35,42,"Même amorce, bifurcation","24:53","35:42","","al-Abbad · 253","https://quranpedia.app/books/b10901/s12.html",new int[]{6},new int[]{6},356,439,253),
        new Entry(6,132,46,19,"Même amorce, bifurcation","6:132","46:19","","al-Abbad · 254","https://quranpedia.app/books/b10901/s12.html",new int[]{5},new int[]{5},145,504,254),
        new Entry(7,37,10,17,"Même amorce, bifurcation","7:37","10:17","","al-Abbad · 255","https://quranpedia.app/books/b10901/s13.html",new int[]{11},new int[]{11},154,210,255),
        new Entry(7,43,15,47,"Même amorce, bifurcation","7:43","15:47","","al-Abbad · 256","https://quranpedia.app/books/b10901/s13.html",new int[]{7},new int[]{7},155,264,256),
        new Entry(7,56,7,85,"Même amorce, bifurcation","7:56","7:85","","al-Abbad · 257","https://quranpedia.app/books/b10901/s13.html",new int[]{7},new int[]{32},157,161,257),
        new Entry(7,63,7,69,"Même amorce, bifurcation","7:63","7:69","","al-Abbad · 258","https://quranpedia.app/books/b10901/s13.html",new int[]{11},new int[]{11},158,159,258),
        new Entry(7,69,7,74,"Même amorce, bifurcation","7:69","7:74","","al-Abbad · 259","https://quranpedia.app/books/b10901/s13.html",new int[]{26},new int[]{21},159,160,259),
        new Entry(7,84,26,173,"Même amorce, bifurcation","7:84","26:173","","al-Abbad · 260","https://quranpedia.app/books/b10901/s13.html",new int[]{4},new int[]{4},161,374,260),
        new Entry(9,5,9,11,"Même amorce, bifurcation","9:5","9:11","","al-Abbad · 261","https://quranpedia.app/books/b10901/s13.html",new int[]{21},new int[]{7},187,188,261),
        new Entry(9,33,48,28,"Même amorce, bifurcation","9:33","48:28","","al-Abbad · 262","https://quranpedia.app/books/b10901/s13.html",new int[]{12},new int[]{12},192,514,262),
        new Entry(11,17,46,12,"Même amorce, bifurcation","11:17","46:12","","al-Abbad · 263","https://quranpedia.app/books/b10901/s13.html",new int[]{16},new int[]{7},223,503,263),
        new Entry(12,21,12,56,"Même amorce, bifurcation","12:21","12:56","","al-Abbad · 264","https://quranpedia.app/books/b10901/s13.html",new int[]{20},new int[]{6},237,242,264),
        new Entry(12,109,47,10,"Même amorce, bifurcation","12:109","47:10","","al-Abbad · 265","https://quranpedia.app/books/b10901/s13.html",new int[]{23},new int[]{12},248,507,265),
        new Entry(15,13,26,201,"Même amorce, bifurcation","15:13","26:201","","al-Abbad · 266","https://quranpedia.app/books/b10901/s13.html",new int[]{4},new int[]{4},262,375,266),
        new Entry(17,83,41,51,"Même amorce, bifurcation","17:83","41:51","","al-Abbad · 267","https://quranpedia.app/books/b10901/s13.html",new int[]{11},new int[]{11},290,482,267),
        new Entry(17,94,18,55,"Même amorce, bifurcation","17:94","18:55","","al-Abbad · 268","https://quranpedia.app/books/b10901/s13.html",new int[]{9},new int[]{9},291,300,268),
        new Entry(18,110,41,6,"Même amorce, bifurcation","18:110","41:6","","al-Abbad · 269","https://quranpedia.app/books/b10901/s13.html",new int[]{12},new int[]{12},304,477,269),
        new Entry(20,130,50,39,"Même amorce, bifurcation","20:130","50:39","","al-Abbad · 271","https://quranpedia.app/books/b10901/s13.html",new int[]{12},new int[]{12},321,520,271),
        new Entry(22,3,22,8,"Même amorce, bifurcation","22:3","22:8","","al-Abbad · 272","https://quranpedia.app/books/b10901/s13.html",new int[]{9},new int[]{9},332,333,272),
        new Entry(22,51,34,5,"Même amorce, bifurcation","22:51","34:5","","al-Abbad · 273","https://quranpedia.app/books/b10901/s13.html",new int[]{7},new int[]{7},338,428,273),
        new Entry(24,14,24,21,"Même amorce, bifurcation","24:14","24:21","","al-Abbad · 274","https://quranpedia.app/books/b10901/s13.html",new int[]{6},new int[]{21},351,352,274),
        new Entry(27,69,30,42,"Même amorce, bifurcation","27:69","30:42","","al-Abbad · 275","https://quranpedia.app/books/b10901/s14.html",new int[]{9},new int[]{9},383,409,275),
        new Entry(30,42,29,20,"Même amorce, bifurcation","30:42","29:20","","al-Abbad · 276","https://quranpedia.app/books/b10901/s14.html",new int[]{7},new int[]{7},409,398,276),
        new Entry(36,29,36,53,"Même amorce, bifurcation","36:29","36:53","","al-Abbad · 278","https://quranpedia.app/books/b10901/s14.html",new int[]{8},new int[]{8},442,443,278),
        new Entry(37,16,37,53,"Même amorce, bifurcation","37:16","37:53","","al-Abbad · 279","https://quranpedia.app/books/b10901/s14.html",new int[]{7},new int[]{7},446,448,279),
        new Entry(39,63,42,12,"Même amorce, bifurcation","39:63","42:12","","al-Abbad · 280","https://quranpedia.app/books/b10901/s14.html",new int[]{5},new int[]{5},465,484,280),
        new Entry(41,30,46,13,"Même amorce, bifurcation","41:30","46:13","","al-Abbad · 281","https://quranpedia.app/books/b10901/s14.html",new int[]{8},new int[]{8},480,503,281),
        new Entry(47,32,47,34,"Même amorce, bifurcation","47:32","47:34","","al-Abbad · 282","https://quranpedia.app/books/b10901/s14.html",new int[]{8},new int[]{8},510,510,282),
        new Entry(48,4,48,7,"Même amorce, bifurcation","48:4","48:7","","al-Abbad · 283","https://quranpedia.app/books/b10901/s14.html",new int[]{18},new int[]{7},511,511,283),
        new Entry(53,23,53,28,"Même amorce, bifurcation","53:23","53:28","","al-Abbad · 284","https://quranpedia.app/books/b10901/s14.html",new int[]{18},new int[]{10},526,527,284),
        new Entry(57,2,57,5,"Même amorce, bifurcation","57:2","57:5","","al-Abbad · 285","https://quranpedia.app/books/b10901/s14.html",new int[]{5},new int[]{5},537,538,285),
        new Entry(57,22,64,11,"Même amorce, bifurcation","57:22","64:11","","al-Abbad · 286","https://quranpedia.app/books/b10901/s14.html",new int[]{5},new int[]{5},540,557,286),
        new Entry(58,5,58,20,"Même amorce, bifurcation","58:5","58:20","","al-Abbad · 287","https://quranpedia.app/books/b10901/s14.html",new int[]{6},new int[]{6},542,544,287),
        new Entry(58,6,58,18,"Même amorce, bifurcation","58:6","58:18","","al-Abbad · 288","https://quranpedia.app/books/b10901/s14.html",new int[]{5},new int[]{5},542,544,288),
        new Entry(58,16,63,2,"Même amorce, bifurcation","58:16","63:2","","al-Abbad · 289","https://quranpedia.app/books/b10901/s14.html",new int[]{8},new int[]{8},544,554,289),
        new Entry(65,2,65,4,"Même amorce, bifurcation","65:2","65:4","","al-Abbad · 290","https://quranpedia.app/books/b10901/s14.html",new int[]{30},new int[]{26},558,558,290),
        new Entry(65,4,65,5,"Même amorce, bifurcation","65:4","65:5","","al-Abbad · 291","https://quranpedia.app/books/b10901/s14.html",new int[]{24},new int[]{9},558,558,291),
        new Entry(2,63,2,93,"Même amorce, bifurcation","2:63","2:93","","Ajout explicite de l’utilisateur · 292","",new int[]{11},new int[]{11},10,14,292),
        new Entry(2,58,7,161,"Variation de mot ou de particule","2:58","7:161","","al-Abbad · 293","https://quranpedia.app/books/b10901/s1.html",new int[]{11,14},new int[]{11,13},9,171,293),
        new Entry(30,37,39,52,"Variation de mot ou de particule","30:37","39:52","","al-Abbad · 294","https://quranpedia.app/books/b10901/s11.html",new int[]{2},new int[]{2},408,464,294)
    ));
    /** Legacy internal name retained for existing callers; contains the complete approved catalogue. */
    static final List<Entry> PILOT=CATALOGUE;
    // END GENERATED APPROVED CATALOGUE

    // Build once, preserve catalogue order and retain every comparison for shared verses.
    private static final Map<String,List<Integer>> BY_VERSE;
    private static final Map<Integer,List<Integer>> BY_PAGE;
    private static final Map<Integer,List<Integer>> BY_SURAH;
    private static final Map<Integer,List<VerseRef>> VERSES_BY_PAGE;
    static {
        Map<String,List<Integer>> verses=new LinkedHashMap<>();
        Map<Integer,List<Integer>> pages=new LinkedHashMap<>();
        Map<Integer,List<Integer>> surahs=new LinkedHashMap<>();
        Map<Integer,List<VerseRef>> pageVerses=new LinkedHashMap<>();
        for(int i=0;i<PILOT.size();i++) {
            Entry e=PILOT.get(i);
            add(verses,e.first.toString(),i);add(verses,e.second.toString(),i);
            add(pages,e.firstPage,i);add(pages,e.secondPage,i);
            add(surahs,e.first.getSurah(),i);add(surahs,e.second.getSurah(),i);
            add(pageVerses,e.firstPage,e.first);add(pageVerses,e.secondPage,e.second);
        }
        BY_VERSE=freeze(verses);BY_PAGE=freeze(pages);BY_SURAH=freeze(surahs);
        VERSES_BY_PAGE=freeze(pageVerses);
    }
    private static <K,V> void add(Map<K,List<V>> map,K key,V value) {
        List<V> values=map.get(key);
        if(values==null){values=new ArrayList<>();map.put(key,values);}
        if(!values.contains(value))values.add(value);
    }
    private static <K,V> Map<K,List<V>> freeze(Map<K,List<V>> source) {
        Map<K,List<V>> result=new LinkedHashMap<>();
        for(Map.Entry<K,List<V>> e:source.entrySet())
            result.put(e.getKey(),Collections.unmodifiableList(new ArrayList<>(e.getValue())));
        return Collections.unmodifiableMap(result);
    }
    private static <K,V> List<V> lookup(Map<K,List<V>> map,K key) {
        List<V> result=map.get(key);return result==null?Collections.emptyList():result;
    }
    static List<Integer> indicesForVerse(int surah,int ayah) {
        return lookup(BY_VERSE,surah+":"+ayah);
    }
    static List<Integer> indicesForPage(int page) {return lookup(BY_PAGE,page);}
    static List<Integer> indicesForSurah(int surah) {return lookup(BY_SURAH,surah);}
    static List<VerseRef> versesOnPage(int page) {return lookup(VERSES_BY_PAGE,page);}
    static int indexFor(int surah,int ayah) {
        List<Integer> indices=indicesForVerse(surah,ayah);
        return indices.isEmpty()?-1:indices.get(0);
    }
    static Entry forVerse(int surah,int ayah) {
        int index=indexFor(surah,ayah);return index<0?null:PILOT.get(index);
    }
    static int preferredIndex(int surah,int ayah) {
        int index=indexFor(surah,ayah);return index<0?0:index;
    }
}
