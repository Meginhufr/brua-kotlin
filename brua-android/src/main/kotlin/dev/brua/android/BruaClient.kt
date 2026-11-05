package dev.brua.android

import dev.brua.AssetResponse
import dev.brua.BruaEdge
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reads the current release of an asset from the edge by its id, through OkHttp: the [AssetResponse] at
 * `{base}/{id}.json`, with no key and no backend (SPEC.md 5.3).
 *
 * The client remembers every release it has read. A release under the edge's minute old is answered from memory,
 * an older one is read again (OkHttp revalidates it with its ETag when the client has a cache), and when the edge
 * can't be reached the last known release is kept, so a merge lands within the minute and a screen still shows its
 * images offline. [cachedAsset] gives the last known release at once, for a composable's first frame.
 */
public class BruaClient internal constructor(
    public val edge: BruaEdge,
    private val http: OkHttpClient,
    private val languages: () -> List<String>,
    private val now: () -> Long,
) {
    public constructor(edge: BruaEdge, http: OkHttpClient = OkHttpClient()) : this(edge, http, { AndroidClient.languages() }, { System.currentTimeMillis() })

    public constructor(base: String, lang: String? = null, http: OkHttpClient = OkHttpClient()) : this(BruaEdge(base, lang), http)

    private class Entry(val asset: AssetResponse?, val freshUntil: Long)

    private val entries = ConcurrentHashMap<String, Entry>()
    private val reads = ConcurrentHashMap<String, CompletableDeferred<Entry>>()

    /** The JSON URL of the asset's current release; without a language it carries the device's. */
    public fun url(id: String): String = edge.url(id, if (edge.lang == null) languages() else emptyList())

    /**
     * The asset's current release, or null when the id is unknown or nothing is released. Reads of one id at the
     * same time share a request. Throws only when the edge can't be reached and no release of the id is known.
     */
    public suspend fun asset(id: String): AssetResponse? {
        val url = url(id)
        entries[url]?.let { if (now() < it.freshUntil) return it.asset }
        return try {
            load(url).asset
        } catch (e: IOException) {
            entries[url]?.let { return it.asset }
            throw e
        }
    }

    /** The last release of the id this client has read, however old, or null when it hasn't read one. */
    public fun cachedAsset(id: String): AssetResponse? = entries[url(id)]?.asset

    /** Reads these ids at the same time, so a list's images draw at once when its items appear. */
    public suspend fun prefetch(ids: List<String>): Unit = coroutineScope {
        ids.distinct().map { id ->
            async {
                try {
                    asset(id)
                } catch (e: IOException) {
                    null
                }
            }
        }.awaitAll()
    }

    /** Reads the URL, or joins the read of it already under way, and keeps what it returns. */
    private suspend fun load(url: String): Entry {
        val mine = CompletableDeferred<Entry>()
        reads.putIfAbsent(url, mine)?.let { return it.await() }
        try {
            val entry = read(url)
            entries[url] = entry
            mine.complete(entry)
            return entry
        } catch (e: Throwable) {
            mine.completeExceptionally(e)
            throw e
        } finally {
            reads.remove(url, mine)
        }
    }

    /**
     * The release at this URL, fresh for the response's max-age. Offline, OkHttp's last copy is used however old,
     * when the client has a cache, so an app started without a connection still has what it read before.
     */
    private suspend fun read(url: String): Entry = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            val cached = http.newCall(request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build()).execute()
            if (cached.code == 504) {
                cached.close()
                throw e
            }
            cached
        }
        response.use {
            val freshFor = it.cacheControl.maxAgeSeconds.takeIf { seconds -> seconds >= 0 } ?: 60
            val freshUntil = now() + freshFor * 1000L
            when {
                it.code == 404 -> Entry(null, freshUntil)
                !it.isSuccessful -> throw IOException("Brua: ${url.substringAfterLast('/')} answered ${it.code}")
                else -> Entry(edge.parse(it.body.string()), freshUntil)
            }
        }
    }
}
