package ru.grishankov.modpack_creator.features.modpack_extractor

import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.grishankov.modpack_creator.features.modpack_extractor.interactors.ModpackExtractorInteractor
import ru.grishankov.modpack_creator.features.modpack_extractor.models.*
import kotlin.test.*

class ModpackFailureTest {
    private class Flow(private val failAt: String? = null) : ModpackExtractorInteractor {
        val calls = mutableListOf<String>()
        private fun step(name: String) { calls += name; check(name != failAt) { "download failed" } }
        override suspend fun getModpackData(): ModpackData {
            step("config")
            return ModpackData("mods", "other", "metadata", "IMPACT", "1", emptyList(), listOf(MavenMod("ImpactAPI")), emptyList())
        }
        override suspend fun createVersionFile(data: ModpackData) { step("version") }
        override suspend fun downloadMods(mods: List<AutoUpdateMod>) { step("external") }
        override suspend fun downloadMavenMods(mods: List<MavenMod>) { step("maven") }
        override suspend fun createClientModpack(data: ModpackData) { step("client") }
        override suspend fun createServerModpack(data: ModpackData) { step("server") }
        override suspend fun cleanTemp() { step("cleanup") }
    }
    @Test fun `Maven failure prevents version bump and both pack archives`() = runBlocking {
        val flow = Flow("maven")
        assertFailsWith<IllegalStateException> { ModpackExtractor(flow).execute() }
        assertEquals(listOf("config", "maven"), flow.calls)
    }
    @Test fun `external download failure also prevents version bump and packaging`() = runBlocking {
        val flow = Flow("external")
        assertFailsWith<IllegalStateException> { ModpackExtractor(flow).execute() }
        assertEquals(listOf("config", "maven", "external"), flow.calls)
    }
    @Test fun `packaging starts only after all downloads succeed`() = runBlocking {
        val flow = Flow()
        ModpackExtractor(flow).execute()
        assertEquals(listOf("config", "maven", "external", "version", "client", "server", "cleanup"), flow.calls)
    }
}
