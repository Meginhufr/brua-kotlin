package dev.brua

/** How a picture fills its box, as CSS object-fit names it. */
public enum class ObjectFit(public val value: String) {
    /** The whole picture, with space around it in a box of another shape. */
    Contain("contain"),

    /** The box filled, the rest of the picture cropped. */
    Cover("cover"),
}

/**
 * How a picture fills its box, the same on every platform (fixtures/sizing). [targetWidth] is the step before the
 * selection rule, so a cover picture gets a file sharp enough for the size it is drawn at.
 */
public object BruaLayout {
    private val position = Regex("^(100|[1-9]?[0-9])% (100|[1-9]?[0-9])%$")

    /** The fit to draw with: the override, else the asset's, else contain. Unknown values are contain. */
    public fun objectFit(asset: AssetResponse, override: String? = null): ObjectFit =
        if ((override ?: asset.objectFit) == ObjectFit.Cover.value) ObjectFit.Cover else ObjectFit.Contain

    /** Where the picture sits in its box, as fractions from 0 to 1: the override, else the asset's, else the centre. */
    public fun objectPosition(asset: AssetResponse, override: String? = null): Pair<Float, Float> {
        val match = position.matchEntire(override ?: asset.objectPosition ?: "") ?: return 0.5f to 0.5f
        return match.groupValues[1].toInt() / 100f to match.groupValues[2].toInt() / 100f
    }

    /**
     * How wide the picture is drawn in a box of this size, in dp, for [ClientState.targetWidth]. Contain, or a box
     * without a height yet, draws it at the box width; cover at the larger of the box width and the box height times
     * the asset's proportions.
     */
    public fun targetWidth(asset: AssetResponse, boxWidth: Double, boxHeight: Double? = null, objectFit: String? = null): Double {
        if (objectFit(asset, objectFit) != ObjectFit.Cover || boxHeight == null || boxHeight <= 0 || asset.height <= 0) return boxWidth
        return maxOf(boxWidth, boxHeight * asset.width / asset.height)
    }
}
