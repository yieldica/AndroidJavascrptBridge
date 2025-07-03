package com.github.yieldica.jsbridge

import android.webkit.WebView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.github.yieldica.jsbridge.ktx.JSON
import com.github.yieldica.jsbridge.model.Args1
import com.github.yieldica.jsbridge.model.Args2
import com.github.yieldica.jsbridge.model.Args3
import com.github.yieldica.jsbridge.model.Args4
import com.github.yieldica.jsbridge.model.BridgeInfo
import com.github.yieldica.jsbridge.model.Callback
import com.github.yieldica.jsbridge.model.PromiseResult
import com.github.yieldica.jsbridge.model.PromiseScript
import com.github.yieldica.jsbridge.model.SerializableError
import com.github.yieldica.jsbridge.model.SerializableThrowable
import com.github.yieldica.jsbridge.script.OnMessageListener
import com.github.yieldica.jsbridge.script.ScriptManager
import com.github.yieldica.jsbridge.script.UserScriptManager
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.lang.ref.WeakReference

class Injector(view: WebView) : OnMessageListener {

    val webView = WeakReference(view)
    private var pluginMap = mutableMapOf<String, AnyPlugin>()
    val userScriptManager: ScriptManager = UserScriptManager(view)

    init {
        userScriptManager.onMessageListener = this
    }

    val lifecycleScope: LifecycleCoroutineScope?
        get() = (webView.get()?.context as? LifecycleOwner)?.lifecycleScope


    override fun onPostMessage(message: String) {
        val bridgeInfo = JSON.decodeFromString<BridgeInfo>(message)
        if (bridgeInfo.identifier == null || bridgeInfo.args == null) {
            return
        }

        pluginMap[bridgeInfo.identifier]?.invoke(bridgeInfo.args)
    }

    private fun inject(script: String, key: String) {
        userScriptManager.inject(script, key)
    }

    fun injectRuntimeScript(script: String, key: String) {
        inject(script, key)
    }

    fun inject(path: String, plugin: () -> Unit) {
        val f: () -> Unit = {
            plugin()
        }
        injectNoArgs(path, f)
    }

    inline fun <reified P0> inject(path: String, crossinline plugin: (P0) -> Unit) {
        val f: (Args1<P0>) -> Unit = {
            plugin(it.arg0 as P0)
        }
        injectPlugin(path, f, serializer(), argsCount = 1)
    }

    inline fun <reified P0, reified P1> inject(path: String, crossinline plugin: (P0, P1) -> Unit) {
        val f: (Args2<P0, P1>) -> Unit = {
            plugin(it.arg0 as P0, it.arg1 as P1)
        }
        injectPlugin(path, f, serializer(), argsCount = 2)
    }

    inline fun <reified P0, reified P1, reified P2> inject(path: String, crossinline plugin: (P0, P1, P2) -> Unit) {
        val f: (Args3<P0, P1, P2>) -> Unit = {
            plugin(it.arg0 as P0, it.arg1 as P1, it.arg2 as P2)
        }
        injectPlugin(path, f, serializer(), argsCount = 3)
    }

    inline fun <reified P0, reified P1, reified P2, reified P3> inject(path: String, crossinline plugin: (P0, P1, P2, P3) -> Unit) {
        val f: (Args4<P0, P1, P2, P3>) -> Unit = {
            plugin(it.arg0 as P0, it.arg1 as P1, it.arg2 as P2, it.arg3 as P3)
        }
        injectPlugin(path, f, serializer(), argsCount = 4)
    }

    inline fun <reified R> injectPromise(path: String, crossinline plugin: suspend () -> R) {
        val f: (Args1<Callback>) -> Unit = {
            lifecycleScope?.launch {
                val callback = it.arg0!!
                try {
                    callbackPromise(callback, plugin())
                } catch (e: Exception) {
                    handleThrowable(e, callback)
                }
            }
        }

        injectPromisePlugin(path, f, serializer(), 0)
    }

    inline fun <reified P0, reified R> injectPromise(path: String, crossinline plugin: suspend (P0) -> R) {
        val f: (Args2<P0, Callback>) -> Unit = {
            lifecycleScope?.launch {
                val callback = it.arg1!!
                try {
                    callbackPromise(callback, plugin(it.arg0 as P0))
                } catch (e: Exception) {
                    handleThrowable(e, callback)
                }
            }
        }

        injectPromisePlugin(path, f, serializer(), 1)
    }

    inline fun <reified P0, reified P1, reified R> injectPromise(path: String, crossinline plugin: suspend (P0, P1) -> R) {
        val f: (Args3<P0, P1, Callback>) -> Unit = {
            lifecycleScope?.launch {
                val callback = it.arg2!!
                try {
                    callbackPromise(callback, plugin(it.arg0 as P0, it.arg1 as P1))
                } catch (e: Exception) {
                    handleThrowable(e, callback)
                }
            }
        }

        injectPromisePlugin(path, f, serializer(), 2)
    }

    inline fun <reified P0, reified P1, reified P2, reified R> injectPromise(path: String, crossinline plugin: suspend (P0, P1, P2) -> R) {
        val f: (Args4<P0, P1, P2, Callback>) -> Unit = {
            lifecycleScope?.launch {
                val callback = it.arg3!!
                try {
                    callbackPromise(
                        callback, plugin(
                            it.arg0 as P0,
                            it.arg1 as P1,
                            it.arg2 as P2,
                        )
                    )
                } catch (e: Exception) {
                    handleThrowable(e, callback)
                }
            }
        }

        injectPromisePlugin(path, f, serializer(), 3)
    }

    @PublishedApi
    internal fun handleThrowable(t: Throwable, callback: Callback) {
        if (t is SerializableThrowable) {
            callback.invokeScript("{ error: ${t.serialize(JSON)} }")
        } else {
            callback.invoke(PromiseResult<Unit>(error = SerializableError.error(t)))
        }
    }

    fun <Arg> injectPlugin(path: String, plugin: (Arg) -> Unit, argsSerializer: KSerializer<Arg>, argsCount: Int) {
        inject(path, plugin, argsSerializer, argsCount)
    }

    fun <Arg> injectPromisePlugin(path: String, plugin: (Arg) -> Unit, argsSerializer: KSerializer<Arg>, argsCount: Int) {
        inject(path, plugin, argsSerializer, argsCount, isPromise = true)
    }

    inline fun <reified R> callbackPromise(callback: Callback, value: R) {
        val script = (value as? PromiseScript)?.script
        if (script != null) {
            callback.invokeScript(script)
        } else {
            if (value === Unit) {
                callback.invokeScript("{ value : undefined }")
            } else {
                callback.invoke(PromiseResult(value))
            }
        }
    }

    private fun <Arg> inject(path: String, plugin: (Arg) -> Unit, argsSerializer: KSerializer<Arg>, argsCount: Int, isPromise: Boolean = false) {
        pluginMap[path] = Plugin(webView.get(), path, argsSerializer, plugin)
        inject(scriptForPlugin(path, argsCount, isPromise), path)
    }

    private fun injectNoArgs(path: String, plugin: () -> Unit) {
        pluginMap[path] = NoArgsPlugin(webView.get(), path, plugin)
        inject(scriptForPlugin(path, 0, false), path)
    }

    private fun scriptForPlugin(path: String, argsCount: Int, isPromise: Boolean = false): String {
        val array = path.split(".")
        val count = array.size - 1
        var pathTmp = "globalThis"
        var code = ""
        var index = 0
        while (index < count) {
            pathTmp += ".${array[index]}"
            code += objectDefineJavascriptCode(pathTmp)
            index += 1
        }
        code += if (isPromise) {
            promiseFunctionDefineCode(path, argsCount)
        } else {
            functionDefineCode(path, argsCount)
        }
        return code
    }

    private fun objectDefineJavascriptCode(path: String): String {
        return """
        if($path==null){ $path = {} }
        """
    }

    private fun functionDefineCode(path: String, argsCount: Int): String {
        if (argsCount == 0) {
            return "if(globalThis.$path==null) { globalThis.$path = function() { window.__bridge__.invoke('$path')} }"
        }

        val args = (0..<argsCount).joinToString(",") { "a${it}" }
        return """
        if(globalThis.$path==null) { globalThis.$path = function($args) { window.__bridge__.invoke('$path', $args)} }
        """
    }

    private fun promiseFunctionDefineCode(path: String, argsCount: Int): String {
        if (argsCount == 0) {
            return """
            if (globalThis.$path == null) {
                globalThis.$path = function() {
                    return new Promise((resolve, reject) => {
                        window.__bridge__.invoke('$path', (result) => {
                            if (result.error != null) {
                                reject(result.error);
                            } else {
                                resolve(result.value);
                            }
                        })
                    })
                }
            }
            """
        }

        val args = (0..<argsCount).joinToString(",") { "a${it}" }
        return """
        if (globalThis.$path == null) {
            globalThis.$path = function($args) {
                return new Promise((resolve, reject) => {
                    window.__bridge__.invoke('$path', $args, (result) => {
                        if (result.error != null) {
                            reject(result.error);
                        } else {
                            resolve(result.value);
                        }
                    })
                })
            }
        }
        """
    }

    inline fun <reified T> injectVariable(variableName: String, value: T) {
        injectScript(variableName, JSON.encodeToString(serializer(), value))
    }

    fun injectScript(variableName: String, json: String) {
        val js = StringBuilder()
        val names = variableName.split(".")
        val finalName = names.last()
        var prefixName: String? = null
        for (name in names) {
            val nodeName = (prefixName?.let { "$it." } ?: "") + name
            if (name != finalName) {
                js.append("if(globalThis.$nodeName == null){ globalThis.$nodeName = {} }\n")
            } else {
                js.append("globalThis.$nodeName = $json")
            }
            prefixName = nodeName
        }

        userScriptManager.inject(js.toString(), forKey = "__webkit.global.options.$variableName")
    }
}