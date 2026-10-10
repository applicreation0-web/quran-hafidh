package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** Real Mushaf geometry, including the Qaf / Adh-Dhariyat physical page junction. */
public final class ItqanCompletionBoundaryPolicyTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadRealMushaf() throws Exception {
        Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
        geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
    }

    @Test public void qafToDhariyat520To521NeverBorrowsAcrossSurahOrPage() throws Exception {
        int accepted=0, rejected=0;
        for(int page:new int[]{518,519,520,521,522}) {
            List<String> ids=geometry.lineIdsOnPage(page);
            assertFalse(ids.isEmpty());
            List<String> following=page<604?geometry.lineIdsOnPage(page+1):new ArrayList<>();
            for(int breakAt=1;breakAt<ids.size();breakAt++) {
                int ownBegin=Math.max(0,breakAt-8);
                List<String> base=new ArrayList<>(ids.subList(ownBegin,breakAt));
                ArrayList<String> next=new ArrayList<>(ids.subList(breakAt,ids.size()));
                for(int i=0;i<Math.min(3,following.size());i++)next.add(following.get(i));
                if(next.size()<2)continue;
                ItqanPageCompletionPolicy.Offer offer=
                    ItqanCompletionBoundaryPolicy.offer(geometry,base,next);
                if(offer.choice==ItqanPageCompletionPolicy.Choice.NONE) {
                    assertEquals(base,offer.actualLineIds);
                    rejected++;
                    continue;
                }
                accepted++;
                assertTrue(offer.bonusLineIds.size()==1 || offer.bonusLineIds.size()==2);
                assertEquals(base.size()+offer.bonusLineIds.size(),offer.actualLineIds.size());
                assertEquals(next.subList(0,offer.bonusLineIds.size()),offer.bonusLineIds);
                Set<String> unique=new HashSet<>(offer.actualLineIds);
                assertEquals("No double ownership of lines",offer.actualLineIds.size(),unique.size());
                List<GeometryRepository.LineMeta> lines=geometry.linesForExactIds(offer.actualLineIds);
                GeometryRepository.LineMeta finalLine=lines.get(lines.size()-1);
                assertEquals(page,finalLine.page);
                for(int i=1;i<lines.size();i++)
                    assertEquals("Only physically adjacent Mushaf lines",
                        lines.get(i-1).globalIndex+1,lines.get(i).globalIndex);
                int surah=lines.get(0).verses.get(0).getSurah();
                for(GeometryRepository.LineMeta line:lines)
                    for(VerseRef verse:line.verses)assertEquals("Never mix surahs",surah,verse.getSurah());
                boolean closesPage=finalLine.id.equals(ids.get(ids.size()-1));
                boolean closesSurah=finalLine.globalIndex+1==geometry.lineCount();
                if(!closesSurah) {
                    GeometryRepository.LineMeta after=geometry.line(finalLine.globalIndex+1);
                    closesSurah=after.verses.isEmpty() || after.verses.get(0).getSurah()!=surah;
                }
                assertTrue("Offer must really finish page or surah",closesPage||closesSurah);

                ItqanPlanSnapshot saved=ItqanPlanSnapshot.undecided(new VerseRef(50,1),
                    new VerseRef(51,60),0).extend(offer.bonusLineIds);
                assertEquals(offer.bonusLineIds,
                    ItqanPlanSnapshot.fromJson(saved.toJson()).bonusLineIds);
                ArrayList<String> remainder=new ArrayList<>(next);
                remainder.removeAll(saved.bonusLineIds);
                assertFalse("Must leave a future portion",remainder.isEmpty());
                Set<String> combined=new HashSet<>(offer.actualLineIds);
                combined.addAll(remainder);
                assertEquals("No loss of borrowed lines",base.size()+next.size(),combined.size());
            }
        }
        assertTrue("Real Qaf/Dhariyat regression needs valid boundary offers",accepted>0);
        assertTrue("Real Qaf/Dhariyat regression needs rejected non-boundary offers",rejected>0);
    }

    @Test public void physicalPageChangeWithoutBonusIsNotAnOffer() {
        for(int page:new int[]{519,520}) {
            List<String> current=geometry.lineIdsOnPage(page);
            List<String> following=geometry.lineIdsOnPage(page+1);
            int n=current.size();
            List<String> base=new ArrayList<>(current.subList(Math.max(0,n-7),n));
            List<String> next=new ArrayList<>(following.subList(0,Math.min(5,following.size())));
            ItqanPageCompletionPolicy.Offer offer=
                ItqanCompletionBoundaryPolicy.offer(geometry,base,next);
            assertEquals(ItqanPageCompletionPolicy.Choice.NONE,offer.choice);
        }
    }
}
