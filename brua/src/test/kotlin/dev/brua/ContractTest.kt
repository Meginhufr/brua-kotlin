package dev.brua

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class ContractTest {
    @Test
    fun fields_added_to_the_contract_later_are_ignored_with_a_plain_json() {
        val json = """{"id":"a","version":1,"url":"https://cdn.test/a/1","width":1,"height":1,"alt":"","futureField":true,
            "variants":[{"type":"image/png","url":"https://cdn.test/a/1/1.png","newSelector":"x"}]}"""

        val asset = Json.decodeFromString<AssetResponse>(json)

        assertTrue(asset.isDecorative)
        assertEquals(1, asset.variants.size)
        assertNull(asset.lang)
    }

    @Test
    fun hints_use_quoted_values_and_decreasing_quality() {
        val headers = BruaHints.headers(ColorScheme.Dark, reducedMotion = true, languages = listOf("nb-NO", "en"))

        assertEquals("nb-NO, en;q=0.9", headers["Accept-Language"])
        assertEquals("\"dark\"", headers["Sec-CH-Prefers-Color-Scheme"])
        assertEquals("\"reduce\"", headers["Sec-CH-Prefers-Reduced-Motion"])
        assertEquals("1", headers["Asset-Contract"])
    }

    @Test
    fun an_app_that_follows_the_system_appearance_sends_no_color_scheme() {
        val headers = BruaHints.headers(null, reducedMotion = false, languages = listOf("en"))

        assertNull(headers["Sec-CH-Prefers-Color-Scheme"])
        assertEquals("\"no-preference\"", headers["Sec-CH-Prefers-Reduced-Motion"])
    }

    @Test
    fun accept_language_stops_at_five_languages_with_a_dot_as_decimal_separator() {
        assertEquals(
            "nb, en;q=0.9, sv;q=0.8, da;q=0.7, de;q=0.6",
            BruaHints.acceptLanguage(listOf("nb", "en", "sv", "da", "de", "fr")),
        )
    }
}
