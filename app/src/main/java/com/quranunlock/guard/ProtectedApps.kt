package com.applicreation0.quransafeguard

import android.content.Context
import android.provider.Settings
import android.telecom.TelecomManager
import java.text.Normalizer
import java.util.Locale

enum class SafeguardTargetCategory {
    SOCIAL,
    BROWSER
}

data class SafeguardTarget(
    val label: String,
    val packageName: String,
    val category: SafeguardTargetCategory
)

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
        // System input methods are transient windows, never awareness targets.
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.android.inputmethod.latin",
        "com.touchtype.swiftkey",
        "com.microsoft.swiftkey",
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

    val socialTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("YouTube", "com.google.android.youtube", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("WhatsApp", "com.whatsapp", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Telegram", "org.telegram.messenger", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Discord", "com.discord", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Reddit", "com.reddit.frontpage", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Snapchat", "com.snapchat.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Instagram", "com.instagram.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("Facebook", "com.facebook.katana", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("X", "com.twitter.android", SafeguardTargetCategory.SOCIAL),
        SafeguardTarget("TikTok", "com.zhiliaoapp.musically", SafeguardTargetCategory.SOCIAL)
    )

    val browserTargets: List<SafeguardTarget> = listOf(
        SafeguardTarget("Chrome", "com.android.chrome", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Firefox", "org.mozilla.firefox", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Microsoft Edge", "com.microsoft.emmx", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Brave", "com.brave.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Opera", "com.opera.browser", SafeguardTargetCategory.BROWSER),
        SafeguardTarget("Samsung Internet", "com.sec.android.app.sbrowser", SafeguardTargetCategory.BROWSER)
    )

    val selectableTargets: List<SafeguardTarget> = socialTargets + browserTargets
    val selectableScopePackages: Set<String> = selectableTargets.map { it.packageName }.toSet()
    val defaultPackages: Set<String> = selectableScopePackages

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

        val defaultInputMethodPackage = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )?.substringBefore('/')
        }.getOrNull()
        if (packageName == defaultInputMethodPackage) return true

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
    fun isSystemProtected(packageName: String): Boolean =
        packageName == ANDROID_SETTINGS

    fun isSelectableTarget(packageName: String): Boolean =
        packageName in selectableScopePackages

    /**
     * Fixed application scope: social/communication targets + the six supported browsers.
     * Anything else must never acquire Safeguard session/history state.
     */
    fun shouldNeverPersist(context: Context, packageName: String): Boolean =
        packageName != context.packageName &&
            !isSystemProtected(packageName) &&
            !isSelectableTarget(packageName)

    fun eventScopePackages(context: Context): Set<String> =
        GuardPrefs.protectedPackages(context) + ANDROID_SETTINGS + context.packageName

    fun isEventScopePackage(context: Context, packageName: String): Boolean =
        packageName in eventScopePackages(context)

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (isSystemProtected(packageName)) return true
        return packageName in GuardPrefs.protectedPackages(context)
    }
}
