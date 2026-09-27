package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

/**
 * P4's canonical Sabqi route past Al-Baqara. Today's Sabqi is a single flat, user-editable
 * {@code sabqiStart..sabqiEnd} range that never advances past its own end without a manual
 * Settings edit. The spec's "next front becomes Āl ʿImrān, then canonical progression to 48:29"
 * needs no jump/gap logic at all: 2:286 (Al-Baqara's last ayah) and 3:1 (Āl ʿImrān's first) are
 * already the natural canonical successor pair — {@code QuranCanon.next(2,286) == 3:1}. The whole
 * route is therefore just one continuous range from wherever Sabqi currently starts up to a fixed
 * ceiling, never a list of surahs to jump between.
 *
 * {@code ROUTE_END} is the verse immediately before Al-Hujurāt (49:1), which is never Sabqi
 * material — it is the Itqān TAIL leg's own start (see the Roadmap's Itqān rotation), so the
 * Sabqi route must stop exactly at 48:29 and never cross into it.
 */
final class SabqiRoute {
    static final VerseRef ROUTE_END = new VerseRef(48, 29);

    private SabqiRoute() {}

    /** True once there is no more canonical Sabqi material left to memorize past this cursor —
     *  the one signal RoadmapPolicy.Input.allNewSabqiComplete needs. */
    static boolean allNewSabqiComplete(VerseRef cursor) {
        return GeometryRepository.ordinal(cursor) > GeometryRepository.ordinal(ROUTE_END);
    }

    /** The working range's own end should never be configured past the route's ceiling — this
     *  clamps a caller-proposed end (e.g. one auto-extended past a finished surah) to 48:29. */
    static VerseRef clampToRouteEnd(VerseRef proposedEnd) {
        return GeometryRepository.ordinal(proposedEnd) > GeometryRepository.ordinal(ROUTE_END)
            ? ROUTE_END : proposedEnd;
    }
}
