# Google Play private setup — Quran Safeguard

## Recommended route

Use Google Play **Internal testing** for the initial private group. It keeps installation and updates inside Google Play while access is limited to invited Google-account email addresses.

## One-time Play Console setup

1. Create the Quran Safeguard application in Play Console.
2. Confirm the final Android package name before the first uploaded artifact; Google Play fixes that package name after upload.
3. Enable **Play App Signing**.
4. Create a signed Android App Bundle (AAB) release.
5. Go to **Test and release → Testing → Internal testing**.
6. Create a tester email list.
7. Add each invited person's Google-account email address.
8. Create the internal release and upload the signed AAB.
9. Copy the tester opt-in/shareable link.
10. Send only that private link to invited testers.

## AccessibilityService declaration

Quran Safeguard is **not** an accessibility tool for disability support. Do not declare `isAccessibilityTool=true`.

The app uses AccessibilityService for one narrow feature: detecting when a selected foreground application is opened so Quran Safeguard can present the voluntary Quran pause.

Before enabling the Android service, the app must visibly disclose:

- what is accessed: window-change events and the foreground application's package/name;
- why: to identify selected applications and trigger the Quran pause;
- what is not accessed: screen contents and text entry;
- sharing: this information is not sent or shared;
- control: the user can decline or disable the service later.

The user must affirmatively accept before being sent to Android Accessibility settings.

When required by the target distribution track, complete the AccessibilityService Permission Declaration in Play Console and provide the requested demonstration video showing:
1. app launch;
2. full disclosure;
3. acceptance path;
4. refusal/later path;
5. activation in Android settings;
6. the core Quran pause feature in action.

## Tester journey

The invited tester should only need to:

1. Open the private Google Play link using the authorised Google account.
2. Join/accept the test if prompted.
3. Install Quran Safeguard from Google Play.
4. Open the app.
5. Read the accessibility disclosure.
6. Tap **J’accepte et je continue**.
7. In Android Accessibility settings, enable **Quran Safeguard — Protection**.
8. Return to Quran Safeguard and choose the applications and Quran rules.

No APK sideloading, developer mode or USB installation is required.

## If distributing outside Google Play

Direct APK distribution is technically possible, but it is not the preferred invited-user flow.

On Android 13 and later, a sideloaded app may be prevented from using sensitive restricted settings until the user explicitly allows them. The user may need to:

1. Install the signed APK from the chosen source.
2. Open **Settings → Apps → Quran Safeguard**.
3. Open the three-dot menu.
4. Tap **Allow restricted settings**.
5. Confirm Android's warning.
6. Then open **Settings → Accessibility** and enable Quran Safeguard.

Other downsides of sideloading:
- more security warnings and user friction;
- users must trust the APK source themselves;
- updates are no longer automatically handled by Google Play unless another trusted updater is built;
- invitation control becomes weaker unless a separate server-side account/entitlement system is added;
- Play App Signing distribution guarantees do not protect that direct-delivery path;
- users are more likely to confuse a copied or modified APK with the official build.

For Quran Safeguard's current goals — private, simple and secure — Internal Testing is the recommended route.


## Do not mix official delivery channels after Play App Signing

When Play App Signing is enabled, Google Play holds the app-signing key used for the APKs delivered to users, while the developer normally keeps a separate upload key for AAB uploads.

Therefore, do not treat an APK signed only with a different local/upload key as an interchangeable update for a Play-installed build. Android requires compatible application signatures for in-place updates.

For invited testers, keep one official route:
**Play Console internal/closed test → Google Play installation → Google Play updates.**
