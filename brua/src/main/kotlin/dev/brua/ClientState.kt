package dev.brua

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What the selection rule needs to know about the device and the view. */
@Serializable
public data class ClientState(
    /** Media types this app can render, most preferred first. */
    val supportedTypes: List<String> = DEFAULT_TYPES,
    val colorScheme: ColorScheme = ColorScheme.Light,
    val reducedMotion: Boolean = false,
    /** Device pixels per dp. */
    val scale: Double = 1.0,
    /** Displayed width in dp, for flexible assets. */
    val targetWidth: Double? = null,
) {
    public companion object {
        /**
         * The raster formats current platforms decode. `brua-android` leaves AVIF out before Android 12,
         * and apps that render animations add their types.
         */
        public val DEFAULT_TYPES: List<String> = listOf("image/avif", "image/webp", "image/png", "image/jpeg")
    }
}

@Serializable
public enum class ColorScheme(public val value: String) {
    @SerialName("light")
    Light("light"),

    @SerialName("dark")
    Dark("dark"),
}
