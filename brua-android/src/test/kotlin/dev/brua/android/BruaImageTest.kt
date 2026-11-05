package dev.brua.android

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ColorImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.onNodeWithText
import dev.brua.BruaEdge
import java.io.IOException
import dev.brua.ColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Fails every request, as a file that can't be loaded. */
private class Failing : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult = ErrorResult(null, chain.request, IOException("No such file"))
}

/** Answers every request with a flat color and remembers what was asked for. */
private class Recorder : Interceptor {
    val urls = mutableListOf<String>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        urls += chain.request.data.toString()
        return SuccessResult(ColorImage(0xFF888888.toInt()), chain.request, DataSource.MEMORY)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BruaImageTest {
    @get:Rule
    val rule = createComposeRule()

    private val recorder = Recorder()
    private val loader = ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>())
        .components { add(recorder) }
        .build()

    @Test
    fun a_fixed_asset_follows_the_color_scheme_and_the_display_density() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(3f)) {
                BruaImage(Sample.icon, Modifier.size(24.dp), colorScheme = ColorScheme.Dark, supportedTypes = listOf("image/png"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/icon/dark@2x.png", requested())
        rule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h800dp")
    fun a_flexible_asset_picks_the_width_that_covers_the_space_it_gets() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                BruaImage(Sample.photo, Modifier.width(300.dp), supportedTypes = listOf("image/avif", "image/jpeg"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/hero/640.avif", requested())
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h800dp")
    fun a_cover_asset_fills_its_box_with_a_file_for_the_size_it_is_drawn_at() {
        // A 16:9 photo filling a 200 dp square is drawn about 356 dp wide: 711 px at 2x, so the 1280 file.
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                BruaImage(Sample.photo.copy(objectFit = "cover", objectPosition = "30% 50%"), Modifier.size(200.dp),
                    supportedTypes = listOf("image/avif"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/hero/1280.avif", requested())
    }

    @Test
    @Config(sdk = [36], qualifiers = "w800dp-h800dp")
    fun contain_in_the_same_box_is_drawn_at_the_box_width() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                BruaImage(Sample.photo.copy(objectFit = "contain"), Modifier.size(200.dp), supportedTypes = listOf("image/avif"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/hero/640.avif", requested())
    }

    @Test
    @Config(sdk = [36], qualifiers = "night")
    fun the_system_appearance_decides_when_no_scheme_is_given() {
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BruaImage(Sample.icon, supportedTypes = listOf("image/png"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/icon/dark@1x.png", requested())
    }

    @Test
    fun an_id_is_read_from_the_edge_and_then_rendered() {
        val edge = FakeEdge()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BruaImage(id = "icon", client = BruaClient("https://cdn.example/assets/org", http = edge.client()), colorScheme = ColorScheme.Light, supportedTypes = listOf("image/png"), imageLoader = loader)
            }
        }

        assertEquals("https://cdn.example/icon/light@1x.png", requested())
        assertEquals(listOf("https://cdn.example/assets/org/icon.json?lang=en-US"), edge.requests, "the device's languages, so each has its own cache entry")
        rule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun a_decorative_asset_has_no_description_for_talkback() {
        rule.setContent {
            BruaImage(Sample.divider, imageLoader = loader)
        }

        requested()
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)).assertCountEquals(0)
    }

    @Composable
    private fun Reason(error: BruaImageError) = BasicText(
        when (error) {
            BruaImageError.NotReleased -> "not released"
            is BruaImageError.Unreachable -> "unreachable"
            is BruaImageError.File -> "file failed: ${error.url}"
        },
    )

    private fun client(edge: FakeEdge) = BruaClient(BruaEdge("https://cdn.example/assets/org/", "en"), edge.client(), { emptyList() }, { 0L })

    @Test
    fun an_id_with_nothing_released_shows_the_error_with_its_reason() {
        val edge = FakeEdge()
        rule.setContent {
            BruaImage("nothing-released", client(edge), Modifier.size(40.dp), imageLoader = loader,
                loading = { BasicText("loading") }, error = { Reason(it) })
        }

        rule.waitUntil(5_000) { rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("not released")))).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("not released").assertExists()
    }

    @Test
    fun an_edge_that_cant_be_reached_shows_the_error_as_unreachable() {
        val edge = FakeEdge().apply { down = true }
        rule.setContent {
            BruaImage("icon", client(edge), Modifier.size(40.dp), imageLoader = loader, error = { Reason(it) })
        }

        rule.waitUntil(5_000) { rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("unreachable")))).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun a_file_that_fails_shows_the_error_with_its_url() {
        val failing = ImageLoader.Builder(ApplicationProvider.getApplicationContext<Context>()).components { add(Failing()) }.build()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                BruaImage(Sample.icon, Modifier.size(24.dp), supportedTypes = listOf("image/png"), imageLoader = failing, error = { Reason(it) })
            }
        }

        rule.waitUntil(5_000) { rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("file failed: https://cdn.example/icon/light@1x.png")))).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun requested(): String {
        rule.waitUntil(5_000) { recorder.urls.isNotEmpty() }
        return recorder.urls.single()
    }
}
