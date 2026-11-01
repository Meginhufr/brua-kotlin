package dev.brua.android

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import dev.brua.BruaHints
import dev.brua.ClientState

/** What this device renders and prefers, for [ClientState] and [BruaHints]. */
public object AndroidClient {
    /** AVIF decodes from Android 12; WebP, PNG and JPEG everywhere. Apps that render animations add their types. */
    public val supportedTypes: List<String> = supportedTypes(Build.VERSION.SDK_INT)

    internal fun supportedTypes(sdk: Int): List<String> =
        if (sdk >= Build.VERSION_CODES.S) ClientState.DEFAULT_TYPES else ClientState.DEFAULT_TYPES - "image/avif"

    /** True when "Remove animations" is on in the accessibility settings, which sets the animator duration scale to zero. */
    public fun reducedMotion(context: Context): Boolean =
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

    /** The user's languages in order, as BCP 47 tags, for [BruaHints.headers]. */
    public fun languages(): List<String> {
        val locales = LocaleList.getDefault()
        return List(locales.size()) { locales[it].toLanguageTag() }
    }
}
