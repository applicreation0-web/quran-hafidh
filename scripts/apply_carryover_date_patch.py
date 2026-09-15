from pathlib import Path

path = Path("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java")
text = path.read_text()

old_constant = '    public static final String EXTRA_MODE = "mode";\n'
new_constant = old_constant + '    public static final String EXTRA_SCHEDULED_DATE = "scheduled_date";\n'
assert text.count(old_constant) == 1, "EXTRA_MODE anchor changed"
text = text.replace(old_constant, new_constant, 1)

old_block = '''        prefs = new HifzPrefs(this);\n        String recordedSessionDate = SABQI_TODAY_REVIEW.equals(mode) && prefs.elapsedFor(mode) > 0L\n            ? prefs.sabqiTodayReviewDate()\n            : "";\n        LocalDate capturedToday = HifzClock.today();\n        try {\n            sessionDate = recordedSessionDate == null || recordedSessionDate.isEmpty()\n                ? capturedToday : LocalDate.parse(recordedSessionDate);\n        } catch (RuntimeException invalidRecordedDate) {\n            sessionDate = capturedToday;\n        }\n'''
new_block = '''        prefs = new HifzPrefs(this);\n        String recordedSessionDate = SABQI_TODAY_REVIEW.equals(mode) && prefs.elapsedFor(mode) > 0L\n            ? prefs.sabqiTodayReviewDate()\n            : "";\n        String scheduledSessionDate = getIntent().getStringExtra(EXTRA_SCHEDULED_DATE);\n        String requestedSessionDate = recordedSessionDate != null && !recordedSessionDate.isEmpty()\n            ? recordedSessionDate\n            : scheduledSessionDate;\n        LocalDate capturedToday = HifzClock.today();\n        try {\n            sessionDate = requestedSessionDate == null || requestedSessionDate.isEmpty()\n                ? capturedToday : LocalDate.parse(requestedSessionDate);\n        } catch (RuntimeException invalidRecordedDate) {\n            sessionDate = capturedToday;\n        }\n'''
assert text.count(old_block) == 1, "session-date anchor changed"
text = text.replace(old_block, new_block, 1)
path.write_text(text)
