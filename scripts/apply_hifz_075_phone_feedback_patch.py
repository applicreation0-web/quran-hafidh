from pathlib import Path

TEST = Path("hifz-app/src/androidTest/java/com/quransafeguard/hifz/preview/HifzV6PersistentStateInstrumentedTest.java")
START = "    @Test public void manualMultiRangesPersistDisjointStatePreserveCursorsAndNeverInventJ10Dates() throws Exception {"
SEED = "    private void seedSingleConflictSchemaFive() {"

text = TEST.read_text(encoding="utf-8")
count = text.count(START)
if count == 2:
    first = text.index(START)
    second = text.index(START, first + len(START))
    seed = text.index(SEED, second)
    text = text[:second] + text[seed:]
    TEST.write_text(text, encoding="utf-8")
    print("DUPLICATE_MULTI_RANGE_TESTS_REMOVED")
elif count == 1:
    print("PHONE_FEEDBACK_CLEANUP_ALREADY_APPLIED")
else:
    raise SystemExit(f"unexpected multi-range test occurrence count: {count}")
