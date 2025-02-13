package ru.grishankov.modpack_creator.features.modpack_extractor

import ru.grishankov.modpack_creator.features.modpack_extractor.interactors.ModpackExtractorInteractor

class ModpackExtractor(
    interactor: ModpackExtractorInteractor,
) : ModpackExtractorInteractor by interactor {

    suspend fun execute() {
        val data = getModpackData()

        downloadMavenMods(data.mavenMods)

        downloadMods(data.autoUpdateMods)

        createClientModpack(data)

        createServerModpack(data)

        cleanTemp()

        println("\n✅ Готово!")
    }
}
