package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class QulSimilarityPolicyTest {
    private static VerseRef v(int s, int a) { return new VerseRef(s,a); }
    private static QulSimilarityPolicy.Candidate c(int s, int a, int score, int words,
            int coverage, QulSimilarityPolicy.Origin origin) {
        return new QulSimilarityPolicy.Candidate(v(s,a),origin,score,words,coverage);
    }
    @Test public void keepsAtMostThreeStrongReferencesWithoutSelfMatchOrDuplicates() {
        VerseRef src=v(30,37);
        List<QulSimilarityPolicy.Candidate> result=QulSimilarityPolicy.shortlist(src,Arrays.asList(
            c(30,37,100,20,100,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(39,52,94,15,94,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(39,52,77,10,60,QulSimilarityPolicy.Origin.MUTASHABIHAT),
            c(2,22,90,8,85,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(30,38,80,8,80,QulSimilarityPolicy.Origin.MUTASHABIHAT),
            c(3,5,75,4,70,QulSimilarityPolicy.Origin.SIMILAR_AYAH)));
        assertEquals(3,result.size());
        assertEquals(v(39,52),result.get(0).verse);
        assertEquals(v(2,22),result.get(1).verse);
        assertEquals(v(30,38),result.get(2).verse);
    }
    @Test public void excludesGenericPhrasesAndInvalidPercentages() {
        assertTrue(QulSimilarityPolicy.shortlist(v(30,37),Arrays.asList(
            c(2,1,95,2,80,QulSimilarityPolicy.Origin.MUTASHABIHAT),
            c(2,2,96,8,20,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(2,3,50,10,70,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(2,4,101,10,90,QulSimilarityPolicy.Origin.SIMILAR_AYAH),
            c(2,5,90,5,101,QulSimilarityPolicy.Origin.SIMILAR_AYAH))).isEmpty());
    }
    @Test public void sameSurahIsAllowedAndNeverRequiresInternet() {
        List<QulSimilarityPolicy.Candidate> got=QulSimilarityPolicy.shortlist(v(55,13),
            Arrays.asList(c(55,16,95,11,90,QulSimilarityPolicy.Origin.MUTASHABIHAT)));
        assertEquals(1,got.size());
        assertEquals(v(55,16),got.get(0).verse);
        assertTrue(QulSimilarityPolicy.shortlist(v(55,13),null).isEmpty());
    }
}
