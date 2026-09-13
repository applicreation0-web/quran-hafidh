from pathlib import Path

root = Path(__file__).resolve().parents[1]
session = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").read_text(encoding="utf-8")
main = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java").read_text(encoding="utf-8")
store = (root / "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewStore.java").read_text(encoding="utf-8")

checks = {
    "session coordinator wired": "J10ReviewCoordinator" in session,
    "new lesson credits J10": "acquireAndReview" in session,
    "J10 priority can preempt structured session": "renderJ10Priority" in session,
    "home exposes J10 sustainability": "refreshJ10Advisory" in main,
    "store can acquire newly learned lines": "acquireLines" in store,
}
failed = [name for name, ok in checks.items() if not ok]
if failed:
    raise SystemExit("J10 integration contract RED: " + "; ".join(failed))
print("J10_INTEGRATION_CONTRACT_OK")
