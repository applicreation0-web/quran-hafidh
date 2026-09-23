from pathlib import Path

# Triggered only on the dedicated final-blockers branch; all replacements are assertive.

def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

# Consolidation is progression/repetition driven. J10 must never consume or close it as a timed host.
replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranHifzApp.java",
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n            || HifzSessionActivity.ITQAN.equals(mode)\n            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)\n            || HifzSessionActivity.MURAJAAH.equals(mode);''',
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n            || HifzSessionActivity.ITQAN.equals(mode)\n            || HifzSessionActivity.MURAJAAH.equals(mode);''')

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10HostBudgetStore.java",
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n            || HifzSessionActivity.ITQAN.equals(mode)\n            || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)\n            || HifzSessionActivity.MURAJAAH.equals(mode);''',
    '''        return HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n            || HifzSessionActivity.ITQAN.equals(mode)\n            || HifzSessionActivity.MURAJAAH.equals(mode);''')

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewActivity.java",
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n                || HifzSessionActivity.ITQAN.equals(mode)\n                || HifzSessionActivity.RECENT_SABQI_REVIEW.equals(mode)\n                || HifzSessionActivity.MURAJAAH.equals(mode)) return mode;''',
    '''        if (HifzSessionActivity.SABQI_TODAY_REVIEW.equals(mode)\n                || HifzSessionActivity.ITQAN.equals(mode)\n                || HifzSessionActivity.MURAJAAH.equals(mode)) return mode;''')

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java",
    '''        return kind == SessionKind.SABQI_TODAY_REVIEW\n            || kind == SessionKind.ITQAN\n            || kind == SessionKind.RECENT_SABQI_REVIEW\n            || kind == SessionKind.OLD_ITQAN_MURAJAAH;''',
    '''        return kind == SessionKind.SABQI_TODAY_REVIEW\n            || kind == SessionKind.ITQAN\n            || kind == SessionKind.OLD_ITQAN_MURAJAAH;''')

print("Applied B1: Consolidation removed from reusable J10 hosts/capacity.")
