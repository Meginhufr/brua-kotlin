package dev.brua.android

import dev.brua.AssetResponse
import dev.brua.AssetVariant

/** The same two assets the web and .NET SDKs' tests use: an icon with a dark override, and a photo with a width ladder. */
internal object Sample {
    val icon = AssetResponse(
        "icon", 3, "https://cdn.example/assets/icon/3", 24, 24, "Settings", "en",
        listOf(
            AssetVariant("image/png", "https://cdn.example/icon/light@1x.png", scale = 1),
            AssetVariant("image/png", "https://cdn.example/icon/light@2x.png", scale = 2),
            AssetVariant("image/avif", "https://cdn.example/icon/light@1x.avif", scale = 1),
            AssetVariant("image/avif", "https://cdn.example/icon/light@2x.avif", scale = 2),
            AssetVariant("image/png", "https://cdn.example/icon/dark@1x.png", prefersColorScheme = "dark", scale = 1),
            AssetVariant("image/png", "https://cdn.example/icon/dark@2x.png", prefersColorScheme = "dark", scale = 2),
            AssetVariant("image/avif", "https://cdn.example/icon/dark@2x.avif", prefersColorScheme = "dark", scale = 2),
        ),
    )

    val photo = AssetResponse(
        "hero", 1, "https://cdn.example/assets/hero/1", 1280, 720, "A harbour at dusk", null,
        listOf(
            AssetVariant("image/jpeg", "https://cdn.example/hero/320.jpg", pixelWidth = 320),
            AssetVariant("image/jpeg", "https://cdn.example/hero/640.jpg", pixelWidth = 640),
            AssetVariant("image/jpeg", "https://cdn.example/hero/1280.jpg", pixelWidth = 1280),
            AssetVariant("image/avif", "https://cdn.example/hero/320.avif", pixelWidth = 320),
            AssetVariant("image/avif", "https://cdn.example/hero/640.avif", pixelWidth = 640),
            AssetVariant("image/avif", "https://cdn.example/hero/1280.avif", pixelWidth = 1280),
        ),
    )

    val divider = AssetResponse(
        "divider", 1, "https://cdn.example/assets/divider/1", 200, 20, "", null,
        listOf(AssetVariant("image/png", "https://cdn.example/divider/200.png", pixelWidth = 200)),
    )
}
