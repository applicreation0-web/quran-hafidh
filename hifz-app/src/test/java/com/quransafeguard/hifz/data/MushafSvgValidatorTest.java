package com.quransafeguard.hifz.data;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class MushafSvgValidatorTest {
    private final MushafSvgValidator validator = new MushafSvgValidator();

    @Test
    public void acceptsLocalSvgWithViewBox() {
        String svg = "<?xml version='1.0'?><svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 20\"><path d=\"M0 0\"/></svg>";
        assertTrue(validator.validate(svg.getBytes(StandardCharsets.UTF_8)).contains("<svg"));
    }

    @Test
    public void rejectsScriptAndExternalHref() {
        assertRejected("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><script/></svg>");
        assertRejected("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1 1\"><image href=\"https://example.test/a\"/></svg>");
    }

    @Test
    public void rejectsMissingViewBox() {
        assertRejected("<svg xmlns=\"http://www.w3.org/2000/svg\"></svg>");
    }

    private void assertRejected(String svg) {
        try {
            validator.validate(svg.getBytes(StandardCharsets.UTF_8));
            fail("Expected SVG rejection");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
