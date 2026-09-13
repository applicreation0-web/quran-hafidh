from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, got {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java",
    '''        boolean keepCurrent = p.getInt("itqanBlockIndex", 0) > 0;''',
    '''        boolean keepCurrent = p.getInt("itqanBlockIndex", 0) > 0 || p.getInt("itqanRep", 0) > 0;'''
)

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java",
    '''        List<AnchoringQueue.Entry> queue = anchoringQueue();
        return queue.isEmpty() ? null : queue.get(anchoringQueueIndex(queue.size()));''',
    '''        List<AnchoringQueue.Entry> queue = anchoringQueue();
        if (!queue.isEmpty() && p.getInt("itqanRep", 0) > 0) {
            VerseRef savedStart = itqanUnitStart();
            VerseRef savedEnd = itqanUnitEnd();
            if (savedStart != null && savedEnd != null) {
                AnchoringQueue.Entry inProgress = AnchoringQueue.findByRange(
                    queue, savedStart.toString(), savedEnd.toString());
                if (inProgress != null) return inProgress;
            }
        }
        return queue.isEmpty() ? null : queue.get(anchoringQueueIndex(queue.size()));'''
)

print("ANCHORING_PROTOCOL_CONTINUITY_PREFS_PATCH_OK")
