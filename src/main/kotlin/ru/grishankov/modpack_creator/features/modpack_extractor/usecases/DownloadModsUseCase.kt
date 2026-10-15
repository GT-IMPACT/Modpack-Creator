package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.core.HttpClient
import ru.grishankov.modpack_creator.core.ModFileDownloader
import ru.grishankov.modpack_creator.features.modpack_extractor.models.AutoUpdateMod
import java.io.File

class DownloadModsUseCase(private val configuration: Configuration) {
    suspend fun execute(mods: List<AutoUpdateMod>) = withContext<Unit>(Dispatchers.IO) {
        println("\n📂 Скачивание внешних модов...")
        if (mods.isEmpty()) return@withContext
        val folder = File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH).canonicalFile
        val downloader = ModFileDownloader(HttpClient.ktorClient)
        mods.map { mod ->
            async {
                val output = File(folder, mod.fileName).canonicalFile
                require(output.parentFile == folder) { "Некорректное имя файла мода: ${mod.fileName}" }
                try { downloader.download(mod.downloadLink, output) }
                catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (failure: Exception) { throw IllegalStateException("Не скачан мод ${mod.fileName}. Сборка остановлена", failure) }
            }
        }.awaitAll()
    }
}
