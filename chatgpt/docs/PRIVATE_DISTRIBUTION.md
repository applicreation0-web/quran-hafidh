# Private distribution and tester onboarding

## Chosen model

During the private phase, Quran Safeguard uses Google Play internal or closed testing instead of public distribution.

This keeps onboarding simple and reduces the risk of users installing modified APKs from unofficial sources.

## Tester journey

The tester should only need to:

1. Receive the private Google Play invitation/opt-in link.
2. Open the link with the Google account whose email address was authorized.
3. Tap **Become a tester / Join** if Google Play shows the opt-in screen.
4. Tap the Google Play install link.
5. Install Quran Safeguard from Google Play.
6. Open the application and complete its normal first-run setup.
7. Optionally allow notifications and approximate location if the tester wants the 20:00 reminder and locally calculated morning/evening adhkar windows. Approximate location is used on-device for prayer-time calculation and is not uploaded by Quran Safeguard.

No APK download, sideloading, developer mode, USB installation, or external installer should be required.

## Admin journey

For each new tester:

1. Add the tester's Google-account email to the internal/closed testing list in Play Console.
2. Send the private opt-in link.
3. Remove the email from the list when private access should end.

## Recommended phase

- Small initial group: Internal testing.
- Larger invitation-only beta: Closed testing.
- Public release: only when explicitly decided later.

## Security rule shown to testers

> Install Quran Safeguard only from the private Google Play link you received. Never install an APK sent by message, email, or a third-party website.

## Application-side principle

The app remains a voluntary awareness tool. Private distribution is a software supply-chain and testing control, not a mechanism to coerce the user or prevent normal control of their Android device.
