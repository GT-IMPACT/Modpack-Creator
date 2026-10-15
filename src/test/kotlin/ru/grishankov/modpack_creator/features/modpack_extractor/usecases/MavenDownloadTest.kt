package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.grishankov.modpack_creator.features.modpack_extractor.models.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

class MavenDownloadTest {
    @get:Rule val temporary = TemporaryFolder()
    private val json = Json { ignoreUnknownKeys = true }
    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout)
    }
    private fun jar(content: String = "valid") = ByteArrayOutputStream().also { bytes ->
        JarOutputStream(bytes).use { it.putNextEntry(JarEntry("test.txt")); it.write(content.toByteArray()); it.closeEntry() }
    }.toByteArray()
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun artifact(name: String, version: String = "1", checksums: Map<String, String> = emptyMap()) = MavenArtefact(name, version,
        listOf(MavenArtefactAsset("https://maven.test/$name-$version.jar", MavenArtefactAssetMaven("jar"), checksums)))
    private fun page(items: List<MavenArtefact>, token: String? = null) = json.encodeToString(MavenArtefacts(items, token))
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test fun `reads all pages encodes continuation and downloads ImpactAPI from page two`() = runBlocking<Unit> {
        val bytes = jar()
        val requests = mutableListOf<String?>()
        val folder = temporary.newFolder()
        client { request ->
            if (request.url.encodedPath == "/search") {
                val token = request.url.parameters["continuationToken"]
                requests += token
                assertEquals("Basic dXNlcjpwYXNz", request.headers[HttpHeaders.Authorization])
                respond(if (token == null) page(listOf(artifact("GregTech", "2")), "next+/?= page")
                    else page(listOf(artifact("ImpactAPI", "0.0.5"), artifact("GregTech", "1"))), headers = jsonHeaders)
            } else respond(bytes)
        }.use { MavenDownloader(folder, "https://maven.test/search", it, "user", "pass").execute(listOf(MavenMod("GregTech"), MavenMod("ImpactAPI"))) }
        assertEquals(listOf(null, "next+/?= page"), requests)
        assertEquals(setOf("GregTech-2.jar", "ImpactAPI-0.0.5.jar"), folder.list()!!.toSet())
    }

    @Test fun `missing configured artifact aborts before downloading anything`() = runBlocking<Unit> {
        var downloads = 0
        client { request ->
            if (request.url.encodedPath == "/search") respond(page(listOf(artifact("Available"))), headers = jsonHeaders)
            else { downloads++; respond(jar()) }
        }.use {
            val failure = assertFailsWith<IllegalStateException> { MavenDownloader(temporary.newFolder(), "https://maven.test/search", it)
                .execute(listOf(MavenMod("Available"), MavenMod("ImpactAPI"))) }
            assertTrue(failure.message!!.contains("ImpactAPI"))
        }
        assertEquals(0, downloads)
    }

    @Test fun `second page HTTP error aborts rather than producing an incomplete selection`() = runBlocking<Unit> {
        val folder = temporary.newFolder()
        client { request ->
            if (request.url.parameters["continuationToken"] == null) respond(page(listOf(artifact("Available")), "next"), headers = jsonHeaders)
            else respond("unavailable", HttpStatusCode.InternalServerError)
        }.use { assertFailsWith<Exception> { MavenDownloader(folder, "https://maven.test/search", it).execute(listOf(MavenMod("Available"))) } }
        assertTrue(folder.list()!!.isEmpty())
    }

    @Test fun `repeated continuation token stops pagination`() = runBlocking<Unit> {
        var pages = 0
        client { pages++; respond(page(emptyList(), "same"), headers = jsonHeaders) }.use {
            assertFailsWith<IllegalStateException> { MavenDownloader(temporary.newFolder(), "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) }
        }
        assertEquals(2, pages)
    }

    @Test fun `selects runtime JAR and never installs a dev classifier`() = runBlocking<Unit> {
        val release = artifact("ImpactAPI")
        val dev = MavenArtefactAsset("https://maven.test/ImpactAPI-1-dev.jar", MavenArtefactAssetMaven("jar", "dev"))
        val paths = mutableListOf<String>()
        client { request ->
            paths += request.url.encodedPath
            if (request.url.encodedPath == "/search") respond(page(listOf(release.copy(assets = listOf(dev) + release.assets))), headers = jsonHeaders)
            else respond(jar())
        }.use { MavenDownloader(temporary.newFolder(), "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) }
        assertEquals(listOf("/search", "/ImpactAPI-1.jar"), paths)
    }

    @Test fun `artifact without runtime JAR aborts`() = runBlocking<Unit> {
        val artifact = artifact("ImpactAPI").let { it.copy(assets = it.assets.map { asset -> asset.copy(maven2 = MavenArtefactAssetMaven("jar", "sources")) }) }
        client { respond(page(listOf(artifact)), headers = jsonHeaders) }.use {
            assertFailsWith<IllegalStateException> { MavenDownloader(temporary.newFolder(), "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) }
        }
    }

    @Test fun `failed JAR download is fatal and leaves no partial output`() = runBlocking<Unit> {
        val folder = temporary.newFolder()
        client { request ->
            if (request.url.encodedPath == "/search") respond(page(listOf(artifact("ImpactAPI"))), headers = jsonHeaders)
            else respond("not found", HttpStatusCode.NotFound)
        }.use {
            val failure = assertFailsWith<IllegalStateException> { MavenDownloader(folder, "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) }
            assertTrue(failure.message!!.contains("ImpactAPI"))
        }
        assertTrue(folder.list()!!.isEmpty())
    }

    @Test fun `corrupt cache is replaced and valid checksum verified cache is reused`() = runBlocking<Unit> {
        val folder = temporary.newFolder()
        val bytes = jar()
        val artifact = artifact("ImpactAPI", checksums = mapOf("sha256" to sha(bytes)))
        File(folder, "ImpactAPI-1.jar").writeText("incomplete previous download")
        var downloads = 0
        client { request ->
            if (request.url.encodedPath == "/search") respond(page(listOf(artifact)), headers = jsonHeaders)
            else { downloads++; respond(bytes) }
        }.use {
            val downloader = MavenDownloader(folder, "https://maven.test/search", it)
            downloader.execute(listOf(MavenMod("ImpactAPI")))
            downloader.execute(listOf(MavenMod("ImpactAPI")))
        }
        assertEquals(1, downloads)
        assertContentEquals(bytes, File(folder, "ImpactAPI-1.jar").readBytes())
        assertEquals(setOf("ImpactAPI-1.jar"), folder.list()!!.toSet())
    }

    @Test fun `checksum mismatch aborts and does not overwrite an existing file`() = runBlocking<Unit> {
        val folder = temporary.newFolder()
        val old = File(folder, "ImpactAPI-1.jar").apply { writeBytes(jar("old")) }
        val original = old.readBytes()
        client { request ->
            if (request.url.encodedPath == "/search") respond(page(listOf(artifact("ImpactAPI", checksums = mapOf("sha256" to "00".repeat(32))))), headers = jsonHeaders)
            else respond(jar("new"))
        }.use { assertFailsWith<IllegalStateException> { MavenDownloader(folder, "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) } }
        assertContentEquals(original, old.readBytes())
        assertEquals(setOf(old.name), folder.list()!!.toSet())
    }

    @Test fun `HTML response and truncated content length cannot be installed as a mod`() = runBlocking<Unit> {
        for (html in listOf(true, false)) {
            val folder = temporary.newFolder()
            client { request ->
                if (request.url.encodedPath == "/search") respond(page(listOf(artifact("ImpactAPI"))), headers = jsonHeaders)
                else if (html) respond("<html>login</html>")
                else respond(jar(), headers = headersOf(HttpHeaders.ContentLength, "999999"))
            }.use { assertFailsWith<IllegalStateException> { MavenDownloader(folder, "https://maven.test/search", it).execute(listOf(MavenMod("ImpactAPI"))) } }
            assertTrue(folder.list()!!.isEmpty())
        }
    }

    @Test fun `empty Maven config does not access network`() = runBlocking<Unit> {
        client { error("Unexpected network request") }.use { MavenDownloader(temporary.newFolder(), "https://maven.test/search", it).execute(emptyList()) }
    }
}
