package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.core.HttpClient
import ru.grishankov.modpack_creator.core.ModFileDownloader
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenArtefact
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenArtefacts
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenMod
import java.io.File
import java.net.URI

class MavenDownloadUseCase(private val configuration: Configuration) {
    suspend fun execute(mods: List<MavenMod>) {
        MavenDownloader(
            File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH),
            "https://${configuration.settingConfig.mavenBaseUrl}/service/rest/v1/search",
            HttpClient.ktorClient,
            System.getenv("MAVEN_USER"), System.getenv("MAVEN_PASSWORD"),
        ).execute(mods)
    }
}

internal class MavenDownloader(
    private val downloadFolder: File,
    private val searchUrl: String,
    private val client: io.ktor.client.HttpClient,
    private val username: String? = null,
    private val password: String? = null,
) {
    suspend fun execute(mods: List<MavenMod>) = withContext<Unit>(Dispatchers.IO) {
        println("\n📂 Скачивание файлов из Maven...")
        if (mods.isEmpty()) {
            println("📂 Список модов из Maven пуст, пропуск шага...")
            return@withContext
        }
        val requested = mods.map { it.artefactName }.toSet()
        require(requested.none { it.isBlank() }) { "Пустое имя Maven-артефакта в конфиге" }
        val latest = linkedMapOf<String, MavenArtefact>()
        val seenTokens = mutableSetOf<String>()
        var token: String? = null
        do {
            val page = client.get(searchUrl) {
                expectSuccess = true
                parameter("sort", "version")
                parameter("direction", "desc")
                parameter("repository", "maven-releases")
                parameter("group", "space.impact")
                parameter("maven.extension", "jar")
                token?.let { parameter("continuationToken", it) }
                if (username != null && password != null) basicAuth(username, password)
            }.body<MavenArtefacts>()
            // Nexus sorts versions descending across pages; keep the first version of each requested name.
            for (artifact in page.items) if (artifact.name in requested && artifact.name !in latest) latest[artifact.name] = artifact
            token = page.continuationToken?.takeIf { it.isNotBlank() }
            check(token == null || seenTokens.add(token)) { "Maven вернул повторяющийся continuationToken; сборка остановлена" }
        } while (token != null)

        val missing = requested - latest.keys
        check(missing.isEmpty()) { "В Maven не найдены моды из конфига: ${missing.sorted().joinToString()}. Сборка остановлена" }
        val selected = latest.values.map { artifact ->
            val asset = artifact.assets.firstOrNull {
                it.maven2.extension == "jar" && it.maven2.classifier.isNullOrBlank()
            } ?: error("Нет обычного JAR для ${artifact.name}:${artifact.version} (dev/sources/javadoc не подходят). Сборка остановлена")
            val fileName = URI(asset.downloadUrl).path.substringAfterLast('/')
            require(fileName.endsWith(".jar", ignoreCase = true) && fileName.none { it in "\\/:" }) { "Некорректное имя Maven JAR для ${artifact.name}" }
            artifact to (asset to File(downloadFolder, fileName))
        }
        check(selected.map { it.second.second.name }.distinct().size == selected.size) { "Maven-артефакты имеют одинаковые имена файлов" }
        val downloader = ModFileDownloader(client, username, password)
        selected.map { (artifact, target) ->
            async {
                try {
                    downloader.download(target.first.downloadUrl, target.second, target.first.checksum)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    throw IllegalStateException("Не скачан мод ${artifact.name}:${artifact.version}. Сборка остановлена", failure)
                }
            }
        }.awaitAll()
    }
}
