package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.features.modpack_extractor.models.AutoUpdateMod
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class DownloadModsUseCase(
    private val configuration: Configuration,
) {

    private val downloadFolder by lazy {
        File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH)
    }

    suspend fun execute(mods: List<AutoUpdateMod>) {
        withContext(Dispatchers.IO) {

            println("\n📂 Скачивание внешних модов...")

            if (mods.isEmpty()) {
                println("📂 Список внешних модов пуст, пропуск шага...")
            }

            mods.map { mod ->
                async {

                    val outputFile = File(downloadFolder, mod.fileName)

                    if (outputFile.exists()) {
                        println("⚠️ ${mod.fileName} уже был скачан")
                        return@async
                    }

                    outputFile.parentFile.mkdirs()

                    try {
                        val connection = URL(mod.downloadLink).openConnection()
                        connection.getInputStream().use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        println("✅ ${mod.fileName} успешно скачан")
                    } catch (e: Exception) {
                        println("❌ Ошибка при скачивании ${mod.fileName}: ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }
}
