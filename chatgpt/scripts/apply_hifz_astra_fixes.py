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
    '''        next.addAll(expected.values());
        int index = anchoringQueueIndex(next.size());
        return p.edit()
            .putString("anchoringQueue", anchoringQueueJson(next))
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", index)
            .commit();''',
    '''        next.addAll(expected.values());
        int index = anchoringQueueIndex(next.size());
        boolean keepCurrent = p.getInt("itqanBlockIndex", 0) > 0;
        next = new ArrayList<>(AnchoringQueue.mergeWithPromotionPriority(
            next, index, keepCurrent, Collections.emptyList()));
        index = 0;
        return p.edit()
            .putString("anchoringQueue", anchoringQueueJson(next))
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", index)
            .commit();'''
)

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java",
    '''    @Override protected void onResume(){super.onResume();if(!sessionCompleted&&!awaitingValidation&&!timedSessionLimitReached)clock.resume();}''',
    '''    @Override protected void onResume(){
        super.onResume();
        if(clock!=null)clock.syncPersistedElapsed(prefs.elapsedFor(mode));
        if(!sessionCompleted&&!awaitingValidation&&!timedSessionLimitReached)clock.resume();
    }'''
)

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java",
    '''    private int consumedMinutes(SessionKind kind, int targetMinutes) {
        if (targetMinutes <= 0) return 0;
        String mode = modeFor(kind);''',
    '''    private int consumedMinutes(SessionKind kind, int targetMinutes) {
        if (targetMinutes <= 0 || !isReusableJ10Kind(kind)) return 0;
        String mode = modeFor(kind);'''
)

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java",
    '''            out[offset] = Math.max(0, plan.getMorning().getTargetMinutes())
                + Math.max(0, plan.getEvening().getTargetMinutes());''',
    '''            out[offset] = reusableCapacityMinutes(
                    plan.getMorning().getKind(), plan.getMorning().getTargetMinutes())
                + reusableCapacityMinutes(
                    plan.getEvening().getKind(), plan.getEvening().getTargetMinutes());'''
)

replace_once(
    "hifz-app/src/main/java/com/quransafeguard/hifz/preview/J10ReviewPlanner.java",
    '''    static int scheduledCapacityMinutes(LocalDate start, int days,
                                        int recentBlockCount, boolean consolidationActivated) {''',
    '''    private static int reusableCapacityMinutes(SessionKind kind, int targetMinutes) {
        return isReusableJ10Kind(kind) ? Math.max(0, targetMinutes) : 0;
    }

    static boolean isReusableJ10Kind(SessionKind kind) {
        return kind == SessionKind.SABQI_TODAY_REVIEW
            || kind == SessionKind.RECENT_SABQI_REVIEW
            || kind == SessionKind.OLD_ITQAN_MURAJAAH;
    }

    static int scheduledCapacityMinutes(LocalDate start, int days,
                                        int recentBlockCount, boolean consolidationActivated) {'''
)

print("ASTRA_SOURCE_PATCH_OK")
