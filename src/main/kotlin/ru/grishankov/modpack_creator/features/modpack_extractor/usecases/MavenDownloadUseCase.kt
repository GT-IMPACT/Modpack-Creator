package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.core.HttpClient
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenArtefacts
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenMod
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MavenDownloadUseCase(
    private val configuration: Configuration,
) {

    private val downloadFolder by lazy {
        File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH)
    }

    private val url = buildString {
        append("https://")
        append(configuration.settingConfig.mavenBaseUrl)
        append("/service/rest/v1/search")
        append("?")
        append("sort=version")
        append("&")
        append("direction=desc")
        append("&")
        append("repository=maven-releases")
        append("&")
        append("group=space.impact")
        append("&")
        append("maven.extension=jar")
    }

    suspend fun execute(mods: List<MavenMod>) {
        withContext(Dispatchers.IO) {

            println("\n📂 Скачивание файлов из Maven...")

            if (mods.isEmpty()) {
                println("📂 Список модов из Maven пуст, пропуск шага...")
            }

            val mavenMods = mods.map { it.artefactName }

            val data = runCatching {
                HttpClient.ktorClient.get(url)
                    .body<MavenArtefacts>()
                    .items
                    .distinctBy { it.name }
                    .mapNotNull { artefact ->

                        if (!mavenMods.contains(artefact.name))
                            return@mapNotNull null

                        artefact.assets.firstOrNull { asset ->
                            asset.downloadUrl.contains(artefact.version, true) && asset.maven2.extension == "jar"
                        }
                    }
            }.getOrElse {
                println("❌ Ошибка при обращении в Maven: ${it}")
                emptyList()
            }

            data.map { asset ->
                async {
                    val fileName = asset.downloadUrl.substringAfterLast("/")
                    val outputFile = File(downloadFolder, fileName)

                    if (outputFile.exists()) {
                        println("⚠️ $fileName уже был скачан")
                        return@async
                    }

                    try {
                        val connection = URL(asset.downloadUrl).openConnection()

                        connection.getInputStream().use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        println("✅ $fileName успешно скачан")
                    } catch (e: Exception) {
                        println("❌ Ошибка при скачивании $fileName: ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }
}
