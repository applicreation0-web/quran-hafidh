package com.quransafeguard.hifz.preview;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** The 06/11 mandate supersedes the old auto-acquis shortcut. Behavioral proof lives in MorningMutationGate11711Test. */
public final class SabqiTinyRemnantSourceContractTest {
    private String read(String file)throws Exception {
        Path p=Paths.get(file);if(!Files.exists(p))p=Paths.get("..",file);return Files.readString(p);
    }
    @Test public void tinyRemnantUsesTheBoundedNormalLesson()throws Exception {
        String s=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(s.contains("sabqiBlock = geometry.fiveLineBlock(cursor,endLimit)"));
        assertFalse(s.contains("creditTinySabqiRemnant"));
    }
    @Test public void compatibilityRemnantEndpointDelegatesToVerifiedProtocol()throws Exception {
        String s=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        int a=s.indexOf("boolean completeSabqiTinyBlockV6(");int b=s.indexOf("public int itqanRep()",a);
        String method=s.substring(a,b);assertTrue(method.contains("return completeSabqiBlockV6("));assertFalse(method.contains("acquired.add("));
    }
}
