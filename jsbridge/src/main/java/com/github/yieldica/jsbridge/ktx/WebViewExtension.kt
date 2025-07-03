package com.github.yieldica.jsbridge.ktx

import android.webkit.WebView


fun WebView.postEvaluateJavascript(source: String) {
    if (Thread.currentThread() == this.webViewLooper.thread) {
        evaluateJavascript(source, null)
    } else {
        post { evaluateJavascript(source, null) }
    }
}

fun WebView.postEvaluateJavascript(source: String, callback: (String?) -> Unit) {
    if (Thread.currentThread() == this.webViewLooper.thread) {
        evaluateJavascript(source) {
            callback(it)
        }
    } else {
        post {
            evaluateJavascript(source) {
                callback(it)
            }
        }
    }
}