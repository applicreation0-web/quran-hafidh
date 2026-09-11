package com.quransafeguard.hifz.data;

import android.content.Context;
import android.content.res.AssetManager;

import org.brotli.dec.BrotliInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Local-only canonical Mushaf source. There is deliberately no network fallback. */
public final class MushafRepository {
    public static final int FIRST_PAGE = 1;
    public static final int LAST_PAGE = 604;
    private static final String ROOT = "mushaf/hafs/kfqc/svg-br/";

    public static final class Page {
        public final int number;
        public final byte[] svgUtf8;

        private Page(int number, byte[] svgUtf8) {
            this.number = number;
            this.svgUtf8 = svgUtf8;
        }

        public String svgText() {
            return new String(svgUtf8, StandardCharsets.UTF_8);
        }
    }

    private final AssetManager assets;
    private final MushafSvgValidator validator;

    public MushafRepository(Context context) {
        this(context.getApplicationContext().getAssets(), new MushafSvgValidator());
    }

    MushafRepository(AssetManager assets, MushafSvgValidator validator) {
        this.assets = assets;
        this.validator = validator;
    }

    public Page load(int page) throws IOException {
        requirePage(page);
        byte[] svg = decompress(pathFor(page));
        validator.validate(svg);
        return new Page(page, svg);
    }

    public boolean isBundled(int page) {
        requirePage(page);
        try (InputStream ignored = assets.open(pathFor(page))) {
            return true;
        } catch (IOException missing) {
            return false;
        }
    }

    public static String pathFor(int page) {
        requirePage(page);
        return ROOT + String.format(java.util.Locale.ROOT, "%03d.svg.br", page);
    }

    private byte[] decompress(String assetPath) throws IOException {
        try (InputStream raw = assets.open(assetPath);
             BrotliInputStream brotli = new BrotliInputStream(raw);
             ByteArrayOutputStream out = new ByteArrayOutputStream(256 * 1024)) {
            byte[] buffer = new byte[32 * 1024];
            int total = 0;
            int read;
            while ((read = brotli.read(buffer)) != -1) {
                total += read;
                if (total > MushafSvgValidator.MAX_UNCOMPRESSED_BYTES) {
                    throw new IOException("Decompressed Mushaf SVG exceeds size limit: " + assetPath);
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static void requirePage(int page) {
        if (page < FIRST_PAGE || page > LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604: " + page);
        }
    }
}
