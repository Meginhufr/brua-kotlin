package dev.brua

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** The fixtures give only the asset's size and fit; the rest of the contract isn't part of the rule. */
@Serializable
private data class SizingFixture(val description: String, val asset: Asset, val box: Box, val objectFit: String? = null, val expected: Double) {
    @Serializable
    data class Asset(val width: Int, val height: Int, val objectFit: String? = null)

    @Serializable
    data class Box(val width: Double, val height: Double? = null)
}

/** Every shared sizing fixture draws at the expected width. */
@RunWith(Parameterized::class)
class SizingTest(private val name: String) {
    @Test
    fun draws_at_the_expected_width() {
        val fixture = Json.decodeFromString<SizingFixture>(File(directory, name).readText())
        val asset = AssetResponse("a", 1, "https://cdn.test/a/1", fixture.asset.width, fixture.asset.height, "", objectFit = fixture.asset.objectFit)

        assertEquals(fixture.expected, BruaLayout.targetWidth(asset, fixture.box.width, fixture.box.height, fixture.objectFit), fixture.description)
    }

    companion object {
        // The shared fixtures: beside the modules in their own repository, two levels further up in the platform's.
        private val directory = listOf(File("../fixtures/sizing"), File("../../../fixtures/sizing")).first { it.isDirectory }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun names(): List<String> = directory.list { _, name -> name.endsWith(".json") }!!.sorted()
    }
}
