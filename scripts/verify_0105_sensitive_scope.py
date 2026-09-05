#!/usr/bin/env python3
"""Release-blocking privacy gate for Quran Safeguard 0.10.5.

0.10.4 used a one-shot unfiltered Accessibility event as an exit sentinel.
0.10.5 forbids that runtime state completely. The legacy service branch remains
source-compatible for now, but its sole policy gate must be a literal constant
false and no alternative broad package/window visibility may be introduced.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SERVICE = ROOT / "app/src/main/java/com/quranunlock/guard/QuranAccessibilityService.kt"
POLICY = ROOT / "app/src/main/java/com/quranunlock/guard/TargetPresenceScopePolicy.kt"
PROTECTED = ROOT / "app/src/main/java/com/quranunlock/guard/ProtectedApps.kt"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
CONFIG = ROOT / "app/src/main/res/xml/accessibility_service_config.xml"
TESTS = ROOT / "app/src/test/java/com/quranunlock/guard/TargetPresenceScopePolicyTest.kt"

service = SERVICE.read_text(encoding="utf-8")
policy = POLICY.read_text(encoding="utf-8")
protected = PROTECTED.read_text(encoding="utf-8")
manifest = MANIFEST.read_text(encoding="utf-8")
config = CONFIG.read_text(encoding="utf-8")
tests = TESTS.read_text(encoding="utf-8")

errors: list[str] = []

def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)

# The only API capable of requesting the old broad scope must be compile-time false.
require(
    re.search(
        r"fun\s+requiresAnonymousExitSentinel\s*\([^)]*\)\s*:\s*Boolean\s*=\s*false\b",
        policy,
        flags=re.S,
    ) is not None,
    "TargetPresenceScopePolicy.requiresAnonymousExitSentinel must be a literal false",
)
require("= true" not in policy.split("fun requiresAnonymousExitSentinel", 1)[-1],
        "sentinel policy contains a true path")

# The service may retain the historical branch only behind the policy above; it
# must not set packageNames=null anywhere else or use another broad-observation API.
require(service.count("packageNames = if (anonymousExitSentinel)") == 1,
        "unexpected Accessibility package-scope assignment")
require(service.count("TargetPresenceScopePolicy.requiresAnonymousExitSentinel") == 1,
        "broad-scope decision must have exactly one policy gate")
require("UsageStatsManager" not in service, "UsageStatsManager is forbidden")
require("PACKAGE_USAGE_STATS" not in service and "PACKAGE_USAGE_STATS" not in manifest,
        "package usage access is forbidden")
require("QUERY_ALL_PACKAGES" not in manifest, "QUERY_ALL_PACKAGES is forbidden")
require("READ_PHONE_STATE" not in manifest, "READ_PHONE_STATE is forbidden")
require("android.permission.INTERNET" not in manifest, "app must remain offline")

# Window contents and interactive-window retrieval remain disabled.
require('android:canRetrieveWindowContent="false"' in config,
        "canRetrieveWindowContent must remain false")
require("FLAG_RETRIEVE_INTERACTIVE_WINDOWS" not in service,
        "interactive-window retrieval flag is forbidden")
require("getWindows()" not in service and ".windows" not in service,
        "Accessibility window enumeration is forbidden")

# Product scope remains exactly social/browser targets plus transition packages.
for marker in (
    "GuardPrefs.protectedPackages(context)",
    "transitionSignalPackages(context)",
    "SYSTEM_UI_PACKAGE",
    "launcherPackage(context)",
    "!isSelectableTarget(packageName)",
):
    require(marker in protected, f"missing fixed-scope invariant: {marker}")

# Regression tests must encode the new expectation, even where a legacy test name
# is retained for the old 0.10.4 source audit.
require("assertFalse" in tests, "privacy tests must assert the sentinel is disabled")
for test_name in (
    "runningSelectedTargetRequiresOneAnonymousExitSignal",
    "outsideApplicationCanNeverOwnTheSharedBudgetScope",
    "runningSelectedTargetNeverEnablesAnonymousExitSentinel",
):
    require(f"fun {test_name}(" in tests, f"missing privacy regression test: {test_name}")

if errors:
    raise SystemExit("0.10.5 SENSITIVE-SCOPE AUDIT FAILURE:\n- " + "\n- ".join(errors))

print("0.10.5 sensitive-app Accessibility audit: PASS")
print("- broad/unfiltered Accessibility sentinel is compile-time disabled")
print("- no UsageStats, QUERY_ALL_PACKAGES, phone-state or window-content fallback")
print("- Accessibility remains limited to selected social/browser targets and transition signals")
print("- excluded banking/security/identity apps are not admitted to the event scope")
