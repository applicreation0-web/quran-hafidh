package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MushafRuntimeVisibilityPolicyTest {
    @Test fun blankParserErrorAndEmptySvgAreRejected() {
        assertFalse(MushafRuntimeVisibilityPolicy.sourceLooksRenderable(null))
        assertFalse(MushafRuntimeVisibilityPolicy.sourceLooksRenderable(""))
        assertFalse(MushafRuntimeVisibilityPolicy.sourceLooksRenderable("<parsererror>bad</parsererror>"))
        assertFalse(MushafRuntimeVisibilityPolicy.sourceLooksRenderable("<svg></svg>"))
    }

    @Test fun substantiveSvgIsAcceptedOnlyWithEnoughGeometry() {
        val few = "<svg>" + "<path d='M0 0'/>".repeat(7) + "</svg>"
        val enough = "<svg>" + "<path d='M0 0'/>".repeat(8) + "</svg>"
        assertFalse(MushafRuntimeVisibilityPolicy.sourceLooksRenderable(few))
        assertTrue(MushafRuntimeVisibilityPolicy.sourceLooksRenderable(enough))
    }

    @Test fun onlyExactDomReadyProbeCanStartReading() {
        assertTrue(MushafRuntimeVisibilityPolicy.probeResultIsReady("\"READY\""))
        assertFalse(MushafRuntimeVisibilityPolicy.probeResultIsReady("READY"))
        assertFalse(MushafRuntimeVisibilityPolicy.probeResultIsReady("\"NO_SVG\""))
        assertFalse(MushafRuntimeVisibilityPolicy.probeResultIsReady("\"NOT_VISIBLE\""))
        assertFalse(MushafRuntimeVisibilityPolicy.probeResultIsReady(null))
    }

    @Test fun probeChecksDomDimensionsVisibilityAndSubstantiveNodes() {
        val probe = MushafRuntimeVisibilityPolicy.probeJavascript()
        assertTrue(probe.contains("document.querySelector('svg')"))
        assertTrue(probe.contains("getBoundingClientRect"))
        assertTrue(probe.contains("getBBox"))
        assertTrue(probe.contains("visibility"))
        assertTrue(probe.contains("opacity"))
        assertTrue(probe.contains("count >= 8"))
    }
}
