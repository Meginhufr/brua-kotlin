package dev.brua

import kotlinx.serialization.json.Json

/**
 * Where an app reads its organization's assets by id: the edge, with no key and no backend (SPEC.md 5.3).
 * [url] is the JSON URL of an asset's current release and [parse] decodes what it answers; the Android module's
 * `BruaClient` does the fetching. A backend passes ids on to its clients and never needs to.
 *
 * @param base the organization's asset prefix on the edge, such as `https://cdn.brua.dev/assets/{org}`.
 * @param lang the language for alt text and language-specific files; null lets the device's languages decide.
 */
public class BruaEdge(base: String, public val lang: String? = null) {
    private val base = base.trimEnd('/')

    public fun url(id: String): String = "$base/$id.json" + (lang?.let { "?lang=$it" } ?: "")

    public fun parse(json: String): AssetResponse = Json.decodeFromString(json)
}
