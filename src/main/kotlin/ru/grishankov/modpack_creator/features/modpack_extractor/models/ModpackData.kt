package ru.grishankov.modpack_creator.features.modpack_extractor.models

import kotlinx.serialization.Serializable

@Serializable
data class ModpackData(
    val modsPath: String,
    val otherPath: String,
    val metadataPath: String,
    val outputPrefixFileName: String,
    val autoUpdateMods: List<AutoUpdateMod>,
    val mavenMods: List<MavenMod>,
    val clientOnlyMods: List<String>,
)

@Serializable
data class AutoUpdateMod(
    val fileName: String,
    val downloadLink: String,
)

@Serializable
data class MavenMod(
    val artefactName: String,
)
