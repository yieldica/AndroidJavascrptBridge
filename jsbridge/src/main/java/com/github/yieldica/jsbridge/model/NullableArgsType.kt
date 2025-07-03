package com.github.yieldica.jsbridge.model

import kotlinx.serialization.Serializable


/**
 * 可空参数版本的Args类型
 */
@Serializable
data class NullableArgs2<P0, P1>(val arg0: P0, val arg1: P1? = null) : ArgsType

/**
 * 第一个参数可空的Args2类型
 * 用于处理第一个参数可能缺失的情况
 */
@Serializable
data class NullableArgs2FirstParam<P0, P1>(val arg0: P0? = null, val arg1: P1) : ArgsType

/**
 * 两个参数都可空的Args2类型
 * 用于处理两个参数都可能缺失的情况
 */
@Serializable
data class NullableArgs2Both<P0, P1>(val arg0: P0? = null, val arg1: P1? = null) : ArgsType

/**
 * 第二个参数可空的Args3类型
 * 用于处理第二个参数可能缺失的情况，第三个参数为回调
 */
@Serializable
data class NullableArgs2SecondParam<P0, P1, P2>(val arg0: P0, val arg1: P1? = null, val arg2: P2) : ArgsType

/**
 * 第 二&三 个参数可空
 * 用于处理第 二&三 个参数可能缺失的情况，第四个参数为回调
 */
@Serializable
data class NullableArgsSecondAndThirdParam<P0, P1, P2, P3>(val arg0: P0, val arg1: P1? = null, val arg2: P2? = null, val arg3: P3) : ArgsType