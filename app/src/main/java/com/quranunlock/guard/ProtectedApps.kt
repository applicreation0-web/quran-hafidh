package com.applicreation0.quransafeguard

import android.content.Context
import android.telecom.TelecomManager
import java.text.Normalizer
import java.util.Locale

object ProtectedApps {
    private val sensitiveDecisionCache = mutableMapOf<String, Boolean>()
    private var defaultDialerLoaded = false
    private var cachedDefaultDialer: String? = null

    const val PLAY_STORE = "com.android.vending"
    const val ANDROID_SETTINGS = "com.android.settings"

    private val alwaysAllowed = setOf(
        PLAY_STORE,
        // Calling/emergency infrastructure must never be intercepted.
        "com.android.server.telecom",
        "com.android.phone",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        // Clock, alarm and emergency/safety surfaces must remain immediately accessible.
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.google.android.apps.safetyhub",
        "com.android.emergency",
        "com.android.safetycenter.resources",
        // Credential / security infrastructure must never be intercepted.
        "com.google.android.gms",
        "com.samsung.android.samsungpass",
        "com.samsung.android.authfw"
    )

    /**
     * Known finance, payment, authentication and password-manager families.
     * This is intentionally conservative: false negatives would reintroduce
     * friction in apps where interruption is unacceptable.
     */
    private val sensitivePackagePrefixes = setOf(
        // Banking / finance / payment
        "com.barclays.",
        "uk.co.hsbc.",
        "com.hsbc.",
        "com.rbs.",
        "com.natwest.",
        "com.lloyds.",
        "com.halifax.",
        "com.starlingbank.",
        "co.uk.getmondo",
        "com.monzo.",
        "com.revolut.",
        "com.chase.",
        "com.jpmorgan.",
        "com.citi.",
        "com.bnpparibas.",
        "com.societegenerale.",
        "fr.creditagricole.",
        "fr.lcl.",
        "com.boursorama.",
        "de.number26.",
        "com.n26.",
        "com.paypal.",
        "com.transferwise.",
        "com.wise.",
        "com.klarna.",
        "com.coinbase.",
        "com.binance.",
        "com.crypto.",
        "com.americanexpress.",
        "com.capitalone.",
        "com.santander.",
        "uk.co.tsb.",
        "com.firstdirect.",
        "com.bunq.",
        "com.sumup.",
        "com.stripe.",
        "com.google.android.apps.wallet",
        "com.samsung.android.spay",
        // Authentication / password / identity protection
        "com.google.android.apps.authenticator",
        "com.microsoft.authenticator",
        "com.azure.authenticator",
        "com.x8bit.bitwarden",
        "com.onepassword.",
        "com.agilebits.onepassword",
        "com.dashlane",
        "com.lastpass.",
        "com.authy.",
        "com.beemdevelopment.aegis",
        "io.ente.auth",
        "com.keepersecurity.",
        "com.callpod.android_apps.keeper"
    )

    private val sensitivePackageFragments = setOf(
        "banking",
        "mobilebank",
        "mobile_banking",
        "mobilebanking",
        "authenticator",
        "passwordmanager",
        "password_manager",
        "identitycheck",
        "identity_check",
        "digitalid",
        "digital_id",
        "securetoken",
        "secure_token"
    )

    private val sensitiveLabelMarkers = setOf(
        "bank",
        "banking",
        "banque",
        "banco",
        "banca",
        "credit union",
        "credit card",
        "finance",
        "financial",
        "fintech",
        "investment",
        "investments",
        "trading",
        "broker",
        "mortgage",
        "insurance",
        "assurance",
        "crypto",
        "cryptocurrency",
        "wallet",
        "payment",
        "payments",
        "paiement",
        "mobile money",
        "authenticator",
        "authentication",
        "password manager",
        "passwords",
        "passkey",
        "passkeys",
        "identity",
        "identity verification",
        "verify identity",
        "identite",
        "id check",
        "digital id",
        "mobile id",
        "security",
        "security key",
        "secure token",
        "antivirus",
        "firewall",
        "vpn",
        "2fa",
        "otp"
    )

    private val defaultAwarenessApps = setOf(
        "com.google.android.youtube",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically"
    )

    val defaultPackages: Set<String> =
        defaultAwarenessApps + BrowserDetector.supportedPackages

    fun isAlwaysAllowed(packageName: String): Boolean =
        packageName in alwaysAllowed ||
            sensitivePackagePrefixes.any { prefix ->
                packageName.lowercase(Locale.ROOT).startsWith(prefix)
            }

    fun isAlwaysAllowed(context: Context, packageName: String): Boolean {
        if (isAlwaysAllowed(packageName)) return true

        val defaultDialer = synchronized(sensitiveDecisionCache) {
            if (!defaultDialerLoaded) {
                cachedDefaultDialer = runCatching {
                    context.getSystemService(TelecomManager::class.java)
                        ?.defaultDialerPackage
                }.getOrNull()
                defaultDialerLoaded = true
            }
            cachedDefaultDialer
        }
        if (packageName == defaultDialer) return true

        return isSensitiveCategory(context, packageName)
    }

    fun isSensitiveCategory(context: Context, packageName: String): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        if (sensitivePackagePrefixes.any { packageLower.startsWith(it) }) return true
        if (sensitivePackageFragments.any { packageLower.contains(it) }) return true

        synchronized(sensitiveDecisionCache) {
            sensitiveDecisionCache[packageName]?.let { return it }
        }

        val label = runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault("")

        val sensitive = looksSensitive(packageName, label)
        synchronized(sensitiveDecisionCache) {
            sensitiveDecisionCache[packageName] = sensitive
        }
        return sensitive
    }

    fun clearClassificationCache() {
        synchronized(sensitiveDecisionCache) {
            sensitiveDecisionCache.clear()
            cachedDefaultDialer = null
            defaultDialerLoaded = false
        }
    }

    internal fun looksSensitive(packageName: String, label: String): Boolean {
        val packageLower = packageName.lowercase(Locale.ROOT)
        if (sensitivePackagePrefixes.any { packageLower.startsWith(it) }) return true
        if (sensitivePackageFragments.any { packageLower.contains(it) }) return true

        val normalizedLabel = normalizeForMatching(label)
        if (normalizedLabel.isBlank()) return false

        return sensitiveLabelMarkers.any { marker ->
            val normalizedMarker = normalizeForMatching(marker)
            containsWordOrPhrase(normalizedLabel, normalizedMarker)
        }
    }

    private fun normalizeForMatching(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    private fun containsWordOrPhrase(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        return (" $haystack ").contains(" $needle ")
    }

    /**
     * Defense-in-depth boundary for any persistence/logging layer.
     * Out-of-scope packages must not be associated with Safeguard state.
     */
    fun shouldNeverPersist(context: Context, packageName: String): Boolean =
        isAlwaysAllowed(context, packageName)

    fun isSystemProtected(packageName: String): Boolean =
        packageName == ANDROID_SETTINGS

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (isSystemProtected(packageName)) return true
        if (isAlwaysAllowed(context, packageName)) return false

        // Web coverage is deliberately limited to the eight supported browsers.
        if (BrowserDetector.isBrowser(context, packageName)) return true

        return packageName in GuardPrefs.protectedPackages(context)
    }
}
