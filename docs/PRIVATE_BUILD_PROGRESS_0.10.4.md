# Quran Safeguard Plus 0.10.4 — private build progress

Scope: strictly personal/private APK. No public distribution.

## Qushayri
- deterministic source parser implemented and locally audited against the exact Sands PDF SHA-256;
- 807 sequential source anchors / 669 structural verses confirmed;
- English-only extraction; Arabic source text excluded;
- 529 verse mappings retained under a fail-closed attribution rule;
- 69 ambiguous multi-verse clusters are deliberately hidden rather than guessed;
- explicit range 4:167–169 and inline 2:68 regressions pass.

## Still required
- integrate the parser into the private build pipeline;
- finish equivalent fail-closed Qurtubi parser/content audit;
- private-scope runtime/manifest gate;
- complete requested Hikam release checks or keep unverified new sharh out of the build;
- successful end-to-end build/test/signing audit.
