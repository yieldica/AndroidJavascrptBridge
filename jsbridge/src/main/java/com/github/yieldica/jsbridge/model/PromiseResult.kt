package com.github.yieldica.jsbridge.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PromiseResult<V>(
    var value: V? = null,
    var error: SerializableError? = null,
)

@Serializable
data class SerializableError(
    var message: String? = null,
    var stackTrace: List<String>? = null,
) {
    companion object {
        fun error(throwable: Throwable): SerializableError {
            return SerializableError(throwable.message, throwable.stackTrace.map { it.toString() })
        }
    }
}

interface SerializableThrowable {
    fun serialize(json: Json): String
}