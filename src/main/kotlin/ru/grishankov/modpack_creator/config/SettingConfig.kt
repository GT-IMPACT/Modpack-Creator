package ru.grishankov.modpack_creator.config

import kotlinx.serialization.Serializable

@Serializable
data class SettingConfig(
    val modpackPath: String = Constants.MODPACK_DEFAULT_PATH,
    val outputPath: String = Constants.MODPACK_DEFAULT_OUTPUT_PATH,
    val tempPath: String = Constants.TEMP_PATH,
) {
    companion object {
        val Default = SettingConfig()
    }
}
