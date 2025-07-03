package com.github.yieldica.jsbridge.model

import android.webkit.WebView
import com.github.yieldica.jsbridge.ktx.JSON
import com.github.yieldica.jsbridge.ktx.postEvaluateJavascript
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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