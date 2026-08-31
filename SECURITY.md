# Quran Safeguard — Security Policy

## Private distribution

Quran Safeguard is not publicly distributed during the current testing phase.

The official Android package must be distributed through a controlled Google Play testing track (internal or closed testing). Direct APK sharing is not an official distribution channel.

## Official build policy

- Release builds must be signed with the official application signing identity.
- Testers should install and update the application only from Google Play.
- Do not distribute release signing keys, service-role keys, passwords, or private credentials in the repository or APK.
- Debug builds are development-only and must not be presented as official releases.
- Dependencies should be pinned and reviewed before release.
- The repository remains private during the private testing phase.

## Invitation model

Access to the private test is granted by adding the tester's Google-account email address to the authorized tester list.

Removing an email from the tester list prevents that account from receiving future private test releases. Existing installations can remain on a device, so sensitive server-side features must never rely only on tester-list membership.

## Malware / tampering guidance

Private access does not replace malware protection. The main protection is the trusted delivery chain:

Developer source -> controlled CI/build -> signed Android App Bundle -> Google Play -> authorized tester.

Users should be instructed not to install APK files received through messaging apps, email attachments, file-sharing sites, or unofficial mirrors.

## Reporting

Security issues should be reported privately to the project owner and should not be posted publicly while the application remains in private testing.
