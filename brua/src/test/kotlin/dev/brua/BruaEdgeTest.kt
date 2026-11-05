package dev.brua

import kotlin.test.Test
import kotlin.test.assertEquals

class BruaEdgeTest {
    @Test
    fun the_url_is_the_current_release_json_with_the_language_when_given() {
        assertEquals("https://cdn.example/assets/org/icon.json?lang=nb", BruaEdge("https://cdn.example/assets/org/", "nb").url("icon"))
        assertEquals("https://cdn.example/assets/org/icon.json", BruaEdge("https://cdn.example/assets/org").url("icon"))
    }

    @Test
    fun without_a_language_the_url_carries_the_devices_languages() {
        val edge = BruaEdge("https://cdn.example/assets/org")
        assertEquals("https://cdn.example/assets/org/icon.json?lang=nb-NO%2C%20en%3Bq%3D0.9", edge.url("icon", listOf("nb-NO", "en")))
        assertEquals("https://cdn.example/assets/org/icon.json?lang=nb", BruaEdge("https://cdn.example/assets/org", "nb").url("icon", listOf("en")))
    }

    @Test
    fun what_the_edge_answers_decodes_with_the_release_ignored() {
        val asset = BruaEdge("https://cdn.example/assets/org").parse(
            """{"id":"icon","version":3,"release":12,"url":"https://cdn.example/assets/org/icon","width":24,"height":24,"alt":"Settings","lang":"en","variants":[]}""",
        )

        assertEquals("icon", asset.id)
        assertEquals(3, asset.version)
    }
}
