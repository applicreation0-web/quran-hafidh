from pathlib import Path

path = Path('hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java')
text = path.read_text(encoding='utf-8')

field_anchor = '    private static final String LEGACY_GATES = "hifz_preview_session_gates";\n    private final SharedPreferences p;\n'
field_replacement = '''    private static final String LEGACY_GATES = "hifz_preview_session_gates";\n    private static volatile String migrationFaultPointForTest;\n    private final SharedPreferences p;\n\n    static void setMigrationFaultPointForTest(String point) {\n        if (point != null\n                && !"BEFORE_MAIN_COMMIT".equals(point)\n                && !"AFTER_MAIN_COMMIT".equals(point)) {\n            throw new IllegalArgumentException("Unknown migration fault point: " + point);\n        }\n        migrationFaultPointForTest = point;\n    }\n\n    private static void maybeInterruptMigrationForTest(String point) {\n        if (point != null && point.equals(migrationFaultPointForTest)) {\n            throw new IllegalStateException("Injected Hifz migration interruption at " + point);\n        }\n    }\n'''
if text.count(field_anchor) != 1:
    raise SystemExit(f'field anchor count={text.count(field_anchor)}')
text = text.replace(field_anchor, field_replacement, 1)

before_anchor = '''        SharedPreferences.Editor e = p.edit()\n            .putString("v6LearnedLineIds", lineIdsJson(state.toAnchorLineIds()))\n'''
before_replacement = '''        maybeInterruptMigrationForTest("BEFORE_MAIN_COMMIT");\n\n        SharedPreferences.Editor e = p.edit()\n            .putString("v6LearnedLineIds", lineIdsJson(state.toAnchorLineIds()))\n'''
if text.count(before_anchor) != 1:
    raise SystemExit(f'before anchor count={text.count(before_anchor)}')
text = text.replace(before_anchor, before_replacement, 1)

after_anchor = '''        if (!e.commit()) {\n            throw new IllegalStateException("Unable to migrate Hifz schema v5 to v6");\n        }\n\n        if (p.getInt("schema", -1) != 6\n'''
after_replacement = '''        if (!e.commit()) {\n            throw new IllegalStateException("Unable to migrate Hifz schema v5 to v6");\n        }\n\n        maybeInterruptMigrationForTest("AFTER_MAIN_COMMIT");\n\n        if (p.getInt("schema", -1) != 6\n'''
if text.count(after_anchor) != 1:
    raise SystemExit(f'after anchor count={text.count(after_anchor)}')
text = text.replace(after_anchor, after_replacement, 1)

path.write_text(text, encoding='utf-8')
print('PATCH_OK schema6 recovery hook')
