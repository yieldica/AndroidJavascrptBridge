package com.github.yieldica.jsbridge.script

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import androidx.webkit.WebViewFeature
import com.github.yieldica.jsbridge.script.impl.CompatScriptManager
//import com.github.yieldica.jsbridge.script.impl.CompatScriptManager
import com.github.yieldica.jsbridge.script.impl.DocumentStartScriptManager

@SuppressLint("RequiresFeature", "RestrictedApi")
class UserScriptManager(view: WebView?) : ScriptManager {
    companion object {
        fun initializer(context: Context) {
            coreScript = context.resources.assets.open("core.js").bufferedReader().readText()
        }

        internal const val CORE_SCRIPT_ID = "web.bridge.core.script"
        internal lateinit var coreScript: String
        internal const val MESSAGE_NAME = "webkit_bridge"

    }

    private val supportedDocumentStartScript = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)

    private val scriptManager: ScriptManager = if (supportedDocumentStartScript) {
//        DocumentStartScriptManager(view)
        CompatScriptManager(view)
    } else {
        CompatScriptManager(view)
    }

    override var onMessageListener: OnMessageListener?
        get() = scriptManager.onMessageListener
        set(value) {
            scriptManager.onMessageListener = value
        }

    override fun inject(script: String, forKey: String) {
        scriptManager.inject(script, forKey)
    }

    override fun remove(forKey: String) {
        scriptManager.remove(forKey)
    }
}