package com.github.yieldica.jsbridge.model

import com.github.yieldica.jsbridge.ktx.JSON
import kotlinx.serialization.Serializable
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages pending async callback requests.
 *
 * Flow:
 * 1. Native calls evaluateJavascript with _callAsync(requestId, callbackId, args)
 * 2. JS executes the callback async, then posts result back via bridge
 * 3. Injector intercepts "__callAsync__" message and calls handleResponse
 * 4. handleResponse resumes the suspended coroutine with the result
 */
object CallAsyncDispatcher {

    @Serializable
    data class AsyncResponse(
        val requestId: String,
        val value: String? = null,
        val error: String? = null
    )

    private val pending = ConcurrentHashMap<String, Continuation<String?>>()

    fun register(requestId: String, continuation: Continuation<String?>) {
        pending[requestId] = continuation
    }

    fun cancel(requestId: String) {
        pending.remove(requestId)
    }

    fun handleResponse(argsJson: String) {
        try {
            // argsJson is Args1 format: {"arg0": "{\"requestId\":...,\"value\":...,\"error\":...}"}
            val args = JSON.decodeFromString<Map<String, String>>(argsJson)
            val responseJson = args["arg0"] ?: return
            val response = JSON.decodeFromString<AsyncResponse>(responseJson)

            val continuation = pending.remove(response.requestId) ?: return

            if (response.error != null) {
                continuation.resumeWithException(
                    Callback.CallAsyncException(response.error)
                )
            } else {
                continuation.resume(response.value)
            }
        } catch (e: Exception) {
            // Malformed response, ignore
        }
    }

    /**
     * JS snippet injected at document start to provide _callAsync on Android.
     * This bridges the gap between Android's evaluateJavascript (sync return only)
     * and iOS's callAsyncJavaScript (native async support).
     */
    const val INJECT_SCRIPT = """
        if (window.__bridge__ && window.__bridge__.CBDispatcher && !window.__bridge__.CBDispatcher._callAsync) {
            window.__bridge__.CBDispatcher._callAsync = function(r, e) {
                var args = Array.prototype.slice.call(arguments, 2);
                var fn = window.__bridge__.VP.get(e);
                Promise.resolve(fn.apply(this, args)).then(function(v) {
                    window.webkit_bridge.postMessage(JSON.stringify({
                        identifier: "__callAsync__",
                        args: JSON.stringify({arg0: JSON.stringify({requestId: r, value: JSON.stringify(v), error: null})})
                    }));
                }).catch(function(err) {
                    window.webkit_bridge.postMessage(JSON.stringify({
                        identifier: "__callAsync__",
                        args: JSON.stringify({arg0: JSON.stringify({requestId: r, value: null, error: (err && err.message) || String(err)})})
                    }));
                });
            };
        }
    """
}
