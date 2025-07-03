package com.github.yieldica.jsbridge.script.impl

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import com.github.yieldica.jsbridge.ktx.md5
import com.github.yieldica.jsbridge.script.OnMessageListener
import com.github.yieldica.jsbridge.script.ScriptManager
import com.github.yieldica.jsbridge.script.UserScriptManager
import com.github.yieldica.jsbridge.script.impl.LocalScriptManager.Companion.DIR
import com.github.yieldica.jsbridge.script.impl.LocalScriptManager.Companion.MIMETYPE_HTML
import com.github.yieldica.jsbridge.script.impl.LocalScriptManager.Companion.MIMETYPE_JAVASCRIPT
import com.github.yieldica.jsbridge.utils.ApplicationHolder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL

class CompatScriptManager(view: WebView?) : ScriptManager {
    companion object {
        private const val TAG = "CompatScriptManager"
        private const val CORE_JS_PATH = "__compat__/core.js"
    }

    private val localScriptManager = LocalScriptManager(this)

    private val webView = WeakReference(view)
    internal val scriptMap = mutableMapOf<String, String>()

    override var onMessageListener: OnMessageListener? = null


    init {
        view?.let {
            it.webViewClient = LocalScriptWebClient(it.webViewClient) {
                WebResourceResponse(MIMETYPE_HTML, null, localScriptManager.handleHtmlSource(it).byteInputStream())
            }
            it.addJavascriptInterface(this, UserScriptManager.MESSAGE_NAME)
        }
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
}

private class LocalScriptManager(private val compatScriptManager: CompatScriptManager) {
    private val context = ApplicationHolder.application
    private val rootDir = context.filesDir.resolve(DIR).also { it.mkdirs() }

    companion object {
        const val DIR = "local_scripts"
        const val MIMETYPE_HTML = "text/html"
        const val MIMETYPE_JAVASCRIPT = "text/javascript"
    }

    fun handleHtmlSource(source: String): String {
        val index = source.indexOf("</head>")
        if (index == -1) {
            return source
        }

        val scriptBuilder = StringBuilder(source.substring(0, index))
        fun appendScript(path: String, js: String) {
            scriptBuilder.append("<script src=\"/").append(makeRelativePath(path, js)).append("\"></script>\n")
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
                Log.i("CompatScriptManager", "makeRelativePath delete: ${it.path}")
                it.deleteOnExit()
            }

            file.writeText(js)
            Log.i("CompatScriptManager", "makeRelativePath: $DIR/${file.name}")
        }
        return "$DIR/${file.name}"
    }
}

internal class LocalScriptWebClient(private val delegate: WebViewClient, private val htmlHandler: (String) -> WebResourceResponse) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (url.startsWith(DIR)) {
            val jsFile = resolveScriptFile(view.context, url)
            return WebResourceResponse(MIMETYPE_JAVASCRIPT, null, jsFile.inputStream())
        }

        val urlPath = url.toUri().path?.removePrefix("/") ?: return null
        if (urlPath.startsWith(DIR)) {
            val jsFile = resolveScriptFile(view.context, urlPath)
            return WebResourceResponse(MIMETYPE_JAVASCRIPT, null, jsFile.inputStream())
        }

        if (request.isForMainFrame) {
            val response = delegate.shouldInterceptRequest(view, request)
            response?.data?.let { BufferedReader(InputStreamReader(it)) }?.use { it.readText() }?.let { return htmlHandler(it) }
            downloadHtml(request)?.let { return htmlHandler(it) }
        }
        return delegate.shouldInterceptRequest(view, request)
    }

    private fun downloadHtml(request: WebResourceRequest): String? {
        try {
            val url = request.url.toString()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            val inputStream = connection.inputStream
            return inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return null
        }
    }

    private fun resolveScriptFile(context: Context, path: String): File {
        return context.filesDir.resolve(DIR).also { it.mkdirs() }.resolveSibling(path)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return delegate.shouldInterceptRequest(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return delegate.shouldOverrideUrlLoading(view, url)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return delegate.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        delegate.onPageStarted(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        delegate.onPageFinished(view, url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        delegate.onLoadResource(view, url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        delegate.onPageCommitVisible(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
        delegate.onTooManyRedirects(view, cancelMsg, continueMsg)
    }

    @Deprecated("Deprecated in Java")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        delegate.onReceivedError(view, errorCode, description, failingUrl)
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        delegate.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        delegate.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        delegate.onFormResubmission(view, dontResend, resend)
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        delegate.doUpdateVisitedHistory(view, url, isReload)
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        delegate.onReceivedSslError(view, handler, error)
    }

    override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
        delegate.onReceivedClientCertRequest(view, request)
    }

    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        delegate.onReceivedHttpAuthRequest(view, handler, host, realm)
    }

    override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
        return delegate.shouldOverrideKeyEvent(view, event)
    }

    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        delegate.onUnhandledKeyEvent(view, event)
    }

    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        delegate.onScaleChanged(view, oldScale, newScale)
    }

    override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
        delegate.onReceivedLoginRequest(view, realm, account, args)
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        return delegate.onRenderProcessGone(view, detail)
    }

    override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
        delegate.onSafeBrowsingHit(view, request, threatType, callback)
    }
}