package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 100% paper erase leaves no dot/haraka residue and never invades a non-erased line. */
public final class FullEraseNoResidueSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void seamsCloseOnlyBetweenTwoErasedLinesAndErasedWordsKeepExactExtents() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue(reader.contains("masked.has(String(prev.id))&&touching(prev,line))"));
        assertTrue(reader.contains("masked.has(String(next.id))&&touching(line,next))"));
        assertTrue("only 100% segments are sealed", reader.contains("if(!seg.fullErase)return;"));
        assertTrue("erased words also covered by their own exact quran-ws box",
            reader.contains("segments.push(...fullEraseWordSegments(segments,lines,svg));")
                && reader.contains("if(!full.some(s=>s.lineId===lineId&&cx>=s.x&&cx<=s.x+s.width))return;"));
        assertTrue("anchor holes keep the word's whole line band so its harakat are never shaved",
            reader.contains("const band=bands.find(l=>cy>=Number(l.top)&&cy<=Number(l.bottom));"));
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue(mushaf.contains("WordGeometryRepository.shared(getContext()).pageBoxes(page)"));
    }
}
