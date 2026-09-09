package com.applicreation0.quransafeguard

/**
 * Runtime readiness contract used by the timed Safeguard reader.
 * A WebView load event alone is never sufficient to start reading time.
 */
object MushafRuntimeVisibilityPolicy {
    private val substantiveElement = Regex(
        "<(path|text|use|polygon|polyline|line|circle|ellipse|rect)(\\s|>)",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun sourceLooksRenderable(svg: String?): Boolean {
        if (svg.isNullOrBlank()) return false
        if (!Regex("<svg(\\s|>)", RegexOption.IGNORE_CASE).containsMatchIn(svg)) return false
        if (svg.contains("<parsererror", ignoreCase = true)) return false
        return substantiveElement.findAll(svg).take(8).count() >= 8
    }

    fun probeJavascript(): String = """
        (() => {
          const svg = document.querySelector('svg');
          if (!svg) return 'NO_SVG';
          const r = svg.getBoundingClientRect();
          const s = getComputedStyle(svg);
          const host = document.body.getBoundingClientRect();
          const count = svg.querySelectorAll(
            'path,text,use,polygon,polyline,line,circle,ellipse,rect'
          ).length;
          let box = null;
          try { box = svg.getBBox(); } catch (_) {}
          const dimensions = r.width > 1 && r.height > 1 &&
            host.width > 1 && host.height > 1 &&
            (!box || (box.width > 1 && box.height > 1));
          const visible = s.display !== 'none' && s.visibility !== 'hidden' &&
            s.opacity !== '0';
          return count >= 8 && dimensions && visible ? 'READY' : 'NOT_VISIBLE';
        })()
    """.trimIndent()

    fun probeResultIsReady(rawJavascriptResult: String?): Boolean =
        rawJavascriptResult == "\"READY\""
}
