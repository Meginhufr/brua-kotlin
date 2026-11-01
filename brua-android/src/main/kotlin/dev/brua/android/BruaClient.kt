package dev.brua.android

import dev.brua.AssetResponse
import dev.brua.BruaEdge
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Reads the current release of an asset from the edge by its id, through OkHttp: the [AssetResponse] at
 * `{base}/{id}.json`, with no key and no backend (SPEC.md 5.3). Give the client a cache and it honours the edge's
 * minute, so a screen with a hundred references costs one small request per asset, and a merge still lands
 * within the minute.
 */
public class BruaClient(
    public val edge: BruaEdge,
    private val http: OkHttpClient = OkHttpClient(),
) {
    public constructor(base: String, lang: String? = null, http: OkHttpClient = OkHttpClient()) : this(BruaEdge(base, lang), http)

    /** The asset's current release, or null when the id is unknown or nothing is released. */
    public suspend fun asset(id: String): AssetResponse? = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(edge.url(id)).build()).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw IOException("Brua: $id answered ${response.code}")
                else -> edge.parse(response.body.string())
            }
        }
    }
}
