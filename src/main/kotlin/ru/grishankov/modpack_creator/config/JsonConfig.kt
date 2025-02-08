package ru.grishankov.modpack_creator.config

import kotlinx.serialization.json.Json

class JsonConfig {
    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
