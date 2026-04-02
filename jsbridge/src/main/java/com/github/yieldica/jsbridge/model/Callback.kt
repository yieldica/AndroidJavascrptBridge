package com.github.yieldica.jsbridge.model

import android.webkit.WebView
import com.github.yieldica.jsbridge.ktx.JSON
import com.github.yieldica.jsbridge.ktx.postEvaluateJavascript
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import java.lang.ref.WeakReference


@Serializable
internal data class CallbackHolder(val id: String)

@Serializable(with = CallbackSerializer::class)
class Callback(val id: String) {
    @Transient
    var webView: WeakReference<WebView>? = null

    inline fun <reified T> invoke(vararg args: T) {
        invokeCallback(args.toList(), kotlinx.serialization.serializer())
    }

    inline fun <reified T> callAsFunction(vararg args: T) {
        invokeCallback(args.toList(), kotlinx.serialization.serializer())
    }

    fun <T> invokeCallback(args: List<T>, serializer: KSerializer<T>) {
        invokeCallbackImpl(args.map { JSON.encodeToString(serializer, it) })
    }

    private fun invokeCallbackImpl(args: List<String>) {
        webView?.get()?.let { webView ->
            try {
                val params = args.joinToString(", ")
                val source = if (params.isNotEmpty()) {
                    "window.__bridge__.CBDispatcher.invoke('$id', $params)"
                } else {
                    "window.__bridge__.CBDispatcher.invoke('$id')"
                }

                webView.postEvaluateJavascript(source)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun invokeScript(script: String) {
        webView?.get()?.let { webView ->
            val source = "window.__bridge__.CBDispatcher.invoke('$id', $script)"
            webView.postEvaluateJavascript(source)
        }
    }

    /**
     * Call the JS callback and await completion (fire-and-forget).
     * Mirrors iOS: Callback.call(_ args: Encodable...) async throws
     */
    suspend fun <T> call(args: List<T>, serializer: KSerializer<T>) {
        callAsyncInternal(args, serializer)
    }

    /**
     * Call the JS callback and await a typed return value.
     * Mirrors iOS: Callback.call<T: Decodable>(_ args: Encodable...) async throws -> T
     */
    suspend fun <T, R> call(args: List<T>, argSerializer: KSerializer<T>, resultSerializer: KSerializer<R>): R {
        val json = callAsyncInternal(args, argSerializer)
        if (json == null || json == "null" || json == "undefined") {
            throw CallAsyncException("JavaScript returned null/undefined")
        }
        return JSON.decodeFromString(resultSerializer, json)
    }

    /**
     * Uses _callAsync (injected by CallAsyncDispatcher) to execute the JS callback async.
     * JS resolves the Promise and posts result back via bridge → CallAsyncDispatcher.handleResponse.
     */
    private suspend fun <T> callAsyncInternal(args: List<T>, serializer: KSerializer<T>): String? {
        val wv = webView?.get() ?: throw CallAsyncException("WebView is not available")
        val requestId = java.util.UUID.randomUUID().toString()
        val params = args.joinToString(", ") { JSON.encodeToString(serializer, it) }
        val source = if (params.isNotEmpty()) {
            "window.__bridge__.CBDispatcher._callAsync('$requestId', '$id', $params)"
        } else {
            "window.__bridge__.CBDispatcher._callAsync('$requestId', '$id')"
        }

        return suspendCancellableCoroutine { cont ->
            CallAsyncDispatcher.register(requestId, cont)
            cont.invokeOnCancellation { CallAsyncDispatcher.cancel(requestId) }
            wv.postEvaluateJavascript(source)
        }
    }

    class CallAsyncException(message: String) : Exception(message)
}

internal class CallbackWithWebViewSerializer(
    webView: WebView?
) : KSerializer<Callback> {
    val webView = WeakReference<WebView>(webView)
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Callback") {
        element<String>("id")
    }

    override fun serialize(encoder: Encoder, value: Callback) {
    }

    override fun deserialize(decoder: Decoder): Callback {
        return decoder.decodeStructure(descriptor) {
            val id = decodeStringElement(descriptor, 0)
            val callback = Callback(id)
            callback.webView = webView
            callback
        }
    }
}

class CallbackSerializer : KSerializer<Callback> {
    private val delegate = CallbackHolder.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor


    override fun serialize(encoder: Encoder, value: Callback) {
        delegate.serialize(encoder, CallbackHolder(value.id))
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Callback {
        val callbackHolder = delegate.deserialize(decoder)
        return Callback(callbackHolder.id).apply {
            this.webView = (decoder.serializersModule.getContextual(Callback::class) as? CallbackWithWebViewSerializer)?.webView
        }
    }
}