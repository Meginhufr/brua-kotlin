package dev.brua

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Serializable
private data class Fixture(val description: String, val asset: AssetResponse, val client: ClientState, val expected: String)

/** Every shared fixture selects the expected URL. */
@RunWith(Parameterized::class)
class FixtureTest(private val name: String) {
    @Test
    fun selects_the_expected_url() {
        val fixture = Json.decodeFromString<Fixture>(File(directory, name).readText())

        assertEquals(fixture.expected, VariantSelector.url(fixture.asset, fixture.client), fixture.description)
    }

    companion object {
        // The shared fixtures: beside the modules in their own repository, two levels further up in the platform's.
        private val directory = listOf(File("../fixtures/selection"), File("../../../fixtures/selection")).first { it.isDirectory }

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun names(): List<String> = directory.list { _, name -> name.endsWith(".json") }!!.sorted()
    }
}
