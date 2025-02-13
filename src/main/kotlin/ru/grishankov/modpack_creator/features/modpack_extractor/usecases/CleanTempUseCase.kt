package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import java.io.File

class CleanTempUseCase(
    private val configuration: Configuration,
) {

    private val downloadFolder by lazy {
        File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH)
    }

    suspend fun execute() {
        withContext(Dispatchers.IO) {
            println("\n📂 Удаление временных файлов...")

            downloadFolder.deleteRecursively()
        }
    }
}
