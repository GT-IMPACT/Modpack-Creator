package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.features.modpack_extractor.models.ModpackData
import java.io.File

class CreateModpackVersionUseCase(
    private val configuration: Configuration,
) {

    suspend fun execute(data: ModpackData) {
        return withContext(Dispatchers.IO) {
            val buildVersion = System.getenv("BUILD_NUMBER") ?: return@withContext

            val otherFolder = File(configuration.settingConfig.modpackPath, data.otherPath)
            if (!otherFolder.exists() && !otherFolder.isDirectory) return@withContext

            val modpackVersionFolder = File(otherFolder, data.outputPrefixFileName)
            modpackVersionFolder.mkdirs()

            val versionFile = File(modpackVersionFolder, "version.txt")
            versionFile.createNewFile()
            val data = buildString {
                append(data.version)
                if (configuration.isDevEnvironment) {
                    append("-")
                    append("dev")
                    append("-")
                    append(buildVersion)
                }
            }
            versionFile.writeText(data)

            println("\n📂 Установка версии модпака: $data...")
        }
    }
}
