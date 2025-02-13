package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.Constants
import ru.grishankov.modpack_creator.core.copyFilesWithStructure
import ru.grishankov.modpack_creator.core.zipFolder
import ru.grishankov.modpack_creator.features.modpack_extractor.models.ModpackData
import java.io.File

class CreateServerModpackUseCase(
    private val configuration: Configuration,
) {

    suspend fun execute(data: ModpackData) {
        return withContext(Dispatchers.IO) {

            println("\n📂 Создание серверного модпака ${data.outputPrefixFileName}...")

            val outputFolder = File(configuration.settingConfig.outputPath)

            outputFolder.mkdirs()

            val serverFolder = File(outputFolder, "${data.outputPrefixFileName}-SERVER")
            serverFolder.mkdirs()

            println("📂 Копирование файлов...")

            // Others
            File(configuration.settingConfig.modpackPath, data.otherPath)
                .listFiles()
                .forEach { it.copyRecursively(File(serverFolder, it.name)) }

            // Mods
            val modsFolder = File(serverFolder, "mods")
            modsFolder.mkdirs()

            copyFilesWithStructure(
                srcDir =  File(configuration.settingConfig.modpackPath, data.modsPath),
                destDir = modsFolder,
                excludeNames = data.clientOnlyMods,
            )

            copyFilesWithStructure(
                srcDir = File(configuration.settingConfig.tempPath, Constants.TEMP_DOWNLOAD_PATH),
                destDir = modsFolder,
                excludeNames = data.clientOnlyMods,
            )

            // Create Zip
            val devPrefix = buildString {
                if (configuration.isDevEnvironment) {
                    val version = System.getenv("BUILD_NUMBER") ?: "dirty"
                    append("-dev")
                    append("-$version")
                }
            }
            val outputZip = File(outputFolder, "${data.outputPrefixFileName}$devPrefix-SERVER.zip")
            outputZip.createNewFile()

            zipFolder(
                folder = serverFolder,
                outputZip = outputZip,
                configuration = configuration,
            )
        }
    }
}
