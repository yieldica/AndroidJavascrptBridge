package com.github.yieldica.jsbridge

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import com.github.yieldica.jsbridge.model.Callback
import com.github.yieldica.jsbridge.model.PromiseScript
import com.github.yieldica.jsbridge.model.SerializableThrowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val webView = findViewById<WebView>(R.id.webview)
        val loader = WebViewAssetLoader.Builder().setDomain("android.webview_example.com").addPathHandler("/") {
            val html = """
           <!DOCTYPE html>
           <html>
             <head>
               <meta charset="utf-8">
               <title>最简单的网页</title>
             </head>
             <body>
               你好，世界！
             </body>
           </html>
                """.trimIndent()
            WebResourceResponse("text/html", "utf-8", html.byteInputStream())
        }.build()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                return loader.shouldInterceptRequest(request.url) ?: super.shouldInterceptRequest(view, request)
            }
        }
        webView.settings.apply {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_OFF)
            }
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        // simple test
        val testHello: () -> Unit = {
            println("hello")
        }
        webView.injector.inject("hello", testHello)

        val testOneArg: (String) -> Unit = {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        webView.injector.inject("testOneArg", testOneArg)

        val testTwoArg: (String, Int) -> Unit = { arg0, arg1 ->
            Toast.makeText(this, "$arg0, $arg1", Toast.LENGTH_SHORT).show()
        }
        webView.injector.inject("testTwoArg", testTwoArg)

        val testNullableArg: (String?) -> Unit = {
            Toast.makeText(this, it ?: "got null", Toast.LENGTH_SHORT).show()
        }
        webView.injector.inject("testNullableArg", testNullableArg)

        val testCallback: (String, Callback) -> Unit = { arg0, callback ->
            callback.invoke("arg0: $arg0")
        }
        webView.injector.inject("testCallback", testCallback)

        val testScriptCallback: (String, Callback) -> Unit = { arg0, callback ->
            callback.invokeScript("{arg0: '$arg0'}")
        }
        webView.injector.inject("testScriptCallback", testScriptCallback)

        // Promise
        val testPromise: () -> String = {
            "hello world"
        }
        webView.injector.injectPromise("testPromise", testPromise)

        val testSuspendPromise: suspend () -> String = {
            delay(2000)
            "hello world"
        }
        webView.injector.injectPromise("testSuspendPromise", testSuspendPromise)

        // void promise
        val testVoidPromise: suspend () -> Unit = {
            withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "hello world", Toast.LENGTH_SHORT).show() }
        }
        webView.injector.injectPromise("testVoidPromise", testVoidPromise)

        val testThrowPromise: suspend () -> Unit = {
            throw Exception("error")
        }
        webView.injector.injectPromise("testThrowPromise", testThrowPromise)

        val testSerializableThrowPromise: suspend () -> Unit = {
            throw TestSerializableError(123, "This is a SerializableError")
        }
        webView.injector.injectPromise("testSerializableThrowPromise", testSerializableThrowPromise)

        val testFunctionPromise: suspend () -> PromiseScript = {
            PromiseScript("function() { console.log('This is a Function result'); }")
        }
        webView.injector.injectPromise("testFunctionPromise", testFunctionPromise)

        val testSerializableArgs: (User, Callback) -> Unit = { user, callback ->
            user.nickname = user.nickname?.uppercase() ?: "Yieldica"
            user.age++
            user.name = user.name.uppercase()
            callback.invoke(user)
        }
        webView.injector.inject("testSerializableArgs", testSerializableArgs)

        load(webView)
    }

    private fun load(webView: WebView) {
        webView.loadUrl("https://android.webview_example.com/index.html")
    }
}

@Serializable
data class User(
    var name: String,
    var age: Int,
    var nickname: String? = null
)

@Serializable
data class TestSerializableError(val code: Int, override val message: String) : SerializableThrowable, Exception(message) {
    override fun serialize(json: Json): String {
        return json.encodeToString(serializer(), this)
    }
}