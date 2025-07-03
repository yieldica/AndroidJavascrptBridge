@file:OptIn(ExperimentalSerializationApi::class)

package com.github.yieldica.jsbridge.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

interface ArgsType

@Serializable
data object Args0 : ArgsType

@Serializable
@JsonIgnoreUnknownKeys
data class Args1<P0>(val arg0: P0? = null) : ArgsType

@Serializable
@JsonIgnoreUnknownKeys
data class Args2<P0, P1>(val arg0: P0? = null, val arg1: P1? = null) : ArgsType

@Serializable
@JsonIgnoreUnknownKeys
data class Args3<P0, P1, P2>(val arg0: P0? = null, val arg1: P1? = null, val arg2: P2? = null) : ArgsType

@Serializable
@JsonIgnoreUnknownKeys
data class Args4<P0, P1, P2, P3>(val arg0: P0? = null, val arg1: P1? = null, val arg2: P2? = null, val arg3: P3? = null) : ArgsType