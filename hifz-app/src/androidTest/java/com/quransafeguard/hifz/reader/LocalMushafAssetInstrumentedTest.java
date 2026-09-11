package com.quransafeguard.hifz.reader;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.data.MushafRepository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Local smoke gate. Run only after vendoring the canonical local assets. */
public final class LocalMushafAssetInstrumentedTest {
    @Test
    public void page001And002DecompressAndValidateLocally() throws Exception {
        MushafRepository repository = new MushafRepository(ApplicationProvider.getApplicationContext());
        assertTrue(repository.isBundled(1));
        assertTrue(repository.isBundled(2));
        assertEquals(1, repository.load(1).number);
        assertEquals(2, repository.load(2).number);
        assertTrue(repository.load(1).svgText().contains("<svg"));
    }
}
