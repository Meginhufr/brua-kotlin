package dev.brua.android

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.compose.LocalPlatformContext
import dev.brua.AssetResponse
import dev.brua.BruaLayout
import dev.brua.ClientState
import dev.brua.ColorScheme
import dev.brua.ObjectFit
import dev.brua.VariantSelector
import java.io.IOException

/**
 * Shows a Brua asset in Compose: the variant follows dark mode, the Remove animations setting and the display
 * density as they change, alt text becomes the TalkBack description, and decorative assets are skipped by TalkBack.
 * Animations show their still; apps render animation types themselves (SPEC.md 7.2).
 *
 * Without a size from [modifier], the image takes the asset's designed size in dp, shrunk to the width available.
 * Pass [colorScheme] when the app switches themes itself; left null, the system appearance decides.
 *
 * The asset's `objectFit` decides the shape: `contain` keeps the asset's proportions ([ContentScale.Fit]); `cover`
 * fills the box [modifier] gives it ([ContentScale.Crop]) and crops around the asset's `objectPosition`, with a file
 * sharp enough for the size it is drawn at. [contentScale] and [alignment] override the asset's.
 *
 * [loading] and [error] draw the file's loading and failure, laid out where the picture goes; left out, [placeholder]
 * shows while the file loads and nothing on failure. They subcompose, so leave them out where a plain placeholder will do.
 */
@Composable
public fun BruaImage(
    asset: AssetResponse,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme? = null,
    supportedTypes: List<String> = AndroidClient.supportedTypes,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
    contentScale: ContentScale? = null,
    placeholder: Painter? = ColorPainter(Color.Gray.copy(alpha = 0.15f)),
    alignment: Alignment? = null,
    loading: (@Composable () -> Unit)? = null,
    error: (@Composable (BruaImageError) -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    val scale = contentScale ?: if (BruaLayout.objectFit(asset) == ObjectFit.Cover) ContentScale.Crop else ContentScale.Fit
    val (x, y) = BruaLayout.objectPosition(asset)
    val placement = alignment ?: BiasAlignment(x * 2 - 1, y * 2 - 1)
    val dark = colorScheme?.let { it == ColorScheme.Dark } ?: isSystemInDarkTheme()
    val reducedMotion = rememberReducedMotion()
    val ratio = asset.width.toFloat() / maxOf(asset.height, 1)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // A crop fills the box it is given, when the box has a height; otherwise the asset's proportions hold it.
        val fills = scale == ContentScale.Crop && constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val width = when {
            fills || constraints.hasFixedWidth -> maxWidth
            constraints.hasBoundedWidth -> minOf(maxWidth, asset.width.dp)
            else -> asset.width.dp
        }
        val drawnWidth = if (fills) {
            BruaLayout.targetWidth(asset, maxWidth.value.toDouble(), maxHeight.value.toDouble(), ObjectFit.Cover.value)
        } else {
            width.value.toDouble()
        }
        val url = VariantSelector.url(
            asset,
            ClientState(
                supportedTypes = supportedTypes,
                colorScheme = if (dark) ColorScheme.Dark else ColorScheme.Light,
                reducedMotion = reducedMotion,
                scale = density.toDouble(),
                targetWidth = drawnWidth,
            ),
        )
        val box = if (fills) Modifier.fillMaxSize() else Modifier.widthIn(max = width).aspectRatio(ratio)
        if (loading == null && error == null) {
            AsyncImage(
                model = url,
                contentDescription = asset.alt.takeUnless { asset.isDecorative },
                imageLoader = imageLoader,
                modifier = box,
                placeholder = placeholder,
                contentScale = scale,
                alignment = placement,
            )
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = asset.alt.takeUnless { asset.isDecorative },
                imageLoader = imageLoader,
                modifier = box,
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { loading?.invoke() } },
                success = { SubcomposeAsyncImageContent() },
                error = { state ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { error?.invoke(BruaImageError.File(url, state.result.throwable)) }
                },
                contentScale = scale,
                alignment = placement,
            )
        }
    }
}

/**
 * The asset with this id in its current release, read from the edge through [client], so a merge reaches this image
 * within a minute with no backend involved. A release the client has read before draws on the first frame, so an
 * item scrolled back into view doesn't blink. Until the asset is known, [loading] shows in [modifier]'s box; when
 * nothing is released or the edge can't be reached, [error] does, with the reason. Left out, nothing shows then.
 */
@Composable
public fun BruaImage(
    id: String,
    client: BruaClient,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme? = null,
    supportedTypes: List<String> = AndroidClient.supportedTypes,
    imageLoader: ImageLoader = SingletonImageLoader.get(LocalPlatformContext.current),
    contentScale: ContentScale? = null,
    placeholder: Painter? = ColorPainter(Color.Gray.copy(alpha = 0.15f)),
    alignment: Alignment? = null,
    loading: (@Composable () -> Unit)? = null,
    error: (@Composable (BruaImageError) -> Unit)? = null,
) {
    val state by produceState(BruaAssetState.of(client.cachedAsset(id)), id, client) {
        BruaAssetState.of(client.cachedAsset(id))?.let { value = it }
        value = BruaAssetState.read(id, client)
    }
    when (val current = state) {
        is BruaAssetState.Loaded ->
            BruaImage(current.asset, modifier, colorScheme, supportedTypes, imageLoader, contentScale, placeholder, alignment, loading, error)
        is BruaAssetState.Failed -> if (error != null) Box(modifier, contentAlignment = Alignment.Center) { error(current.error) }
        null -> if (loading != null) Box(modifier, contentAlignment = Alignment.Center) { loading() }
    }
}

/** How far a [BruaImage] has got with the asset itself, before its file; null while it loads. */
internal sealed class BruaAssetState {
    data class Loaded(val asset: AssetResponse) : BruaAssetState()
    data class Failed(val error: BruaImageError) : BruaAssetState()

    companion object {
        fun of(remembered: AssetResponse?): BruaAssetState? = remembered?.let(::Loaded)

        /** The client keeps the last known release when the edge can't be reached, so an error means none is known. */
        suspend fun read(id: String, client: BruaClient): BruaAssetState = try {
            client.asset(id)?.let(::Loaded) ?: Failed(BruaImageError.NotReleased)
        } catch (e: IOException) {
            Failed(BruaImageError.Unreachable(e))
        }
    }
}

/** Whether "Remove animations" is on, updated when the setting changes. */
@Composable
public fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current.applicationContext
    var reduced by remember(context) { mutableStateOf(AndroidClient.reducedMotion(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = AndroidClient.reducedMotion(context)
            }
        }
        val setting = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(setting, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}
