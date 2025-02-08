package ru.grishankov.modpack_creator.features.modpack_extractor.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.features.modpack_extractor.models.ModpackData
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class ModpackDataExtractorUseCase(
    private val configuration: Configuration,
    private val json: Json,
) {

    companion object {
        private const val MODPACK_DATA_JSON = "data.json"
    }

    suspend fun execute(): ModpackData {
        return withContext(Dispatchers.IO) {
            val file = File(configuration.settingConfig.modpackPath, MODPACK_DATA_JSON)
            json.decodeFromStream(file.inputStream())
        }
    }
}
