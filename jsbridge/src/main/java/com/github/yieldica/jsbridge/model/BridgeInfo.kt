package com.github.yieldica.jsbridge.model

import kotlinx.serialization.Serializable


@Serializable
data class BridgeInfo(
    val identifier: String? = null,
    val args: String? = null,
)
