package com.github.yieldica.jsbridge.script.impl

import android.webkit.WebResourceResponse
import java.io.InputStream

interface LocalScriptInjector {
    fun shouldInterceptRequest(path: String): WebResourceResponse?
    fun handleLocalHtml(mimeType: String, inputStream: InputStream): WebResourceResponse?
}

/**
 * 宿主 WebView 实现该接口，[CompatScriptManager] 会把自己注册到其
 * [localScriptInjector] 字段上，后续 HTML 拦截走这个 injector。
 *
 * 可空声明是刻意的：宿主在构造期 `CompatScriptManager(view)` 时尚未决定是否
 * 回退到 Compat 路径；只有当前 WebView 不支持 DOCUMENT_START_SCRIPT 时才会
 * 真正绑定一个非空 injector。
 */
interface LocalScriptInjectorOwner {
    var localScriptInjector: LocalScriptInjector?
}