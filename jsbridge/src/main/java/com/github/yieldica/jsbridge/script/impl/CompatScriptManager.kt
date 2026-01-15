package com.github.yieldica.jsbridge.script.impl

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.core.net.toUri
import androidx.webkit.internal.AssetHelper
import com.github.yieldica.jsbridge.ktx.md5
import com.github.yieldica.jsbridge.script.OnMessageListener
import com.github.yieldica.jsbridge.script.ScriptManager
import com.github.yieldica.jsbridge.script.UserScriptManager
import com.github.yieldica.jsbridge.script.impl.LocalScriptManager.Companion.MIMETYPE_HTML
import com.github.yieldica.jsbridge.utils.ApplicationHolder
import java.io.File
import java.io.InputStream

class CompatScriptManager(view: WebView?) : ScriptManager, LocalScriptInjector {
    companion object {
        private const val TAG = "CompatScriptManager"
    }

    private val localScriptManager = LocalScriptManager(this)

    internal val scriptMap = mutableMapOf<String, String>()

    override var onMessageListener: OnMessageListener? = null


    init {
        (view as? LocalScriptInjectorOwner)?.localScriptInjector = this
        view?.addJavascriptInterface(this, UserScriptManager.MESSAGE_NAME)
    }

    override fun inject(script: String, forKey: String) {
        scriptMap[forKey] = script
    }

    override fun remove(forKey: String) {
        Log.w(TAG, "not supported remove script by compat script manager.")
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        onMessageListener?.onPostMessage(message)
    }

    override fun handleLocalHtml(mimeType: String, inputStream: InputStream): WebResourceResponse? {
        if (mimeType == MIMETYPE_HTML) {
            return WebResourceResponse(
                MIMETYPE_HTML,
                null,
                localScriptManager.handleHtmlSource(inputStream.bufferedReader().readText())
                    .byteInputStream()
            )
        }
        return null
    }

    override fun shouldInterceptRequest(path: String): WebResourceResponse? {
        return localScriptManager.shouldInterceptRequest(path)
    }
}


private class LocalScriptManager(private val compatScriptManager: CompatScriptManager) {
    private val context = ApplicationHolder.application
    private val rootDir = context.filesDir.resolve(DIR).also {
        it.mkdirs()
    }

    companion object {
        private const val DIR = "local_scripts"
        const val MIMETYPE_HTML = "text/html"
        const val CONTENT_TYPE = "Content-Type"
        private const val TIMEOUT = 5000L
    }

    @SuppressLint("RestrictedApi")
    private fun maybeHtmlUrl(url: String): Boolean {
        val mimeType = AssetHelper.guessMimeType(url)
        return if (mimeType == MIMETYPE_HTML) {
            true
        } else {
            mimeType == AssetHelper.DEFAULT_MIME_TYPE
        }
    }

    @SuppressLint("RestrictedApi")
    fun shouldInterceptRequest(url: String): WebResourceResponse? {
        if (url.startsWith(DIR)) {
            val jsFile = resolveScriptFile(url)
            return WebResourceResponse(AssetHelper.guessMimeType(url), null, jsFile.inputStream())
        }

        val urlPath = url.toUri().path?.removePrefix("/") ?: return null
        if (urlPath.startsWith(DIR)) {
            val jsFile = resolveScriptFile(urlPath)
            return WebResourceResponse(AssetHelper.guessMimeType(url), null, jsFile.inputStream())
        }

        return null
    }

    fun getMimeType(contentType: String?): String? {
        return contentType?.substringBefore(";")?.trim()
    }

    fun handleHtmlSource(source: String): String {
        val index = source.indexOf("</head>")
        if (index == -1) {
            return source
        }

        val scriptBuilder = StringBuilder(source.take(index))
        fun appendScript(path: String, js: String) {
            scriptBuilder.append("<script src=\"/").append(makeRelativePath(path, js))
                .append("\"></script>\n")
        }

        appendScript("core", UserScriptManager.coreScript)
        compatScriptManager.scriptMap.forEach { (t, u) ->
            appendScript(t, u)
        }
        scriptBuilder.append(source.substring(index))
        return scriptBuilder.toString()
    }

    fun makeRelativePath(path: String, js: String): String {
        val fileMD5 = js.md5
        val file = rootDir.resolve("${path}_$fileMD5.js")
        if (!file.exists()) {
            rootDir.listFiles { child ->
                child.name.startsWith("${path}_")
            }?.forEach {
                it.deleteOnExit()
            }

            file.writeText(js)
        }
        return "$DIR/${file.name}"
    }


    fun resolveScriptFile(path: String): File {
        return rootDir.resolveSibling(path)
    }
}