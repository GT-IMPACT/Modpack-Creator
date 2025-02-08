package ru.grishankov.modpack_creator.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.config.JsonConfig

val mainDiModule = module(createdAtStart = true) {
    single { JsonConfig().json }
    singleOf(::Configuration)
}
