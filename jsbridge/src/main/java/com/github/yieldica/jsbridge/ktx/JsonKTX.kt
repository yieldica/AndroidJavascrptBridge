package com.github.yieldica.jsbridge.ktx

import kotlinx.serialization.json.Json

@PublishedApi
internal val JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}