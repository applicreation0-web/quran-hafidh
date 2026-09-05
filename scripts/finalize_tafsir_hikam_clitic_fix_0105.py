#!/usr/bin/env python3
"""Keep Hikam technical-term matching exact while handling common Arabic clitics.

This is deliberately narrower than substring matching. It expands only a small,
well-defined set of attached Arabic particles so forms such as والبسط and للنفس
can resolve to the same exact lexical token, while verbs such as يقبض and the bare
carpet noun بسط remain distinct from technical القبض / البسط.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(rel: str, old: str, new: str) -> None:
    p = ROOT / rel
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected exactly one target, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/quranunlock/guard/HikamTechnicalLexicon.kt",
    '''    private fun normalizedWords(text: String): Set<String> = arabicWord
        .findAll(arabicDiacritics.replace(text, ""))
        .map { it.value }
        .toSet()
''',
    '''    private fun normalizedWords(text: String): Set<String> = buildSet {
        arabicWord.findAll(arabicDiacritics.replace(text, "")).forEach { match ->
            val token = match.value
            add(token)

            // Common conjunctions attach directly to the following word.
            // Example: والبسط -> البسط. Never strip a lexical prefix such as يـ.
            if ((token.startsWith("و") || token.startsWith("ف")) && token.length > 3) {
                add(token.drop(1))
            }

            // Preposition + definite article. Examples: بالبسط / كالبسط -> البسط.
            if ((token.startsWith("بال") || token.startsWith("كال")) && token.length > 4) {
                add("ال" + token.drop(3))
            }

            // Contracted li- + definite article. Example: للنفس -> النفس.
            if (token.startsWith("لل") && token.length > 3) {
                add("ال" + token.drop(2))
            }
        }
    }
''',
)

replace_once(
    "scripts/audit_hikam_technical_lexicon_0105.py",
    '''def words(text: str) -> set[str]:
    return set(ARABIC_WORD.findall(DIACRITICS.sub("", text)))
''',
    '''def words(text: str) -> set[str]:
    out: set[str] = set()
    for token in ARABIC_WORD.findall(DIACRITICS.sub("", text)):
        out.add(token)
        # Match the Android runtime's deliberately conservative clitic handling.
        if len(token) > 3 and token[0] in {"و", "ف"}:
            out.add(token[1:])
        if len(token) > 4 and (token.startswith("بال") or token.startswith("كال")):
            out.add("ال" + token[3:])
        if len(token) > 3 and token.startswith("لل"):
            out.add("ال" + token[2:])
    return out
''',
)

print("0.10.5 Hikam clitic normalization applied")
print("- exact-token discipline preserved")
print("- و/ف conjunctions, بال/كال and contracted لل are normalized")
print("- يقبض and bare بسط remain distinct from technical القبض / البسط")
