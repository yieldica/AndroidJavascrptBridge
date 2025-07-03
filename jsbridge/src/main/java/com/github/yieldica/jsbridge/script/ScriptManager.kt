package com.github.yieldica.jsbridge.script

interface OnMessageListener {
    fun onPostMessage(message: String)
}

interface ScriptManager {
    var onMessageListener: OnMessageListener?
    fun inject(script: String, forKey: String)
    fun remove(forKey: String)
}