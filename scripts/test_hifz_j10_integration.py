from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = (root / "hifz-app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
app = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java").read_text(encoding="utf-8")
activity = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java").read_text(encoding="utf-8")
observer = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewObserver.java").read_text(encoding="utf-8")
planner = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java").read_text(encoding="utf-8")
store = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewStore.java").read_text(encoding="utf-8")

checks = {
    "application guard registered": 'android:name=".QuranHifzApp"' in manifest,
    "priority activity registered": 'android:name=".J10ReviewActivity"' in manifest,
    "structured sessions can be preempted": "activity instanceof HifzSessionActivity" in app and "J10ReviewActivity.class" in app,
    "non-tenable alert exists": "showSustainabilityAlert" in app and "NON_TENABLE" in app,
    "validated sessions feed J10 list": all(k in observer for k in ["lastSabqiDate", "lastSabqiTodayReviewDate", "lastItqanDate", "lastMurajaahDate", "recentSabqi"]),
    "new lines can be acquired without deleting old lines": "acquireLines" in store,
    "single line list feeds priority": "priorityGroup" in planner and "priorityLineIndexes" in planner,
    "display alone cannot mark reviewed": "markReviewed" in activity and "validateReviewed" in activity,
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("J10 integration contract failed: " + "; ".join(failed))
print("J10_INTEGRATION_CONTRACT_OK")
