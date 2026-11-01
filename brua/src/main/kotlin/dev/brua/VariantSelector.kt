package dev.brua

/** The variant selection rule (SPEC.md 7.3), tested against the shared fixtures in `fixtures/selection`. */
public object VariantSelector {
    private val knownColorSchemes = setOf("light", "dark")
    private val knownReducedMotion = setOf("reduce")

    /** The URL to load: the chosen variant's, or the asset's single URL when no variant fits. */
    public fun url(asset: AssetResponse, client: ClientState): String = select(asset, client)?.url ?: asset.url

    /** The variant to load, or null when no file suits the client; the single URL still does. */
    public fun select(asset: AssetResponse, client: ClientState): AssetVariant? {
        // 1. Usable: a supported type, and no value this client doesn't know.
        var candidates = asset.variants.filter { variant ->
            variant.type in client.supportedTypes &&
                (variant.prefersColorScheme == null || variant.prefersColorScheme in knownColorSchemes) &&
                (variant.prefersReducedMotion == null || variant.prefersReducedMotion in knownReducedMotion)
        }

        // 2. Motion.
        val reduced = candidates.filter { it.prefersReducedMotion == "reduce" }
        val moving = candidates.filter { it.prefersReducedMotion == null }
        if (client.reducedMotion) {
            if (reduced.isNotEmpty()) candidates = reduced
        } else if (moving.isNotEmpty()) {
            candidates = moving
        }

        // 3. Color scheme.
        candidates = firstNonEmpty(
            candidates.filter { it.prefersColorScheme == client.colorScheme.value },
            candidates.filter { it.prefersColorScheme == null },
            candidates.filter { it.prefersColorScheme == "light" },
            candidates,
        )

        // 4. Language.
        candidates = firstNonEmpty(
            candidates.filter { it.lang != null && it.lang == asset.lang },
            candidates.filter { it.lang == null },
            candidates,
        )

        // 5. Type.
        val type = client.supportedTypes.firstOrNull { type -> candidates.any { it.type == type } } ?: return null
        candidates = candidates.filter { it.type == type }

        // 6. Size.
        if (candidates.any { it.scale != null }) {
            return pickAtLeast(candidates, { (it.scale ?: 1).toDouble() }, client.scale)
        }
        if (candidates.any { it.pixelWidth != null }) {
            val need = client.targetWidth?.let { it * client.scale } ?: Double.POSITIVE_INFINITY
            return pickAtLeast(candidates, { (it.pixelWidth ?: 0).toDouble() }, need)
        }
        return candidates.first()
    }

    private fun firstNonEmpty(vararg lists: List<AssetVariant>): List<AssetVariant> =
        lists.firstOrNull { it.isNotEmpty() } ?: emptyList()

    /** The smallest value at or above [target], else the largest. */
    private fun pickAtLeast(variants: List<AssetVariant>, value: (AssetVariant) -> Double, target: Double): AssetVariant {
        val sorted = variants.sortedBy(value)
        return sorted.firstOrNull { value(it) >= target } ?: sorted.last()
    }
}
