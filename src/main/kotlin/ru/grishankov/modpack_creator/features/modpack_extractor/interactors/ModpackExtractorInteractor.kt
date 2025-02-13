package ru.grishankov.modpack_creator.features.modpack_extractor.interactors

import ru.grishankov.modpack_creator.features.modpack_extractor.models.AutoUpdateMod
import ru.grishankov.modpack_creator.features.modpack_extractor.models.MavenMod
import ru.grishankov.modpack_creator.features.modpack_extractor.models.ModpackData
import ru.grishankov.modpack_creator.features.modpack_extractor.usecases.*

interface ModpackExtractorInteractor {

    suspend fun getModpackData(): ModpackData

    suspend fun downloadMods(mods: List<AutoUpdateMod>)

    suspend fun downloadMavenMods(mods: List<MavenMod>)

    suspend fun createClientModpack(data: ModpackData)

    suspend fun createServerModpack(data: ModpackData)

    suspend fun cleanTemp()
}

internal class ModpackExtractorInteractorImpl(
    private val modpackDataExtractorUseCase: ModpackDataExtractorUseCase,
    private val downloadModsUseCase: DownloadModsUseCase,
    private val createClientModpackUseCase: CreateClientModpackUseCase,
    private val createServerModpackUseCase: CreateServerModpackUseCase,
    private val cleanTempUseCase: CleanTempUseCase,
    private val mavenDownloadUseCase: MavenDownloadUseCase,
) : ModpackExtractorInteractor {

    override suspend fun getModpackData(): ModpackData {
        return modpackDataExtractorUseCase.execute()
    }

    override suspend fun downloadMods(mods: List<AutoUpdateMod>) {
        downloadModsUseCase.execute(mods)
    }

    override suspend fun downloadMavenMods(mods: List<MavenMod>) {
        mavenDownloadUseCase.execute(mods)
    }

    override suspend fun createClientModpack(data: ModpackData) {
        createClientModpackUseCase.execute(data)
    }

    override suspend fun createServerModpack(data: ModpackData) {
        createServerModpackUseCase.execute(data)
    }

    override suspend fun cleanTemp() {
        cleanTempUseCase.execute()
    }
}
