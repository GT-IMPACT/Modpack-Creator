package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.grishankov.modpack_creator.config.SettingConfig
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenMod
import java.io.File
import java.util.jar.JarFile
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/** Explicit opt-in: downloads only ImpactAPI into a disposable directory, never builds a modpack. */
class MavenLiveTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun `configured Nexus supplies runtime ImpactAPI with required alignment class`() = runBlocking<Unit> {
        assumeTrue(System.getProperty("modpack.mavenSmoke") == "true")
        val username = requireNotNull(System.getenv("MAVEN_USER"))
        val password = requireNotNull(System.getenv("MAVEN_PASSWORD"))
        val json = Json { ignoreUnknownKeys = true }
        val settings = json.decodeFromString<SettingConfig>(File("config/config.json").readText())
        val folder = temporary.newFolder()
        HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) { requestTimeoutMillis = 300000; connectTimeoutMillis = 20000; socketTimeoutMillis = 60000 }
        }.use { client ->
            MavenDownloader(folder, "https://${settings.mavenBaseUrl}/service/rest/v1/search", client, username, password)
                .execute(listOf(MavenMod("ImpactAPI")))
        }
        val jars = folder.listFiles()!!.filter { it.extension == "jar" }
        assertEquals(1, jars.size)
        JarFile(jars.single()).use { assertNotNull(it.getJarEntry("space/impact/api/multiblocks/alignment/IAlignmentProvider.class")) }
    }
}
