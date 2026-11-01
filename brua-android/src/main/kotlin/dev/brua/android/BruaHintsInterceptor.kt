package dev.brua.android

import android.content.Context
import dev.brua.BruaHints
import dev.brua.ColorScheme
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the hints (SPEC.md 7.5) to every request of the OkHttp client that calls the app's own backend.
 * [colorScheme] is asked per request; it returns null when the app follows the system appearance live,
 * so the backend returns both schemes.
 */
public class BruaHintsInterceptor(
    context: Context,
    private val colorScheme: () -> ColorScheme? = { null },
) : Interceptor {
    private val context = context.applicationContext

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        BruaHints.headers(colorScheme(), AndroidClient.reducedMotion(context), AndroidClient.languages())
            .forEach { (name, value) -> request.header(name, value) }
        return chain.proceed(request.build())
    }
}
