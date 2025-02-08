package ru.grishankov.modpack_creator.features.modpack_extractor.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.grishankov.modpack_creator.features.modpack_extractor.ModpackExtractor
import ru.grishankov.modpack_creator.features.modpack_extractor.interactors.ModpackExtractorInteractor
import ru.grishankov.modpack_creator.features.modpack_extractor.interactors.ModpackExtractorInteractorImpl
import ru.grishankov.modpack_creator.features.modpack_extractor.usecases.*

val featureModpackExtractorDiModule = module {

    singleOf(::ModpackDataExtractorUseCase)
    singleOf(::DownloadModsUseCase)
    singleOf(::CreateClientModpackUseCase)
    singleOf(::CreateServerModpackUseCase)
    singleOf(::CleanTempUseCase)

    singleOf(::ModpackExtractorInteractorImpl).bind<ModpackExtractorInteractor>()

    singleOf(::ModpackExtractor)
}
