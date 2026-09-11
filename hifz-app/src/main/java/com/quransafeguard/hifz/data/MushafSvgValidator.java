package com.quransafeguard.hifz.data;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Small, deterministic validation gate before any SVG reaches the renderer. */
public final class MushafSvgValidator {
    public static final int MAX_UNCOMPRESSED_BYTES = 8 * 1024 * 1024;

    public String validate(byte[] utf8) {
        if (utf8 == null || utf8.length == 0) {
            throw new IllegalArgumentException("Empty Mushaf SVG");
        }
        if (utf8.length > MAX_UNCOMPRESSED_BYTES) {
            throw new IllegalArgumentException("Mushaf SVG exceeds size limit");
        }

        final String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(utf8))
                .toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IllegalArgumentException("Mushaf SVG is not valid UTF-8", invalidUtf8);
        }

        String normalized = text.startsWith("\uFEFF") ? text.substring(1) : text;
        String lower = normalized.toLowerCase(Locale.ROOT);
        int svg = lower.indexOf("<svg");
        if (svg < 0 || svg > 1024) {
            throw new IllegalArgumentException("Missing SVG root");
        }
        if (!lower.contains("xmlns=\"http://www.w3.org/2000/svg\"") &&
            !lower.contains("xmlns='http://www.w3.org/2000/svg'")) {
            throw new IllegalArgumentException("Unexpected SVG namespace");
        }
        if (!lower.contains("viewbox=")) {
            throw new IllegalArgumentException("Mushaf SVG requires a viewBox");
        }

        reject(lower, "<!doctype", "DOCTYPE is forbidden");
        reject(lower, "<!entity", "ENTITY is forbidden");
        reject(lower, "<script", "script is forbidden");
        reject(lower, "<foreignobject", "foreignObject is forbidden");
        reject(lower, "<iframe", "iframe is forbidden");
        reject(lower, "<object", "object is forbidden");
        reject(lower, "<embed", "embed is forbidden");
        reject(lower, "href=\"http:", "external href is forbidden");
        reject(lower, "href='http:", "external href is forbidden");
        reject(lower, "href=\"https:", "external href is forbidden");
        reject(lower, "href='https:", "external href is forbidden");
        reject(lower, "href=\"file:", "file href is forbidden");
        reject(lower, "href='file:", "file href is forbidden");
        reject(lower, "href=\"content:", "content href is forbidden");
        reject(lower, "href='content:", "content href is forbidden");
        reject(lower, "url(http:", "external CSS url is forbidden");
        reject(lower, "url(https:", "external CSS url is forbidden");

        return normalized;
    }

    private static void reject(String value, String needle, String message) {
        if (value.contains(needle)) throw new IllegalArgumentException(message);
    }
}
