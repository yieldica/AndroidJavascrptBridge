package com.github.yieldica.jsbridge

import android.util.Log
import android.webkit.WebView
import com.github.yieldica.jsbridge.ktx.JSON
import com.github.yieldica.jsbridge.ktx.postEvaluateJavascript
import com.github.yieldica.jsbridge.model.Callback
import com.github.yieldica.jsbridge.model.CallbackWithWebViewSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import java.lang.ref.WeakReference

interface AnyPlugin {
    fun invoke(argString: String) {}
}

abstract class BasePlugin(webView: WebView?, val path: String) : AnyPlugin {
    val webView: WeakReference<WebView> = WeakReference(webView)

    final override fun invoke(argString: String) {
        try {
            invokePlugin(argString)
        } catch (e: Exception) {
            val message = "Cannot invoke plugin($path) with args: $argString, error: ${e.message}"
            Log.e("🍎 [Plugin]", message, e)
            webView.get()?.postEvaluateJavascript("console.log(\"$message\")")
        }
    }

    abstract fun invokePlugin(argString: String)
}

class NoArgsPlugin(webView: WebView?, path: String, val f: () -> Unit) : BasePlugin(webView, path) {
    override fun invokePlugin(argString: String) {
        f()
    }
}

class Plugin<T>(webView: WebView?, path: String, private val argSerializer: KSerializer<T>, val f: (T) -> Unit) : BasePlugin(webView, path) {
    override fun invokePlugin(argString: String) {
        val json = Json(from = JSON) {
            serializersModule += SerializersModule {
                contextual(Callback::class, CallbackWithWebViewSerializer(webView.get()))
            }
        }
        val arg = json.decodeFromString(argSerializer, argString)
        f(arg)
    }
}