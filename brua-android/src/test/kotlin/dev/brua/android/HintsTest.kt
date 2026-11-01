package dev.brua.android

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.brua.ColorScheme
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HintsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun the_interceptor_adds_the_hints_to_every_request() {
        Locale.setDefault(Locale.forLanguageTag("nb-NO"))
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var sent: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(BruaHintsInterceptor(context) { ColorScheme.Dark })
            .addInterceptor(Interceptor { chain ->
                sent = chain.request()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                    .body("".toResponseBody()).build()
            })
            .build()

        client.newCall(Request.Builder().url("https://backend.example/api/pages/front").build()).execute().close()

        assertEquals("nb-NO", sent!!.header("Accept-Language"))
        assertEquals("\"dark\"", sent.header("Sec-CH-Prefers-Color-Scheme"))
        assertEquals("\"reduce\"", sent.header("Sec-CH-Prefers-Reduced-Motion"))
        assertEquals("1", sent.header("Asset-Contract"))
    }

    @Test
    fun reduced_motion_follows_the_animator_duration_scale() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        assertFalse(AndroidClient.reducedMotion(context))

        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertTrue(AndroidClient.reducedMotion(context))
    }

    @Test
    fun avif_is_supported_from_android_12() {
        assertEquals(listOf("image/avif", "image/webp", "image/png", "image/jpeg"), AndroidClient.supportedTypes(31))
        assertEquals(listOf("image/webp", "image/png", "image/jpeg"), AndroidClient.supportedTypes(30))
    }
}
