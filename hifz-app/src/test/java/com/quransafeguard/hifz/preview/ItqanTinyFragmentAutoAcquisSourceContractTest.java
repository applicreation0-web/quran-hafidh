package com.quransafeguard.hifz.preview;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** 06/11: fragments must follow normal repetitions; old auto-acquis policy is superseded. */
public final class ItqanTinyFragmentAutoAcquisSourceContractTest {
    private String read(String file)throws Exception {Path p=Paths.get(file);if(!Files.exists(p))p=Paths.get("..",file);return Files.readString(p);}
    @Test public void renderingNoLongerOffersProtocolBypass()throws Exception {
        String s=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(s.contains("creditTinyItqanBlockAndChain"));assertFalse(s.contains("setPositiveButton(\"Valider en Acquis\""));
    }
    @Test public void oldFragmentEndpointCannotGrantAcquiredCredit()throws Exception {
        String s=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        int a=s.indexOf("boolean completeItqanTinyBlockV6(");int b=s.indexOf("\n    }",a);
        String method=s.substring(a,b);assertTrue(method.contains("return false"));assertFalse(method.contains("acquired.add("));
    }
}
