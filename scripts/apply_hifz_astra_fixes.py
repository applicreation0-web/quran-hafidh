from pathlib import Path


def replace_once(path, old, new):
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, got {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    '''            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
        } else {''',
    '''            itqanUnit=new GeometryRepository.VerseUnit(currentPage,savedStart,savedEnd,verses,lineIds);
            AnchoringQueue.Entry inProgress = AnchoringQueue.findByRange(
                prefs.anchoringQueue(), savedStart.toString(), savedEnd.toString());
            if (inProgress != null) anchoringEntry = inProgress;
        } else {'''
)

print("ANCHORING_PROTOCOL_CONTINUITY_PATCH_OK")
