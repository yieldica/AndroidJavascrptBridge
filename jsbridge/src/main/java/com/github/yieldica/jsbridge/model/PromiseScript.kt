package com.github.yieldica.jsbridge.model

class PromiseScript(source: String? = null) {
    var script: String = source?.let { "{ value: $it}" } ?: "{ value: null }"
}