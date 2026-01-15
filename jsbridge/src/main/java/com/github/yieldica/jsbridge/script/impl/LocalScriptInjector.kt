package com.github.yieldica.jsbridge.script.impl

import android.webkit.WebResourceResponse
import java.io.InputStream

interface LocalScriptInjector {
    fun shouldInterceptRequest(path: String): WebResourceResponse?
    fun handleLocalHtml(mimeType: String, inputStream: InputStream): WebResourceResponse?
}

interface LocalScriptInjectorOwner {
    var localScriptInjector: LocalScriptInjector?
}