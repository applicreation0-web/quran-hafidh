package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only index over the frozen V2.1 semantic corpus.
 *
 * The semantic JSON is never repaired or normalized at runtime. Any schema/hash/completeness
 * mismatch disables this optional feature while the rest of Quran Haafidh keeps its prior behavior.
 * Exact visual geometry is deliberately NOT inferred from reader109 line cells: those cells are
 * source-ink groups, not linguistic words.
 */
final class SemanticPassageRepository {
    static final String ASSET_PATH = "semantic/semantic_passages_v2_1.json";
    static final String EXPECTED_SHA256 =
        "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7";
    static final String TITLE_ASSET_PATH = "semantic/semantic_titles_v2_3.json";
    static final String EXPECTED_TITLE_SHA256 =
        "46c8c905beaf2e03b2589c296d1574e7417dbab59f9e580911e9ebe0c8073bea";
    static final String EXPECTED_V23_CORPUS_SHA256 =
        "19b7a5048ef2201d2a8967652c0f533c4fc46bd1aa66d7b31f12daf196857336";
    static final int EXPECTED_GLOBAL_PASSAGES = 1256;
    static final int EXPECTED_CANONICAL_MUNIR_PASSAGES = 1243;
    static final int EXPECTED_PAGE_RECORDS = 1644;
    private static final Pattern MUNIR_RANGE = Pattern.compile(
        "^(\\d+):(\\d+)[–-](\\d+)(?:\\s+(.*))?$");

    private static final Map<String, String> VERIFIED_MUNIR_MISSING_TITLES =
        buildVerifiedMunirMissingTitles();
    private static Map<String, String> buildVerifiedMunirMissingTitles() {
        Map<String, String> out = new HashMap<>();
        out.put("2:1–2:5", "صفات المؤمنين وجزاء المتقين");
        out.put("2:14–2:16", "صفات المنافقين- ٣-");
        out.put("2:221–2:221", "زواج المسلم بالمشركة");
        out.put("2:256–2:257", "منع الإكراه على الدين والله هو الهادي إلى الإيمان");
        out.put("2:282–2:283", "آية الدين وآية الرهن توثيق الدين المؤجل بالكتابة أو الشهادة أو الرهن");
        out.put("3:42–3:44", "قصة مريم");
        out.put("3:79–3:80", "افتراء أهل الكتاب على الأنبياء");
        out.put("3:86–3:91", "أنواع الكفار من حيث التوبة");
        out.put("4:15–4:16", "جزاء الفاحشة في مبدأ التشريع");
        out.put("4:25–4:25", "شروط الزواج بالأمة وعقوبة فاحشتها");
        out.put("5:1–5:2", "الوفاء بالعقود ومنع الاعتداء والتعاون على الخير وتعظيم شعائر الله");
        out.put("6:12–6:16", "أدلة أخرى لإثبات الوحدانية والبعث");
        out.put("6:38–6:39", "كمال علم الله وتمام قدرته وعدم التفريط بشيء في القرآن");
        out.put("6:50–6:53", "انحصار مصدر علم النّبي صلّى الله عليه وسلّم بالوحي ومهمته في الإنذار وطرد الضعفاء");
        out.put("6:136–6:140", "شريعة الجاهلية في الزروع والثمار والأنعام وقتل الأولاد");
        out.put("6:158–6:158", "إنذار أخير للكفار بسوء العذاب");
        out.put("7:155–7:155", "اختيار موسى سبعين رجلا لميقات الكلام والرؤية ومناجاته ربه");
        out.put("7:163–7:166", "حيلة اليهود على صيد الأسماك يوم السبت وعقاب المخالفين");
        out.put("8:30–8:31", "ألوان الكيد والمؤامرة من المشركين على النبي صلّى الله عليه وآله وسلم");
        out.put("9:11–9:12", "مصير المشركين إما التوبة وإما القتال");
        out.put("9:90–9:90", "نفاق الأعراب واستئذانهم للتخلف عن الجهاد");
        out.put("10:20–10:20", "طلب المشركين إنزال آية كونية");
        out.put("12:54–12:57", "الفصل التّاسع من قصّة يوسف يوسف في رئاسة الحكم ووزارة الماليّة");
        out.put("12:58–12:62", "الفصل العاشر من قصة يوسف أولاد يعقوب يشترون القمح من أخيهم يوسف ومطالبته إياهم بإحضار أخيهم");
        out.put("14:21–14:23", "الحوار بين الأشقياء يوم العذاب والمناظرة بين الشيطان وأتباعه وظفر السعداء بالجنة");
        out.put("14:35–14:41", "دعاء إبراهيم عليه السلام مستقبل البيت الحرام");
        out.put("16:98–16:105", "ما يتعلق بالقرآن الاستعاذة والنسخ وعربية القرآن");
        out.put("16:114–16:119", "الحلال الطيب والحرام الخبيث من المأكولات");
        out.put("19:54–19:55", "قصة إسماعيل عليه السلام");
        out.put("21:87–21:88", "القصة الثامنة- قصة يونس عليه السلام");
        out.put("22:1–22:4", "الأمر بتقوى الله تعالى");
        out.put("23:51–23:56", "مبادئ التشريع في الحياة");
        out.put("23:81–23:90", "إنكار المشركين البعث وإثباته بالأدلة القاطعة");
        out.put("23:101–23:111", "موازين النجاة في حساب الآخرة");
        out.put("25:45–25:54", "أدلة خمسة على وجود الله وتوحيده");
        out.put("26:1–26:9", "تكذيب المشركين بالقرآن وإنذارهم وإثبات وحدانية الله");
        out.put("27:67–27:75", "إنكار المشركين البعث");
        out.put("30:55–30:57", "أحوال البعث ومقارنتها بأحوال الدنيا");
        out.put("36:59–36:68", "جزاء المجرمين");
        out.put("37:1–37:5", "إعلان وحدانية الله");
        out.put("44:1–44:9", "إنزال القرآن في ليلة القدر المباركة وصفات منزله");
        out.put("47:1–47:3", "بيان الفرق بين الكفار والمؤمنين");
        out.put("48:20–48:24", "مغانم وفتوحات ونعم كثيرة أخرى للمؤمنين");
        out.put("50:16–50:22", "تقرير خلق الإنسان وعلم اللَّه بأحواله");
        out.put("52:1–52:16", "وقوع القيامة وإثبات العذاب في اليوم الموعود");
        out.put("54:1–54:8", "انشقاق القمر وموقف المشركين منه");
        out.put("57:1–57:6", "التسبيح لله في جميع الأوقات وأسبابه");
        out.put("61:1–61:4", "الدعوة إلى القتال في سبيل الله صفا واحدا");
        out.put("64:8–64:10", "المطالبة بالإيمان والتحذير من أهوال القيامة");
        out.put("68:1–68:7", "كمال الدين والخلق عند النبي صلّى الله عليه وسلّم");
        out.put("77:1–77:15", "وقوع يوم القيامة حتما ووقته وعلاماته");
        out.put("81:1–81:14", "أحوال القيامة وأهوالها");
        out.put("88:1–88:7", "هول القيامة وأحوال أهل النار");
        out.put("95:1–95:8", "حال النوع الإنساني خلقا وعملا");
        out.put("97:1–97:5", "بدء نزول القرآن وفضائل ليلة القدر");
        out.put("102:1–102:8", "التفاخر في الدنيا والسؤال عن الأعمال");
        out.put("106:1–106:4", "التذكير بنعم اللَّه على قريش");
        out.put("112:1–112:4", "سورة التوحيد والتنزيه للَّه عز وجل");
        return Collections.unmodifiableMap(out);
    }

    static final class CellRange {
        final String lineId;
        final int fromCell;
        final int toCell;

        CellRange(String lineId, int fromCell, int toCell) {
            this.lineId = lineId;
            this.fromCell = fromCell;
            this.toCell = toCell;
        }
    }

    static final class Cue {
        final String passageId;
        final int page;
        final int indexOnPage;
        final String title;
        final String anchorArabic;
        final int anchorWordCount;
        final VerseRef startVerse;
        final VerseRef endVerse;
        final int startPage;
        final int endPage;
        final int startLine;
        final int firstWordId;
        final int lastWordId;
        final int firstWordPosition;
        final int lastWordPosition;
        final boolean anchorOnCurrentPage;
        final List<CellRange> visualRanges;

        Cue(String passageId, int page, int indexOnPage, String title, String anchorArabic,
            int anchorWordCount, VerseRef startVerse, VerseRef endVerse, int startPage, int endPage,
            int startLine, int firstWordId, int lastWordId, int firstWordPosition,
            int lastWordPosition, boolean anchorOnCurrentPage, List<CellRange> visualRanges) {
            this.passageId = passageId;
            this.page = page;
            this.indexOnPage = indexOnPage;
            this.title = title;
            this.anchorArabic = anchorArabic;
            this.anchorWordCount = anchorWordCount;
            this.startVerse = startVerse;
            this.endVerse = endVerse;
            this.startPage = startPage;
            this.endPage = endPage;
            this.startLine = startLine;
            this.firstWordId = firstWordId;
            this.lastWordId = lastWordId;
            this.firstWordPosition = firstWordPosition;
            this.lastWordPosition = lastWordPosition;
            this.anchorOnCurrentPage = anchorOnCurrentPage;
            this.visualRanges = Collections.unmodifiableList(new ArrayList<>(visualRanges));
        }

        boolean hasExactVisualRange() {
            return !visualRanges.isEmpty();
        }
    }

    private static final class PassageMeta {
        final String canonicalId;
        final VerseRef start;
        final VerseRef end;
        final int startPage;
        final int endPage;
        final String titleMunirAr;

        PassageMeta(String canonicalId, VerseRef start, VerseRef end, int startPage, int endPage,
                    String titleMunirAr) {
            this.canonicalId = canonicalId;
            this.start = start;
            this.end = end;
            this.startPage = startPage;
            this.endPage = endPage;
            this.titleMunirAr = titleMunirAr == null ? "" : titleMunirAr;
        }
    }

    private static final class GroupBuilder {
        final String canonicalId;
        final VerseRef start;
        final VerseRef end;
        int startPage = Integer.MAX_VALUE;
        int endPage = 0;
        String titleMunirAr = "";

        GroupBuilder(String canonicalId, VerseRef start, VerseRef end) {
            this.canonicalId = canonicalId;
            this.start = start;
            this.end = end;
        }
    }

    private static final class AnchorData {
        final String anchor;
        final int wordCount;
        final int startLine;
        final int firstWordId;
        final int lastWordId;
        final int firstWordPosition;
        final int lastWordPosition;

        AnchorData(String anchor, int wordCount, int startLine, int firstWordId, int lastWordId,
                   int firstWordPosition, int lastWordPosition) {
            this.anchor = anchor;
            this.wordCount = wordCount;
            this.startLine = startLine;
            this.firstWordId = firstWordId;
            this.lastWordId = lastWordId;
            this.firstWordPosition = firstWordPosition;
            this.lastWordPosition = lastWordPosition;
        }
    }

    private final Map<Integer, List<Cue>> byPage = new HashMap<>();
    private final Map<String, Cue> byId = new HashMap<>();
    private final List<Cue> orderedCues = new ArrayList<>();
    private final boolean available;
    private final WordGeometryRepository wordGeometry;

    private static volatile SemanticPassageRepository shared;

    /** One verified, parsed corpus per process: the ~7 MB parse + SHA-256 runs once, not on
     *  every Lecture/session opening. */
    static SemanticPassageRepository shared(Context context) {
        SemanticPassageRepository local = shared;
        if (local != null) return local;
        synchronized (SemanticPassageRepository.class) {
            if (shared == null) shared = new SemanticPassageRepository(context.getApplicationContext());
            return shared;
        }
    }

    /** Warm the shared corpus off the UI thread (called from the home screen). */
    static void preloadAsync(Context context) {
        Context app = context.getApplicationContext();
        Thread loader = new Thread(() -> shared(app), "al-munir-preload");
        loader.setDaemon(true);
        loader.setPriority(Thread.MIN_PRIORITY);
        loader.start();
    }

    private SemanticPassageRepository(Context context) {
        wordGeometry = new WordGeometryRepository(context);
        boolean loaded = false;
        try {
            byte[] raw = readAsset(context, ASSET_PATH);
            if (!EXPECTED_SHA256.equals(sha256(raw))) {
                throw new IllegalStateException("semantic V2.1 SHA-256 mismatch");
            }
            byte[] titleRaw = readAsset(context, TITLE_ASSET_PATH);
            if (!EXPECTED_TITLE_SHA256.equals(sha256(titleRaw))) {
                throw new IllegalStateException("semantic V2.3 title SHA-256 mismatch");
            }
            Map<String, String> titlesV23 =
                parseTitleOverlay(new String(titleRaw, StandardCharsets.UTF_8));
            parseInto(new String(raw, StandardCharsets.UTF_8), titlesV23, byPage, byId);
            Map<String, Cue> canonical = new HashMap<>();
            for (Cue cue : byId.values()) canonical.put(cue.passageId, cue);
            orderedCues.addAll(canonical.values());
            orderedCues.sort((a, b) -> a.startVerse.compareTo(b.startVerse));
            loaded = byPage.size() == 604 && byId.size() == EXPECTED_GLOBAL_PASSAGES
                && orderedCues.size() == EXPECTED_CANONICAL_MUNIR_PASSAGES;
        } catch (Throwable unavailableAsset) {
            // Fail closed: semantic cues are an optional enhancement, never a reader dependency.
            byPage.clear();
            byId.clear();
            orderedCues.clear();
        }
        available = loaded;
    }

    /** JVM tests: the exact runtime parse of the materialized assets, without Android. */
    static final class ParsedForTest {
        final Map<Integer, List<Cue>> byPage = new HashMap<>();
        final Map<String, Cue> byId = new HashMap<>();
    }

    static ParsedForTest parseForTest(String raw, String titleRaw) throws Exception {
        ParsedForTest out = new ParsedForTest();
        parseInto(raw, parseTitleOverlay(titleRaw), out.byPage, out.byId);
        return out;
    }

    /** Start verse of every canonical Al-Munīr unit (empty when the corpus failed closed). */
    Set<VerseRef> canonicalStarts() {
        HashSet<VerseRef> out = new HashSet<>();
        for (Cue cue : orderedCues) out.add(cue.startVerse);
        return Collections.unmodifiableSet(out);
    }

    boolean isAvailable() {
        return available;
    }

    List<Cue> cuesForPage(int page) {
        List<Cue> cues = byPage.get(page);
        return cues == null ? Collections.emptyList() : cues;
    }

    Cue cue(String passageId) {
        return passageId == null ? null : byId.get(passageId);
    }

    /** Passage containing a verse, using only the frozen audited V2.1 verse boundaries. */
    Cue cueContaining(VerseRef verse) {
        if (verse == null) return null;
        for (Cue cue : orderedCues) {
            if (verse.compareTo(cue.startVerse) < 0) return null;
            if (verse.compareTo(cue.endVerse) <= 0) return cue;
        }
        return null;
    }

    /** First complete semantic passage usable from this acquired-corpus cursor, with canonical wrap. */
    Cue firstEligibleCueAtOrContaining(VerseRef cursor, EligibleCorpus corpus) {
        if (cursor == null || corpus == null || orderedCues.isEmpty()) return null;
        Cue containing = cueContaining(cursor);
        if (fullyEligible(containing, corpus)) return containing;

        int start = 0;
        while (start < orderedCues.size()
                && orderedCues.get(start).startVerse.compareTo(cursor) < 0) start++;
        for (int offset = 0; offset < orderedCues.size(); offset++) {
            Cue candidate = orderedCues.get((start + offset) % orderedCues.size());
            if (fullyEligible(candidate, corpus)) return candidate;
        }
        return null;
    }

    /** Next complete acquired passage after the current semantic unit, wrapping canonically. */
    Cue nextEligibleCue(Cue current, EligibleCorpus corpus) {
        if (current == null || corpus == null || orderedCues.isEmpty()) return null;
        int index = -1;
        for (int i = 0; i < orderedCues.size(); i++) {
            if (orderedCues.get(i).passageId.equals(current.passageId)) {
                index = i;
                break;
            }
        }
        if (index < 0) return firstEligibleCueAtOrContaining(current.endVerse, corpus);
        for (int offset = 1; offset <= orderedCues.size(); offset++) {
            Cue candidate = orderedCues.get((index + offset) % orderedCues.size());
            if (fullyEligible(candidate, corpus)) return candidate;
        }
        return null;
    }

    private static boolean fullyEligible(Cue cue, EligibleCorpus corpus) {
        return cue != null && corpus.contains(cue.startVerse) && corpus.contains(cue.endVerse);
    }

    /**
     * Exact recall is enabled only when the page's six geographic landmark words and every
     * Al-Munir amorce beginning on the page have exact word boxes in the pinned QCF sidecar.
     * No line-cell arithmetic or proportional approximation is accepted.
     */
    boolean hasCompleteExactGeometryForPage(int page) {
        if (wordGeometry.pageLandmarkBoxes(page).length() != 6) return false;
        for (Cue cue : cuesForPage(page)) {
            if (!cue.anchorOnCurrentPage) continue;
            if (wordGeometry.anchorBoxes(page, cue.startVerse, cue.anchorWordCount).length()
                    != cue.anchorWordCount) return false;
        }
        return true;
    }

    JSONArray pageLandmarkBoxes(int page) {
        return wordGeometry.pageLandmarkBoxes(page);
    }

    JSONArray readerCuesForPage(int page) {
        try {
            JSONArray out = new JSONArray();
            for (Cue cue : cuesForPage(page)) {
                if (!cue.anchorOnCurrentPage) continue;
                JSONObject item = new JSONObject();
                item.put("id", cue.passageId);
                item.put("index", cue.indexOnPage);
                // Public semantic title payload is canonical Al-Munir Arabic only.
                item.put("title", cue.title);
                item.put("titleMunirAr", cue.title);
                item.put("anchor", cue.anchorArabic);
                item.put("anchorWordCount", cue.anchorWordCount);
                item.put("startLine", cue.startLine);

                // Exact viewBox-space boxes come from the pinned quran-ws word sidecar.
                // An empty array is an explicit fail-closed state; the reader never estimates.
                item.put("boxes", wordGeometry.anchorBoxes(
                    page, cue.startVerse, cue.anchorWordCount));

                JSONArray ranges = new JSONArray();
                for (CellRange range : cue.visualRanges) {
                    ranges.put(new JSONObject()
                        .put("lineId", range.lineId)
                        .put("fromCell", range.fromCell)
                        .put("toCell", range.toCell));
                }
                item.put("ranges", ranges);
                out.put(item);
            }
            return out;
        } catch (JSONException invalidReaderPayload) {
            throw new IllegalStateException("semantic cue serialization failed", invalidReaderPayload);
        }
    }

    private static void parseInto(String raw, Map<String, String> titlesV23,
            Map<Integer, List<Cue>> byPage, Map<String, Cue> byId) throws JSONException {
        JSONObject root = new JSONObject(raw);
        require("V2.1".equals(root.optString("schema_version", "")), "wrong semantic schema");
        require(!root.optBoolean("boundaries_changed", true), "semantic boundaries changed");
        require(root.optInt("global_passage_count", 0) == EXPECTED_GLOBAL_PASSAGES,
            "wrong global passage count");

        JSONArray globals = root.optJSONArray("global_passages");
        JSONArray records = root.optJSONArray("page_passage_records");
        require(globals != null && globals.length() == EXPECTED_GLOBAL_PASSAGES,
            "global_passages incomplete");
        require(records != null && records.length() == EXPECTED_PAGE_RECORDS,
            "page_passage_records incomplete");

        Set<String> globalIds = new HashSet<>();
        Map<String, String> legacyToGroupKey = new HashMap<>();
        Map<String, GroupBuilder> groups = new HashMap<>();

        for (int i = 0; i < globals.length(); i++) {
            JSONObject row = globals.getJSONObject(i);
            String id = requiredText(row, "passage_global_id");
            require(globalIds.add(id), "duplicate global passage id " + id);
            requiredAuditedFields(row);

            VerseRef legacyStart = new VerseRef(
                positiveInt(row, "surah_start"), positiveInt(row, "ayah_start"));
            VerseRef legacyEnd = new VerseRef(
                positiveInt(row, "surah_end"), positiveInt(row, "ayah_end"));
            require(legacyStart.compareTo(legacyEnd) <= 0, "invalid global verse span for " + id);
            int legacyStartPage = positiveInt(row, "starts_on_page");
            int legacyEndPage = positiveInt(row, "ends_on_page");
            require(legacyStartPage <= legacyEndPage && legacyEndPage <= 604,
                "invalid global page span for " + id);

            // Keep V2.3 as an integrity-checked historical asset, but do not use its editorial
            // wording as the runtime semantic title. Runtime structure and title come from the
            // explicit Al-Munir grouping only.
            String historicalTitle = titlesV23.get(id);
            require(historicalTitle != null && !historicalTitle.trim().isEmpty(),
                "missing audited V2.3 title for " + id);

            String grouping = requiredText(row, "tafsir_munir_grouping");
            VerseRef canonicalStart;
            VerseRef canonicalEnd;
            String titleAr;

            // Direct Al-Munir control: An-Nas is one explicit unit, 114:1-6. The older audit
            // metadata split its final verses and is therefore overridden rather than propagated.
            if ("SP1255".equals(id) || "SP1256".equals(id)) {
                canonicalStart = new VerseRef(114, 1);
                canonicalEnd = new VerseRef(114, 6);
                titleAr = "الاستعاذة من شرّ الشياطين";
            } else {
                Matcher m = MUNIR_RANGE.matcher(grouping);
                require(m.matches(), "unparseable Al-Munir grouping for " + id + ": " + grouping);
                int surah = Integer.parseInt(m.group(1));
                canonicalStart = new VerseRef(surah, Integer.parseInt(m.group(2)));
                canonicalEnd = new VerseRef(surah, Integer.parseInt(m.group(3)));
                titleAr = m.group(4) == null ? "" : m.group(4).trim();
                if (titleAr.isEmpty()) {
                    String exactKey = canonicalStart.toString() + "–" + canonicalEnd.toString();
                    titleAr = VERIFIED_MUNIR_MISSING_TITLES.getOrDefault(exactKey, "");
                }
                // Al-Munir prints a surah-level heading for Al-Fatiha before the 1:1-7 block;
                // older semantic metadata retained the exact range but omitted that heading.
                if (canonicalStart.equals(new VerseRef(1, 1))
                        && canonicalEnd.equals(new VerseRef(1, 7))
                        && titleAr.isEmpty()) {
                    titleAr = "سورة الفاتحة مكية وآياتها سبع نزلت بعد المدّثّر";
                }
            }

            require(canonicalStart.compareTo(legacyStart) <= 0
                    && canonicalEnd.compareTo(legacyEnd) >= 0,
                "legacy passage is outside Al-Munir unit for " + id);

            String key = canonicalStart.toString() + "–" + canonicalEnd.toString();
            GroupBuilder group = groups.get(key);
            if (group == null) {
                group = new GroupBuilder(id, canonicalStart, canonicalEnd);
                groups.put(key, group);
            }
            group.startPage = Math.min(group.startPage, legacyStartPage);
            group.endPage = Math.max(group.endPage, legacyEndPage);
            if (!titleAr.isEmpty()) {
                require(group.titleMunirAr.isEmpty() || group.titleMunirAr.equals(titleAr),
                    "conflicting Al-Munir titles inside " + key);
                group.titleMunirAr = titleAr;
            }
            // One explicit Al-Munir heading missing from the old metadata is known and directly
            // source-verified; keep the exact Arabic heading, not a synthesized French title.
            if (canonicalStart.equals(new VerseRef(24, 11))
                    && canonicalEnd.equals(new VerseRef(24, 22))) {
                group.titleMunirAr = "الحكم الخامس قصة الإفك";
            }
            legacyToGroupKey.put(id, key);
        }

        require(titlesV23.keySet().equals(globalIds),
            "V2.3 title IDs do not exactly match frozen V2.1 passages");
        require(groups.size() == EXPECTED_CANONICAL_MUNIR_PASSAGES,
            "unexpected Al-Munir canonical passage count: " + groups.size());
        Map<String, PassageMeta> globalMeta = new HashMap<>();
        for (String id : globalIds) {
            GroupBuilder group = groups.get(legacyToGroupKey.get(id));
            require(group != null, "missing Al-Munir group for " + id);
            globalMeta.put(id, new PassageMeta(group.canonicalId, group.start, group.end,
                group.startPage, group.endPage, group.titleMunirAr));
        }

        Set<Integer> pages = new HashSet<>();
        Set<String> recordIds = new HashSet<>();
        Map<String, AnchorData> anchors = new HashMap<>();

        // First pass: validate every frozen record and capture only the representative block-start
        // amorce. Artificial V2.1 sub-split anchors are deliberately discarded.
        for (int i = 0; i < records.length(); i++) {
            JSONObject row = records.getJSONObject(i);
            int page = row.optInt("page", 0);
            require(page >= 1 && page <= 604, "page outside Mushaf");
            pages.add(page);

            String id = requiredText(row, "passage_global_id");
            require(globalIds.contains(id), "unknown passage id " + id);
            recordIds.add(id);
            int startLine = row.optInt("start_line", 0);
            int endLine = row.optInt("end_line", 0);
            require(startLine >= 1 && endLine >= startLine, "invalid line span for " + id);
            requiredText(row, "title_fr_v2_1");
            String anchor = requiredText(row, "anchor_arabic_v2_1");
            int anchorWordCount = row.optInt("anchor_word_count_v2_1", 0);
            require(anchorWordCount >= 1, "invalid audited anchor length for " + id);
            require("AUDITED_V2_1".equals(requiredText(row, "minimality_verified_v2_1")),
                "anchor is not audited V2.1 for " + id);

            PassageMeta meta = globalMeta.get(id);
            boolean representative = id.equals(meta.canonicalId);
            boolean anchorOnPage = yes(row.opt("anchor_is_on_current_page"));
            if (representative && anchorOnPage) {
                require(!anchors.containsKey(meta.canonicalId),
                    "multiple start amorces for Al-Munir unit " + meta.canonicalId);
                anchors.put(meta.canonicalId, new AnchorData(
                    anchor, anchorWordCount, startLine,
                    positiveInt(row, "first_word_id"), positiveInt(row, "last_word_id"),
                    positiveInt(row, "first_word_position"), positiveInt(row, "last_word_position")));
            }
        }

        require(pages.size() == 604, "semantic page coverage incomplete");
        require(recordIds.equals(globalIds), "page records do not cover global passages");
        require(anchors.size() == EXPECTED_CANONICAL_MUNIR_PASSAGES,
            "not every Al-Munir unit has exactly one Quranic start amorce");

        Map<String, Cue> canonicalById = new HashMap<>();
        Set<String> emittedPageGroups = new HashSet<>();

        // Second pass: preserve page coverage but collapse all legacy sub-splits into one Al-Munir
        // unit. Only the unit's real first occurrence carries the visible amorce.
        for (int i = 0; i < records.length(); i++) {
            JSONObject row = records.getJSONObject(i);
            int page = row.optInt("page", 0);
            String legacyId = requiredText(row, "passage_global_id");
            PassageMeta meta = globalMeta.get(legacyId);
            AnchorData anchor = anchors.get(meta.canonicalId);
            require(anchor != null, "missing canonical amorce for " + meta.canonicalId);

            String pageGroup = page + "|" + meta.canonicalId;
            if (!emittedPageGroups.add(pageGroup)) continue;

            int index = row.optInt("passage_index_on_page", 0);
            require(index >= 1, "missing passage_index_on_page for " + legacyId);
            boolean anchorOnPage = page == meta.startPage;

            // Exact word-to-cell geometry is joined separately; never infer it from line-cell
            // counts or proportional arithmetic here.
            List<CellRange> ranges = Collections.emptyList();

            require(!meta.titleMunirAr.trim().isEmpty(),
                "missing Arabic Al-Munir heading for canonical unit " + meta.canonicalId);
            Cue cue = new Cue(meta.canonicalId, page, index, meta.titleMunirAr, anchor.anchor,
                anchor.wordCount, meta.start, meta.end, meta.startPage, meta.endPage,
                anchor.startLine, anchor.firstWordId, anchor.lastWordId,
                anchor.firstWordPosition, anchor.lastWordPosition, anchorOnPage, ranges);
            byPage.computeIfAbsent(page, ignored -> new ArrayList<>()).add(cue);
            if (anchorOnPage) canonicalById.put(meta.canonicalId, cue);
        }

        require(canonicalById.size() == EXPECTED_CANONICAL_MUNIR_PASSAGES,
            "canonical Al-Munir cue index incomplete");

        // Keep legacy IDs as read-only aliases so persisted cursors from earlier builds migrate
        // without data loss; every alias points to the same canonical Al-Munir Cue object.
        for (String id : globalIds) {
            PassageMeta meta = globalMeta.get(id);
            Cue cue = canonicalById.get(meta.canonicalId);
            require(cue != null, "missing canonical cue alias target for " + id);
            byId.put(id, cue);
        }

        for (Map.Entry<Integer, List<Cue>> entry : byPage.entrySet()) {
            entry.getValue().sort((a, b) -> Integer.compare(a.indexOnPage, b.indexOnPage));
            entry.setValue(Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
    }


    private static Map<String, String> parseTitleOverlay(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        require("V2.3_TITLES".equals(root.optString("schema_version", "")),
            "wrong V2.3 title schema");
        require(EXPECTED_SHA256.equals(root.optString("source_v2_1_sha256", "")),
            "V2.3 title overlay is not tied to frozen V2.1");
        require(EXPECTED_V23_CORPUS_SHA256.equals(
                root.optString("source_v2_3_corpus_sha256", "")),
            "V2.3 title overlay is not tied to audited final corpus");
        require(root.optInt("title_count", 0) == EXPECTED_GLOBAL_PASSAGES,
            "wrong V2.3 title count");

        JSONArray rows = root.optJSONArray("titles");
        require(rows != null && rows.length() == EXPECTED_GLOBAL_PASSAGES,
            "V2.3 title rows incomplete");
        Map<String, String> titles = new HashMap<>();
        Set<String> uniqueTitles = new HashSet<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject titleRow = rows.getJSONObject(i);
            String id = requiredText(titleRow, "passage_global_id");
            String title = requiredText(titleRow, "title_fr_v2_3");
            String status = requiredText(titleRow, "title_audit_status_v2_3");
            String distinctiveness = requiredText(titleRow, "title_distinctiveness_v2_3");
            require("FULL_WASIT_REVIEWED_CONFIRMED".equals(status)
                    || "FULL_WASIT_REVIEWED_CHANGED".equals(status),
                "unaudited V2.3 title status for " + id);
            require("HIGH".equals(distinctiveness) || "MEDIUM".equals(distinctiveness),
                "unacceptable V2.3 title distinctiveness for " + id);
            require(titles.put(id, title) == null, "duplicate V2.3 title ID " + id);
            require(uniqueTitles.add(title), "duplicate V2.3 title text");
        }
        require(titles.size() == EXPECTED_GLOBAL_PASSAGES, "V2.3 title index incomplete");
        return titles;
    }

    private static void requiredAuditedFields(JSONObject row) {
        requiredText(row, "title_fr_v2_1");
        requiredText(row, "anchor_arabic_v2_1");
        require(row.optInt("anchor_word_count_v2_1", 0) >= 1, "invalid audited anchor length");
        require("AUDITED_V2_1".equals(requiredText(row, "minimality_verified_v2_1")),
            "global anchor is not audited V2.1");
    }

    private static String requiredText(JSONObject row, String key) {
        String value = row.optString(key, "").trim();
        require(!value.isEmpty(), "missing " + key);
        return value;
    }

    private static int positiveInt(JSONObject row, String key) {
        int value = row.optInt(key, 0);
        require(value >= 1, "invalid " + key);
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static boolean yes(Object raw) {
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        String value = raw == null ? "" : raw.toString().trim();
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static byte[] readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return hex.toString();
    }
}
