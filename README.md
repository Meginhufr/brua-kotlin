# Brua for Kotlin

Two modules: `dev.brua:brua` is plain Kotlin for the JVM, with the `AssetResponse` models, the variant selection rule and the request hints, for Android apps and Kotlin backends alike. `dev.brua:brua-android` adds the Android side: a `BruaImage` composable for Jetpack Compose, the device's state and an OkHttp interceptor. Android 7 (API 24) or later.

The modules are on Maven Central:

```kotlin
implementation("dev.brua:brua-android:0.1.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")   // or coil-network-ktor3: Coil needs one
```

A backend takes `dev.brua:brua` alone. To build from source instead, `includeBuild("path/to/brua-kotlin")` in your `settings.gradle.kts` resolves the same coordinates.

## From an id

Your backend passes asset ids on, and the app reads each asset's current release from the edge with no key (SPEC.md 5.3). One client per app, with your organization's asset prefix; give its OkHttp client a cache and each asset is kept for the edge's minute, so a merge in Brua reaches the screen within that minute with no release of yours:

```kotlin
import dev.brua.android.BruaClient

val brua = BruaClient("https://cdn.brua.dev/assets/<your organization id>", http = okHttpClientWithCache)
val asset = brua.asset("gift-theme-christmas-header")   // null when nothing is released under the id
```

`dev.brua.BruaEdge` gives the URL and the decoding on the plain JVM; a backend passes ids on and never needs to fetch.

## Or embedded by your backend

A service that wants a frozen snapshot, for email, push or print, calls the delivery API and embeds the asset in its own response (SPEC.md 7.1). The models are `@Serializable` data classes that decode with any `Json`, on their own or inside your own types, and a field the contract gains later is ignored:

```kotlin
@Serializable
data class GiftTheme(val title: String, val thumbnail: AssetResponse)
```

## Compose

```kotlin
import dev.brua.android.BruaImage

BruaImage(id = theme.thumbnailId, client = brua, Modifier.width(160.dp))   // nothing until it has loaded
BruaImage(theme.thumbnail, Modifier.width(160.dp))
```

The image follows dark mode, the Remove animations setting and the display density as they change, the alt text becomes the TalkBack description, and decorative assets are skipped by TalkBack. Without a size, it takes the asset's designed size in dp, shrunk to the width it gets. Animations show their still.

Pass `colorScheme` when the app switches themes itself:

```kotlin
BruaImage(brand.mark, Modifier.size(28.dp), colorScheme = if (dark) ColorScheme.Dark else ColorScheme.Light)
```

Files load through Coil's singleton `ImageLoader`, or the one you pass as `imageLoader`. Android decodes AVIF from Android 12, and WebP, PNG and JPEG everywhere; `AndroidClient.supportedTypes` says which. To use an asset's SVG files, add `coil-svg`, register `SvgDecoder` in the loader and pass `supportedTypes = listOf("image/svg+xml") + AndroidClient.supportedTypes`. Lottie and Rive are rendered by their own libraries: add their type to `supportedTypes` and give those variants to the animation view.

## One URL

For a notification, a widget or a custom loader, pick the variant yourself:

```kotlin
val url = VariantSelector.url(theme.thumbnail, ClientState(colorScheme = ColorScheme.Dark, scale = 3.0, targetWidth = 160.0))
```

`ClientState` says what the client renders and under which conditions; the rule is the one every Brua SDK and the edge run (SPEC.md 7.3).

## Hints

Add the interceptor to the OkHttp client that calls your backend, which forwards the headers to the delivery API (SPEC.md 7.5):

```kotlin
OkHttpClient.Builder()
    .addInterceptor(BruaHintsInterceptor(context))
    .build()
```

It sends `Accept-Language` from the device's languages, the reduced motion hint, and the contract version. Give it a `colorScheme` lambda if the app doesn't follow the system appearance live; otherwise the backend returns both schemes and `BruaImage` switches without a new request (SPEC.md 5.6). On the JVM, `BruaHints.headers(...)` gives the same headers as a map for any HTTP client.

## Tests

```bash
JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home ./gradlew test
```

needs a JDK 17 or later and the Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`). `brua` runs the shared fixtures in `fixtures/selection`; `brua-android` renders `BruaImage` under Robolectric and checks which file it asks for.
