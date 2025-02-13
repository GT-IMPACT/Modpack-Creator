package ru.grishankov.modpack_creator

import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import ru.grishankov.modpack_creator.config.Configuration
import ru.grishankov.modpack_creator.di.mainDiModule
import ru.grishankov.modpack_creator.features.modpack_extractor.ModpackExtractor
import ru.grishankov.modpack_creator.features.modpack_extractor.di.featureModpackExtractorDiModule

suspend fun main(args: Array<String>) {
    startKoin {
        modules(
            mainDiModule,
            featureModpackExtractorDiModule,
        )
    }

    val config = Injector.get<Configuration>()

    args.find { it.contains("--dev") }?.also {
        config.isDevEnvironment = true
    }

    args.find { it.contains("--debug") }?.also {
        config.isDebug = true
    }

    args.forEach { arg ->
        arg.command("--modpack") {
            val extractor = Injector.get<ModpackExtractor>()
            extractor.execute()
        }
    }
}

private inline fun String.command(name: String, onAction: (String) -> Unit) {
    if (this.contains(name, ignoreCase = true)) {
        onAction(this)
    }
}

object Injector : KoinComponent {
    inline fun <reified T : Any> get() = getKoin().get<T>()
}
