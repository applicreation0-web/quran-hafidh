package com.applicreation0.quransafeguard

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Reader109StateSanitizerTest {
    @Test fun validReader109StateIsPreserved() {
        val raw = JSONObject().apply {
            put("schema", 1)
            put("page", 499)
            put("zoom", 1.3)
            put("marks", JSONArray().put(JSONObject().put("page", 305).put("category", "Signet")))
            put("sessions", JSONArray().put(JSONObject().put("id", "s1").put("verses", JSONArray().put("2:3"))))
            put("active", "s1")
            put("ui", JSONObject().put("mode", "MEMORIZATION").put("selectStart", "2:3").put("selectEnd", JSONObject.NULL))
        }.toString()
        val result = Reader109StateSanitizer.sanitize(raw, 1)
        val out = JSONObject(result.json)
        assertEquals(499, result.page)
        assertEquals("MEMORIZATION", out.getJSONObject("ui").getString("mode"))
        assertEquals(1, out.getJSONArray("marks").length())
        assertEquals(1, out.getJSONArray("sessions").length())
        assertEquals("s1", out.getString("active"))
    }

    @Test fun missingPageFallsBackWithoutDiscardingValidBookmarkAndSession() {
        val raw = JSONObject().apply {
            put("schema", 1)
            put("marks", JSONArray().put(JSONObject().put("page", 304)))
            put("sessions", JSONArray().put(JSONObject().put("id", "s1")))
            put("active", "s1")
            put("ui", JSONObject().put("mode", "MEMORIZATION"))
        }.toString()
        val out = JSONObject(Reader109StateSanitizer.sanitize(raw, 12).json)
        assertEquals(12, out.getInt("page"))
        assertEquals(304, out.getJSONArray("marks").getJSONObject(0).getInt("page"))
        assertEquals("s1", out.getString("active"))
        assertEquals("MEMORIZATION", out.getJSONObject("ui").getString("mode"))
    }

    @Test fun invalidPagesZeroAbove604NullStringAndFractionUseDeterministicFallback() {
        val invalid = listOf<Any?>(0, 605, JSONObject.NULL, "499", 499.5)
        invalid.forEach { bad ->
            val raw = JSONObject().apply { put("schema", 1); put("page", bad) }.toString()
            val result = Reader109StateSanitizer.sanitize(raw, 2)
            assertEquals("bad=$bad", 2, result.page)
            assertEquals(2, JSONObject(result.json).getInt("page"))
        }
    }

    @Test fun malformedAndPartialJsonNeverPropagateAnInvalidPage() {
        listOf("{", "[]", "null", "", null).forEach { raw ->
            val result = Reader109StateSanitizer.sanitize(raw, 604)
            assertEquals(604, result.page)
            assertEquals(1, JSONObject(result.json).getInt("schema"))
        }
    }

    @Test fun malformedContainersAreDroppedButReaderRemainsBootable() {
        val raw = """{"schema":1,"page":305,"marks":"bad","sessions":7,"active":{},"ui":"bad"}"""
        val out = JSONObject(Reader109StateSanitizer.sanitize(raw).json)
        assertEquals(305, out.getInt("page"))
        assertEquals(0, out.getJSONArray("marks").length())
        assertEquals(0, out.getJSONArray("sessions").length())
        assertTrue(out.isNull("active"))
        assertEquals("READING", out.getJSONObject("ui").getString("mode"))
    }

    @Test fun invalidBookmarksAreRemovedIndependentlyOfValidOnes() {
        val marks = JSONArray()
            .put(JSONObject().put("page", 1))
            .put(JSONObject().put("page", 0))
            .put(JSONObject().put("page", 604))
            .put(JSONObject().put("page", "499"))
        val raw = JSONObject().put("schema", 1).put("page", 304).put("marks", marks).toString()
        val out = JSONObject(Reader109StateSanitizer.sanitize(raw).json).getJSONArray("marks")
        assertEquals(2, out.length())
        assertEquals(1, out.getJSONObject(0).getInt("page"))
        assertEquals(604, out.getJSONObject(1).getInt("page"))
    }

    @Test fun danglingActiveSessionIsClearedWithoutDroppingSessions() {
        val sessions = JSONArray().put(JSONObject().put("id", "kept"))
        val raw = JSONObject().put("schema", 1).put("page", 1).put("sessions", sessions).put("active", "missing").toString()
        val out = JSONObject(Reader109StateSanitizer.sanitize(raw).json)
        assertTrue(out.isNull("active"))
        assertEquals("kept", out.getJSONArray("sessions").getJSONObject(0).getString("id"))
    }

    @Test fun fallbackItselfIsBounded() {
        assertEquals(1, Reader109StateSanitizer.sanitize("{\"schema\":1,\"page\":0}", 0).page)
        assertEquals(1, Reader109StateSanitizer.sanitize("{\"schema\":1,\"page\":0}", 999).page)
    }
}
