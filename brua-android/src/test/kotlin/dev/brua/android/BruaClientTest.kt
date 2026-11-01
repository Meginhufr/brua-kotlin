package dev.brua.android

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/** Answers the icon's URL with its JSON, anything else with 404, and remembers what was asked for. */
internal class FakeEdge : Interceptor {
    val requests = mutableListOf<String>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requests += request.url.toString()
        val known = request.url.encodedPath.endsWith("/icon.json")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (known) 200 else 404)
            .message(if (known) "OK" else "Not Found")
            .body((if (known) ICON else "Unknown asset").toResponseBody("application/json".toMediaType()))
            .build()
    }

    fun client(): OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

    private companion object {
        const val ICON = """{"id":"icon","version":3,"release":12,"url":"https://cdn.example/assets/org/icon","width":24,"height":24,"alt":"Settings","lang":"en",
            "variants":[{"type":"image/png","url":"https://cdn.example/icon/light@1x.png","scale":1},{"type":"image/png","url":"https://cdn.example/icon/dark@1x.png","prefersColorScheme":"dark","scale":1}]}"""
    }
}

class BruaClientTest {
    @Test
    fun an_id_is_read_from_the_edge_with_the_language_in_the_url_and_an_unknown_one_is_null() = runBlocking {
        val edge = FakeEdge()
        val client = BruaClient("https://cdn.example/assets/org/", lang = "nb", http = edge.client())

        val icon = client.asset("icon")
        val missing = client.asset("nothing-released")

        assertEquals("Settings", icon?.alt)
        assertEquals(2, icon?.variants?.size)
        assertNull(missing)
        assertEquals(listOf("https://cdn.example/assets/org/icon.json?lang=nb", "https://cdn.example/assets/org/nothing-released.json?lang=nb"), edge.requests)
    }
}
