package dev.brua

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

/**
 * The asset contract (SPEC.md 5): the HTML picture model as JSON. Decodes with any `Json`;
 * a field added to the contract later is ignored (SPEC.md 14).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
public data class AssetResponse(
    val id: String,
    val version: Int,
    val url: String,
    val width: Int,
    val height: Int,
    /** Empty for decorative assets, which TalkBack skips. */
    val alt: String,
    val lang: String? = null,
    val variants: List<AssetVariant> = emptyList(),
    /** How a layout's box of another shape holds the asset, as CSS object-fit: "contain" or "cover". */
    val objectFit: String? = null,
    /** Where it sits in the box, as CSS object-position: "50% 35%", the point a cover crop keeps in view. */
    val objectPosition: String? = null,
) {
    val isDecorative: Boolean get() = alt.isEmpty()
}

/** Value fields are strings, not enums, so a value added to the API later never breaks decoding (SPEC.md 14). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
public data class AssetVariant(
    val type: String,
    val url: String,
    val prefersColorScheme: String? = null,
    val prefersReducedMotion: String? = null,
    val lang: String? = null,
    val scale: Int? = null,
    val pixelWidth: Int? = null,
)
