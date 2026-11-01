package dev.brua

import java.util.Locale

/** The headers an app sends to its own backend, which forwards them to the delivery API (SPEC.md 7.5). */
public object BruaHints {
    /** The contract version this SDK implements (SPEC.md 14). */
    public const val CONTRACT_VERSION: String = "1"

    /**
     * Accept-Language, the color scheme and reduced motion hints, and the contract version.
     * Leave [colorScheme] null if the app follows the system appearance live, so the backend returns both schemes.
     * [languages] are BCP 47 tags in the app's order of preference.
     */
    public fun headers(
        colorScheme: ColorScheme?,
        reducedMotion: Boolean,
        languages: List<String> = listOf(Locale.getDefault().toLanguageTag()),
    ): Map<String, String> = buildMap {
        put("Accept-Language", acceptLanguage(languages))
        if (colorScheme != null) put("Sec-CH-Prefers-Color-Scheme", "\"${colorScheme.value}\"")
        put("Sec-CH-Prefers-Reduced-Motion", if (reducedMotion) "\"reduce\"" else "\"no-preference\"")
        put("Asset-Contract", CONTRACT_VERSION)
    }

    /** "nb-NO, en;q=0.9": the app's languages in order, with falling quality values. */
    internal fun acceptLanguage(languages: List<String>): String =
        languages.take(5).mapIndexed { index, language ->
            if (index == 0) language else "$language;q=${String.format(Locale.ROOT, "%.1f", 1.0 - index * 0.1)}"
        }.joinToString(", ")
}
