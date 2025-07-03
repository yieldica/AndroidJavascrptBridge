package com.github.yieldica.jsbridge.script.impl

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ScriptHandler
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewCompat.WebMessageListener
import androidx.webkit.WebViewFeature
import com.github.yieldica.jsbridge.script.OnMessageListener
import com.github.yieldica.jsbridge.script.ScriptManager
import com.github.yieldica.jsbridge.script.UserScriptManager
import java.lang.ref.WeakReference

@SuppressLint("RequiresFeature")
class DocumentStartScriptManager(view: WebView?) : ScriptManager, WebMessageListener {
    private val webView = WeakReference(view)
    private val scriptMap = mutableMapOf<String, String>()
    private val scriptHandlers = mutableMapOf<String, ScriptHandler>()

    override var onMessageListener: OnMessageListener? = null

    init {
        view?.let {
            WebViewCompat.addWebMessageListener(it, UserScriptManager.MESSAGE_NAME, setOf("*"), this)
        }
        reinject()
    }

    override fun inject(script: String, forKey: String) {
        scriptHandlers[forKey]?.remove()
        scriptMap[forKey] = script
        addScript(script, forKey)
    }

    override fun remove(forKey: String) {
        scriptHandlers[forKey]?.remove()
        scriptHandlers.remove(forKey)
        scriptMap.remove(forKey)
    }

    @Suppress("SpellCheckingInspection")
    private fun reinject() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            return
        }
        scriptHandlers.values.forEach { it.remove() }
        scriptHandlers.clear()
        addScript(UserScriptManager.coreScript, UserScriptManager.CORE_SCRIPT_ID)
        scriptMap.forEach { addScript(it.value, it.key) }
    }

    @SuppressLint("RestrictedApi", "RequiresFeature")
    private fun addScript(script: String, forKey: String) {
        webView.get()?.let {
            val handler = WebViewCompat.addDocumentStartJavaScript(it, script, setOf("*"))
            scriptHandlers[forKey] = handler
        }
    }

    override fun onPostMessage(view: WebView, message: WebMessageCompat, sourceOrigin: Uri, isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy) {
        val messageData = message.data ?: return
        onMessageListener?.onPostMessage(messageData)
    }
}