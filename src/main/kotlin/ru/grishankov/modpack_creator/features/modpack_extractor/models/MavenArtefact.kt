package ru.grishankov.modpack_creator.features.modpack_extractor.models

import kotlinx.serialization.Serializable

@Serializable
data class MavenArtefacts(
    val items: List<MavenArtefact>,
    val continuationToken: String? = null,
)

@Serializable
data class MavenArtefact(
    val name: String,
    val version: String,
    val assets: List<MavenArtefactAsset>,
)

@Serializable
data class MavenArtefactAsset(
    val downloadUrl: String,
    val maven2: MavenArtefactAssetMaven,
    val checksum: Map<String, String> = emptyMap(),
)

@Serializable
data class MavenArtefactAssetMaven(
    val extension: String,
    val classifier: String? = null,
)
