# Quran Hifz 0.7.3 — final CI trigger

This commit exists only to trigger the official `hifz-test-app.yml` pipeline on the post-Claude blocking-fix candidate tree.

- Candidate application parent: `3fa36da0ed0499385d84e12655325e631c48cd30`
- Blocking fixes in scope: B1 restored; B3 now resolves the persisted in-progress Anchoring unit before any queue-head protocol and preserves started-unit priority during reconcile.
- Application code is unchanged by this marker.
- Publication and signing remain NO-GO until the official pipeline and final independent Claude review pass.
