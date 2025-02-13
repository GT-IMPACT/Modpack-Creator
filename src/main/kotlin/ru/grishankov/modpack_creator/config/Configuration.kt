package ru.grishankov.modpack_creator.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File

@OptIn(ExperimentalSerializationApi::class)
class Configuration(private val json: Json) {

    private val configFolder = File(Constants.CONFIG_PATH)

    var settingConfig = SettingConfig.Default
        private set

    var isDevEnvironment = false
    var isDebug = false

    init {
        onCreate()
    }

    private fun onCreate() {
        configFolder.mkdirs()

        val configFile = File(configFolder, Constants.CONFIG_FILE_NAME)

        if (!configFile.exists()) {
            configFile.createNewFile()
            json.encodeToStream(settingConfig, configFile.outputStream())
        }

        settingConfig = json.decodeFromString(configFile.readText())
        json.encodeToStream(settingConfig, configFile.outputStream())

        createTempFiles()
    }

    private fun createTempFiles() {
        val tempFolder = File(settingConfig.tempPath)
        tempFolder.mkdirs()

        val downloadsFolder = File(tempFolder, Constants.TEMP_DOWNLOAD_PATH)
        downloadsFolder.mkdirs()
    }
}
