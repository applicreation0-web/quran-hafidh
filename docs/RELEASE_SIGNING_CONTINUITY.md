# Quran Safeguard — release signing continuity

Date: 2026-09-02

## Release-blocking rule

An Android update can be installed over the existing application only when the
new APK is signed by the same certificate as the installed APK. Conversation
history is not sufficient evidence: the exact binaries and the device state are
authoritative.

## Verified certificate fingerprints

| Artifact / retained report | SHA-256 signing certificate |
| --- | --- |
| Quran-Safeguard-0.3.2-release.apk | `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC` |
| Quran-Safeguard-0.8.2 release report | `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC` |
| Quran-Safeguard-release.apk | `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC` |
| Quran-Safeguard-0.9.3 device-fix report | `6C:70:6F:4E:A4:4E:F6:67:D0:B9:69:8C:07:A3:9E:92:9B:1E:28:6D:23:97:66:55:04:41:1D:ED:B3:74:57:AC` |
| Quran-Safeguard-0.9.0-first-install-release.apk | `9C:66:DE:17:3F:EB:0E:10:24:5F:8D:84:9C:2A:7C:B6:4B:BF:75:F5:0B:63:C5:F8:30:A2:28:AB:D1:00:9F:E7` |

The retained Quran-Safeguard release PKCS12 record identifies the `6C:70:…:AC`
certificate. The 0.9.0 first-install binary is therefore a separate signing
lineage even though its certificate subject uses the same application name.

## Mandatory device decision before release

1. Do not publish or distribute a candidate yet.
2. On the test phone, identify which APK was installed most recently and verify
   its certificate fingerprint, or attempt an update only after preserving app
   state and the installed APK.
3. If the installed certificate is `6C:70:…:AC`, sign the final candidate with
   the retained release PKCS12 and verify the exact signed APK.
4. If the installed certificate is `9C:66:…:E7`, the retained `6C:70:…:AC`
   PKCS12 cannot provide an in-place update. Recover the matching old private key
   or explicitly plan a clean reinstall; never disguise that as an update.
5. Never commit the PKCS12 file, its alias password or its key password.

Release authorization remains blocked until this device check is recorded.
