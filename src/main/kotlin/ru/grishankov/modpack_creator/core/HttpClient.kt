package ru.grishankov.modpack_creator.core

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.grishankov.modpack_creator.config.Configuration

object HttpClient : KoinComponent {

    private val config by inject<Configuration>()
    private val json by inject<Json>()

    val ktorClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        if (config.isDebug) install(Logging) {
            level = LogLevel.ALL
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
        }
    }
}
