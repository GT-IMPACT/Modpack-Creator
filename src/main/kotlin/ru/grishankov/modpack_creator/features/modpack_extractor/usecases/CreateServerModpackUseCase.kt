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

            val outputName = buildString {
                append(data.outputPrefixFileName)
                if (data.version.isNotBlank()) {
                    append("-")
                    append(data.version)
                }
            }

            println("\n📂 Создание серверного модпака $outputName...")

            val outputFolder = File(configuration.settingConfig.outputPath)

            outputFolder.mkdirs()

            val serverFolder = File(outputFolder, "$outputName-SERVER")
            serverFolder.mkdirs()

            println("📂 Копирование файлов...")

            // Others
            File(configuration.settingConfig.modpackPath, data.otherPath)
                .listFiles()
                .orEmpty()
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
            val outputZip = File(outputFolder, "$outputName$devPrefix-SERVER.zip")
            outputZip.createNewFile()

            zipFolder(
                folder = serverFolder,
                outputZip = outputZip,
                configuration = configuration,
            )
        }
    }
}
