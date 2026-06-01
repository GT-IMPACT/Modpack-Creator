package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.core.zipFolder
import ru.grishankov.modpack_creator.features.modpack_extractor.models.ModpackData
import java.io.File

class CreateClientModpackUseCase(
    private val configuration: Configuration,
) {

    suspend fun execute(data: ModpackData) {
        return withContext(Dispatchers.IO) {

            println("\n📂 Создание клиентского модпака ${data.outputPrefixFileName}...")

            val outputFolder = File(configuration.settingConfig.outputPath)

            if (outputFolder.exists()) {
                outputFolder.deleteRecursively()
            }

            outputFolder.mkdirs()

            val clientFolder = File(outputFolder, "${data.outputPrefixFileName}-CLIENT")
            clientFolder.mkdirs()

            println("📂 Копирование файлов...")

            // Metadata
            File(configuration.settingConfig.modpackPath, data.metadataPath)
                .listFiles()
                .orEmpty()
                .forEach { it.copyRecursively(File(clientFolder, it.name)) }

            // Others
            val dotMinecraftFolder = File(clientFolder, ".minecraft")
            dotMinecraftFolder.mkdirs()

            File(configuration.settingConfig.modpackPath, data.otherPath)
                .listFiles()
                .orEmpty()
                .forEach { it.copyRecursively(File(dotMinecraftFolder, it.name)) }

            // Mods
            val modsFolder = File(dotMinecraftFolder, "mods")
            modsFolder.mkdirs()
            File(configuration.settingConfig.modpackPath, data.modsPath)
                .listFiles()
                .orEmpty()
                .forEach { it.copyRecursively(File(modsFolder, it.name)) }

            File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH)
                .listFiles()
                .orEmpty()
                .forEach { it.copyRecursively(File(modsFolder, it.name)) }

            // Create Zip
            val devPrefix = buildString {
                if (configuration.isDevEnvironment) {
                    val version = System.getenv("BUILD_NUMBER") ?: "dirty"
                    append("-dev")
                    append("-$version")
                }
            }
            val outputZip = File(outputFolder, "${data.outputPrefixFileName}$devPrefix-CLIENT.zip")
            outputZip.createNewFile()

            zipFolder(
                folder = clientFolder,
                outputZip = outputZip,
                configuration = configuration,
            )
        }
    }
}
