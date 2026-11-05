package dev.brua.android

import dev.brua.BruaEdge
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Answers the icon's URL with its JSON, anything else with 404, with the edge's minute, and remembers what was asked
 * for. [down] makes every request fail as a lost connection.
 */
internal class FakeEdge : Interceptor {
    val requests = mutableListOf<String>()

    @Volatile
    var down = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        synchronized(requests) { requests += request.url.toString() }
        if (down) throw IOException("No connection")
        val known = request.url.encodedPath.endsWith("/icon.json")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(if (known) 200 else 404)
            .message(if (known) "OK" else "Not Found")
            .header("Cache-Control", "public, max-age=60")
            .body((if (known) ICON else "Unknown asset").toResponseBody("application/json".toMediaType()))
            .build()
    }

    fun client(): OkHttpClient = OkHttpClient.Builder().addInterceptor(this).build()

    private companion object {
        const val ICON = """{"id":"icon","version":3,"release":12,"url":"https://cdn.example/assets/org/icon","width":24,"height":24,"alt":"Settings","lang":"en",
            "variants":[{"type":"image/png","url":"https://cdn.example/icon/light@1x.png","scale":1},{"type":"image/png","url":"https://cdn.example/icon/dark@1x.png","prefersColorScheme":"dark","scale":1}]}"""
    }
}

/** A clock the test moves by hand. */
private class Clock {
    @Volatile
    var now = 1_800_000_000_000L
}

private fun client(edge: FakeEdge, lang: String? = "nb", clock: Clock = Clock(), languages: List<String> = listOf("nb-NO", "en")) =
    BruaClient(BruaEdge("https://cdn.example/assets/org/", lang), edge.client(), { languages }, { clock.now })

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

    @Test
    fun without_a_language_the_url_carries_the_devices_languages() {
        val edge = FakeEdge()
        assertEquals("https://cdn.example/assets/org/icon.json?lang=nb-NO%2C%20en%3Bq%3D0.9", client(edge, lang = null).url("icon"))
    }

    @Test
    fun a_release_under_a_minute_old_is_answered_from_memory_and_one_older_is_read_again() = runBlocking {
        val edge = FakeEdge()
        val clock = Clock()
        val client = client(edge, clock = clock)

        assertNull(client.cachedAsset("icon"))
        client.asset("icon")
        clock.now += 30_000
        client.asset("icon")
        assertEquals(1, edge.requests.size)
        assertEquals("Settings", client.cachedAsset("icon")?.alt)

        clock.now += 31_000
        client.asset("icon")
        assertEquals(2, edge.requests.size)
    }

    @Test
    fun an_id_with_nothing_released_is_remembered_for_the_minute_too() = runBlocking {
        val edge = FakeEdge()
        val client = client(edge)

        client.asset("nothing-released")
        client.asset("nothing-released")

        assertEquals(1, edge.requests.size)
    }

    @Test
    fun reads_of_one_id_at_the_same_time_share_one_request() = runBlocking {
        val edge = FakeEdge()
        val client = client(edge)

        val first = async { client.asset("icon") }
        val second = async { client.asset("icon") }

        assertEquals(first.await(), second.await())
        assertEquals(1, edge.requests.size)
    }

    @Test
    fun a_stale_release_is_served_when_the_refresh_fails_and_without_one_the_error_comes_through() = runBlocking<Unit> {
        val edge = FakeEdge()
        val clock = Clock()
        val client = client(edge, clock = clock)
        client.asset("icon")

        edge.down = true
        clock.now += 120_000

        assertEquals("Settings", client.asset("icon")?.alt)
        assertFailsWith<IOException> { client.asset("never-read") }
    }

    @Test
    fun prefetch_reads_every_id_once_so_they_are_in_memory() = runBlocking {
        val edge = FakeEdge()
        val client = client(edge)

        client.prefetch(listOf("icon", "icon", "nothing-released"))

        assertEquals("Settings", client.cachedAsset("icon")?.alt)
        assertEquals(2, edge.requests.size)
    }
}
